package cn.yusiwen.nettymvc;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.yusiwen.nettymvc.handler.DelimiterBasedFrameDecoder;
import cn.yusiwen.nettymvc.handler.DispatcherHandler;
import cn.yusiwen.nettymvc.handler.LengthFieldAndDelimiterFrameDecoder;
import cn.yusiwen.nettymvc.handler.MessageDecoderWrapper;
import cn.yusiwen.nettymvc.handler.MessageEncoderWrapper;
import cn.yusiwen.nettymvc.handler.TCPMessageAdapter;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author yusiwen
 */
public class TCPServer extends AbstractServer {

    protected TCPServer(NettyConfig config) {
        super(config);
    }

    protected AbstractBootstrap initialize() {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        workerGroup = new NioEventLoopGroup(config.workerCore,
                new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        if (config.businessCore > 0) {
            businessGroup = new ThreadPoolExecutor(config.businessCore, config.businessCore, 1L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    new DefaultThreadFactory(config.name + "-B", true, Thread.NORM_PRIORITY));
        }
        return new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .option(NioChannelOption.SO_REUSEADDR, true).option(NioChannelOption.SO_BACKLOG, 1024)
                .childOption(NioChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {

                    private final TCPMessageAdapter adapter = new TCPMessageAdapter(config.sessionManager);
                    private final MessageDecoderWrapper decoder = new MessageDecoderWrapper(config.decoder);
                    private final MessageEncoderWrapper encoder = new MessageEncoderWrapper(config.encoder);
                    private final DispatcherHandler dispatcher = new DispatcherHandler(config.handlerMapping,
                            config.handlerInterceptor, businessGroup, config.enableStopwatch);

                    @Override
                    public void initChannel(NioSocketChannel channel) {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(config.readerIdleTime, config.writerIdleTime,
                                        config.allIdleTime))
                                .addLast("frameDecoder", frameDecoder()).addLast("adapter", adapter)
                                .addLast("decoder", decoder).addLast("encoder", encoder)
                                .addLast("dispatcher", dispatcher);
                    }
                });
    }

    private ByteToMessageDecoder frameDecoder() {
        if (config.lengthField == null) {
            return new DelimiterBasedFrameDecoder(config.maxFrameLength, config.delimiters);
        }
        return new LengthFieldAndDelimiterFrameDecoder(config.maxFrameLength, config.lengthField, config.delimiters);
    }
}
