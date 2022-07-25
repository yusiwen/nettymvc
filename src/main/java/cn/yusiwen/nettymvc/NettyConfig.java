package cn.yusiwen.nettymvc;

import cn.yusiwen.nettymvc.codec.Delimiter;
import cn.yusiwen.nettymvc.codec.LengthField;
import cn.yusiwen.nettymvc.codec.MessageDecoder;
import cn.yusiwen.nettymvc.codec.MessageEncoder;
import cn.yusiwen.nettymvc.core.HandlerInterceptor;
import cn.yusiwen.nettymvc.core.HandlerMapping;
import cn.yusiwen.nettymvc.session.SessionManager;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.ObjectUtil;

/**
 * @author yusiwen
 */
public class NettyConfig {

    /**
     * Worker core
     */
    protected final int workerCore;
    /**
     * Business core
     */
    protected final int businessCore;
    /**
     * Reader idle time
     */
    protected final int readerIdleTime;
    /**
     * Writer idle time
     */
    protected final int writerIdleTime;
    /**
     * All idle time
     */
    protected final int allIdleTime;
    /**
     * Port
     */
    protected final Integer port;
    /**
     * Max frame length
     */
    protected final Integer maxFrameLength;
    /**
     * Length field
     */
    protected final LengthField lengthField;
    /**
     * Delimiters
     */
    protected final Delimiter[] delimiters;
    /**
     * Decoder
     */
    protected final MessageDecoder decoder;
    /**
     * Encoder
     */
    protected final MessageEncoder encoder;
    /**
     * HandlerMapping
     */
    protected final HandlerMapping handlerMapping;
    /**
     * HandlerInterceptor
     */
    protected final HandlerInterceptor handlerInterceptor;
    /**
     * Session manager
     */
    protected final SessionManager sessionManager;
    /**
     * Enable UDP or not
     */
    protected final boolean enableUDP;
    /**
     * Server instance
     */
    protected final AbstractServer server;
    /**
     * Server name
     */
    protected final String name;

    /**
     * Stopwatch
     */
    protected boolean enableStopwatch;

    private NettyConfig(int workerGroup, int businessGroup, int readerIdleTime, int writerIdleTime, int allIdleTime,
            Integer port, Integer maxFrameLength, LengthField lengthField, Delimiter[] delimiters,
            MessageDecoder decoder, MessageEncoder encoder, HandlerMapping handlerMapping,
            HandlerInterceptor handlerInterceptor, SessionManager sessionManager, boolean enableUDP,
            boolean enableStopwatch, String name) {
        ObjectUtil.checkNotNull(port, "port");
        ObjectUtil.checkPositive(port, "port");
        ObjectUtil.checkNotNull(decoder, "decoder");
        ObjectUtil.checkNotNull(encoder, "encoder");
        ObjectUtil.checkNotNull(handlerMapping, "handlerMapping");
        ObjectUtil.checkNotNull(handlerInterceptor, "handlerInterceptor");
        if (!enableUDP) {
            ObjectUtil.checkNotNull(maxFrameLength, "maxFrameLength");
            ObjectUtil.checkPositive(maxFrameLength, "maxFrameLength");
            ObjectUtil.checkNotNull(delimiters, "delimiters");
        }

        int processors = NettyRuntime.availableProcessors();
        this.workerCore = workerGroup > 0 ? workerGroup : processors + 2;
        this.businessCore = businessGroup > 0 ? businessGroup : Math.max(1, processors >> 1);
        this.readerIdleTime = readerIdleTime;
        this.writerIdleTime = writerIdleTime;
        this.allIdleTime = allIdleTime;
        this.port = port;
        this.maxFrameLength = maxFrameLength;
        this.lengthField = lengthField;
        this.delimiters = delimiters;
        this.decoder = decoder;
        this.encoder = encoder;
        this.handlerMapping = handlerMapping;
        this.handlerInterceptor = handlerInterceptor;
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
        this.enableUDP = enableUDP;
        this.enableStopwatch = enableStopwatch;

        if (enableUDP) {
            this.name = name != null ? name : "UDP";
            this.server = new UDPServer(this);
        } else {
            this.name = name != null ? name : "TCP";
            this.server = new TCPServer(this);
        }
    }

    public AbstractServer build() {
        return server;
    }

    public static NettyConfig.Builder custom() {
        return new Builder();
    }

    public int getWorkerCore() {
        return workerCore;
    }

    public int getBusinessCore() {
        return businessCore;
    }

