package com.rains.iot.mqtt.server.broker.protocol;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;

public interface Protocol {
     void process(Channel channel, MqttMessage msg);
}
