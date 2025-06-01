package com.wheelproject.rpc.protocol.codec;

import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageSerializerEnum;
import com.wheelproject.rpc.serializer.Serializer;
import com.wheelproject.rpc.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义格式编码
 */
public class NettyMessageEncoder extends MessageToByteEncoder<ProtocolMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) throws Exception {
        if (msg == null || msg.getHeader() == null) {
            throw new Exception("编码失败，数据信息不完整");
        }

        ProtocolMessage.Header header = msg.getHeader();

        // 写入头部信息
        out.writeByte(header.getMagic());
        out.writeByte(header.getVersion());
        out.writeByte(header.getSerializer());
        out.writeByte(header.getType());
        out.writeByte(header.getStatus());
        out.writeLong(header.getRequestId());

        // 获取序列化器
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum
                .getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new Exception("序列化协议不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());

        // 序列化消息体
        byte[] bodyBytes = serializer.serialize(msg.getBody());

        // 写入body长度和数据
        out.writeInt(bodyBytes.length);
        out.writeBytes(bodyBytes);
    }
}