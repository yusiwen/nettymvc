package cn.yusiwen.nettymvc.codec;

import cn.yusiwen.nettymvc.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 基础消息编码
 *
 * @author yusiwen
 *
 * @param <T>
 *            Message
 */
public interface MessageEncoder<T> {

    ByteBuf encode(T message, Session session);

}