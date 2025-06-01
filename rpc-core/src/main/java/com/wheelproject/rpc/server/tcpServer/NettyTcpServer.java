package com.wheelproject.rpc.server.tcpServer;

import com.wheelproject.rpc.protocol.codec.NettyMessageDecoder;
import com.wheelproject.rpc.protocol.codec.NettyMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * 应用层自定义协议头 传输使用TCP
 */
@Slf4j
public class NettyTcpServer {
    public void run(int port) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                //服务器套接字上等待连接的最大队列长度
                .option(ChannelOption.SO_BACKLOG, 1024)
                //启用TCP层心跳机制，以保持长时间未活动的连接
                .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                .channel(NioServerSocketChannel.class)//NIO模式
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024,0,4,0,4));
                        //ch.pipeline().addLast(new LengthFieldPrepender(4));
                        ch.pipeline().addLast(new NettyMessageDecoder());
                        ch.pipeline().addLast(new NettyMessageEncoder());

                        ch.pipeline().addLast(new NettyTcpServerHandler());
                    }
                });

        ChannelFuture channelFuture = serverBootstrap.bind(port);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    System.out.println("Netty TCP Listening now... port:" + port);
                } else {
                    System.out.println("Sorry, no listening... >_<");
                }
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

    public static void main(String[] args) {
        new NettyTcpServer().run(8888);
    }
}

