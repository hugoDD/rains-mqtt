# rains-iot-mqtt

#### 项目介绍
轻量级物联网MQTT服务器, 快速部署, 支持集群(待实现，规划使用sofa-jraft内部选举，不依赖外部).

#### 软件架构说明
基于netty+springboot+ignite技术栈实现
1. 使用netty实现通信及协议解析
2. 使用springboot提供依赖注入及属性配置
3. 使用ignite实现存储, 分布式锁, 集群和集群间通信

#### 项目结构
```
rains-iot-mqtt
  ├── rains-mqtt-auth -- MQTT服务连接时用户名和密码认证
  ├── rains-mqtt-broker -- MQTT服务器功能的核心实现
  ├── rains-mqtt-common -- 公共类及其他模块使用的服务接口及对象
  ├── rains-mqtt-store -- MQTT服务器会话信息, 主题信息等内容的持久化存储
```

#### 功能说明
1. 参考MQTT3.1.1规范实现
2. 完整的QoS服务质量等级实现
3. 遗嘱消息, 保留消息及消息分发重试
4. 心跳机制
5. 连接认证(强制开启)
5. SSL方式连接(不支持非SSL连接)
6. 主题过滤(未完全实现标准: 以#或+符号开头的、以/符号结尾的及不存在/符号的订阅按非法订阅处理, 这里没有参考标准协议)
7. websocket支持
8. 集群功能

#### 快速开始
- [下载已打包好的可运行的jar文件](https://github.com/hugoDD/rains-iot-mqtt/releases)
- 运行jar文件(如果需要修改配置项,可以在同级目录下新建config/application.yml进行修改)
- 打开mqtt-spy客户端, 填写相应配置[下载](https://github.com/eclipse/paho.mqtt-spy/wiki/Downloads)
- 连接端口:8885, websocket端口: 9995 websocket path: /mqtt
- 连接使用的用户名:testOne
- 连接使用的密码: 5E6DFBD55C3898615623C3F9E6BC235DBEB4B212E07E671DE657D2A9820F41777078B2DC2A41368AD8B567557E11537DA3484554934FC6AE4AC4A6B44E50F061
- 连接使用的证书[rains-mqtt-broker.cer](https://gitee.com/recallcode/iot-mqtt-server/releases)

#### 集群使用


#### 自定义 - 连接认证
- 默认只是简单使用对用户名进行RSA密钥对加密生成密码, 连接认证时对密码进行解密和相应用户名进行匹配认证
- 使用中如果需要实现连接数据库或其他方式进行连接认证, 只需要重写`rains-mqtt-auth`模块下的相应方法即可

#### 自定义 - 服务端证书
- 服务端证书存储在`rains-mqtt-broker`的`resources/keystore/mqtt-broker.pfx`
- 用户可以制作自己的证书, 但存储位置和文件名必须使用上述描述的位置及文件名

