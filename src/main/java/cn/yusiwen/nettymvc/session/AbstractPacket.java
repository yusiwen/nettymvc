package cn.yusiwen.nettymvc.session;

import cn.yusiwen.nettymvc.core.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

/**
 * @author yusiwen
 */
public abstract class AbstractPacket {

    /**
     * Session
     */
    protected final Session session;

    /**
     * Message
     */
    protected Message message;
    /**
     * ByteBuf
     */
    protected ByteBuf byteBuf;

    private AbstractPacket(Session session, Message message, ByteBuf byteBuf) {
        this.session = session;
        this.message = message;
        this.byteBuf = byteBuf;
    }

    public static AbstractPacket of(Session session, Message message) {
        if (session.isUdp()) {
            return new UDP(session, message, null);
        }
        return new TCP(session, message, null);
    }

    public static AbstractPacket of(Session session, ByteBuf message) {
        if (session.isUdp()) {
            return new UDP(session, null, message);
        }
        return new TCP(session, null, message);
    }

    public AbstractPacket replace(Message message) {
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

    public Message getMessage() {
        return message;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    private static class TCP extends AbstractPacket {
        private TCP(Session session, Message message, ByteBuf byteBuf) {
            super(session, message, byteBuf);
        }

        @Override
        public Object wrap(ByteBuf byteBuf) {
            return byteBuf;
        }
    }

    private static class UDP extends AbstractPacket {
        private UDP(Session session, Message message, ByteBuf byteBuf) {
            super(session, message, byteBuf);
        }

        @Override
        public Object wrap(ByteBuf byteBuf) {
            return new DatagramPacket(byteBuf, session.remoteAddress());
        }
    }
}