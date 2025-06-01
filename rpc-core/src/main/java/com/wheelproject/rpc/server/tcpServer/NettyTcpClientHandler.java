package com.wheelproject.rpc.server.tcpServer;

import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Netty 的 TCP 客户端处理器
 * 接收解码后的 ProtocolMessage<RpcResponse>，并通过 CompletableFuture 返回结果
 */
public class NettyTcpClientHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    private CompletableFuture responseFuture;

    public NettyTcpClientHandler(CompletableFuture<RpcResponse> responseFuture) {
        this.responseFuture = responseFuture;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {
        // 收到响应后，将其设置为 CompletableFuture 的结果
        responseFuture.complete(msg.getBody());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        responseFuture.completeExceptionally(cause);
    }
}