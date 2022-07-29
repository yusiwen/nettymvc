package cn.yusiwen.nettymvc.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yusiwen
 */
public class StopWatch {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StopWatch.class);

    /**
     * Count
     */
    private final AtomicInteger count = new AtomicInteger();
    /**
     * Thread
     */
    private final Thread thread;

    @SuppressWarnings("PMD.AvoidManuallyCreateThreadRule")
    public StopWatch() {
        thread = new Thread(() -> {
            long start;
            while (true) {
                if (count.get() > 0) {
                    start = System.currentTimeMillis();
                    break;
                }
                try {
                    Thread.sleep(1L);
                } catch (Exception ignored) {
                }
            }
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (Exception ignored) {
                }
                int num = count.get();
                long time = (System.currentTimeMillis() - start) / 1000;
                LOG.info(time + "\t" + num + "\t" + num / time);
            }
        });
        thread.setName(Thread.currentThread().getName() + "-c");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
    }

    public StopWatch start() {
        this.thread.start();
        return this;
    }

    public int increment() {
        return count.incrementAndGet();
    }

}
