package cn.yusiwen.nettymvc.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

/**
 * @author yusiwen
 */
public class SessionManager {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    /**
     * Session map
     */
    private final Map<String, Session> sessionMap;

    /**
     * Offline cache
     */
    private final Cache<String, Object> offlineCache;

    /**
     * Session listener
     */
    private final SessionListener sessionListener;

    /**
     * Session key class
     */
    private final Class<? extends Enum> sessionKeyClass;

    public SessionManager() {
        this(null, null);
    }

    public SessionManager(SessionListener sessionListener) {
        this(null, sessionListener);
    }

    public SessionManager(Class<? extends Enum> sessionKeyClass, SessionListener sessionListener) {
        this.sessionMap = new ConcurrentHashMap<>();
        this.offlineCache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
        this.sessionKeyClass = sessionKeyClass;
        this.sessionListener = sessionListener;
    }

    public Session get(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public Collection<Session> all() {
        return sessionMap.values();
    }

    public Session newInstance(Channel channel) {
        InetSocketAddress sender = (InetSocketAddress) channel.remoteAddress();
        Session session = new Session(this, channel, sender, s -> {
            channel.close();
            return true;
        }, false);
        if (sessionListener != null) {
            try {
                sessionListener.sessionCreated(session);
            } catch (Exception e) {
                LOG.error("sessionCreated", e);
            }
        }
        return session;
    }

    public Session newInstance(Channel channel, InetSocketAddress sender, Function<Session, Boolean> remover) {
        Session session = new Session(this, channel, sender, remover, true);
        if (sessionListener != null) {
            try {
                sessionListener.sessionCreated(session);
            } catch (Exception e) {
                LOG.error("sessionCreated", e);
            }
        }
        return session;
    }

    protected void remove(Session session) {
        boolean remove = sessionMap.remove(session.getId(), session);
        if (remove && sessionListener != null) {
            try {
                sessionListener.sessionDestroyed(session);
            } catch (Exception e) {
                LOG.error("sessionDestroyed", e);
            }
        }
    }

    protected void add(Session newSession) {
        sessionMap.put(newSession.getId(), newSession);
        if (sessionListener != null) {
            try {
                sessionListener.sessionRegistered(newSession);
            } catch (Exception e) {
                LOG.error("sessionRegistered error: ", e);
            }
        }
    }

    public void setOfflineCache(String clientId, Object value) {
        offlineCache.put(clientId, value);
    }

    public Object getOfflineCache(String clientId) {
        return offlineCache.getIfPresent(clientId);
    }

    public Class<? extends Enum> getSessionKeyClass() {
        return sessionKeyClass;
    }
}
