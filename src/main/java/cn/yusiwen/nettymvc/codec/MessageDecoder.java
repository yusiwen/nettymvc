package cn.yusiwen.nettymvc.codec;

import cn.yusiwen.nettymvc.core.model.Message;
import cn.yusiwen.nettymvc.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 基础消息解码
 *
 * @author yusiwen
 *
 * @param <T>
 *            Message
 */
public interface MessageDecoder<T extends Message> {

    T decode(ByteBuf buf, Session session);

}