package cn.yusiwen.nettymvc.session;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.core.model.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * @author yusiwen
 */
public class Session {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    /**
     * Rejected mono
     */
    private static final Mono REJECTED = Mono.error(new RejectedExecutionException("客户端暂未响应，请勿重复发送"));

    /**
     * Channel
     */
    protected final Channel channel;

    /**
     * Is UDP or not
     */
    private final boolean udp;
    /**
     * Remover
     */
    private final Function<Session, Boolean> remover;
    /**
     * SessionManager
     */
    private final SessionManager sessionManager;
    /**
     * Remote address
     */
    private final InetSocketAddress remoteAddress;
    /**
     * Remote address string
     */
    private final String remoteAddressStr;

    /**
     * Creation time
     */
    private final long creationTime;
    /**
     * Last accessed time
     */
    private long lastAccessedTime;
    /**
     * Attributes
     */
    private final Map<Object, Object> attributes;

    /**
     * Session id
     */
    private String sessionId;
    /**
     * Client id
     */
    private String clientId;
    /**
     * Serial No.
     */
    private final AtomicInteger serialNo = new AtomicInteger(0);

    /**
     * Topic subscribers map
     */
    private final Map<String, MonoSink> topicSubscribers = new HashMap<>();

    public Session(SessionManager sessionManager, Channel channel, InetSocketAddress remoteAddress,
            Function<Session, Boolean> remover, boolean udp) {
        this.channel = channel;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.sessionManager = sessionManager;
        this.remoteAddress = remoteAddress;
        this.remoteAddressStr = remoteAddress.toString();
        this.remover = remover;
        this.udp = udp;

        if (sessionManager != null && sessionManager.getSessionKeyClass() != null) {
            this.attributes = new EnumMap(sessionManager.getSessionKeyClass());
        } else {
            this.attributes = new TreeMap<>();
        }
    }

    /**
     * 注册到SessionManager
     *
     * @param message
     *            Message
     */
    public void register(Message message) {
        register(message.getClientId(), message);
    }

    public void register(String sessionId, Message message) {
        if (sessionId == null) {
            throw new NullPointerException("sessionId not null");
        }
        this.sessionId = sessionId;
        this.clientId = message.getClientId();
        if (sessionManager != null) {
            sessionManager.add(this);
        }
        LOG.info("<<<<<终端注册{}", this);
    }

    public boolean isRegistered() {
        return sessionId != null;
    }

    public String getId() {
        return sessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public long access() {
        lastAccessedTime = System.currentTimeMillis();
        return lastAccessedTime;
    }

    public Collection<Object> getAttributeNames() {
        return attributes.keySet();
    }

    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(Object name) {
        return attributes.get(name);
    }

    public void setAttribute(Object name, Object value) {
        attributes.put(name, value);
    }

    public Object removeAttribute(Object name) {
        return attributes.remove(name);
    }

    public Object getOfflineCache(String clientId) {
        if (sessionManager != null) {
            return sessionManager.getOfflineCache(clientId);
        }
        return null;
    }

    public void setOfflineCache(String clientId, Object value) {
        if (sessionManager != null) {
            sessionManager.setOfflineCache(clientId, value);
        }
    }

    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public int nextSerialNo() {
        int current;
        int next;
        do {
            current = serialNo.get();
            next = current > 0xffff ? 0 : current;
        } while (!serialNo.compareAndSet(current, next + 1));
        return next;
    }

    public void invalidate() {
        if (isRegistered() && sessionManager != null) {
            sessionManager.remove(this);
        }
        remover.apply(this);
    }

    public String getRemoteAddressStr() {
        return remoteAddressStr;
    }

    public boolean isUdp() {
        return udp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(remoteAddressStr);
        sb.append('/').append(sessionId);
        if (!Objects.equals(sessionId, clientId)) {
            sb.append('/').append(clientId);
        }
        return sb.toString();
    }

    /**
     * 异步发送通知类消息<br>
     * 同步发送 mono.block()<br>
     * 订阅回调 mono.doOnSuccess({处理成功}).doOnError({处理异常}).subscribe()开始订阅
     *
     * @param message
     *            Message
     * @return Mono
     */
    public Mono<Void> notify(Message message) {
        return mono(channel.writeAndFlush(AbstractPacket.of(this, message)));
    }

    public Mono<Void> notify(ByteBuf message) {
        return mono(channel.writeAndFlush(AbstractPacket.of(this, message)));
    }

    private static Mono<Void> mono(ChannelFuture channelFuture) {
        Mono<Void> mono = Mono.create(sink -> channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                sink.success();
            } else {
                sink.error(future.cause());
            }
        }));
        return mono;
    }

    /**
     * 异步发送消息，接收响应<br>
     * 同步接收 mono.block()<br>
     * 订阅回调 mono.doOnSuccess({处理成功}).doOnError({处理异常}).subscribe()开始订阅
     *
     * @param <T>
     *            Response class type
     * @param request
     *            Message
     * @param responseClass
     *            Response class
     * @return Mono
     */
    public <T> Mono<T> request(Message request, Class<T> responseClass) {
        String key = requestKey(request, responseClass);
        Mono<T> receive = this.subscribe(key);
        if (receive == null) {
            return REJECTED;
        }

        ChannelFuture channelFuture = channel.writeAndFlush(AbstractPacket.of(this, request));
        return Mono.create(sink -> channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                sink.success(future);
            } else {
                sink.error(future.cause());
            }
        })).then(receive).doFinally(signal -> unsubscribe(key));
    }

    /**
     * 消息响应
     *
     * @param message
     *            Message
     * @return Boolean
     */
    public boolean response(Message message) {
        MonoSink sink = topicSubscribers.get(responseKey(message));
        if (sink != null) {
            sink.success(message);
            return true;
        }
        return false;
    }

    private Mono subscribe(String key) {
        synchronized (topicSubscribers) {
            if (!topicSubscribers.containsKey(key)) {
                return Mono.create(sink -> topicSubscribers.put(key, sink));
            }
        }
        return null;
    }

    private void unsubscribe(String key) {
        topicSubscribers.remove(key);
    }

    private static String requestKey(Message request, Class responseClass) {
        String className = responseClass.getName();
        if (Response.class.isAssignableFrom(responseClass)) {
            int serialNo = request.getSerialNo();
            return className + '.' + serialNo;
        }
        return className;
    }

    private static String responseKey(Object response) {
        String className = response.getClass().getName();
        if (response instanceof Response) {
            int serialNo = ((Response) response).getResponseSerialNo();
            return className + '.' + serialNo;
        }
        return className;
    }
}
