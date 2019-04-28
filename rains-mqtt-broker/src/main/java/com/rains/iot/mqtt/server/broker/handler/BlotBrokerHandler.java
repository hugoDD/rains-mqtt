package com.rains.iot.mqtt.server.broker.handler;

import com.rains.iot.mqtt.server.broker.protocol.Protocol;
import com.rains.iot.mqtt.server.broker.protocol.ProtocolProcess;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.Objects;

@ChannelHandler.Sharable
public class BlotBrokerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws Exception {
        MqttMessageType msgType =mqttMessage.fixedHeader().messageType();
        Protocol protocol= ProtocolProcess.getProtocols().get(msgType);
        if(Objects.nonNull(protocol)){
            protocol.process(ctx.channel(),mqttMessage);
        }
    }
}
