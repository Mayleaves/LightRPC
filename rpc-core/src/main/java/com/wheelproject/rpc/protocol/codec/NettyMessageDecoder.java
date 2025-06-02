package com.wheelproject.rpc.protocol.codec;

import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.protocol.common.ProtocolConstant;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageSerializerEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageTypeEnum;
import com.wheelproject.rpc.serializer.Serializer;
import com.wheelproject.rpc.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Netty TCP 协议消息解码器
 */
public class NettyMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的数据读取头部 (17 bytes)
        if (in.readableBytes() < ProtocolConstant.MESSAGE_HEADER_LENGTH) {
            return;
        }

        // 标记当前读指针位置
        in.markReaderIndex();

        ProtocolMessage.Header header = new ProtocolMessage.Header();

        // 读取并验证魔数
        byte magic = in.readByte();
        if (magic != ProtocolConstant.PROTOCOL_MAGIC) {
            throw new RuntimeException("消息magic非法");
        }
        header.setMagic(magic);

        header.setVersion(in.readByte());
        header.setSerializer(in.readByte());
        header.setType(in.readByte());
        header.setStatus(in.readByte());
        header.setRequestId(in.readLong());
        int bodyLength = in.readInt();
        header.setBodyLength(bodyLength);

        // 检查是否有足够的数据读取body
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex(); // 重置读指针
            return;
        }

        // 读取body数据
        byte[] bodyBytes = new byte[bodyLength];
        in.readBytes(bodyBytes);

        // 反序列化消息体
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum
                .getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new RuntimeException("序列化协议不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());

        ProtocolMessageTypeEnum typeEnum = ProtocolMessageTypeEnum
                .getEnumByKey(header.getType());
        if (typeEnum == null) {
            throw new RuntimeException("序列化消息类型不存在");
        }

        switch (typeEnum) {
            case REQUEST:
                RpcRequest request = serializer.deserialize(bodyBytes, RpcRequest.class);
                ProtocolMessage<RpcRequest> requestMessage = new ProtocolMessage<>();
                requestMessage.setHeader(header);
                requestMessage.setBody(request);
                out.add(requestMessage);
                break;
            case RESPONSE:
                RpcResponse response = serializer.deserialize(bodyBytes, RpcResponse.class);
                ProtocolMessage<RpcResponse> responseMessage = new ProtocolMessage<>();
                responseMessage.setHeader(header);
                responseMessage.setBody(response);
                out.add(responseMessage);
                break;
            case HEART_BEAT:
            case OTHERS:
            default:
                throw new RuntimeException("不支持此消息类型");
        }
    }
}