package com.wheelproject.rpc.server.httpServer;

import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.serializer.JdkSerializer;
import com.wheelproject.rpc.serializer.Serializer;
import com.wheelproject.rpc.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 通道内处理请求
 *
 * 1.反序列化请求为对象，并从对象中获取参数
 * 2.根据服务名从本地注册器中获取服务实现类
 * 3.通过反射，拿到返回结果
 * 4.对返回结果进行封装和序列化，并写入响应
 */

public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 指定序列化器
    // 1. 硬编码
//    final Serializer serializer = new JdkSerializer();
    // 2. 自定义实现 SPI
    final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        //拿到http请求
        String uri = msg.uri();
        System.out.println("Received request:" + msg.method() + " " + uri);

        if (msg.method().equals(HttpMethod.POST)) {
            handleHttpPostRequest(ctx, msg);
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }
    }


    private void handleHttpPostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        ByteBuf content = request.content();
        byte[] bytes = new byte[content.readableBytes()];  // 拿到可读的字节大小并创建
        content.readBytes(bytes);

        RpcRequest rpcRequest;
        RpcResponse rpcResponse = new RpcResponse();

        try {
            // 得到反序列化后的请求体
            rpcRequest = serializer.deserialize(bytes, RpcRequest.class);
        } catch (IOException e) {
            e.printStackTrace();
            rpcResponse.setMessage("Failed to deserialize RpcRequest: " + e.getMessage());
            sendResponse(ctx, HttpResponseStatus.BAD_REQUEST, rpcResponse);
            return;
        }

        if (rpcRequest == null) {
            rpcResponse.setMessage("RpcRequest not found");
            sendResponse(ctx, HttpResponseStatus.BAD_REQUEST, rpcResponse);
            return;
        }

        // 反射调用实现类方法
        try {
            Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
            // 调用真实实现类的方法
            Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            Object result = method.invoke(implClass.newInstance(), rpcRequest.getArgs());

            // 封装返回结果
            rpcResponse.setData(result);
            rpcResponse.setDataType(method.getReturnType());
            rpcResponse.setMessage("Succeed");
        } catch (Exception e) {
            e.printStackTrace();
            rpcResponse.setMessage("Error invoking service method: " + e.getMessage());
            rpcResponse.setException(e);
        }
        sendResponse(ctx, HttpResponseStatus.OK, rpcResponse);
        ctx.close();
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, RpcResponse rpcResponse) {
        byte[] serialized;
        try {
            serialized = serializer.serialize(rpcResponse);
        } catch (IOException e) {
            e.printStackTrace();
            serialized = new byte[0];
        }

        ByteBuf content = Unpooled.copiedBuffer(serialized);

        // 设置头信息
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
        ctx.close();
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

        ctx.writeAndFlush(response);
        ctx.close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close(); // 发生异常时关闭通道
    }
}