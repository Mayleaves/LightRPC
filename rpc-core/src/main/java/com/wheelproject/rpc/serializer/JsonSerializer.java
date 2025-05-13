package com.wheelproject.rpc.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;

import java.io.IOException;

/**
 * JSON 序列化器：简单、可读性好、适合各种语言。
 */
public class JsonSerializer implements Serializer{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(object);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        T object = OBJECT_MAPPER.readValue(bytes, type);
        if (object instanceof RpcRequest){
            return handleRequest((RpcRequest)object, type);
        }
        if (object instanceof RpcResponse){
            return handleResponse((RpcResponse)object, type);
        }
        return object;
    }


    /**
     * 处理
     * 获取请求的 参数类型数组 和 参数数组，遍历每个参数。如果参数的类型与声明的类型不一致，将其重新序列化为正确的类型。
     * @param rpcRequest
     * @param type
     * @return
     * @param <T>
     * @throws IOException
     */
    private <T> T handleRequest(RpcRequest rpcRequest,Class<T> type) throws IOException{
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] args = rpcRequest.getArgs();
        // 循环处理每个参数的类型
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            // 如果参数类型不同，要重新处理
            if(!parameterType.isAssignableFrom(args[i].getClass())){
                byte[] argBytes = OBJECT_MAPPER.writeValueAsBytes(args[i]);
                args[i] = OBJECT_MAPPER.readValue(argBytes, parameterType);
            }
        }
        return type.cast(rpcRequest);
    }

    /**
     * 处理响应
     * @param rpcResponse
     * @param type
     * @return
     * @param <T>
     */
    private <T> T handleResponse(RpcResponse rpcResponse, Class<T> type) throws IOException{
        // 处理响应数据
        byte[] dataBytes = OBJECT_MAPPER.writeValueAsBytes(rpcResponse.getData());
        rpcResponse.setData(OBJECT_MAPPER.readValue(dataBytes,rpcResponse.getDataType()));
        return type.cast(rpcResponse);
    }
}
