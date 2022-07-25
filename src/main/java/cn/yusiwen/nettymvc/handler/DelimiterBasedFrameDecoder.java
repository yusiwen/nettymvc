package cn.yusiwen.nettymvc.handler;

import static io.netty.util.internal.ObjectUtil.checkPositive;

import java.util.List;

import cn.yusiwen.nettymvc.codec.Delimiter;
import cn.yusiwen.nettymvc.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.ObjectUtil;

/**
 * @author yusiwen
 */
public class DelimiterBasedFrameDecoder extends ByteToMessageDecoder {

    /**
     * Delimiters
     */
    private final Delimiter[] delimiters;
    /**
     * Max frame length
     */
    private final int maxFrameLength;
    /**
     * Fail-fast or not
     */
    private final boolean failFast;
    /**
     * Discarding long frame or not
     */
    private boolean discardingTooLongFrame;
    /**
     * Frame length to drop
     */
    private int tooLongFrameLength;

    public DelimiterBasedFrameDecoder(int maxFrameLength, Delimiter... delimiters) {
        this(maxFrameLength, true, delimiters);
    }

    public DelimiterBasedFrameDecoder(int maxFrameLength, boolean failFast, Delimiter... delimiters) {
        validateMaxFrameLength(maxFrameLength);
        ObjectUtil.checkNonEmpty(delimiters, "delimiters");

        this.delimiters = delimiters;
        this.maxFrameLength = maxFrameLength;
        this.failFast = failFast;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) {
        // Try all delimiters and choose the delimiter which yields the shortest frame.
        int minFrameLength = Integer.MAX_VALUE;
        Delimiter minDelim = null;
        for (Delimiter delim : delimiters) {
            int frameLength = ByteBufUtils.indexOf(buffer, delim.getValue());
            if (frameLength >= 0 && frameLength < minFrameLength) {
                minFrameLength = frameLength;
                minDelim = delim;
            }
        }

        if (minDelim != null) {
            int minDelimLength = minDelim.getValue().length;
            ByteBuf frame = null;

            if (discardingTooLongFrame) {
                // We've just finished discarding a very large frame.
                // Go back to the initial state.
                discardingTooLongFrame = false;
                buffer.skipBytes(minFrameLength + minDelimLength);

                int dropLength = this.tooLongFrameLength;
                this.tooLongFrameLength = 0;
                if (!failFast) {
                    fail(dropLength);
                }
                return null;
            }

            if (minFrameLength > maxFrameLength) {
                // Discard read frame.
                buffer.skipBytes(minFrameLength + minDelimLength);
                fail(minFrameLength);
                return null;
            }

            if (minDelim.isStrip()) {
                // 忽略长度等于0的报文
                if (minFrameLength != 0) {
                    frame = buffer.readRetainedSlice(minFrameLength);
                }
                buffer.skipBytes(minDelimLength);
            } else {
                if (minFrameLength != 0) {
                    frame = buffer.readRetainedSlice(minFrameLength + minDelimLength);
                } else {
                    buffer.skipBytes(minDelimLength);
                }
            }

            return frame;
        } else {
            if (!discardingTooLongFrame) {
                if (buffer.readableBytes() > maxFrameLength) {
                    // Discard the content of the buffer until a delimiter is found.
                    tooLongFrameLength = buffer.readableBytes();
                    buffer.skipBytes(buffer.readableBytes());
                    discardingTooLongFrame = true;
                    if (failFast) {
                        fail(tooLongFrameLength);
                    }
                }
            } else {
                // Still discarding the buffer since a delimiter is not found.
                tooLongFrameLength += buffer.readableBytes();
                buffer.skipBytes(buffer.readableBytes());
            }
            return null;
        }
    }

    private void fail(long frameLength) {
        if (frameLength > 0) {
            throw new TooLongFrameException(
                    "frame length exceeds " + maxFrameLength + ": " + frameLength + " - discarded");
        } else {
            throw new TooLongFrameException("frame length exceeds " + maxFrameLength + " - discarding");
        }
    }

    private static void validateMaxFrameLength(int maxFrameLength) {
        checkPositive(maxFrameLength, "maxFrameLength");
    }
}
