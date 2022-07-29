package cn.yusiwen.nettymvc.core;

import java.util.List;

import cn.yusiwen.nettymvc.core.annotation.Endpoint;
import cn.yusiwen.nettymvc.util.ClassUtils;

/**
 * @author yusiwen
 */
public class DefaultHandlerMapping extends AbstractHandlerMapping {

    public DefaultHandlerMapping(String endpointPackage) {
        List<Class<?>> endpointClasses = ClassUtils.getClassList(endpointPackage, Endpoint.class);

        for (Class<?> endpointClass : endpointClasses) {
            try {
                Object bean = endpointClass.getDeclaredConstructor((Class<?>[]) null).newInstance((Object[]) null);
                super.registerHandlers(bean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
