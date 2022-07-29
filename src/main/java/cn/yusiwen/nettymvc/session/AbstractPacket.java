package cn.yusiwen.nettymvc.session;

import cn.yusiwen.nettymvc.core.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

/**
 * @param <T>
 *            Message
 * @author yusiwen
 */
public abstract class AbstractPacket<T extends Message> {

    /**
     * Session
     */
    protected final Session session;

    /**
     * Message
     */
    protected T message;
    /**
     * ByteBuf
     */
    protected ByteBuf byteBuf;

    private AbstractPacket(Session session, T message, ByteBuf byteBuf) {
        this.session = session;
        this.message = message;
        this.byteBuf = byteBuf;
    }

    public static <T extends Message> AbstractPacket<T> of(Session session, T message) {
        if (session.isUdp()) {
            return new UDP<>(session, message, null);
        }
        return new TCP<>(session, message, null);
    }

    public static <T extends Message> AbstractPacket<T> of(Session session, ByteBuf message) {
        if (session.isUdp()) {
            return new UDP<>(session, null, message);
        }
        return new TCP<>(session, null, message);
    }

    public AbstractPacket<T> replace(T message) {
        this.message = message;
        return this;
    }

    public ByteBuf take() {
        ByteBuf temp = this.byteBuf;
        this.byteBuf = null;
        return temp;
    }

    public abstract Object wrap(ByteBuf byteBuf);

    public Session getSession() {
        return session;
    }

    public T getMessage() {
        return message;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    private static class TCP<T extends Message> extends AbstractPacket<T> {
        private TCP(Session session, T message, ByteBuf byteBuf) {
            super(session, message, byteBuf);
        }

        @Override
        public Object wrap(ByteBuf byteBuf) {
            return byteBuf;
        }
    }

    private static class UDP<T extends Message> extends AbstractPacket<T> {
        private UDP(Session session, T message, ByteBuf byteBuf) {
            super(session, message, byteBuf);
        }

        @Override
        public Object wrap(ByteBuf byteBuf) {
            return new DatagramPacket(byteBuf, session.remoteAddress());
        }
    }
}