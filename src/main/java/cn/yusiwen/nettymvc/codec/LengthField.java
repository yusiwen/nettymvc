package cn.yusiwen.nettymvc.codec;

import static io.netty.util.internal.ObjectUtil.checkPositive;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author yusiwen
 */
public class LengthField {
    /**
     * Prefix
     */
    private final byte[] prefix;
    /**
     * Max frame length
     */
    private final int maxFrameLength;
    /**
     * Offset
     */
    private final int offset;
    /**
     * Length
     */
    private final int length;
    /**
     * Offset of end
     */
    private final int endOffset;
    /**
     * Length adjustment
     */
    private final int lengthAdjustment;
    /**
     * Initial bytes to strip
     */
    private final int initialBytesToStrip;

    public LengthField(byte[] prefix, int maxFrameLength, int offset, int length) {
        this(prefix, maxFrameLength, offset, length, 0, 0);
    }

    public LengthField(byte[] prefix, int maxFrameLength, int offset, int length, int lengthAdjustment,
            int initialBytesToStrip) {
        checkPositive(maxFrameLength, "maxFrameLength_LengthField");
        checkPositiveOrZero(offset, "lengthFieldOffset");
        checkPositiveOrZero(initialBytesToStrip, "initialBytesToStrip");
        if (offset > maxFrameLength - length) {
            throw new IllegalArgumentException("maxFrameLength_LengthField (" + maxFrameLength
                    + ") must be equal to or greater than lengthFieldOffset (" + offset + ") + lengthFieldLength ("
                    + length + ").");
        } else {
            this.prefix = prefix;
            this.maxFrameLength = maxFrameLength;
            this.offset = offset;
            this.length = length;
            this.lengthAdjustment = lengthAdjustment;
            this.endOffset = offset + length;
            this.initialBytesToStrip = initialBytesToStrip;
        }
    }

    public byte[] getPrefix() {
        return prefix;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLengthAdjustment() {
        return lengthAdjustment;
    }

    public int getInitialBytesToStrip() {
        return initialBytesToStrip;
    }
}