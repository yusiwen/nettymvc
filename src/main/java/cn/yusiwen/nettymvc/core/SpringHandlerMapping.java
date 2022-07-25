package cn.yusiwen.nettymvc.core;

import java.util.Map;

import cn.yusiwen.nettymvc.core.annotation.Endpoint;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author yusiwen
 */
public class SpringHandlerMapping extends AbstractHandlerMapping implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> endpoints = applicationContext.getBeansWithAnnotation(Endpoint.class);
        for (Object bean : endpoints.values()) {
            super.registerHandlers(bean);
        }
    }
}
