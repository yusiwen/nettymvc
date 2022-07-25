package cn.yusiwen.nettymvc.endpoint;

import cn.yusiwen.nettymvc.core.annotation.Endpoint;
import cn.yusiwen.nettymvc.core.annotation.Mapping;
import cn.yusiwen.nettymvc.session.Session;
import cn.yusiwen.nettymvc.model.MyHeader;
import cn.yusiwen.nettymvc.model.MyMessage;

@Endpoint
public class MyEndpoint {

    @Mapping(types = 1, desc = "注册")
    public MyMessage register(MyMessage request, Session session) {
        session.register(request);
        return new MyMessage(new MyHeader(2, "123", 2), "ack");
    }
}
