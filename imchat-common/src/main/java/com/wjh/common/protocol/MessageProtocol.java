package com.wjh.common.protocol;

import lombok.Data;

/**
 * 自定义消息协议
 * <pre>
 * 协议格式：
 * 魔数(4B) + 版本(1B) + 消息类型(1B) + 序列化方式(1B) + 状态(1B) + 数据长度(4B) + 数据(NB)
 * </pre>
 */
@Data
public class MessageProtocol {

    /**
     * 魔数，固定为0xCAFEBABE
     */
    public static final int MAGIC = 0xCAFEBABE;

    /**
     * 版本号
     */
    public static final byte VERSION = 0x01;

    /**
     * 序列化方式：0-JSON，1-Protobuf
     */
    public static final byte SERIALIZER_JSON = 0x00;

    /**
     * 消息类型
     * @see com.wjh.common.enums.MessageTypeEnum
     */
    private Byte messageType;

    /**
     * 状态：0-成功，1-失败
     */
    private Byte status;

    /**
     * 消息体长度
     */
    private Integer length;

    /**
     * 消息体
     */
    private byte[] body;
}
