package cn.yusiwen.nettymvc.core.model;

import java.io.Serializable;

/**
 * 消息体
 *
 * @author yusiwen
 */
public interface Message extends Serializable {

    /**
     * 客户端唯一标识
     *
     * @return 客户端唯一标识
     */
    String getClientId();

    /**
     * 消息Id
     *
     * @return 消息Id
     */
    int getMessageId();

    /**
     * 消息流水号
     *
     * @return 消息流水号
     */
    int getSerialNo();
}
