/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.rains.iot.mqtt.server.broker.protocol;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PINGREQ连接处理
 */
public class PingReq implements Protocol{

	private static final Logger LOGGER = LoggerFactory.getLogger(PingReq.class);

	public void processPingReq(Channel channel, MqttMessage msg) {
		MqttMessage pingRespMessage = MqttMessageFactory.newMessage(
			new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0), null, null);
		LOGGER.debug("PINGREQ - clientId: {}", (String) channel.attr(AttributeKey.valueOf("clientId")).get());
		channel.writeAndFlush(pingRespMessage);
	}

	@Override
	public void process(Channel channel, MqttMessage msg) {
		processPingReq(channel,msg);
	}
}
