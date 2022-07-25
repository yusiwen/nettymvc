package cn.yusiwen.nettymvc.core.handler;

import java.lang.reflect.Method;

import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.Session;

/**
 * 同步处理
 *
 * @author yusiwen
 */
public class SimpleHandler extends AbstractHandler {

    public SimpleHandler(Object actionClass, Method actionMethod, String desc, boolean async) {
        super(actionClass, actionMethod, desc, async);
    }

    public <T extends Message> T invoke(T request, Session session) throws Exception {
        return super.invoke(request, session);
    }
}
