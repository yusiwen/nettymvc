package cn.yusiwen.nettymvc;

import java.nio.charset.StandardCharsets;

import cn.yusiwen.nettymvc.codec.MyMessageDecoder;
import cn.yusiwen.nettymvc.codec.MyMessageEncoder;
import cn.yusiwen.nettymvc.core.DefaultHandlerMapping;
import cn.yusiwen.nettymvc.endpoint.MyHandlerInterceptor;
import cn.yusiwen.nettymvc.session.SessionManager;

/**
 * @author yusiwen
 */
public class QuickStart {

    public static final int port = 7611;

    public static void main(String[] args) {
        AbstractServer udpServer = new NettyConfig.Builder().setPort(port)
                // .setThreadGroup(0, 1)
                .setDelimiters(new byte[][] {"|".getBytes(StandardCharsets.UTF_8)}).setDecoder(new MyMessageDecoder())
                .setEncoder(new MyMessageEncoder())
                .setHandlerMapping(new DefaultHandlerMapping("io.github.yezhihao.netmc.endpoint"))
                .setHandlerInterceptor(new MyHandlerInterceptor()).setSessionManager(new SessionManager())
                .setEnableUDP(true).setEnableStopwatch(true).build();
        udpServer.start();

        AbstractServer tcpServer = new NettyConfig.Builder().setPort(port).setMaxFrameLength(2048)
                .setDelimiters(new byte[][] {"|".getBytes(StandardCharsets.UTF_8)}).setDecoder(new MyMessageDecoder())
                .setEncoder(new MyMessageEncoder())
                .setHandlerMapping(new DefaultHandlerMapping("io.github.yezhihao.netmc.endpoint"))
                .setHandlerInterceptor(new MyHandlerInterceptor()).setSessionManager(new SessionManager())
                .setEnableStopwatch(true).build();
        tcpServer.start();
    }
}
