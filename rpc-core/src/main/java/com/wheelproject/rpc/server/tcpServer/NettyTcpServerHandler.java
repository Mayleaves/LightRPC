package com.wheelproject.rpc.server.tcpServer;

import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageStatusEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageTypeEnum;
import com.wheelproject.rpc.registry.LocalRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.Method;

/**
 * Netty TCP 服务端处理器
 * 1. 接收 ProtocolMessage<RpcRequest> 并解析
 * 2. 反射调用目标方法
 * 3. 返回 ProtocolMessage<RpcResponse>
 */
public class NettyTcpServerHandler extends SimpleChannelInboundHandler<ProtocolMessage> {
    /**
     * 服务端处理请求
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {
        if (msg != null ){
            System.out.println("Receive message:"+msg.getBody());
            handleRequest(ctx,msg);
        }else {
            throw new Exception("传入信息为空");
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, ProtocolMessage msg){
        RpcRequest rpcRequest = (RpcRequest) msg.getBody();
        //反射调用实现类方法
        RpcResponse rpcResponse = new RpcResponse();
        try{
            Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
            // 调用真实实现类的方法
            Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            Object result= method.invoke(implClass.newInstance(), rpcRequest.getArgs());

            //封装返回结果
            rpcResponse.setData(result);
            rpcResponse.setDataType(method.getReturnType());
            rpcResponse.setMessage("Succeed");
        }catch (Exception e){
            e.printStackTrace();
            rpcResponse.setMessage("Error invoking service method: " + e.getMessage());
            rpcResponse.setException(e);
        }
        sendResponse(ctx,msg, rpcResponse);
        ctx.close();
    }

    private void sendResponse(ChannelHandlerContext ctx, ProtocolMessage msg, RpcResponse rpcResponse){
        msg.setBody(rpcResponse);
        ProtocolMessage.Header header = msg.getHeader();
        header.setType((byte)ProtocolMessageTypeEnum.RESPONSE.getKey());
        ctx.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close(); // 发生异常时关闭通道
    }
}