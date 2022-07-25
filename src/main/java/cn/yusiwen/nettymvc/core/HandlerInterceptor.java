package cn.yusiwen.nettymvc.core;

import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.Session;

/**
 * 消息拦截器
 *
 * @author yusiwen
 * @param <T>
 *            Message
 */
public interface HandlerInterceptor<T extends Message> {

    /**
     * @param request
     *            Request message
     * @param session
     *            Session
     * @return Response 未找到对应的Handle
     */
    T notSupported(T request, Session session);

    /**
     * @param request
     *            Request message
     * @param session
     *            Session
     * @return boolean 调用之前
     */
    boolean beforeHandle(T request, Session session);

    /**
     * @param request
     *            Request message
     * @param session
     *            Session
     * @return Response 调用之后，返回值为void的
     */
    T successful(T request, Session session);

    /**
     * 调用之后，有返回值的
     *
     * @param request
     *            Request message
     * @param response
     *            Response message
     * @param session
     *            Session
     */
    void afterHandle(T request, T response, Session session);

    /**
     * @param request
     *            Request message
     * @param session
     *            Session
     * @param e
     *            Exception
     * @return Response 调用之后抛出异常的
     */
    T exceptional(T request, Session session, Exception e);
}
