package com.wheelproject.rpc.server.httpServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Netty HTTP 服务器
 */
public class NettyHttpServer implements HttpServer {
    @Override
    public void run(int port) {
        // 负责处理进来的连接请求
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 负责处理已连接的客户端的 I/O 操作
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // Netty 用来配置和启动服务器的工具类
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup, workerGroup)
                //服务器套接字上等待连接的最大队列长度
                .option(ChannelOption.SO_BACKLOG, 1024)  // 128 → 1024，否会有 EndOfStreamException 错误
                //启用TCP层心跳机制，以保持长时间未活动的连接
                .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536));
                        ch.pipeline().addLast(new NettyHttpServerHandler());
                    }
                });
        // 将服务器绑定到指定的端口上，开始监听连接。
        ChannelFuture channelFuture = serverBootstrap.bind(port);

        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {  // new ChannelFutureListener() 匿名类 → Lambda 表达式
            if (channelFuture1.isSuccess()) {
                System.out.println("Netty listening now... port:" + port);  // 绑定成功
            } else {
                System.out.println("Sorry, no listening... >_<");  // 绑定失败
                channelFuture1.cause().printStackTrace();  // 打印出绑定失败的原因
            }
        });
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}