package cn.yusiwen.nettymvc.codec;

import java.nio.charset.StandardCharsets;

import cn.yusiwen.nettymvc.model.MyHeader;
import cn.yusiwen.nettymvc.model.MyMessage;
import cn.yusiwen.nettymvc.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MyMessageEncoder implements MessageEncoder<MyMessage> {

    @Override
    public ByteBuf encode(MyMessage message, Session session) {
        MyHeader header = message.getHeader();

        String msg = header.getClientId() + ',' + header.getType() + ',' + header.getSerialNo() + ';'
                + message.getBody();

        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        return Unpooled.wrappedBuffer(bytes);
    }
}
