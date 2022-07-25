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
 * @author yusiwen
 */
@ChannelHandler.Sharable
public class MessageDecoderWrapper extends ChannelInboundHandlerAdapter {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageDecoderWrapper.class);

    /**
     * Message decoder
     */
    private final MessageDecoder decoder;

    public MessageDecoderWrapper(MessageDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        AbstractPacket packet = (AbstractPacket) msg;
        ByteBuf input = packet.take();
        try {
            Message message = decoder.decode(input, packet.getSession());
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
