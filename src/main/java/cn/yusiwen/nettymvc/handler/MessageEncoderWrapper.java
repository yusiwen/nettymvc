package cn.yusiwen.nettymvc.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.codec.MessageEncoder;
import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.AbstractPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;

/**
 * 基础消息编码
 *
 * @param <T>
 *            Message
 * @author yusiwen
 */
@ChannelHandler.Sharable
public class MessageEncoderWrapper<T extends Message> extends ChannelOutboundHandlerAdapter {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageEncoderWrapper.class);

    /**
     * Message encoder
     */
    private final MessageEncoder<T> encoder;

    public MessageEncoderWrapper(MessageEncoder<T> encoder) {
        this.encoder = encoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        AbstractPacket<T> packet = (AbstractPacket<T>) msg;
        ByteBuf output = packet.take();
        try {
            if (output == null) {
                output = encoder.encode(packet.getMessage(), packet.getSession());
            }

            if (output.isReadable()) {
                ctx.write(packet.wrap(output), promise);
            } else {
                output.release();
                ctx.write(packet.wrap(Unpooled.EMPTY_BUFFER), promise);
            }
            output = null;
        } catch (EncoderException e) {
            LOG.error("消息编码异常" + packet.getMessage(), e);
            throw e;
        } catch (Throwable e) {
            LOG.error("消息编码异常" + packet.getMessage(), e);
            throw new EncoderException(e);
        } finally {
            if (output != null) {
                output.release();
            }
        }
    }
}
