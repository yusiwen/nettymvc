package cn.yusiwen.nettymvc.session;

/**
 * @author yusiwen
 */
public interface SessionListener {

    /**
     * 客户端建立连接
     *
     * @param session
     *            Session
     */
    default void sessionCreated(Session session) {
    }

    /**
     * 客户端完成注册或鉴权
     *
     * @param session
     *            Session
     */
    default void sessionRegistered(Session session) {
    }

    /**
     * 客户端注销或离线
     *
     * @param session
     *            Session
     */
    default void sessionDestroyed(Session session) {
    }
}
