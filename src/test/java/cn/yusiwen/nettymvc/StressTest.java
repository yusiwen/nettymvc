package cn.yusiwen.nettymvc;

import java.nio.charset.StandardCharsets;

import cn.yusiwen.nettymvc.util.Client;
import cn.yusiwen.nettymvc.util.StopWatch;

/**
 * 压力测试<br>
 * 模拟1200台设备，每100毫秒发送一次报文
 *
 * @author yusiwen
 */
public class StressTest {

    private static final byte[] bytes = "|123,1,123;testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest|"
            .getBytes(StandardCharsets.UTF_8);
    private static final StopWatch STOPWATCH = new StopWatch().start();

    public static final String host = "127.0.0.1";
    public static final int port = QuickStart.port;

    private static final int size = 1200;
    private static final long Interval = 100;

    public static void main(String[] args) throws Exception {
        Client[] udps = null;
        Client[] tcps = null;
        udps = Client.clientUdp(host, port, size);
        tcps = Client.clientTcp(host, port, size);

        while (true) {
            for (int i = 0; i < size; i++) {
                if (udps != null) {
                    udps[i].send(bytes);
                    STOPWATCH.increment();
                }
                if (tcps != null) {
                    tcps[i].send(bytes);
                    STOPWATCH.increment();
                }
            }
            Thread.sleep(Interval);
        }
    }
}
