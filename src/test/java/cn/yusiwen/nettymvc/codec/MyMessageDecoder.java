package cn.yusiwen.nettymvc.codec;

import java.nio.charset.StandardCharsets;

import cn.yusiwen.nettymvc.model.MyHeader;
import cn.yusiwen.nettymvc.session.Session;
import cn.yusiwen.nettymvc.model.MyMessage;
import io.netty.buffer.ByteBuf;

public class MyMessageDecoder implements MessageDecoder {

    @Override
    public MyMessage decode(ByteBuf buf, Session session) {
        String msgStr = buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8).toString();
        String[] allStr = msgStr.split(";");
        String[] headStr = allStr[0].split(",");
        String bodyStr = allStr[1];

        MyHeader header = new MyHeader();
        header.setClientId(headStr[0]);
        header.setType(Integer.parseInt(headStr[1]));
        header.setSerialNo(Integer.parseInt(headStr[2]));

        MyMessage message = new MyMessage();
        message.setHeader(header);
        message.setBody(bodyStr);
        return message;
    }
}
