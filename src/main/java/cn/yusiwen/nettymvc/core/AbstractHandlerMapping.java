package cn.yusiwen.nettymvc.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.yusiwen.nettymvc.core.annotation.Async;
import cn.yusiwen.nettymvc.core.annotation.AsyncBatch;
import cn.yusiwen.nettymvc.core.annotation.Mapping;
import cn.yusiwen.nettymvc.core.handler.AbstractHandler;
import cn.yusiwen.nettymvc.core.handler.AsyncBatchHandler;
import cn.yusiwen.nettymvc.core.handler.SimpleHandler;

/**
 * 消息处理映射
 *
 * @author yusiwen
 */
public abstract class AbstractHandlerMapping implements HandlerMapping {

    /**
     * Handler map
     */
    private final Map<Object, AbstractHandler> handlerMap = new HashMap<>(64);

    /**
     * 将Endpoint中被@Mapping标记的方法注册到映射表
     *
     * @param bean
     *            Handler bean
     */
    protected synchronized void registerHandlers(Object bean) {
        Class<?> beanClass = bean.getClass();
        Method[] methods = beanClass.getDeclaredMethods();

        for (Method method : methods) {

            Mapping mapping = method.getAnnotation(Mapping.class);
            if (mapping != null) {

                String desc = mapping.desc();
                int[] types = mapping.types();

                AsyncBatch asyncBatch = method.getAnnotation(AsyncBatch.class);
                AbstractHandler handler;

                if (asyncBatch != null) {
                    handler = new AsyncBatchHandler(bean, method, desc, asyncBatch.poolSize(), asyncBatch.maxElements(),
                            asyncBatch.maxWait());

                } else {
                    handler = new SimpleHandler(bean, method, desc, method.isAnnotationPresent(Async.class));
                }

                for (int type : types) {
                    handlerMap.put(type, handler);
                }
            }
        }
    }

    /**
     * 根据消息类型获取Handler
     *
     * @param messageId
     *            Message id
     * @return Handler
     */
    public AbstractHandler getHandler(int messageId) {
        return handlerMap.get(messageId);
    }
}