    public int getReaderIdleTime() {
        return readerIdleTime;
    }

    public int getWriterIdleTime() {
        return writerIdleTime;
    }

    public int getAllIdleTime() {
        return allIdleTime;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getMaxFrameLength() {
        return maxFrameLength;
    }

    public LengthField getLengthField() {
        return lengthField;
    }

    public Delimiter[] getDelimiters() {
        return delimiters;
    }

    public MessageDecoder getDecoder() {
        return decoder;
    }

    public MessageEncoder getEncoder() {
        return encoder;
    }

    public HandlerMapping getHandlerMapping() {
        return handlerMapping;
    }

    public HandlerInterceptor getHandlerInterceptor() {
        return handlerInterceptor;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public boolean isEnableUDP() {
        return enableUDP;
    }

    public AbstractServer getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    public boolean isEnableStopwatch() {
        return enableStopwatch;
    }

    public static class Builder {

        /**
         * Worker core
         */
        private int workerCore;
        /**
         * Business core
         */
        private int businessCore;
        /**
         * Reader idle time
         */
        private int readerIdleTime = 240;
        /**
         * Writer idle time
         */
        private int writerIdleTime = 0;
        /**
         * All idle time
         */
        private int allIdleTime = 0;
        /**
         * Port
         */
        private Integer port;
        /**
         * Max frame length
         */
        private Integer maxFrameLength;
        /**
         * Length field
         */
        private LengthField lengthField;
        /**
         * Delimiters
         */
        private Delimiter[] delimiters;
        /**
         * Decoder
         */
        private MessageDecoder decoder;
        /**
         * Encoder
         */
        private MessageEncoder encoder;
        /**
         * HandlerMapping
         */
        private HandlerMapping handlerMapping;
        /**
         * HandlerInterceptor
         */
        private HandlerInterceptor handlerInterceptor;
        /**
         * SessionManager
         */
        private SessionManager sessionManager;
        /**
         * Enable UDP or not
         */
        private boolean enableUDP;
        /**
         * Server name
         */
        private String name;

        /**
         * Stopwatch
         */
        private boolean enableStopwatch;

        public Builder() {
        }

        public Builder setThreadGroup(int workerCore, int businessCore) {
            this.workerCore = workerCore;
            this.businessCore = businessCore;
            return this;
        }

        public Builder setIdleStateTime(int readerIdleTime, int writerIdleTime, int allIdleTime) {
            this.readerIdleTime = readerIdleTime;
            this.writerIdleTime = writerIdleTime;
            this.allIdleTime = allIdleTime;
            return this;
        }

        public Builder setPort(Integer port) {
            this.port = port;
            return this;
        }

        public Builder setMaxFrameLength(Integer maxFrameLength) {
            this.maxFrameLength = maxFrameLength;
            return this;
        }

        public Builder setLengthField(LengthField lengthField) {
            this.lengthField = lengthField;
            return this;
        }

        public Builder setDelimiters(byte[][] delimiters) {
            Delimiter[] t = new Delimiter[delimiters.length];
            for (int i = 0; i < delimiters.length; i++) {
                t[i] = new Delimiter(delimiters[i]);
            }
            this.delimiters = t;
            return this;
        }

        public Builder setDelimiters(Delimiter... delimiters) {
            this.delimiters = delimiters;
            return this;
        }

        public Builder setDecoder(MessageDecoder decoder) {
            this.decoder = decoder;
            return this;
        }

        public Builder setEncoder(MessageEncoder encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder setHandlerMapping(HandlerMapping handlerMapping) {
            this.handlerMapping = handlerMapping;
            return this;
        }

        public Builder setHandlerInterceptor(HandlerInterceptor handlerInterceptor) {
            this.handlerInterceptor = handlerInterceptor;
            return this;
        }

        public Builder setSessionManager(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }

        public Builder setEnableUDP(boolean enableUDP) {
            this.enableUDP = enableUDP;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEnableStopwatch(boolean flag) {
            this.enableStopwatch = flag;
            return this;
        }

        public AbstractServer build() {
            return new NettyConfig(this.workerCore, this.businessCore, this.readerIdleTime, this.writerIdleTime,
                    this.allIdleTime, this.port, this.maxFrameLength, this.lengthField, this.delimiters, this.decoder,
                    this.encoder, this.handlerMapping, this.handlerInterceptor, this.sessionManager, this.enableUDP,
                    this.enableStopwatch, this.name).build();
        }
    }
}