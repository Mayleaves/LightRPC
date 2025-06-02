> 建议：
>
> - 简易版：Java JDK>= 8；
> - 扩展版：Java JDK >= 11；
> - IDEA Community <= 2019.3.5；IDEA Ultimate > 2019.3.5

# 1. 整体框架

RPC（Remote Procedure Call，远程过程调用）允许一个程序（称为服务消费者）像调用自己程序的方法一样，调用另一个程序（称为服务提供者）的接口，而不需要了解数据的传输处理过程、底层网络通信的细节等。主流 RPC 框架：Dubbo、gRPC。

## 1.1 简易版

整体流程：**服务消费者**通过**代理对象**发起调用，调用被拦截并封装为一个 RPC 请求对象，包含服务名、方法名和参数。**请求客户端**负责将这些信息序列化为字节流，打包成符合 HTTP/自定义协议格式的消息体，并通过底层通信框架（如 Netty 或 Vert.x）发送给服务端。**Web 服务器**作为服务端的入口，监听指定端口接收请求。由**请求处理器**反序列化并查询**本地注册器**获取对应的实现类，通过反射调用具体方法并返回结果，最后将执行结果序列化，通过同样的通信通道返回给请求客户端。请求客户端收到响应后进行反序列化，还原为原始调用的返回值并交还给调用者。

1. 代理对象：“伪装”成本地对象（实际上在背后发起远程调用），让消费者感觉使用起来像本地调用一样方便。

   - 拦截调用：拦截消费者调用的接口方法，捕获方法名、参数等信息；
   - 封装请求：将服务名、方法名、参数等封装成一个 RPC 请求对象（`RpcRequest`）；
   - 将结果返回给消费者：把服务端执行的结果返回给最初调用方法的本地代码。

2. 请求客户端：

   - 接收代理对象构造的 RPC 请求对象（`RpcRequest`）；

   - 序列化：RPC 请求对象（`RpcRequest`）→ RPC 请求（字节流）=服务名+方法名+参数；

   - 协议封装：通过底层通信框架发送协议请求到远端服务器；

     > 协议请求 = 控制信息 + 实际数据
     > Eg：HTTP 请求 = 请求行 + 请求头 + 请求体，其中请求体 = RPC 请求。

   - 处理响应：接收服务器返回的字节流，反序列化为响应对象（`RpcResponse`）。

3. Web 服务器：

   - 监听端口：类似于酒店员工等候接听（监听）前台电话（端口）；
   - 接收序列化数据。

4. 请求处理器：

   - 反序列化 RPC 请求（字节流 → `RpcRequest`）；
   - 查找服务：根据服务名查找实现类；
   - 反射调用：根据方法名、参数调用实现类中的具体方法；
   - 序列化 RPC 响应对象（`RpcResponse`  → 字节流）。

5. 本地服务注册器：

   -  Key-Value 映射表：ConcurrentHashMap（线程安全）；
   -  Key 服务名，Value 实现类。

6. 提供者/服务端 = Web服务器 + RPC框架 + 业务实现类。

下图虚线部分就是简易的 RPC 框架：

