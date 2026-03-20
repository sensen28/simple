package com.wjh.netty.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.common.protocol.MessageProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 自定义消息编解码器
 */
@Slf4j
public class MessageCodec extends ByteToMessageCodec<MessageProtocol> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageProtocol msg, ByteBuf out) throws Exception {
        // 写入魔数
        out.writeInt(MessageProtocol.MAGIC);
        // 写入版本
        out.writeByte(MessageProtocol.VERSION);
        // 写入消息类型
        out.writeByte(msg.getMessageType());
        // 写入序列化方式，默认JSON
        out.writeByte(MessageProtocol.SERIALIZER_JSON);
        // 写入状态
        out.writeByte(msg.getStatus());
        // 写入数据长度
        out.writeInt(msg.getLength());
        // 写入数据
        out.writeBytes(msg.getBody());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查可读字节数是否小于协议头长度（4+1+1+1+1+4=12字节）
        if (in.readableBytes() < 12) {
            return;
        }
        // 标记读取位置
        in.markReaderIndex();
        // 读取魔数
        int magic = in.readInt();
        if (magic != MessageProtocol.MAGIC) {
            log.error("无效的魔数: {}", magic);
            in.resetReaderIndex();
            return;
        }
        // 读取版本
        byte version = in.readByte();
        if (version != MessageProtocol.VERSION) {
            log.error("不支持的协议版本: {}", version);
            in.resetReaderIndex();
            return;
        }
        // 读取消息类型
        byte messageType = in.readByte();
        // 读取序列化方式
        byte serializer = in.readByte();
        if (serializer != MessageProtocol.SERIALIZER_JSON) {
            log.error("不支持的序列化方式: {}", serializer);
            in.resetReaderIndex();
            return;
        }
        // 读取状态
        byte status = in.readByte();
        // 读取数据长度
        int length = in.readInt();
        // 检查可读字节数是否小于数据长度
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        // 读取数据
        byte[] body = new byte[length];
        in.readBytes(body);
        // 构造消息对象
        MessageProtocol message = new MessageProtocol();
        message.setMessageType(messageType);
        message.setStatus(status);
        message.setLength(length);
        message.setBody(body);
        out.add(message);
    }
}
