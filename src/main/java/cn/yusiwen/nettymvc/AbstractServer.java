package cn.yusiwen.nettymvc;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

/**
 * @author yusiwen
 */
public abstract class AbstractServer {

    /**
     * Logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractServer.class);
    /**
     * Running or not
     */
    protected boolean isRunning;
    /**
     * Netty config
     */
    protected final NettyConfig config;
    /**
     * Boss group
     */
    protected EventLoopGroup bossGroup;
    /**
     * Worker group
     */
    protected EventLoopGroup workerGroup;
    /**
     * Business group
     */
    protected ExecutorService businessGroup;

    protected AbstractServer(NettyConfig config) {
        this.config = config;
    }

    protected abstract AbstractBootstrap initialize();

    public synchronized boolean start() {
        if (isRunning) {
            LOG.info("==={}已经启动,port:{}===", config.name, config.port);
            return isRunning;
        }

        AbstractBootstrap bootstrap = initialize();
        ChannelFuture future = bootstrap.bind(config.port).awaitUninterruptibly();
        future.channel().closeFuture().addListener(f -> {
            if (isRunning) {
                stop();
            }
        });
        if (future.cause() != null) {
            LOG.error("启动失败", future.cause());
        }

        if (isRunning == future.isSuccess()) {
            LOG.info("==={}启动成功,port:{}===", config.name, config.port);
        }
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
        bossGroup.shutdownGracefully();
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (businessGroup != null) {
            businessGroup.shutdown();
        }
        LOG.info("==={}已经停止,port:{}===", config.name, config.port);
    }
}
