package cn.yusiwen.nettymvc.codec;

/**
 * @author yusiwen
 */
public class Delimiter {
    /**
     * Value
     */
    private final byte[] value;
    /**
     * Strip or not
     */
    private final boolean strip;

    public Delimiter(byte[] value) {
        this(value, true);
    }

    public Delimiter(byte[] value, boolean strip) {
        this.value = value;
        this.strip = strip;
    }

    public byte[] getValue() {
        return value;
    }

    public boolean isStrip() {
        return strip;
    }
}