![image-20250601111645751](https://gitee.com/Koletis/pic-go/raw/master/202506011116996.png)

## 1.2 扩展

1. 服务注册、发现

   问题 1：消费者如何知道提供者的调用地址呢？

   设置一个注册中心用于保存服务提供者的地址，消费者要调用服务时，只需从注册中心获取对应服务的提供者地址即可。一般用现成的第三方注册中心，如 Redis、Zookeeper、Etcd。

   > **Redis** 是一个高性能的内存数据库，主要用于缓存和数据存储，适合处理高并发的读取和写入操作。
   >
   > **ZooKeeper** 是一个分布式协调工具，用于在分布式系统中管理一致性、配置和服务发现，确保多个节点之间的协调与同步。
   >
   > **Etcd** 是一个 Go 语言实现的、开源的、分布式的键值存储系统，它主要用于分布式系统中的服务发现、配置管理和分布锁等场景。
   >
   > **“分布式”**是指将计算、存储和服务等资源分散在多个物理或虚拟的节点上，这些节点通过网络相互通信和协作，完成共同的任务或提供某项服务。

2. 负载均衡

   问题 2：如果有多个服务提供者，消费者应该调用哪个服务提供者呢？

   为服务调用方增加负载均衡能力，通过指定不同的算法来决定调用哪一个服务提供者，比如轮询、随机、根据性能动态调用等。

   > **负载均衡**能力是指分布式系统或网络架构中，能够有效地将任务、请求或工作负载均匀地分配到多个计算节点或服务器上的能力。
   >
   > - **轮询**：将请求按照顺序依次分配给不同的服务器，不考虑服务器的负载情况，适用于负载较为均衡的场景。
   > - **加权轮询**：为不同的服务器设置不同的权重，权重高的服务器会分配到更多的请求，适用于服务器性能不均的情况。
   > - **最小连接数**：将请求分配给当前连接数最少的服务器，确保负载相对均衡。
   > - **IP哈希**：根据客户端的IP地址来确定请求分配给哪台服务器，适用于需要确保同一客户端每次请求都落在同一台服务器上的场景。

3. 容错机制

   问题 3：如果服务调用失败，应该如何处理呢？

   为了保证分布式系统的高可用，我们通常会给服务的调用增加一定的容错机制，比如失败重试、降级调用其他接口等。

4. 其他

   - 服务提供者下线了怎么办？需要一个失效节点剔除机制 `destroy()`。
   - 服务消费者每次都从注册中心拉取信息，性能会不会很差？可以使用缓存来优化性能。
   - 如何优化 RPC 框架的传输通讯性能？比如选择合适的网络框架、自定义协议头、节约传输体积等。
   - 如何让整个框架更利于扩展？比如使用 Java 的 SPI 机制、配置化等。



# 2. 环境配置

## 2.1 rpc-easy 4

环境：IDEA Community 2023.3.3、JDK 11.0.26、Maven 3.9.9、Windows 11。

初始化：Empty Project（LightRPC）、Maven Archetype（quickstart）。

添加的 Maven Module：example-common、example-consumer、example-provider、rpc-easy。

## 2.2  rpc-core 5-12

环境：IDEA Community 2023.3.3、JDK 11.0.26、Maven 3.9.9、Windows 11、Etcd 3.5.12、EtcdKeeper 0.7.8、ZooKeeper 3.8.3。

初始化：复制 rpc-easy，并修改 pom.xml。

添加的 Maven Module：rpc-core。

## 2.3 rpc-spring-boot-starter 12

环境：IDEA Ultimate 2023.3.3、JDK 11.0.26、Maven 3.9.9、Windows 11、Spring Boot 2.6.13。

初始化：Server URL（start.aliyun.com）、Sring Configuration Processor。

添加的 Spring Initializr Module：example-springboot-consumer、example-springboot-provider、rpc-spring-boot-starter。



# 3. 消费/提供者

1. 公共模块（example-common）：跨消费者与提供者的共享库，存放双方需共同依赖的接口和模型，减少重复代码。

- User：用户实体类，需要实现序列化接口。

- UserService：用户服务接口，获取用户。



2. 服务消费者（example-consumer）：需要调用服务的模块。

>  pom.xml：基于 example-common 和 rpc-easy/rpc-core。

- EasyConsumerExample：简易的服务消费者启动类。
- ConsumerExample：服务消费者启动类。



3. 服务提供者（example-provider）：真正实现了接口的模块。

> pom.xml：基于 example-common 和 rpc-easy/rpc-core。

- EasyProviderExample：简易的服务提供者启动类。

- ProviderExample：服务提供者启动类。

- UserServiceImpl：用户服务实现类。



4. 示例 Spring Boot 服务消费者（example-springboot-consumer）：

> pom.xml：基于 example-common 和 rpc-spring-boot-starter

- ExampleSpringbootProviderApplication：示例 Spring Boot 服务提供者应用。
- UserServiceImpl：用户服务实现类。



5. 示例 Spring Boot 服务提供者（example-springboot-provider）：

> pom.xml：基于 example-common 和 rpc-spring-boot-starter

- ExampleSpringbootConsumerApplication：示例 Spring Boot 服务消费者应用。
- ExampleServiceImpl：服务消费者实例实现类。



# 4. 简易版 RPC 框架

1. 代理对象 - proxy

> 静态代理：指为每一个特定类型的接口/对象，编写一个代理类（实现类）。非常麻烦，灵活性差。
>
> 动态代理：根据要生成的对象的类型，自动生成一个代理对象。
>
> -  **JDK 动态代理**：简单易用、无需引入额外的库，但只能对接口进行代理。
> -  基于字节码生成的动态代理（如 CGLIB）：更灵活、可以对任何类进行代理，但性能略低于 JDK 动态代理。

- UserServiceProxy（example-consumer）：静态代理，实现 UserService 接口和 getUser 方法（构造 HTTP 请求调用服务提供者）。
- ServiceProxy JDK：动态代理，需要实现 InvocationHandler 接口的 invoke 方法。
- ServiceProxyFactory：动态代理工厂，通过 `Proxy.newProxyInstance` 方法为指定类创建动态代理对象。



2. 请求客户端



3. 序列化/反序列化 - serializer

- Serializer：序列化接口，提供了序列化和反序列化方法。

> 序列化：将 Java 对象转为可传输的字节数组。
>
> 反序列化：将字节数组转换为 Java 对象。

- JdkSerializer：Java 原生序列化器，实现了 Serializer 接口。



4. Web 服务器 - server

> Web 服务器种类：Tomcat（Spring Boot 内嵌）、Netty（NIO 框架）、Vert.x（NIO 框架）。

- HttpServer：web 服务器接口，定义统一的启动服务器方法，便于后续的扩展。可以用不同的底层通信框架（Vert.x/Netty）。

- VertxHttpServer：基于 Vert.x 实现的 web 服务器，能够监听指定端口并处理请求。

> Vert.x 是一个基于 Netty 实现的轻量级、响应式、模块化的应用开发框架（封装更高）。
>
> - 异步非阻塞，天然支持响应式编程模型；
> - 内置 HTTP、WebSocket 等；
> - 支持多语言：Java、JS、Ruby 等；
> - 模块化，易于扩展。  



5. 请求处理器 - model

> 请求处理器是 RPC 框架的实现关键，其作用是：处理接收到的请求，并根据请求参数找到对应的服务和方法，通过反射实现调用，最后封装返回结果并响应请求。

- RpcRequest：请求类，封装调用所需的信息，如服务名称、方法名称、调用参数的类型列表、参数列表。**消费者 → 提供者**
- RpcResponse：响应类，封装调用方法得到的返回值、以及调用的信息（比如异常情况）等。**提供者 → 消费者**
- VertxHttpServerHandler（server）：请求处理器。

> 业务流程如下:
>
> 1. 反序列化：反序列化 PRC 请求为对象，并从请求对象中获取参数；
> 2. 查找服务：根据服务名称从本地注册器中获取到对应的服务实现类；
> 3. 反射调用：通过反射机制调用方法，得到返回结果；
> 4. 序列化：对返回结果进行封装和序列化，并写入到响应中。
>
> 需要注意，不同的 web 服务器对应的请求处理器实现方式也不同。



6. 本地服务注册器 - registry

- LocalRegistry：本地服务注册器，key(服务名)-value(实现类)。

> 注意，本地服务注册器和注册中心的作用是有区别的。
>
> - 注册中心：侧重于管理注册的服务提供者地址、提供服务提供者的信息给消费者；
>
> - 本地服务注册器：根据服务名（key）获取到对应的实现类（value），是完成调用必不可少的模块。



## 4.1 测试验证

> [!IMPORTANT]
>
> 启动服务消费者（EasyConsumerExample）的时候，服务提供者（EasyProviderExample）要保持运行，<mark>**不能中断**</mark>。

测试运行顺序：`EasyProviderExample` > `EasyConsumerExample`。

以 Run/Debug 模式启动服务提供者（EasyProviderExample），执行main方法。

![image-20250503181924317](https://gitee.com/Koletis/pic-go/raw/master/202505031819382.png)

以 Run/Debug 模式启动服务消费者（EasyConsumerExample），执行main方法。

![image-20250503181642273](https://gitee.com/Koletis/pic-go/raw/master/202505031816347.png)

> 注：
>
> Run 模式启动没有，Debug 模式有：
> Connected to the target VM, address: '127.0.0.1:X', transport: 'socket'
> Disconnected from the target VM, address: '127.0.0.1:X', transport: 'socket'



# 5. 全局配置加载

**需求分析**：我们需要设计并实现一个全局配置加载功能，并维护一个全局配置对象，以便 RPC 框架能够轻松从配置文件中读取配置内容，快速且一致地获取配置信息。

下图是与简易 RPC 模块的交互图：

![image-20250512091348761](https://gitee.com/Koletis/pic-go/raw/master/202505120913858.png)

**配置文件**：一种**存储程序参数或设置**的文本文件（如 `.properties`、`.yml`、`.json` 等），用于在不修改代码的情况下动态调整程序行为。

**全局配置加载功能**：

- **读取**配置文件，可以使用 Java 的 Properties 类、Hutool 的 Setting 模块。
- **解析**配置内容（如将 `.yml` 转换成 Java 对象）。
- **校验**配置合法性。
- **填充**到全局配置对象中。

**全局配置对象**：

- **存储**所有配置项。
- **提供 Getter 方法**供其他模块调用（静态配置：无接口调用、动态配置：有接口调用）。
- 在引入 RPC 框架的项目启动时，从配置文件中读取配置并创建对象实例，之后加可以集中地从这个对象中获取配置信息，而不用每次加载配置时再重新读取配置、创建新对象，减少了性能开销。

**其他模块**（如 Web 服务器、请求处理器）：

- **直接访问全局配置对象**获取参数，而不是自己读配置

## 5.1 开发实现

1. RpcApplication：相当于 Holder，存放了项目全局用到的变量。

> `Holder` 类通常作为一个容器，用来传递和保存值。它通常是一个泛型类，能容纳任意类型的对象。

- **触发加载**：调用 `ConfigUtils.loadConfig()`，从配置文件读取配置并解析成 `RpcConfig` 对象。
  - 支持默认配置`init()`和自定义配置`init(RpcConfig)`（存在容错机制：捕获配置加载异常，降级为默认配置`new RpcConfig()`）。

- **读取存储**：将加载后的 `RpcConfig` 对象存储到静态变量中，供其他模块通过 `getRpcConfig()` 访问。

  - 通过**双检锁单例模式**（`volatile + synchronized`）保证 `RpcConfig` 全局唯一。

  - 提供访问入口（`getRpcConfig()`）。

> **双检锁单例模式**是一种常用的单例模式实现方式，旨在确保在多线程环境中只有一个实例被创建，同时提高性能。
>
> 1. **单例模式**的目的是确保一个类只有一个实例，并提供一个全局的访问点。
> 2. **双检锁**的工作原理：
>    - 第一次检查：在方法的入口处检查实例是否已经创建。如果已经创建，直接返回现有的实例，不需要进行同步操作。
>    - 加锁：如果实例为空，则进行加锁，保证在同一时刻只有一个线程能进入创建实例的代码块。
>    - 第二次检查：锁定后，再次检查实例是否为空。这样做是为了防止多个线程同时通过第一次检查后进入加锁区域，在锁被释放后重新创建多个实例。

2. config

- RpcConfig：RPC 框架配置项，保存全局配置基本属性。

3. utils

- ConfigUtils：读取配置文件并返回配置对象，可以简化调用。

4. constant

- RpcConstant：接口，用于存储 RPC 框架默认配置项。

5. 配置文件

- example-consumer > resources > application.properties
- example-provider  > resources > application.properties

下图是代码间的层级结构：

![image-20250512092227939](https://gitee.com/Koletis/pic-go/raw/master/202505120922074.png)

## 5.2 测试验证

测试运行顺序：`ProviderExample` > `ConsumerExample`。



## 5.3 Netty HTTP 开发实现


1. rpc-core 的 `pom.xml` 中引入 Netty（简化了 TCP、HTTP 协议的编写，并且支持高效的异步通信）、Jackson（序列化 / 反序列化）

2. Web 服务器 - server

> Netty（NIO 框架）代替 Vert.x（NIO 框架）。

- NettyHttpServer：Netty HTTP 服务器。

- NettyHttpServerHandler：Netty HTTP 请求处理器。

> Netty 是一个基于 Java NIO 的高性能、异步事件驱动的网络通信框架。
>
> - 异步非阻塞，基于 NIO；
> - 高性能、低延迟（得益于零拷贝、内存池化等优化机制）；
> - 可高度自定义管道处理器（ChannelPipeline）。



# 6. 接口 Mock

**需求分析**：RPC 框架的核心功能是调用远程服务。但是在实际开发和测试过程中，有时可能无法直接访问真实的远程服务，或者访问真实的远程服务可能会产生不可控的影响，例如网络延迟、服务不稳定等。在这种情况下，就需要使用 Mock 服务来模拟远程服务的行为，以便进行接口的测试、开发和调试。

Mock 是指模拟对象，通常用于**测试代码**中，特别是在单元测试中，便于开发者调用服务接口、跑通业务流程，而不依赖于真实的远程服务。

轻量级 Mock：消费者调用Mock代理，直接返回预设值。

![image-20250512110615095](https://gitee.com/Koletis/pic-go/raw/master/202505121106204.png)

深度Mock：模拟完整 RPC 调用链，但无真实网络通信。
具体流程：Mock 服务注册到本地服务注册器。然后请求客户端接收 Mock 代理请求，从本地注册器获取 Mock 服务地址，返回 Mock 响应。

![image-20250512110702451](https://gitee.com/Koletis/pic-go/raw/master/202505121107545.png)

1. proxy

- MockServiceProxy：Mock 服务代理（JDK 动态代理）。

## 6.1 测试验证

> [!IMPORTANT]
>
> 启动服务消费者（ConsumerExample）的时候，服务提供者（ProviderExample）要保持运行，<mark>**不能中断**</mark>。

测试运行顺序：`ProviderExample` > `ConsumerExample`。

`ConsumerExample`：

![image-20250512110931762](https://gitee.com/Koletis/pic-go/raw/master/202505121109855.png)



# 7. 序列化器与 SPI 机制

> [!NOTE]
>
> 在 `ProviderExample` 和 `ConsumerExample` 的 `application.properties`  中修改 `rpc.serializer` 来切换序列化器。

**需求分析：**

**序列化器的作用**：在请求和响应过程中，都需要进行参数传输。由于 Java 对象存在于 JVM 虚拟机中，若要在其他位置存储访问或在网络中传输，就需要进行序列化和反序列化处理。

在 *4.* 编写了通用的序列化器接口 `Serializer`，并且已经实现了基于 Java 原生序列化的序列化器 `JdkSerializer`。但是对于一个完善的 RPC 框架，还要思考以下 3 个问题：

1. 有没有更好的序列化器实现方式？
2. 如何让框架使用者能够选择序列化器？
3. 如何让框架使用者自定义序列化器？

下图为思路流程：

![image-20250513140819973](https://gitee.com/Koletis/pic-go/raw/master/202505131408123.png)

**针对第一个问题 - 序列化器实现方式**：序列化方式包括  Java 原生序列化（JDK）、JSON、Hessian、Kryo、protobuf 等。

|          | 优点                                                         | 缺点                                                         |
| :------: | ------------------------------------------------------------ | :----------------------------------------------------------- |
|   JSON   | 1. 易读，便于理解和调试。<br/>2. 支持跨语言，几乎所有编程语言都有 JSON 的解析和生成库。 | 1. 序列化后的数据量相对较大，因为 JSON 使用文本格式存储数据，需要额外的字符表示键、值和数据结构。<br/>2. 不能很好地处理复杂的数据结构和循环引用，可能导致性能下降或者序列化失败。 |
| Hessian  | 1. 二进制序列化，序列化后的数据量较小，网络传输效率高。<br/>2. 支持跨语言，适用于分布式系统中的服务调用。 | 1. 性能较 JSON 略低，因为需要将对象转换为二进制格式。<br/>2. 对象必须实现 Serializable 接口，限制了可序列化的对象范围。 |
|   Kryo   | 1. 性能高，序列化和反序列化速度快。<br/>2. 支持循环引用和自定义序列化器，适用于复杂的对象结构。<br/>3. 无需实现 Serializable 接口，可以序列化任意对象。 | 1. 不支持跨语言，只适用于 Java。<br/>2. 对象的序列化格式不够友好，不易读和调试。 |
| protobuf | 1. 高效的二进制序列化，序列化后的数据量极小。<br/>2. 支持跨语言，并且提供了多种语言的实现库。<br/>3. 支持版本化和向前 / 向后兼容性。 | 1. 配置相对复杂，需要先定义数据结构的消息格式。<br/>2. 对象的序列化格式不易读和调试。 |

**针对第二个问题 - 动态使用序列化器**：只需要定义一个 `<序列化器名称, 序列化器实现类对象>` 的 Map，然后根据名称从 Map 中获取对象即可。

**针对第三个问题 - 自定义序列化器**：只要 RPC 框架能够读取到用户自定义的类路径，然后加载这个类，作为 Serializer 序列化器接口的实现即可。



**如何实现自定义序列化器呢？**—— SPI 机制。

SPI ：是 Java 提供的一种机制，主要用于实现模块化开发和插件化扩展。

- SPI 允许开发者通过特定的配置文件将自己的实现注册到系统中，系统则通过反射机制在运行时动态加载这些实现，无需修改原始框架的代码，从而实现系统的解耦、提高可扩展性。
- 典型的 SPI 应用场景是 JDBC（Java 数据库连接库），不同的数据库驱动程序开发者可以使用 JDBC 库，然后定制自己的数据库驱动程序。
- 此外，许多主流 Java 开发框架中，几乎都使用到了 SPI 机制，比如 Servlet 容器、日志框架、ORM 框架、Spring 框架。

下图是 API 和 SPI 的对比图：

![image-20250512134433029](https://gitee.com/Koletis/pic-go/raw/master/202505121344146.png)

```plaintext
API 流程：
服务消费者（调用方） → 调用 → UserService接口 ← 实现 ← 服务提供者（实现方）

SPI 流程：
Dubbo框架（调用方） → 调用 → LoadBalance接口 ← 实现 ← 开发者（实现方）
```

**如何实现 SPI？**——系统默认实现、自定义实现。

- 系统默认实现：直接使用 Java 中 SPI 相关的 API 接口。
  - 在需要使用 SPI 的文件中导入 `import java.util.ServiceLoader;` 包；
  - 并在 `resources/META-INF/services/com.wheelproject.rpc.serializer.Serializer` 内填写自定义的接口实现类完整类路径（全限定名）。
- 自定义实现：

> 思路：
>
> - 虽然 Java 自带的 SPI 实现机制相对简单，但它存在一个限制：**当我们定义了多个接口实现类时，无法直接在框架中指定使用哪一个具体实现**。SPI 默认的做法是遍历所有实现类并加载，**无法满足“通过配置快速指定某个序列化器”这类需求**。
> - 因此，我们需要自行实现一套 SPI 机制，使其能够**根据配置动态加载指定的实现类**。→ **动态使用序列化器**
> - 例如，读取如下配置文件，构建出一个 `<序列化器名称, 序列化器实现类对象>` 的映射，之后就可以根据用户配置的序列化器名称动态加载指定实现类对象。
>
> ```
> jdk=com.wheelproject.rpc.serializer.JdkSerializer
> json=com.wheelproject.rpc.serializer.JsonSerializer
> hessian=com.wheelproject.rpc.serializer.HessianSerializer
> kryo=com.wheelproject.rpc.serializer.KryoSerializer
> ```

## 7.1 自定义实现

1. serializer：

- JsonSerializer：JSON 序列化器的实现相对复杂，要考虑一些对象转换的兼容性问题，比如 Object 数组在序列化后会丢失类型。
- HessianSerializer：Hessian 实现简单。
- KryoSerializer：Kryo 本身是线程不安全的，所以需要使用 ThreadLocal 保证每个线程有一个单独的 Kryo 对象实例。
- SerializerKeys：定义序列化器名称常量。
- SerializerFactory：用于获取序列化器对象。序列化器对象是可以复用的，没必要每次执行序列化操作前都创建一个新对象，所以使用设计模式中的 **工厂模式 + 单例模式** 来简化创建和获取序列化器对象的操作。

> 工厂模式：是一种创建型设计模式，它提供了一种创建对象的最佳方式，而无需暴露创建逻辑。类似封装的思想。
> 单例模式：确保一个类只有一个实例，并提供一个全局访问点。



2. spi：

- SpiLoader：读取配置并加载实现类。
  - **读取配置**：扫描指定路径，读取每个配置文件，获取到 `<键名, 实现类>` 信息并存储在 Map 中；
  - **加载实现类**：根据用户传入的接口名、键名（`<接口名, <键名, 实现类>>`），从 Map 中找到对应的实现类，然后通过反射获取到实现类对象。
  - 此外，维护一个对象实例缓存，创建过一次的对象从缓存中读取即可。

各个代码关系如下：

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505161119643.png" alt="image-20250516111920528" style="zoom:80%;" />



## 7.2 测试验证

> [!IMPORTANT]
>
> 启动服务消费者（ConsumerExample）的时候，服务提供者（ProviderExample）要保持运行，<mark>**不能中断**</mark>。
>
> 配置文件 `application.properties` 中的 `rpc.serializer` 必须一致。
>
> ![image-20250513124348709](https://gitee.com/Koletis/pic-go/raw/master/202505131243921.png)

测试运行顺序：`ProviderExample` > `ConsumerExample`。

以下均为 `ConsumerExample` 运行结果：

- jdk、json：

![image-20250513124558464](https://gitee.com/Koletis/pic-go/raw/master/202505131245573.png)

- kryo：WARNING 与 **Java 9+** 版本中的 **模块系统（JPMS）** 相关的。

![image-20250513124627494](https://gitee.com/Koletis/pic-go/raw/master/202505131246630.png)

- hessian：

1. WARNING 与 **Java 9+** 版本中的 **模块系统（JPMS）** 相关的。

2. （2025/5/13 <mark>未解决</mark>）`at com.sun.proxy.$Proxy0.getNumber(Unknown Source)`：输出代码按以下内容修改后，没有出现错误，但是无输出（类似第二幅图）。

```java
// ConsumerExample 
// 原输出代码
long number = userService.getNumber();
System.out.println(number);
// 修改后
System.out.println(userService.getNumber());
```

![image-20250513125015577](https://gitee.com/Koletis/pic-go/raw/master/202505131250703.png)

 2.1（2025/5/13）在 `UserService` 中，将 `getNumber` 输出按以下内容修改，没有出现错误，但是依旧无输出。

```Java
// UserService
// 原输出代码
default short getNumber(){
    return 12345;
}
// 修改后
default short getNumber(){
    return (short)12345;
}
```

![image-20250513125348739](https://gitee.com/Koletis/pic-go/raw/master/202505131253871.png)



# 8. 注册中心

**注册中心**：RPC 框架的核心模块，目的是帮助服务消费者获取到服务提供者的调用地址，而不是将调用地址硬编码到项目中。主流的注册中心实现中间件有 Redis、ZooKeeper、Etcd。

核心功能：

1. 数据分布式存储：实现注册信息数据的集中管理，包括数据存储、读取、共享；

2. **服务注册**：服务提供者注册地址信息；

3. **服务发现**：服务消费者从注册中心拉取服务提供者信息；

4. **动态负载均衡**：消费者根据策略选择提供者；

5. 心跳检测：定期检查服务提供者的存活状态；

6. 服务注销：支持手动剔除节点，或自动剔除失效节点；

7. 其他：容错机制、服务消费者缓存策略等。

下图是简单的服务消费者获、服务提供者、注册中心的关系：

![image-20250602133833537](https://gitee.com/Koletis/pic-go/raw/master/202506021338755.png)

下图是注册中心在简易版 RPC 框架中的位置：

![image-20250601111724279](https://gitee.com/Koletis/pic-go/raw/master/202506011117543.png)

## 1. Etcd 开发实现

这里选择使用 Etcd、层级结构查询。

> Redis：列表结构查询。
>
> ZooKeeper、Etcd：层级结构查询。

1. model

- ServiceMetaInfo：服务元信息（注册信息），提供基本属性、基本方法。



2. config

- RegistryConfig：RPC 框架注册中心配置项。



3. registry

- Registry：注册中心（接口）。

- EtcdRegistry：Etcd 注册中心。

- RegistryKeys：注册中心键名常量。

- RegistryFactory：获取注册中心对象。

各代码关系如下：

![image-20250602133801802](https://gitee.com/Koletis/pic-go/raw/master/202506021338091.png)

### 1.1 测试验证

> [!IMPORTANT]
>
> 每次重新测试的时候，都要先重新运行 `ProviderExample` ，再运行 `ConsumerExample` 。
>
> `ProviderExample` 内：
>
> ```java
> // 以下两句必须写，否则：http://null:null。
> serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
> serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
> // 以下两句可以不写，因为重写了 getServiceAddress 方法。
> serviceMetaInfo.setServiceAddress("http://" + rpcConfig.getServerHost() + ":" + rpcConfig.getServerPort());
> serviceMetaInfo.setServiceAddress(rpcConfig.getServerHost() + ":" + rpcConfig.getServerPort());
> ```

测试运行顺序：`etcd.exe` > `etcdkeeper.exe` >`ProviderExample` > `ConsumerExample` > `RegistryTest` 。

`etcd.exe`：

![image-20250516115600146](https://gitee.com/Koletis/pic-go/raw/master/202505161156285.png)

`etcdkeeper.exe`，需要切换端口：

```cmd
etcdkeeper --p 8082
```

注：红框是运行 Java 后显示的结果。

![image-20250517133241841](https://gitee.com/Koletis/pic-go/raw/master/202505171332987.png)

（运行 Java 后显示）访问网页`http://127.0.0.1:8082/etcdkeeper`：

![image-20250517133527348](https://gitee.com/Koletis/pic-go/raw/master/202505171335470.png)

（运行 Java 后显示）以下是树形展示结果：

![image-20250517131511743](https://gitee.com/Koletis/pic-go/raw/master/202505171315898.png)

`ProviderExample`：8080

![image-20250516115635347](https://gitee.com/Koletis/pic-go/raw/master/202505161156486.png)

 `ConsumerExample`：8081

![image-20250516115651332](https://gitee.com/Koletis/pic-go/raw/master/202505161156459.png)

 `RegistryTest`：

![image-20250516115658533](https://gitee.com/Koletis/pic-go/raw/master/202505161156671.png)

## 2. 优化

优化点：

1. 心跳检测和续期机制。
2. 服务节点下线机制。
3. 消费端服务缓存。
4. 基于 ZooKeeper 的注册中心实现。

通俗来说：

1. 高可用性：保证注册中心本身不会宕机。
2. 数据一致性：服务提供者如果下线了，注册中心需要及时更新，剔除下线节点。否则消费者可能会调用到已经下线的节点。
3. 性能优化：服务消费者每次都需要从注册中心获取服务，可以使用缓存进行优化。
4. 可扩展性：实现更多其他种类的注册中心。

### 2.1 心跳检测和续期机制

> 心跳检测（HeartBeat）是一种用于监测系统是否正常工作的机制。它通过定期发送心跳信号（请求）来检测目标系统的状态。如果接收方在一定时间内没有收到心跳信号或者未能正常响应请求，就会认为目标系统故障或不可用，从而触发相应的处理或告警机制。
>
> 关键点：**定时、网络请求**。

因为 Etcd 自带 key 过期机制，所以实现心跳检测会更简单一些，具体思路为：给节点（服务提供者）注册信息一个过期时间（TTL），让节点定期续期，重置自己的 TTL。如果节点一直不续期，Etcd 就会删除过期 key。一句话总结：到时间还不续期就是寄了。

在 Etcd 中，要实现心跳检测和续期机制，可以遵循如下步骤：

1. 服务提供者向 Etcd 注册自己的服务信息，并在注册时设置 TTL。
2. Etcd 在接收到服务提供者的注册信息后，会自动维护服务信息的 TTL，并在 TTL 过期时删除该服务信息。
3. 服务提供者定期请求 Etcd 续签自己的注册信息，重写 TTL。

需要注意的是，续期时间（即服务节点发送心跳的间隔）一定要小于 TTL，允许一次容错的机会。

- 允许至少一次容错机会：通常设置 `续期时间 = TTL / 2`（如 TTL=10s，续期间隔=5s），这样即使 **某次心跳失败**，节点仍有额外时间重试。

#### 2.1.1 开发实现

1. registry

- Registry：增加心跳检测方法 `void heartBeat ();`。

- EtcdRegistry：实现 `void heartBeat ();`，并维护过期字典 `localRegisterNodeKeySet`。

#### 2.1.2 测试验证

> [!IMPORTANT]
>
> 对于`RegistryTest`，应该运行单个方法（eg：`Run 'heartBeat()'`），而不是直接运行整个类（`Run 'RegistryTest'`）。

测试运行顺序：`etcd.exe` > `etcdkeeper.exe` >`ProviderExample` > `ConsumerExample` > `RegistryTest` 。

`etcd.exe`：

![image-20250516115600146](https://gitee.com/Koletis/pic-go/raw/master/202505161156285.png)

`etcdkeeper.exe`，需要切换端口：

```cmd
etcdkeeper --p 8082
```

（运行 Java 后显示）访问网页 `http://127.0.0.1:8082/etcdkeeper`：

![image-20250521150953069](https://gitee.com/Koletis/pic-go/raw/master/202505211509241.png)

**使用可视化工具观察节点底部的过期时间，当 TTL 到 20 左右的时候，又会重置为 30，说明心跳检测和续期机制正常执行。**

`ProviderExample`：8080

![image-20250521151040846](https://gitee.com/Koletis/pic-go/raw/master/202505211510976.png)

 `ConsumerExample`：8081

![image-20250521151222144](https://gitee.com/Koletis/pic-go/raw/master/202505211512271.png)

 `RegistryTest`：

![image-20250521151527848](https://gitee.com/Koletis/pic-go/raw/master/202505211515990.png)

### 2.2 服务节点下线机制

当服务提供者节点宕机时，应该从注册中心移除掉已注册的节点，否则会影响消费端调用。

> 服务节点下线分为：
>
> - 主动下线：服务提供者项目正常退出时，主动从注册中心移除注册信息。
> - 被动下线：服务提供者项目异常退出时，利用 Etcd 的 key 过期机制自动移除。

利用 JVM 的 ShutdownHook 能实现主动下线（被动下线，利用 Etcd 机制即可 ）。

> JVM 的 ShutdownHook 是 Java 虚拟机提供的一种机制，允许开发者在 JVM 即将关闭之前执行一些清理工作或其他必要的操作，例如关闭数据库连接、释放资源、保存临时数据等。
> Spring Boot 也提供了类似的停机能力。

#### 2.2.1 开发实现

1. RpcApplication：`init` 方法（初始化方法）中，注册 `ShutdownHook`，当程序正常退出时会执行注册中心（Registry）的 `destroy` 方法。

2. registry

- EtcdRegistry：实现 `destroy();`。

#### 2.2.2 测试验证

测试运行顺序：`ProviderExample` > `ConsumerExample`。

①、②：`ProviderExample` 停止前。

③、④：`ProviderExample` 停止后。

![image-20250521160018218](https://gitee.com/Koletis/pic-go/raw/master/202505211600394.png)

### 2.3 消费端服务缓存

正常情况下，服务节点（服务提供者）信息列表的更新频率是不高的，所以在服务消费者从注册中心获取到服务节点信息列表后，完全可以缓存在本地，下次就不用再请求注册中心获取了，能够提高性能。

#### 2.3.1 开发实现

1、增加本地缓存：用列表来实现本地缓存，存储服务信息、提供缓存操作（包括：写缓存、读缓存、清空缓存）。

1. registry：

- RegistryServiceCache（已停用）：本地缓存，用于存储服务提供者信息的本地缓存。
- RegistryServiceMultiCache：注册中心服务本地缓存（支持多个服务）。

2、使用本地缓存

- registry > EtcdRegistry：增加本地缓存对象，`serviceDiscovery` 先从本地缓存对象中获取服务。

3、服务缓存更新——监听机制

当服务注册信息发生变更（比如节点下线）时，需要即时更新消费端缓存。通过 Etcd 的 `Watch` 监听机制可以检测服务注册信息变更信息，当监听的某个 key 发生修改或删除时，就会触发事件来通知监听者。

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505211653372.png" alt="image-20250521165354153" style="zoom: 50%;" />

因为服务消费者维护和使用缓存，所以是服务消费者去 watch。

1. registry

- Registry：增加监听方法 `void watch(String serviceNodeKey);`。

- EtcdRegistry：实现 `void watch(String serviceNodeKey);`，并维护监听集合`watchingKeySet`。

#### 2.3.2 测试验证

准备工作：

1. 修改 `ConsumerExample` 代码。

```java
package com.wheelproject.example.consumer;

import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.proxy.ServiceProxyFactory;
import com.wheelproject.rpc.utils.ConfigUtils;
import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;

import java.io.IOException;
/**
 * 服务消费者示例
 */
public class ConsumerExample {

    public static void main(String[] args) {
        RpcConfig rpc = ConfigUtils.loadConfig(RpcConfig.class, "rpc");
        System.out.println(rpc);

        // 获取代理
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("ky");

        // 第一次调用（会查询注册中心并写入缓存）
        System.out.println("=== 第一次调用 ===");
        callServiceAndPrint(userService, user);

        // 第二次调用（应该命中缓存）
        System.out.println("=== 第二次调用 ===");
        callServiceAndPrint(userService, user);

        // 第三次调用前，等待用户手动停止服务提供者
        System.out.println("=== 请手动停止服务提供者，然后按回车继续 ===");
        waitForUserInput();

        // 第三次调用（会触发监听，清空缓存后重新查询注册中心）
        System.out.println("=== 第三次调用 ===");
        callServiceAndPrint(userService, user);
    }
    
    private static void callServiceAndPrint(UserService userService, User user) {
        User result = userService.getUser(user);
        if (result != null) {
            System.out.println("调用结果: " + result.getName());
        } else {
            System.out.println("调用结果: user == null");
        }
    }

    private static void waitForUserInput() {
        try {
            System.in.read(); // 等待用户按回车
        } catch (IOException e) {
            System.err.println("等待输入时发生错误，继续执行...");
        }
    }
}
```

2. 断点：如图 5 个位置打上断点。

```java
// RegistryServiceCache
this.serviceCache = newServiceCache;
return this.serviceCache;
this.serviceCache = null;
// EtcdRegistry
if (cachedServiceMetaInfoList != null)
registryServiceCache.clearCache();
```

![image-20250521214111599](https://gitee.com/Koletis/pic-go/raw/master/202505212141431.png)

3. （Debug 模式）测试运行顺序：`etcd.exe` > `ProviderExample` > `ConsumerExample`。

第一次调用：

![image-20250521215114953](https://gitee.com/Koletis/pic-go/raw/master/202505212151448.png)

```java
// RegistryServiceCache
List<ServiceMetaInfo> readCache(){
        return this.serviceCache;  // 断点：serviceCache: null
}

// EtcdRegistry > public List<ServiceMetaInfo> serviceDiscovery(String serviceKey)
cachedServiceMetaInfoList: null

// RegistryServiceCache
void writeCache(List<ServiceMetaInfo> newServiceCache){  // newServiceCache: size = 1
        this.serviceCache = newServiceCache;  // 断点：serviceCache: null
}
```

第二次调用：

![image-20250521215735623](https://gitee.com/Koletis/pic-go/raw/master/202505212157053.png)

```java
// RegistryServiceCache
List<ServiceMetaInfo> readCache(){
        return this.serviceCache;  // 断点：serviceCache: size = 1
}

// EtcdRegistry > public List<ServiceMetaInfo> serviceDiscovery(String serviceKey)
if (cachedServiceMetaInfoList != null)  // 断点：cachedServiceMetaInfoList: size = 1
```

第三次调用：

![image-20250521220800644](https://gitee.com/Koletis/pic-go/raw/master/202505212208168.png)

```java
// 手动停止 ProviderExample

// EtcdRegistry > public void watch(String serviceNodeKey)
registryServiceCache.clearCache();  // 断点：registryServiceCache: RegistryServiceCache@3006

// RegistryServiceCache
void clearCache(){
        this.serviceCache = null;  // 断点：serviceCache: size = 1
}

// 回车

List<ServiceMetaInfo> readCache(){
        return this.serviceCache;  // 断点：serviceCache: null
}

// EtcdRegistry > public List<ServiceMetaInfo> serviceDiscovery(String serviceKey)
if (cachedServiceMetaInfoList != null)  // 断点：cachedServiceMetaInfoList: null
    
void writeCache(List<ServiceMetaInfo> newServiceCache){  // newServiceCache: size = 0
        this.serviceCache = newServiceCache;  // 断点：erviceCache: null
}

// Exception in thread "main" java.lang.RuntimeException: 暂无服务地址
```

### 2.4 ZooKeeper 开发实现

> [!NOTE]
>
> 在 `ProviderExample` 和 `ConsumerExample` 的 `application.properties`  中修改 `rpc.registryConfig.registry` 和 `rpc.registryConfig.address` 来切换注册中心。

这里选择 3.8.3 版本。

实现方式：

1. 安装 ZooKeeper：开发编程相关教程.md。
2. 引入客户端依赖 `pom.xml`：curator-x-discovery。
3. 实现接口：ZooKeeperRegistry。
4. SPl 补充 ZooKeeper 注册中心：`com.wheelproject.rpc.registry.Registry`。

#### 2.4.1 测试验证

> [!IMPORTANT]
>
> 需要在 `zoo.cfg` 内配置 `admin.serverPort`，否则测试验证会出现以下报错：
>
> ![image-20250524231123967](https://gitee.com/Koletis/pic-go/raw/master/202505242312139.png)
>
> 将 `NettyHttpServer` 中的队列容量从 128 改为 1024，否则测试验证会出现以下错误：
>
> ![image-20250524232954220](https://gitee.com/Koletis/pic-go/raw/master/202505242329414.png)
>
> ![image-20250524233014112](https://gitee.com/Koletis/pic-go/raw/master/202505242330277.png)

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `ProviderExample` > `ConsumerExample` 。

`zkServer.cmd`：

![image-20250524231655551](https://gitee.com/Koletis/pic-go/raw/master/202505242316740.png)

`zkCli.cmd`：

![image-20250524231709423](https://gitee.com/Koletis/pic-go/raw/master/202505242317686.png)

`ProviderExample`：

![image-20250524233142776](https://gitee.com/Koletis/pic-go/raw/master/202505242331966.png)

`ConsumerExample` ：

![image-20250524233116133](https://gitee.com/Koletis/pic-go/raw/master/202505242331334.png)



# 9. 自定义协议

自定义 RPC 协议可以分为 2 大核心部分：

- 自定义网络传输。
- 自定义消息结构。

1、网络传输设计

目标：选择一个能够高性能通信的网络协议（HTTP、**TCP**、IP等）和传输方式（**单播**、广播、组播、任播）。

HTTP 协议由于请求头较大且无状态，每次请求都需要重新建立连接，影响传输性能。HTTP/1.1 虽然引入了持久连接（Keep-Alive）来复用 TCP 连接，但作为应用层协议，其性能仍不如底层的 TCP 协议。

为了追求更高的传输效率，可以采用 TCP 协议。

此外，单播适用一对一通信，组播适用一对多场景，广播适用局域网通知，任播则能选择最优节点。

2、消息结构设计

目标：用最少的空间（bit）传递需要的信息。

RPC 消息所需的信息：

- 请求头 header：
  - 魔数 magic：作用是安全校验，防止服务器处理非框架发来的消息（类似 HTTP 的安全证书）；
  - 版本号 version：保证请求和响应的一致性（类似 HTTP 协议有1.0/2.0 等版本）；
  - 序列化方式 serializer：告诉服务端和客户端如何解析数据（类似 HTTP 的 Content-Type 内容类型）；
  - 类型 type：标识是请求、响应还是心跳检测等（类似 HTTP 有请求头和响应头）；
  - 状态 status：如果是响应，记录响应的结果（类似 HTTP 的状态码）；
  - 请求id requestId：唯一标识某个请求，因为 TCP 是双向通信的，需要有个唯一标识来追踪每个请求；、
  - 请求体数据长度 bodyLength：能够保证完整地获取 body 内容数据，因为 TCP 存在半包和粘包问题，每次传输的数据可能不完整。

- 请求体 body：要发送 body 内容数据（类似于 HTTP 请求中发送的 RpcRequest）。

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505251838314.png" alt="image-20250525183842012" style="zoom: 67%;" />

实际上，这些信息是紧凑的，请求头信息总长 17 个字节（=136 bit）。也就是说，上述消息结构，本质上就是拼接在一起的一个字节数组。

后续实现需要消息编码器、消息解码器：

- 消息编码器先 new 一个空的 Buffer 缓冲区，然后按顺序向缓冲区依次写入这些数据；
- 消息解码器在读取时按顺序依次读取，还原出编码前的数据。

通过这种约定方式，就不用记录请求头信息。比如 magic 魔数，无需存储“magic"字符串，只要读取第一个字节（前 8 bit）就能获取。

## 9.1 Vert.x TCP 开发实现

**1、网络传输**

1. server > httpServer：之前写的 Vert.x 和 Netty。

2. server > tcpServer

- VertxTcpClient：Vertx TCP 客户端。

- VertxTcpServer：Vertx TCP 服务器。

**2、消息结构**

1. protocol > common

- ProtocolMessage：协议消息结构（请求头+请求体）。

- ProtocolConstant：协议默认常量。

2. protocol > messageEnum

- ProtocolMessageStatusEnum：协议消息状态枚举。

- ProtocolMessageTypeEnum：协议消息类型枚举。

- ProtocolMessageSerializerEnum：协议消息序列化器枚举。

**3、编码器、解码器**

Vert.x 的 TCP 服务器收发的消息是 Buffer 类型，不能直接写入一个对象。因此，需要编码器和解码器，处理 Java 的消息对象和 Buffer。

![image-20250526100726302](https://gitee.com/Koletis/pic-go/raw/master/202505261007521.png)

使用 HTTP 协议：

- 请求时，请求客户端从代理对象中获取请求对象（`RpcRequest`），将其序列化为字节流（RPC 请求），**HTTP 协议封装（请求行、请求头和请求体）、网络发送、HTTP 协议解析**，然后请求处理器反序列化为 `RpcRequest` 对象；即：代理 → 序列化（请求客户端）→ HTTP 请求封装 → 网络 → HTTP 请求解析 → 反序列化（请求处理器）。
- 响应时，请求处理器将 `RpcResponse` 对象序列化为字节流（RPC 响应），**HTTP 协议封装（状态行、响应头和响应体）、网络发送、HTTP 协议解析**，然后请求客户端反序列化为`RpcResponse` 对象，最终返回给代理对象；即：序列化（请求处理器）→ HTTP 响应封装 → 网络 → HTTP 响应解析 → 反序列化（客户端）→ 代理。

使用 TCP 协议：

- 请求时，请求客户端从代理对象中获取请求对象（`RpcRequest`），将其序列化为字节流（RPC 请求），**编码器将字节流写入网络缓冲区（Vert.x - Buffer、Netty - ByteBuf）、网络发送、解码器读取网络缓冲区中的字节流**，然后请求处理器反序列化为 `RpcRequest` 对象；即，代理 → 序列化（请求客户端） → 编码 → 网络 → 解码 → 反序列化（请求处理器）。
- 响应时，请求处理器将响应对象（`RpcResponse`）序列化为字节流（RPC 响应），**编码器将字节流写入网络缓冲区（Vert.x - Buffer、Netty - ByteBuf）、网络发送、解码器读取网络缓冲区中的字节流**，然后请求客户端反序列化为 `RpcResponse` 对象，最终返回给代理对象；即，序列化（请求处理器） → 编码 → 网络 → 解码 → 反序列化（请求客户端）→ 代理 。

![image-20250602153257462](https://gitee.com/Koletis/pic-go/raw/master/202506021532683.png)

1. protocol > codec

- VertxMessageDecoder：Vertx TCP 协议消息解码器。

- VertxMessageEncoder：Vertx TCP 协议消息编码器。

2. 单元测试类：rpc > protocol > VertxProtocolMessageTest

![image-20250602153913571](https://gitee.com/Koletis/pic-go/raw/master/202506021539823.png)

**4、请求处理器 - 服务提供者**

作用：反序列化、查找服务、反射调用、序列化。

1. server > tcpServer

- VertxTcpServerHandler：TCP 请求处理器。

**5、代理对象 - 服务消费者**

1. proxy

- ServiceProxy：改 HTTP 请求为 TCP 请求。

## 9.2 测试验证

`ProviderExample` 代码，改为启动 TCP 服务器。

```java
// 启动 web 服务
// 1. Vertx
// 1.1 Http
// VertxHttpServer httpServer = new VertxHttpServer();
// httpServer.run(RpcApplication.getRpcConfig().getServerPort());
// 1.2 TCP
VertxTcpServer vertxTcpServer = new VertxTcpServer();
vertxTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` >`ProviderExample` > `ConsumerExample` 。

`ProviderExample`：

![image-20250526103247449](https://gitee.com/Koletis/pic-go/raw/master/202505261032670.png)

 `ConsumerExample`：

> （2025/5/26 已解决）出现以下异常，暂时怀疑是 `VertxTcpServer` 中 `handleRequest` 方法没有正确配置。
>
> 注册中心换为 Ectd 一样会出现 `java.lang.RuntimeException: 消息 magic 非法`。
>
> ```java
> private byte[] handleRequest(byte[] requestData) {
>  // 在这里编写处理请求的逻辑，根据 requestData 构造响应数据并返回
>  // 这里只是一个示例，实际逻辑需要根据具体的业务需求来实现
>  return "Hello, client!".getBytes();
> }
> ```
>
> ![image-20250526103334665](https://gitee.com/Koletis/pic-go/raw/master/202505261107547.png)
>
> 【解决方法】`VertxTcpServer` 中处理请求语句如下：
>
> ```java
> server.connectHandler(new VertxTcpServerHandler());
> ```
>
> 这样修改后，注册中心换为 Ectd 也能成功运行。

![image-20250526110441473](https://gitee.com/Koletis/pic-go/raw/master/202505261104690.png)



## 9.3 粘包半包

理想情况下，假如客户端连续 2 次发送的消息是：

```java
// 第一次
Hello, server!Hello, server!Hello, server!Hello, server!
// 第二次
Hello, server!Hello, server!Hello, server!Hello, server!
```

但服务端收到的消息情况可能是：

1）每次收到的数据更少了，这种情况叫做**半包**：

```java
// 第一次
Hello, server!Hello, server!
// 第二次
Hello, server!Hello, server!Hello, server!
```

2）每次收到的数据更多了，这种情况叫做**粘包**：

```java
// 第三次
Hello, server!Hello, server!Hello, server!Hello, server!
```

### 9.3.1 问题演示

- server > tcpServer

VertxTcpClient：`socket.write` 连续发 1000 次消息。

```java
// 发送数据
// socket.write("Hello, server!");
// 测试半包、粘包
for (int i = 0; i < 1000; i++) {
    socket.write("Hello, server!Hello, server!Hello, server!Hello, server!");
}
```

VertxTcpServer：打印出每次收到的消息。

```java
// 处理请求
// server.connectHandler(new VertxTcpServerHandler());
server.connectHandler(socket -> {
    socket.handler(buffer -> {
        String testMessage = "Hello, server!Hello, server!Hello, server!Hello, server!";
        int messageLength = testMessage.getBytes().length;
        if (buffer.getBytes().length < messageLength) {
            System.out.println("半包，length = " + buffer.getBytes().length);
            return;
        }
        if (buffer.getBytes().length > messageLength) {
            System.out.println("粘包，length = " + buffer.getBytes().length);
            return;
        }
        String str = new String(buffer.getBytes(0, messageLength));
        System.out.println(str);
        if (testMessage.equals(str)) {
            System.out.println("good");
        }
    });
});
```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `VertxTcpServer` > `VertxTcpClient` 。

`VertxTcpServer`：

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505261119192.png" alt="image-20250526111941982" style="zoom:80%;" />

`VertxTcpClient` ：

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505261119448.png" alt="image-20250526111912207" style="zoom: 67%;" />

### 9.3.2 解决粘包、半包

解决粘包核心思路：每次只读取指定长度的数据，超过长度的留着下一次接收到消息时再读取。

```java
// 解决粘包问题，只读指定长度的数据
byte[] bodyBytes = buffer.getBytes(17, 17 + header.getBodyLength());
```

解决半包核心思路：在消息请求头中设置请求体的长度，服务端接收时，判断每次消息的长度是否符合预期，不完整就不读，留到下一次接收到消息时再读取。

```java
if (buffer == null || buffer.length() == 0) {
    throw new RuntimeException ("消息 buffer 为空");
}
if (buffer.getBytes().length < ProtocolConstant.MESSAGE_HEADER_LENGTH) {
    throw new RuntimeException ("出现了半包问题");
}
```

Vert.x 解决粘包和半包：使用内置的 `RecordParser` 解决半包粘包，它的作用是保证下次读取到特定长度的字符。

1.  `RecordParser` 读取固定长度内容

```java
// VertxTcpServer
// 处理请求
// server.connectHandler(new VertxTcpServerHandler());
server.connectHandler(socket -> {
    String testMessage = "Hello, server!Hello, server!Hello, server!Hello, server!";
    int messageLength = testMessage.getBytes().length;
    // 为 parser 指定每次读取固定值长度的内容
    RecordParser parser = RecordParser.newFixed(messageLength);
    parser.setOutput(new Handler<Buffer>() {
        @Override
        public void handle(Buffer buffer) {
            String str = new String(buffer.getBytes());
            System.out.println(str);
            if (testMessage.equals(str)) {
                System.out.println("good");
            }
        }
    });
    socket.handler(parser);
});
```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `VertxTcpServer` > `VertxTcpClient` 。

`VertxTcpServer`：

![image-20250526113955537](https://gitee.com/Koletis/pic-go/raw/master/202505261139755.png)

2. `RecordParser` 读取变长内容

思路：

- 先完整读取请求头信息，由于请求头信息长度是固定的，可以使用 `RecordParser` 保证每次都完整读取。
- 再根据请求头长度信息更改 `RecordParser` 的固定长度，保证完整获取到请求体。

```java
// VertxTcpServer
// 处理请求
// server.connectHandler(new VertxTcpServerHandler());
server.connectHandler(socket -> {
    // 为 parser 指定每次读取固定值长度的内容
    RecordParser parser = RecordParser.newFixed(8);
    parser.setOutput(new Handler<Buffer>() {
        // 初始化
        int size = -1;
        // 一次完整的读取（头+体）
        Buffer resultBuffer = Buffer.buffer();
        @Override
        public void handle(Buffer buffer) {
            if (-1 == size) {
                // 读取消息体长度
                size = buffer.getInt(4);
                parser.fixedSizeMode(size);
                // 写入请求头信息到结果
                resultBuffer.appendBuffer(buffer);
            } else {
                // 写入消息体到结果
                resultBuffer.appendBuffer(buffer);
                System.out.println(resultBuffer.toString());
                // 重置一轮
                parser.fixedSizeMode(8);
                size = -1;
                resultBuffer = Buffer.buffer();
            }
        }
    });
    socket.handler(parser);
});
```

```java
// VertxTcpClient
// 发送数据
// socket.write("Hello, server!");
// 测试半包、粘包
for (int i = 0; i < 1000; i++) {
    Buffer buffer = Buffer.buffer();
    String str = "Hello, server!Hello, server!Hello, server!Hello, server!";
    buffer.appendInt(0);
    buffer.appendInt(str.getBytes().length);
    buffer.appendBytes(str.getBytes());
    socket.write(buffer);
}
```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `VertxTcpServer` > `VertxTcpClient` 。

`VertxTcpServer`：

![image-20250526115907274](https://gitee.com/Koletis/pic-go/raw/master/202505261159495.png)

### 9.3.3 封装粘包、半包处理器

采用装饰者设计模式，通过 RecordParser 对基础 Buffer 处理器进行包装扩展。

> 装饰者设计模式（Decorator Pattern）是一种结构型设计模式，它允许向一个现有的对象添加新的功能，同时又不改变其结构。

- server > tcpServer

VertxTcpBufferHandlerWrapper：TCP 消息处理器包装，用于解决半包、粘包问题。

VertxTcpServerHandler：使用 VertxTcpBufferHandlerWrapper 来代替处理请求代码。

VertxTcpClient：存放 `ServiceProxy` 发送请求、处理响应的代码。

- proxy

ServiceProxy：代理对象发送 TCP 请求。

```java
// 2. 发送 TCP 请求
RpcResponse rpcResponse = VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo);
```

### 9.3.4 测试验证

> [!IMPORTANT]
>
> `VertxTcpServer` 中处理请求应该选择调用 `VertxTcpServerHandler`，而不是选择 *8.3.2* 中的代码。不然会出现 `ConsumerExample` 一直被阻塞的情况。
>
> ```java
> // 处理请求
> server.connectHandler(new VertxTcpServerHandler());
> /*
> server.connectHandler(socket -> {
>  // 为 parser 指定每次读取固定值长度的内容
>  RecordParser parser = RecordParser.newFixed(8);
>  parser.setOutput(new Handler<Buffer>() {
>      // 初始化
>      int size = -1;
>      // 一次完整的读取（头+体）
>      Buffer resultBuffer = Buffer.buffer();
>      @Override
>      public void handle(Buffer buffer) {
>          if (-1 == size) {
>              // 读取消息体长度
>              size = buffer.getInt(4);
>              parser.fixedSizeMode(size);
>              // 写入请求头信息到结果
>              resultBuffer.appendBuffer(buffer);
>          } else {
>              // 写入消息体到结果
>              resultBuffer.appendBuffer(buffer);
>              System.out.println(resultBuffer.toString());
>              // 重置一轮
>              parser.fixedSizeMode(8);
>              size = -1;
>              resultBuffer = Buffer.buffer();
>          }
>      }
>  });
>  socket.handler(parser);
> });
> */
> ```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `ProviderExample` > `ConsumerExample` 。

> Etcd 同样能正常运行。

`ProviderExample`：

![image-20250526162755158](https://gitee.com/Koletis/pic-go/raw/master/202505261627434.png)

 `ConsumerExample` ：

![image-20250526162827643](https://gitee.com/Koletis/pic-go/raw/master/202505261628972.png)

## 9.4 Netty TCP 开发实现

> [!NOTE]
>
> 服务提供者：在 `ProviderBootstrap` 内修改代码，来切换通信框架。
>
> ```java
> // 启动 web 服务
> // 1. Vertx
> // 1.1 Http
> // VertxHttpServer httpServer = new VertxHttpServer();
> // httpServer.run(RpcApplication.getRpcConfig().getServerPort());
> // 1.2 TCP
> // VertxTcpServer vertxTcpServer = new VertxTcpServer();
> // vertxTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
> // 2. Netty
> // 2.1 Http
> // NettyHttpServer httpServer = new NettyHttpServer();
> // httpServer.run(RpcApplication.getRpcConfig().getServerPort());
> // 2.2 TCP
> NettyTcpServer nettyTcpServer = new NettyTcpServer();
> nettyTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
> ```
>
> 服务消费者：在 `ServiceProxy` 内修改代码，来切换通信框架。
>
> ```java
> // 1. 发送 Vertx/Netty HTTP 请求
> // doHttpRequest(rpcRequest, selectedServiceMetaInfo)
> // 2.1 发送 Vertx TCP 请求
> // VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
> // 2.2 发送 Netty TCP 请求
> NettyTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
> ```

- protocol > codec

NettyMessageDecoder：Netty TCP 协议消息解码器。

NettyMessageEncoder：Netty TCP 协议消息编码器。

- server > tcpServer

NettyTcpClient：Netty TCP 客户端。

NettyTcpServer：Netty TCP 服务端。

NettyTcpClientHandler：Netty TCP 客户端处理器。

NettyTcpServerHandler：Netty TCP 服务端处理器。

下图为 Netty TCP 交互图，与 RPC 框架的关系可以看 *8.1*：

![image-20250601130659629](https://gitee.com/Koletis/pic-go/raw/master/202506011307017.png)

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `ProviderExample` > `ConsumerExample` 。

`ProviderExample`：

![image-20250601131113271](https://gitee.com/Koletis/pic-go/raw/master/202506011311555.png)

 `ConsumerExample`：

![image-20250601130959407](https://gitee.com/Koletis/pic-go/raw/master/202506011309680.png)



# 10. 负载均衡

**负载均衡**：是一种用来分配网络或计算负载到多个资源上的技术。

1. 何为负载？可以把负载理解为要处理的工作和压力，比如网络请求、事务、数据处理任务等。
2. 何为均衡？把工作和压力平均地分配给多个工作者，从而分摊每个工作者的压力，保证大家正常工作。
3. 目的：是确保每个资源都能够有效地处理负载、增加系统的并发量、避免某些资源过载而导致性能下降或服务不可用的情况。
4. 作用：是从一组可用的服务提供者中选择一个进行调用。对于本项目而言，服务消费者在注册中心中选择一个服务提供者发起请求，而不是每次都请求同一个服务提供者。
5. 实现技术：Nginx（七层负载均衡）、LVS（四层负载均衡）。
6. 主流算法：

1）轮询（RoundRobin）：按照循环的顺序将请求分配给每个服务器，适用于各服务器性能相近的情况。
假如有5台服务器节点，请求调用顺序如下：

```
1,2,3,4,5,1,2,3,4,5
```

2）随机（Random）：随机选择一个服务器来处理请求，适用于服务器性能相近且负载均匀的情况。
假如有5台服务器节点，请求调用顺序如下：

```
3,2,4,1,2,5,2,1,3,4
```

3）加权轮询（Weighted Round Robin）：根据服务器的性能或权重分配请求，性能更好的服务器会获得更多的请求，适用于服务器性能不均的情况。
假如有1台（1）千兆带宽的服务器节点和4台（2、3、4、5）百兆带宽的服务器节点，请求调用顺序可能如下：

```
1,1,1,2, 1,1,1,3, 1,1,1,4, 1,1,1,5
```

4）加权随机（Weighted Random）：根据服务器的权重随机选择一个服务器处理请求，适用于服务器性能不均的情况。
假如有2台（1、2）千兆带宽的服务器节点和3台（3、4、5）百兆带宽的服务器节点，请求调用顺序可能如下：

```
1,2,2,1,3, 1,1,1,2,4, 2,2,2,1,5
```

5）最小连接数（LeastConnections）：选择当前连接数最少的服务器来处理请求，适用于长连接场景。

6）IP Hash：根据客户端IP地址的哈希值选择服务器处理请求，确保同一客户端的请求始终被分配到同一台服务器上，适用于需要保持会话一致性的场景。

7）一致性哈希（Consistent Hashing）：将整个哈希值空间划分成一个环状结构，每个节点或服务器在环上占据一个位置，每个请求根据其哈希值映射到环上的一个点，然后顺时针寻找第一个大于或等于该哈希值的节点，将请求路由到该节点上。

- 一致性哈希环结构如下图：

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505280920682.png" alt="image-20250528092038383" style="zoom: 67%;" />

服务器 A 负责哈希值在 (300, 100]（即(300, MAX]∪[0, 100]）范围内的请求，服务器 B  负责 (100, 200] 的请求，服务器 C 负责 (200, 300] 的请求。上图中，请求 A 会交给服务器 C 来处理。

- 一致性哈希解决了节点下线和倾斜问题：

a）节点下线：当某个节点下线时，其负载会转移到顺时针方向的下一个节点，仅该区间内的请求需要重新路由，其他请求的路由不受影响，因此系统整体稳定性较高。

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505280926040.png" alt="image-20250528092631816" style="zoom: 50%;" />

如上图，服务器 C 下线后，顺时针寻找第下一个节点，选择第一个节点A（服务器A），此时服务器 A 负责 (200, 100]，而服务器 B 接收到的请求保持不变。

b）倾斜问题：通过虚拟节点的引入，将每个物理节点映射到多个虚拟节点上，使得节点在哈希环上的分布更加均匀，减少了节点间的负载差异。

举个例子，节点很少的情况下，环的情况可能如下图：

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505280933464.png" alt="image-20250528093311245" style="zoom:50%;" />

这样就会导致绝大多数的请求都会发给服务器C，而服务器 A 负责 (80, 100]，几乎不会有请求。引入虚拟节点后，环的情况变为下图，每个服务器接受到的请求会更容易平均。

<img src="https://gitee.com/Koletis/pic-go/raw/master/202505281022167.png" alt="image-20250528102234929" style="zoom:55%;" />

## 10.1 开发实现

1、负载均衡器实现

1. loadbalancer

- LoadBalancer：负载均衡器通用接口。

- RoundRobinLoadBalancer：轮询负载均衡器。

- RandomLoadBalancer：随机负载均衡器。

- ConsistentHashLoadBalancer：一致性哈希负载均衡器。

2、支持配置和扩展负载均衡器：**SPI + 工厂模式**

1. loadbalancer

- LoadBalancerKeys：负载均衡器键名常量。

- LoadBalancerFactory：负载均衡器工厂，支持根据 key 从 SPI 获取负载均衡器对象实例。

2. resources > META-INF > rpc > system

- com.wheelproject.rpc.loadbalancer.LoadBalancer：编写负载均衡器接口的 SPI 配置文件。

3. config

- RpcConfig：添加负载均衡器配置。

3、应用负载均衡器

- ServiceProxy：将“固定调用第一个服务节点”改为“调用负载均衡器获取一个服务节点"。

## 10.2 测试验证

> [!IMPORTANT]
>
> 以下测试不适用于 Etcd，因为 `EtcdRegistry` 的 `heartBeat` 涉及 `CronUtil`，`CronUtil` 设计为单例模式，如果在同一 JVM 中启动多个服务实例（如 8080 和 8083），它们会共享静态的 `CronUtil`，导致冲突。
>
> 以下测试配置是 Vertx - TCP。经测试 Vertx - HTTP、Netty - HTTP 也能正常运行。

`ProviderExample`：

```java
public class ProviderExample {
    /**
     * 启动单个服务实例
     * @param port 服务端口号
     */
    public static void startServer(int port) {
        new Thread(() -> {
            try {
                // 初始化RPC框架
                RpcApplication.init();
                // 设置当前实例的端口
                RpcConfig rpcConfig = RpcApplication.getRpcConfig();
                rpcConfig.setServerPort(port);
                // 注册服务
                String serviceName = UserService.class.getName();
                LocalRegistry.register(serviceName, UserServiceImpl.class);
                // 注册服务到注册中心
                RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
                Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
                ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
                serviceMetaInfo.setServiceName(serviceName);
                serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
                serviceMetaInfo.setServicePort(port);  // 使用指定端口
                registry.register(serviceMetaInfo);
                // 启动TCP服务
                VertxTcpServer vertxTcpServer = new VertxTcpServer();
                System.out.println("Starting server on port: " + port);
                vertxTcpServer.run(port);
            } catch (Exception e) {
                System.err.println("Failed to start server on port " + port);
                e.printStackTrace();
            }
        }).start();
    }
    public static void main(String[] args) {
        // 同时启动两个服务实例
        startServer(8080);  // 第一个实例
        startServer(8083);  // 第二个实例
        System.out.println("Two service instances started on ports 8080 and 8081");
        // 保持主线程运行（防止JVM退出）
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

`ZooKeeperRegistry`：

```java
public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
    // 添加调试日志
    System.out.println("Discovering services for key: " + serviceKey);
    // ...原有代码...
}
```

`VertxTcpClient`：

```java
public RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) {
    System.out.println("Calling service at " + serviceMetaInfo.getServiceAddress());
    // ...原有代码...
}
```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > `ProviderExample` > `ConsumerExample` > `LoadBalancerTest`。

 `ProviderExample`：

![image-20250528114531912](https://gitee.com/Koletis/pic-go/raw/master/202505281145323.png)

`ConsumerExample` ：

![image-20250528114731906](https://gitee.com/Koletis/pic-go/raw/master/202505281147149.png)

`LoadBalancerTest`：

![image-20250528114852892](https://gitee.com/Koletis/pic-go/raw/master/202505281148148.png)



# 11. 重试机制

**重试机制**：是分布式系统中常用的一种容错策略，用于应对临时性故障（如网络抖动、服务暂时不可用）。当请求失败时，系统会自动尝试重新发送请求，以提高操作的成功率。

重试机制的核心是重试策略，一般来说，包含以下 4 个考虑点：

1. **重试条件**：什么时候、什么条件下重试？

发生网络抖动时，触发重试。

2. **重试时间/等待**：确定下一次的重试时间。

主流的重试时间算法：

1）固定重试间隔（Fixed Retry Interval）：在每次重试之间使用固定的时间间隔。
比如近 5 次重试的时间点如下：

```
1s 2s 3s 4s 5s
```

2）指数退避重试（Exponential Backoff Retry）：在每次失败后，重试的时间间隔会以指数级增加，以避免请求过于密集。
比如近 5 次重试的时间点如下：

```
1s 3s（多等2s）7s（多等4s）15s（多等8s）31s（多等16s）
```

3）随机延迟重试（Random Delay Retry）：在每次重试之间使用随机的时间间隔，以避免请求的同时发生。

4）可变延迟重试（Variable Delay Retry）：这种策略更“高级”了，根据先前重试的成功或失败情况，动态调整下一次重试的延迟时间。

3. **重试停止**：什么时候、什么条件下停止重试？

主流的停止重试策略：

1）最大尝试次数：一般重试当达到最大次数时不再重试。

2）超时停止：重试达到最大时间的时候，停止重试。

4. **重试工作**：重试后要做什么？

一般来说就是重复执行原本要做的操作，比如发送请求失败了，那就再发一次请求。



需要注意的是，当重试次数超过上限时，往往还要进行其他的操作，比如：

1. 通知告警：让开发者人工介入。

2. 降级容错：改为调用其他接口、或者执行其他操作。

## 11.1 开发实现

1、重试策略实现

1. pom.xml

- Guava-Retrying：实现重试算法。

2. fault > retry

- RetryStrategy：重试策略通用接口。

- NoRetryStrategy：不重试策略。

- FixedIntervalRetryStrategy：固定重试间隔策略。

3. 重试策略单元测试类：rpc > fault > retry > RetryTest

2、支持配置和扩展重试策略：**SPI + 工厂模式**

1. fault > retry

- RetryStrategyKeys：重试策略键名常量。

- RetryStrategyFactory：重试策略工厂，支持根据 key 从 SPI 获取重试策略对象实例。

2. resources > META-INF > rpc > system

- com.wheelproject.rpc.fault.retry.RetryStrategy：编写重试策略接口的 SPI 配置文件。

3. config

- RpcConfig：添加重试策略配置。

3、应用重试功能

- ServiceProxy：为 RPC 请求添加重试机制。

## 11.2 测试验证

> 以下测试配置是 Vertx - TCP - ZooKeeper - NoRetryStrategy。
>
> 经测试 Vertx / Netty、TCP / HTTP、Etcd / ZooKeeper、NoRetryStrategy / FixedIntervalRetryStrategy 均可以。

准备工作：如图 4 个位置打上断点。

```java
// ServiceProxy
RpcResponse rpcResponse = retryStrategy.doRetry(() ->
VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
throw new RuntimeException("调用失败");
// VertxTcpClient
responseFuture.completeExceptionally(new RuntimeException("连接失败："+ result.cause()));
```

![image-20250528213828714](https://gitee.com/Koletis/pic-go/raw/master/202505282138966.png)

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > (Run)`ProviderExample` > (Debug)`ConsumerExample` > `RetryTest`。

 `ConsumerExample`：当断点触发时，手动停止服务提供者。

![image-20250528214122440](https://gitee.com/Koletis/pic-go/raw/master/202505282141768.png)

`RetryTest`：

![image-20250528214721973](https://gitee.com/Koletis/pic-go/raw/master/202505282147268.png)

# 12. 容错机制

容错是指系统在出现异常情况时，通过容错策略保证系统仍然稳定运行，从而提高系统的可靠性和健壮性。

常用的容错策略：

1）Fail-Over 故障转移：一次调用失败后，切换一个其他节点再次进行调用，也算是一种重试。

2）Fail-Back 失败自动恢复：系统的某个功能出现调用失败或错误时，通过其他的方法，恢复该功能的正常。可以理解为降级，比如重试、调用其他服务等。

3）Fail-Safe 静默处理：系统出现部分非重要功能的异常时，直接忽略掉，不做任何处理，就像错误没有发生过一样。

4）Fail-Fast 快速失败：系统出现调用错误时，立刻报错，交给外层调用方处理。

容错实现方式：

1）重试：重试本质上也是一种容错的降级策略，系统错误后再试一次。

2）限流：当系统压力过大、已经出现部分错误时，通过限制执行操作（接受请求）的频率或数量，对系统进行保护。

3）降级：系统出现错误后，改为执行其他更稳定可用的操作。也可以叫做“兜底”或“有损服务”，这种方式的本质是：即使牺牲一定的服务质量，也要保证系统的部分功能可用，保证基本的功能需求得到满足。

4）熔断：系统出现故障或异常时，暂时中断对该服务的请求，而是执行其他操作，以避免连锁故障。

5）超时控制：如果请求或操作长时间没处理完成，就进行中断，防止阻塞和资源占用。

## 12.1 开发实现

1、容错策略实现

1. fault > tolerant

- TolerantStrategy：容错机制通用接口。

- FailSafeTolerantStrategy：Fail-Safe静默处理。

- FailFastTolerantStrategy：Fail-Fast 快速失败。

- FailBackTolerantStrategy：Fail-Back 失败自动恢复。未完成。

- FailOverTolerantStrategy：Fail-Over 故障转移。未完成。

2、支持配置和扩展容错策略：**SPI + 工厂模式**

1. fault > tolerant

- TolerantStrategyKeys：容错策略键名常量。

- TolerantStrategyFactory：容错策略工厂，支持根据 key 从 SPI 获取重试策略对象实例。

2. resources > META-INF > rpc > system

- com.wheelproject.rpc.fault.tolerant.TolerantStrategy：编写容错策略接口的 SPl 配置文件。

3. config

- RpcConfig：添加容错策略配置。

3、应用容错功能

- ServiceProxy：先重试再容错。在发生错误后，首先尝试重试操作，如果重试多次仍然失败，则触发容错机制。

## 12.2 测试验证

> 以下测试配置是 Vertx - TCP - ZooKeeper。

准备工作：如图 3 个位置打上断点。

![image-20250529115209460](https://gitee.com/Koletis/pic-go/raw/master/202505291152781.png)

```java
// ServiceProxy
RpcResponse rpcResponse = retryStrategy.doRetry(() ->
rpcResponse = tolerantStrategy.doTolerant(null, e);
throw new RuntimeException("调用失败");
```

测试运行顺序：`zkServer.cmd` > `zkCli.cmd` > (Run)`ProviderExample` > (Debug)`ConsumerExample` > `RetryTest`。

 `ConsumerExample`：当断点触发时，手动停止服务提供者。

![image-20250529115437596](https://gitee.com/Koletis/pic-go/raw/master/202505291154832.png)



# 13. 启动机制和注解驱动

> IDEA Ultimate：2023.3.3

启动机制设计：把启动代码封装成一个专门的启动类/方法，然后供服务提供者/服务消费者调用即可。

注解驱动设计：

1. 主动扫描：让开发者指定要扫描的路径，然后遍历所有的类文件，针对有注解的类文件，执行自定义的操作。
2. 监听 Bean 加载：在 Spring 项目中，可以通过实现 Bean Post Processor 接口，在 Bean 初始化后执行自定义的操作。

## 13.1 开发实现

1、**启动机制**实现

1. model

- ServiceRegisterInfo：服务注册信息。

2. bootstrap

- ProviderBootstrap：服务提供者启动类。→ 简化 `ProviderExample`。

- ConsumerBootstrap：服务消费者启动类。→ 修改 `ConsumerExample`。

2、SpringBoot Starter **注解驱动**

1）Spring Boot Starter 项目初始化：**rpc-spring-boot-starter**

2）定义注解

1. annotation

- @EnableRpc：用于全局标识项目需要引入 RPC 框架、执行初始化方法。

- @RpcService：服务提供者注解，在需要注册和提供的服务类上使用。

- @RpcReference：服务消费者注解，在需要注入服务代理对象的属性上使用，类似 Spring 中的 @Resource 注解。

3）注解驱动

1. bootstrap

- RpcInitBootstrap：RPC 框架全局启动类。

- RpcProviderBootstrap：RPC 服务提供者启动类。

- RpcConsumerBootstrap：RPC 服务消费者启动类。

4）注册已编写的启动类

在 `@EnableRpc` 前加上注解 `@Import({RpcInitBootstrap.class, RpcProviderBootstrap.class, RpcConsumerBootstrap.class})`。

## 13.2 测试验证

> [!IMPORTANT]
>
> 只能运行 Vertx - TCP - Etcd

1. example-springboot-consumer：示例 Spring Boot 消费者

- ExampleSpringbootProviderApplication：示例 Spring Boot 服务提供者应用。

- UserServiceImpl：用户服务实现类。

2. example-springboot-provider：示例 Spring Boot 提供者

- ExampleSpringbootConsumerApplication：示例 Spring Boot 服务消费者应用。

- ExampleServiceImpl：服务消费者实例实现类。

3. 单元测试类：ExampleServiceImplTest。

测试运行顺序：`etcd.exe` > `ExampleSpringbootProviderApplication` >  `ExampleSpringbootConsumerApplication` > `ExampleServiceImplTest`。

`ExampleSpringbootProviderApplication`：

![image-20250601152420154](https://gitee.com/Koletis/pic-go/raw/master/202506011524538.png)

`ExampleSpringbootConsumerApplication`：

![image-20250601152453866](https://gitee.com/Koletis/pic-go/raw/master/202506011524168.png)

`ExampleServiceImplTest`：

![image-20250601152527993](https://gitee.com/Koletis/pic-go/raw/master/202506011525307.png)

