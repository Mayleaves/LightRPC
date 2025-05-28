package com.wheelproject.rpc.server.tcpServer;

import cn.hutool.core.util.IdUtil;
import com.google.protobuf.ByteString;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.protocol.codec.ProtocolMessageEncoder;
import com.wheelproject.rpc.protocol.common.ProtocolConstant;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageSerializerEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageStatusEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageTypeEnum;
import com.wheelproject.rpc.serializer.Serializer;
import com.wheelproject.rpc.serializer.SerializerFactory;
import com.wheelproject.rpc.server.httpServer.HttpServer;
import io.etcd.jetcd.api.Role;
import io.etcd.jetcd.api.User;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;          // 字节缓冲区
import java.nio.charset.StandardCharsets; // 标准字符集

/**
 * Vertx TCP 服务器
 */
@Slf4j
public class VertxTcpServer implements HttpServer {

    @Override
    public void run(int port) {
        // 创建 Vert.x 实例
        Vertx vertx = Vertx.vertx();

        // 创建 TCP 服务器
        NetServer server = vertx.createNetServer();

        // 处理请求
         server.connectHandler(new VertxTcpServerHandler());

        // 启动 TCP 服务器并监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                log.info("TCP server started on port " + port);
            } else {
                log.info("Failed to start TCP server: " + result.cause());
            }
        });
    }

    public static void main(String[] args) {
        new VertxTcpServer().run(8888);
    }
}
