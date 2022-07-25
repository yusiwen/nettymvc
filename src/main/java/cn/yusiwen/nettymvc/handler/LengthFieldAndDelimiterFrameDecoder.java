package cn.yusiwen.nettymvc.handler;

import java.util.List;

import cn.yusiwen.nettymvc.codec.Delimiter;
import cn.yusiwen.nettymvc.codec.LengthField;
import cn.yusiwen.nettymvc.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.ObjectUtil;

/**
 * @see io.netty.handler.codec.LengthFieldBasedFrameDecoder
 */
public class LengthFieldAndDelimiterFrameDecoder extends DelimiterBasedFrameDecoder {
    /**
     * Prefix
     */
    protected final byte[] prefix;
    /**
     * Max frame length
     */
    private final int maxFrameLength;
    /**
     * Length field offset
     */
    private final int lengthFieldOffset;
    /**
     * Length field length
     */
    private final int lengthFieldLength;
    /**
     * Length field end offset
     */
    private final int lengthFieldEndOffset;
    /**
     * Length adjustment
     */
    private final int lengthAdjustment;
    /**
     * Initial bytes to strip
     */
    private final int initialBytesToStrip;
    /**
     * Fail-fast or not
     */
    private final boolean failFast;
    /**
     * Drop too long frame or not
     */
    private boolean discardingTooLongFrame;
    /**
     * Frame length to drop
     */
    private int tooLongFrameLength;
    /**
     * Bytes to discard
     */
    private int bytesToDiscard;

    public LengthFieldAndDelimiterFrameDecoder(int maxFrameLength, LengthField lengthField, Delimiter... delimiters) {
        this(maxFrameLength, true, lengthField, delimiters);
    }

    public LengthFieldAndDelimiterFrameDecoder(int maxFrameLength, boolean failFast, LengthField lengthField,
            Delimiter... delimiters) {
        super(maxFrameLength, failFast, delimiters);
        ObjectUtil.checkPositive(maxFrameLength, "delimiterMaxFrameLength");
        ObjectUtil.checkNonEmpty(delimiters, "delimiters");

        this.prefix = lengthField.getPrefix();
        this.maxFrameLength = lengthField.getMaxFrameLength();
        this.lengthFieldOffset = lengthField.getOffset();
        this.lengthFieldLength = lengthField.getLength();
        this.lengthFieldEndOffset = lengthField.getEndOffset();
        this.lengthAdjustment = lengthField.getLengthAdjustment();
        this.initialBytesToStrip = lengthField.getInitialBytesToStrip();
        this.failFast = failFast;
    }

    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (discardingTooLongFrame) {
            discardingTooLongFrame(in);
        }

        Object decoded;
        if (ByteBufUtils.startsWith(in, prefix)) {
            decoded = this.decode(ctx, in);
        } else {
            decoded = super.decode(ctx, in);
        }
        if (decoded != null) {
            out.add(decoded);
        }

    }

    private void discardingTooLongFrame(ByteBuf in) {
        int discardBytes = this.bytesToDiscard;
        int localBytesToDiscard = Math.min(discardBytes, in.readableBytes());
        in.skipBytes(localBytesToDiscard);
        discardBytes -= localBytesToDiscard;
        this.bytesToDiscard = discardBytes;
        this.failIfNecessary(false);
    }

    private static void failOnNegativeLengthField(ByteBuf in, int frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
    }

    private static void failOnFrameLengthLessThanLengthFieldEndOffset(ByteBuf in, int frameLength,
            int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("Adjusted frame length (" + frameLength
                + ") is less than lengthFieldEndOffset: " + lengthFieldEndOffset);
    }

    private void exceededFrameLength(ByteBuf in, int frameLength) {
        int discard = frameLength - in.readableBytes();
        this.tooLongFrameLength = frameLength;
        if (discard < 0) {
            in.skipBytes(frameLength);
        } else {
            this.discardingTooLongFrame = true;
            this.bytesToDiscard = discard;
            in.skipBytes(in.readableBytes());
        }

        this.failIfNecessary(true);
    }

    private static void failOnFrameLengthLessThanInitialBytesToStrip(ByteBuf in, int frameLength,
            int initialBytesToStrip) {
        in.skipBytes(frameLength);
        throw new CorruptedFrameException(
                "Adjusted frame length (" + frameLength + ") is less than initialBytesToStrip: " + initialBytesToStrip);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() < this.lengthFieldEndOffset) {
            return null;
        } else {
            int actualLengthFieldOffset = in.readerIndex() + this.lengthFieldOffset;
            int frameLength = this.getUnadjustedFrameLength(in, actualLengthFieldOffset, this.lengthFieldLength);
            if (frameLength < 0) {
                failOnNegativeLengthField(in, frameLength, this.lengthFieldEndOffset);
            }

            frameLength += this.lengthAdjustment + this.lengthFieldEndOffset;
            if (frameLength < this.lengthFieldEndOffset) {
                failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, this.lengthFieldEndOffset);
            }

            if (frameLength > this.maxFrameLength) {
                this.exceededFrameLength(in, frameLength);
                return null;
            } else {
                if (in.readableBytes() < frameLength) {
                    return null;
                } else {
                    if (this.initialBytesToStrip > frameLength) {
                        failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
                    }

                    in.skipBytes(this.initialBytesToStrip);
                    int readerIndex = in.readerIndex();
                    int actualFrameLength = frameLength - this.initialBytesToStrip;
                    ByteBuf frame = in.retainedSlice(readerIndex, actualFrameLength);
                    in.readerIndex(readerIndex + actualFrameLength);
                    return frame;
                }
            }
        }
    }

    protected int getUnadjustedFrameLength(ByteBuf buf, int offset, int length) {
        int frameLength;
        switch (length) {
            case 2:
                frameLength = buf.getUnsignedShort(offset);
                break;
            case 3:
                frameLength = buf.getUnsignedMedium(offset);
                break;
            case 4:
                frameLength = buf.getInt(offset);
                break;
            default:
                throw new DecoderException(
                        "unsupported lengthFieldLength: " + lengthFieldLength + " (expected:  2, 3, 4)");
        }
        return frameLength;
    }

    private void failIfNecessary(boolean firstDetectionOfTooLongFrame) {
        if (this.bytesToDiscard == 0) {
            int dropFrameLength = this.tooLongFrameLength;
            this.tooLongFrameLength = 0;
            this.discardingTooLongFrame = false;
            if (!this.failFast || firstDetectionOfTooLongFrame) {
                this.fail(dropFrameLength);
            }
        } else if (this.failFast && firstDetectionOfTooLongFrame) {
            this.fail(this.tooLongFrameLength);
        }
    }

    private void fail(long frameLength) {
        if (frameLength > 0) {
            throw new TooLongFrameException(
                    "Adjusted frame length exceeds " + this.maxFrameLength + ": " + frameLength + " - discarded");
        } else {
            throw new TooLongFrameException("Adjusted frame length exceeds " + this.maxFrameLength + " - discarding");
        }
    }
}
