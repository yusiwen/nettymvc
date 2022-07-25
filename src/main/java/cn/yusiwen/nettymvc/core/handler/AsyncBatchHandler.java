package cn.yusiwen.nettymvc.core.handler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.Session;
import cn.yusiwen.nettymvc.util.VirtualList;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * 异步批量处理
 *
 * @author yusiwen
 */
public class AsyncBatchHandler extends AbstractHandler {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AsyncBatchHandler.class);

    /**
     * Queue
     */
    private final ConcurrentLinkedQueue<Message> queue;

    /**
     * ThreadPoolExecutor
     */
    private final ThreadPoolExecutor executor;

    /**
     * Pool size
     */
    private final int poolSize;

    /**
     * Max elements
     */
    private final int maxElements;

    /**
     * Max wait time
     */
    private final int maxWait;

    /**
     * Warning lines
     */
    private final int warningLines;

    public AsyncBatchHandler(Object actionClass, Method actionMethod, String desc, int poolSize, int maxElements,
            int maxWait) {
        super(actionClass, actionMethod, desc);

        Class<?>[] parameterTypes = actionMethod.getParameterTypes();
        if (parameterTypes.length > 1) {
            throw new RuntimeException("@AsyncBatch方法仅支持一个List参数:" + actionMethod);
        }
        if (!parameterTypes[0].isAssignableFrom(List.class)) {
            throw new RuntimeException("@AsyncBatch方法的参数不是List类型:" + actionMethod);
        }

        this.poolSize = poolSize;
        this.maxElements = maxElements;
        this.maxWait = maxWait;
        this.warningLines = maxElements * poolSize * 50;

        this.queue = new ConcurrentLinkedQueue<>();
        this.executor = new ThreadPoolExecutor(this.poolSize, this.poolSize, 1000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(400),
                new DefaultThreadFactory(actionMethod.getName(), true, Thread.NORM_PRIORITY));

        for (int i = 0; i < poolSize; i++) {
            boolean master = i == 0;
            executor.execute(() -> {
                try {
                    startInternal(master);
                } catch (Exception e) {
                    LOG.error("批处理线程出错", e);
                }
            });
        }
    }

    public <T extends Message> T invoke(T request, Session session) {
        queue.offer(request);
        return null;
    }

    public void startInternal(boolean master) {
        Message[] array = new Message[maxElements];
        long logtime = 0;
        long starttime = 0;

        for (;;) {
            Message temp;
            int i = 0;
            while ((temp = queue.poll()) != null) {
                array[i++] = temp;
                if (i >= maxElements) {
                    break;
                }
            }

            if (i > 0) {
                starttime = System.currentTimeMillis();
                try {
                    targetMethod.invoke(targetObject, new VirtualList<>(array, i));
                } catch (Exception e) {
                    LOG.warn(targetMethod.getName(), e);
                }
                long time = System.currentTimeMillis() - starttime;
                if (time > 1000L) {
                    LOG.warn("批处理耗时:{}ms,共{}条记录", time, i);
                }
            }

            if (i < maxElements) {
                try {
                    for (int j = 0; j < i; j++) {
                        array[j] = null;
                    }
                    Thread.sleep(maxWait);
                } catch (InterruptedException ignored) {
                }
            } else if (master) {
                if (logtime < starttime) {
                    logtime = starttime + 5000L;

                    int size = queue.size();
                    if (size > warningLines) {
                        LOG.warn("批处理队列繁忙, size:{}", size);
                    }
                }
            }
        }
    }
}
