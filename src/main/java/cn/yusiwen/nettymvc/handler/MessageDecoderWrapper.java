package cn.yusiwen.nettymvc.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.codec.MessageDecoder;
import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.AbstractPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;

/**
 * 基础消息解码
 *
 * @param <T>
 *            Message
 * @author yusiwen
 */
@ChannelHandler.Sharable
public class MessageDecoderWrapper<T extends Message> extends ChannelInboundHandlerAdapter {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageDecoderWrapper.class);

    /**
     * Message decoder
     */
    private final MessageDecoder<T> decoder;

    public MessageDecoderWrapper(MessageDecoder<T> decoder) {
        this.decoder = decoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        AbstractPacket<T> packet = (AbstractPacket<T>) msg;
        ByteBuf input = packet.take();
        try {
            T message = decoder.decode(input, packet.getSession());
            if (message != null) {
                ctx.fireChannelRead(packet.replace(message));
            }
            input.skipBytes(input.readableBytes());
        } catch (Exception e) {
            LOG.error("消息解码异常[" + ByteBufUtil.hexDump(input, 0, input.writerIndex()) + "]", e);
            throw new DecoderException(e);
        } finally {
            input.release();
        }
    }
}
