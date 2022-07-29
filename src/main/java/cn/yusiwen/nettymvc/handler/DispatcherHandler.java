package cn.yusiwen.nettymvc.handler;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.core.HandlerInterceptor;
import cn.yusiwen.nettymvc.core.HandlerMapping;
import cn.yusiwen.nettymvc.core.handler.AbstractHandler;
import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.AbstractPacket;
import cn.yusiwen.nettymvc.session.Session;
import cn.yusiwen.nettymvc.util.StopWatch;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @param <T>
 *            Message
 * @author yusiwen
 */
@ChannelHandler.Sharable
public class DispatcherHandler<T extends Message> extends ChannelInboundHandlerAdapter {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DispatcherHandler.class);

    /**
     * Stopwatch
     */
    private StopWatch s;

    /**
     * HandlerMapping
     */
    private final HandlerMapping handlerMapping;

    /**
     * HandlerInterceptor
     */
    private final HandlerInterceptor<T> interceptor;

    /**
     * ExecutorService
     */
    private final ExecutorService executor;

    /**
     * stop watch flag
     */
    private boolean enableStopwatch;

    public DispatcherHandler(HandlerMapping handlerMapping, HandlerInterceptor<T> interceptor, ExecutorService executor,
            boolean enableStopwatch) {
        this.handlerMapping = handlerMapping;
        this.interceptor = interceptor;
        this.executor = executor;
        this.enableStopwatch = enableStopwatch;

        if (isEnableStopwatch()) {
            s = new StopWatch().start();
        }
    }

    public boolean isEnableStopwatch() {
        return enableStopwatch;
    }

    public void setEnableStopwatch(boolean flag) {
        this.enableStopwatch = flag;
    }

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (isEnableStopwatch()) {
            s.increment();
        }

        AbstractPacket<T> packet = (AbstractPacket<T>) msg;
        T request = packet.getMessage();
        AbstractHandler handler = handlerMapping.getHandler(request.getMessageId());

        if (handler == null) {
            T response = interceptor.notSupported(request, packet.getSession());
            if (response != null) {
                ctx.writeAndFlush(packet.replace(response));
            }
        } else {
            if (handler.isAsync()) {
                executor.execute(() -> channelRead0(ctx, packet, handler));
            } else {
                channelRead0(ctx, packet, handler);
            }
        }
    }

    private void channelRead0(ChannelHandlerContext ctx, AbstractPacket<T> packet, AbstractHandler handler) {
        Session session = packet.getSession();
        T request = packet.getMessage();
        T response;
        long time = System.currentTimeMillis();

        try {
            if (!interceptor.beforeHandle(request, session)) {
                return;
            }

            response = handler.invoke(request, session);
            if (handler.isReturnVoid()) {
                response = interceptor.successful(request, session);
            } else {
                interceptor.afterHandle(request, response, session);
            }
        } catch (Exception e) {
            LOG.warn(String.valueOf(request), e);
            response = interceptor.exceptional(request, session, e);
        }
        time = System.currentTimeMillis() - time;
        if (time > 100) {
            LOG.info("====={},慢处理耗时{}ms", handler, time);
        }
        if (response != null) {
            ctx.writeAndFlush(packet.replace(response));
        }
    }
}
