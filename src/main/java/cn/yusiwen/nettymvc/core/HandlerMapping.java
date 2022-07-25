package cn.yusiwen.nettymvc.core;

import cn.yusiwen.nettymvc.core.handler.AbstractHandler;

/**
 * 消息映射接口
 *
 * @author yusiwen
 */
public interface HandlerMapping {

    AbstractHandler getHandler(int messageId);

}