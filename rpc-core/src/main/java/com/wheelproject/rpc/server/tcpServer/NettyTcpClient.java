package com.wheelproject.rpc.server.tcpServer;

import cn.hutool.core.util.IdUtil;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import com.wheelproject.rpc.protocol.codec.NettyMessageDecoder;
import com.wheelproject.rpc.protocol.codec.NettyMessageEncoder;
import com.wheelproject.rpc.protocol.common.ProtocolConstant;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageSerializerEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageTypeEnum;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Netty TCP 客户端（仿 Vert.x 风格，单次请求模式）
 */
public class NettyTcpClient {

    /**
     * 发送 RPC 请求（类似 VertxTcpClient.doRequest）
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo)
            throws InterruptedException, ExecutionException, TimeoutException {
        // 解析服务地址
        String host = serviceMetaInfo.getServiceHost();
        int port = serviceMetaInfo.getServicePort();

        // 使用 CompletableFuture 异步获取结果
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

        // Netty 客户端配置
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new NettyMessageEncoder());
                            pipeline.addLast(new NettyMessageDecoder());
                            pipeline.addLast(new NettyTcpClientHandler(responseFuture));
                        }
                    });

            // 连接服务器
            ChannelFuture connectFuture = bootstrap.connect(host, port).sync();

            // 构造 ProtocolMessage
            ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
            ProtocolMessage.Header header = new ProtocolMessage.Header();
            header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
            header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
            header.setSerializer((byte) ProtocolMessageSerializerEnum.getEnumByValue(RpcApplication.getRpcConfig().getSerializer()).getKey());
            header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
            header.setRequestId(IdUtil.getSnowflakeNextId());
            protocolMessage.setHeader(header);
            protocolMessage.setBody(rpcRequest);

            // 发送请求
            connectFuture.channel().writeAndFlush(protocolMessage);

            // 等待响应（可设置超时）
            return responseFuture.get(5, TimeUnit.SECONDS);
        } finally {
            group.shutdownGracefully(); // 关闭连接
        }
    }
}