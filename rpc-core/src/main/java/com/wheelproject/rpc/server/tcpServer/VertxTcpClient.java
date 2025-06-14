package com.wheelproject.rpc.server.tcpServer;

import cn.hutool.core.util.IdUtil;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.constant.MessageConstant;
import com.wheelproject.rpc.exception.ProtocolMessageEncodingErrorException;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import com.wheelproject.rpc.protocol.codec.VertxMessageDecoder;
import com.wheelproject.rpc.protocol.codec.VertxMessageEncoder;
import com.wheelproject.rpc.protocol.common.ProtocolConstant;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageSerializerEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageTypeEnum;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Vertx TCP 客户端
 */
public class VertxTcpClient {

    /**
     * 发送请求
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo)
            throws InterruptedException, ExecutionException {
        System.out.println("Calling service at " + serviceMetaInfo.getServiceAddress());

        // 发送 TCP 请求
        Vertx vertx = Vertx.vertx();
        NetClient netClient = vertx.createNetClient();
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        netClient.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(),
                result -> {
                    if (!result.succeeded()) {
                        System.err.println("Failed to connect to TCP server");
                        // 连接失败时，主动触发异常
                        // 不写这一句，会导致没有异常抛出，外层的 RetryStrategy 无法感知失败，不会触发重试。
                        responseFuture.completeExceptionally(new RuntimeException("连接失败："+ result.cause()));
                        return;
                    }
                    System.out.println("Connected to TCP server");
                    NetSocket socket = result.result();
                    // 发送数据
                    // 构造消息
                    ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                    ProtocolMessage.Header header = new ProtocolMessage.Header();
                    header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                    header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                    header.setSerializer((byte) ProtocolMessageSerializerEnum.
                            getEnumByValue(RpcApplication.getRpcConfig().getSerializer()).getKey());
                    header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
                    // 生成全局请求 ID
                    header.setRequestId(IdUtil.getSnowflakeNextId());
                    protocolMessage.setHeader(header);
                    protocolMessage.setBody(rpcRequest);

                    // 编码请求
                    try {
                        Buffer encodeBuffer = VertxMessageEncoder.encode(protocolMessage);
                        socket.write(encodeBuffer);
                    } catch (IOException e) {
                        throw new ProtocolMessageEncodingErrorException(MessageConstant.PROTOCOL_MESSAGE_ENCODING_ERROR);
                    }

                    // 接收响应
                    VertxTcpBufferHandlerWrapper bufferHandlerWrapper = new VertxTcpBufferHandlerWrapper(
                            buffer -> {
                                try {
                                    ProtocolMessage<RpcResponse> rpcResponseProtocolMessage =
                                            (ProtocolMessage<RpcResponse>) VertxMessageDecoder.decode(buffer);
                                    responseFuture.complete(rpcResponseProtocolMessage.getBody());
                                } catch (IOException e) {
                                    throw new ProtocolMessageEncodingErrorException(MessageConstant.PROTOCOL_MESSAGE_ENCODING_ERROR);
                                }
                            }
                    );
                    socket.handler(bufferHandlerWrapper);
                });

        RpcResponse rpcResponse = responseFuture.get();
        // 记得关闭连接
        netClient.close();
        return rpcResponse;
    }
}
