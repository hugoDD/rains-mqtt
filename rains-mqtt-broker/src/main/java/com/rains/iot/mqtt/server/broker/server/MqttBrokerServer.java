package com.rains.iot.mqtt.server.broker.server;

import com.rains.iot.mqtt.server.broker.handler.BlotBrokerHandler;
import com.alipay.remoting.*;
import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.*;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.alipay.remoting.rpc.protocol.UserProcessorRegisterHelper;
import com.alipay.remoting.util.NettyEventLoopUtil;
import com.alipay.remoting.util.RemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MqttBrokerServer extends AbstractRemotingServer {
    /** logger */
    private static final Logger logger                  = BoltLoggerFactory
            .getLogger("MqttBrokerServer");
    /** server bootstrap */
    private ServerBootstrap bootstrap;

    /** channelFuture */
    private ChannelFuture channelFuture;

    /** connection event handler */
    private ConnectionEventHandler connectionEventHandler;

    /** connection event listener */
    private ConnectionEventListener connectionEventListener = new ConnectionEventListener();

    /** user processors of rpc server */
    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors          = new ConcurrentHashMap<String, UserProcessor<?>>(
            4);

    /** boss event loop group, boss group should not be daemon, need shutdown manually*/
    private final EventLoopGroup bossGroup               = NettyEventLoopUtil
            .newEventLoopGroup(
                    1,
                    new NamedThreadFactory(
                            "Rpc-netty-server-boss",
                            false));
    /** worker event loop group. Reuse I/O worker threads between rpc servers. */
    private static final EventLoopGroup                 workerGroup             = NettyEventLoopUtil
            .newEventLoopGroup(
                    Runtime
                            .getRuntime()
                            .availableProcessors() * 2,
                    new NamedThreadFactory(
                            "Rpc-netty-server-worker",
                            true));

    /** address parser to get custom args */
    private RemotingAddressParser                       addressParser;

    /** connection manager */
    private DefaultConnectionManager                    connectionManager;

    static {
        if (workerGroup instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) workerGroup).setIoRatio(ConfigManager.netty_io_ratio());
        } else if (workerGroup instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) workerGroup).setIoRatio(ConfigManager.netty_io_ratio());
        }
    }


    public MqttBrokerServer(int port) {
        super(port);
    }

    public MqttBrokerServer(String ip, int port) {
        super(ip, port);
    }

    @Override
    protected void doInit() {
        if (this.addressParser == null) {
            this.addressParser = new RpcAddressParser();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
            this.connectionEventHandler = new RpcConnectionEventHandler(switches());
            this.connectionManager = new DefaultConnectionManager(new RandomSelectStrategy());
            this.connectionEventHandler.setConnectionManager(this.connectionManager);
            this.connectionEventHandler.setConnectionEventListener(this.connectionEventListener);
        } else {
            this.connectionEventHandler = new ConnectionEventHandler(switches());
            this.connectionEventHandler.setConnectionEventListener(this.connectionEventListener);
        }
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, workerGroup)
                .channel(NettyEventLoopUtil.getServerSocketChannelClass())
                .option(ChannelOption.SO_BACKLOG, ConfigManager.tcp_so_backlog())
                .option(ChannelOption.SO_REUSEADDR, ConfigManager.tcp_so_reuseaddr())
                .childOption(ChannelOption.TCP_NODELAY, ConfigManager.tcp_nodelay())
                .childOption(ChannelOption.SO_KEEPALIVE, ConfigManager.tcp_so_keepalive());

        // set write buffer water mark
        initWriteBufferWaterMark();

        // init byte buf allocator
        if (ConfigManager.netty_buffer_pooled()) {
            this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        } else {
            this.bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        }

        // enable trigger mode for epoll if need
        NettyEventLoopUtil.enableTriggeredMode(bootstrap);

        final boolean idleSwitch = ConfigManager.tcp_idle_switch();
        final int idleTime = ConfigManager.tcp_server_idle();
        final ChannelHandler serverIdleHandler = new ServerIdleHandler();
        final BlotBrokerHandler handler = new BlotBrokerHandler();
        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("decoder", new MqttDecoder());
                pipeline.addLast("encoder",  MqttEncoder.INSTANCE);
                if (idleSwitch) {
                    pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, idleTime,
                            TimeUnit.MILLISECONDS));
                    pipeline.addLast("serverIdleHandler", serverIdleHandler);
                }
                pipeline.addLast("connectionEventHandler", connectionEventHandler);
                pipeline.addLast("handler", handler);
                createConnection(channel);
            }

            /**
             * create connection operation<br>
             * <ul>
             * <li>If flag manageConnection be true, use {@link DefaultConnectionManager} to add a new connection, meanwhile bind it with the channel.</li>
             * <li>If flag manageConnection be false, just create a new connection and bind it with the channel.</li>
             * </ul>
             */
            private void createConnection(SocketChannel channel) {
                Url url = addressParser.parse(RemotingUtil.parseRemoteAddress(channel));
                if (switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
                    connectionManager.add(new Connection(channel, url), url.getUniqueKey());
                } else {
                    new Connection(channel, url);
                }
                channel.pipeline().fireUserEventTriggered(ConnectionEventType.CONNECT);
            }
        });
    }

    @Override
    protected boolean doStart() throws InterruptedException {
        this.channelFuture = this.bootstrap.bind(new InetSocketAddress(ip(), port())).sync();
        return this.channelFuture.isSuccess();
    }

    /**
     * Notice: only {@link GlobalSwitch#SERVER_MANAGE_CONNECTION_SWITCH} switch on, will close all connections.
     *
     * @see AbstractRemotingServer#doStop()
     */
    @Override
    protected boolean doStop() {
        if (null != this.channelFuture) {
            this.channelFuture.channel().close();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_SYNC_STOP)) {
            this.bossGroup.shutdownGracefully().awaitUninterruptibly();
        } else {
            this.bossGroup.shutdownGracefully();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)
                && null != this.connectionManager) {
            this.connectionManager.removeAll();
            logger.warn("Close all connections from server side!");
        }
        logger.warn("Rpc Server stopped!");
        return true;
    }

    @Override
    public void registerProcessor(byte protocolCode, CommandCode commandCode, RemotingProcessor<?> processor) {
        throw new RuntimeException("not protocol processor register");
    }

    @Override
    public void registerDefaultExecutor(byte protocolCode, ExecutorService executor) {
        throw new RuntimeException("not default executor  register");
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> processor) {
        UserProcessorRegisterHelper.registerUserProcessor(processor, this.userProcessors);
    }

    /**
     * init netty write buffer water mark
     */
    private void initWriteBufferWaterMark() {
        int lowWaterMark = this.netty_buffer_low_watermark();
        int highWaterMark = this.netty_buffer_high_watermark();
        if (lowWaterMark > highWaterMark) {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "[server side] bolt netty high water mark {%s} should not be smaller than low water mark {%s} bytes)",
                                    highWaterMark, lowWaterMark));
        } else {
            logger.warn(
                    "[server side] bolt netty low water mark is {} bytes, high water mark is {} bytes",
                    lowWaterMark, highWaterMark);
        }
        this.bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                lowWaterMark, highWaterMark));
    }
}
