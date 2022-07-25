package cn.yusiwen.nettymvc.handler;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.codec.Delimiter;
import cn.yusiwen.nettymvc.session.AbstractPacket;
import cn.yusiwen.nettymvc.session.Session;
import cn.yusiwen.nettymvc.session.SessionManager;
import cn.yusiwen.nettymvc.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;

/**
 * UDP消息适配器
 *
 * @author yusiwen
 */
@ChannelHandler.Sharable
public class UDPMessageAdapter extends ChannelInboundHandlerAdapter {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UDPMessageAdapter.class);

    /**
     * SessionManager
     */
    private final SessionManager sessionManager;

    /**
     * Reader idel time
     */
    private final long readerIdleTime;

    /**
     * Session map
     */
    private final Map<Object, Session> sessionMap = new ConcurrentHashMap<>();

    private UDPMessageAdapter(SessionManager sessionManager, int readerIdleTime) {
        this.sessionManager = sessionManager;
        this.readerIdleTime = TimeUnit.SECONDS.toMillis(readerIdleTime);
    }

    public static UDPMessageAdapter newInstance(SessionManager sessionManager, int readerIdleTime,
            Delimiter[] delimiters) {
        if (delimiters == null) {
            return new UDPMessageAdapter(sessionManager, readerIdleTime);
        }
        return new DelimiterBasedFrameImpl(sessionManager, readerIdleTime, delimiters);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        Session session = getSession(ctx, packet.sender());
        session.access();
        ctx.fireChannelRead(AbstractPacket.of(session, buf));
    }

    protected Session getSession(ChannelHandlerContext ctx, InetSocketAddress sender) {
        Session session = sessionMap.get(sender);
        if (session == null) {
            session = sessionManager.newInstance(ctx.channel(), sender, s -> sessionMap.remove(sender, s));
            sessionMap.put(sender, session);
            LOG.info("<<<<<终端连接{}", session);
        }
        return session;
    }

    @Override
    @SuppressWarnings("PMD.AvoidManuallyCreateThreadRule")
    public void channelActive(ChannelHandlerContext ctx) {
        Thread thread = new Thread(() -> {
            for (;;) {
                long nextDelay = readerIdleTime;
                long now = System.currentTimeMillis();

                for (Session session : sessionMap.values()) {
                    long time = readerIdleTime - (now - session.getLastAccessedTime());

                    if (time <= 0) {
                        LOG.warn(">>>>>终端心跳超时 {}", session);
                        session.invalidate();
                    } else {
                        nextDelay = Math.min(time, nextDelay);
                    }
                }
                try {
                    Thread.sleep(nextDelay);
                } catch (Throwable e) {
                    LOG.warn("IdleStateScheduler", e);
                }
            }
        });
        thread.setName(Thread.currentThread().getName() + "-c");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    private static class DelimiterBasedFrameImpl extends UDPMessageAdapter {

        /**
         * Delimiters
         */
        private final Delimiter[] delimiters;

        private DelimiterBasedFrameImpl(SessionManager sessionManager, int readerIdleTime, Delimiter[] delimiters) {
            super(sessionManager, readerIdleTime);
            this.delimiters = delimiters;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            DatagramPacket packet = (DatagramPacket) msg;
            ByteBuf buf = packet.content();
            Session session = getSession(ctx, packet.sender());

            try {
                List<ByteBuf> out = decode(buf);
                for (ByteBuf t : out) {
                    ctx.fireChannelRead(AbstractPacket.of(session, t));
                }
            } catch (DecoderException e) {
                throw e;
            } catch (Exception e) {
                throw new DecoderException(e);
            } finally {
                buf.release();
            }
        }

        protected List<ByteBuf> decode(ByteBuf in) {
            List<ByteBuf> out = new LinkedList<>();
            while (in.isReadable()) {

                for (Delimiter delim : delimiters) {
                    int minDelimLength = delim.getValue().length;

                    int frameLength = ByteBufUtils.indexOf(in, delim.getValue());
                    if (frameLength >= 0) {

                        if (delim.isStrip()) {
                            if (frameLength != 0) {
                                out.add(in.readRetainedSlice(frameLength));
                            }
                            in.skipBytes(minDelimLength);
                        } else {
                            if (frameLength != 0) {
                                out.add(in.readRetainedSlice(frameLength + minDelimLength));
                            } else {
                                in.skipBytes(minDelimLength);
                            }
                        }
                    } else {
                        int i = in.readableBytes();
                        if (i > 0) {
                            out.add(in.readRetainedSlice(i));
                        }
                    }
                }
            }
            return out;
        }
    }
}
