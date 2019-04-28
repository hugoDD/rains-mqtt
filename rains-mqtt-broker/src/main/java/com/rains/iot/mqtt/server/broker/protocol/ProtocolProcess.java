/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.rains.iot.mqtt.server.broker.protocol;

import com.rains.iot.mqtt.server.broker.internal.InternalCommunication;
import com.rains.iot.mqtt.server.common.auth.IAuthService;
import com.rains.iot.mqtt.server.common.message.IDupPubRelMessageStoreService;
import com.rains.iot.mqtt.server.common.message.IDupPublishMessageStoreService;
import com.rains.iot.mqtt.server.common.message.IMessageIdService;
import com.rains.iot.mqtt.server.common.message.IRetainMessageStoreService;
import com.rains.iot.mqtt.server.common.session.ISessionStoreService;
import com.rains.iot.mqtt.server.common.subscribe.ISubscribeStoreService;
import io.netty.handler.codec.mqtt.MqttMessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议处理
 */
@Component
public class ProtocolProcess {

	@Autowired
	private ISessionStoreService sessionStoreService;

	@Autowired
	private ISubscribeStoreService subscribeStoreService;

	@Autowired
	private IAuthService authService;

	@Autowired
	private IMessageIdService messageIdService;

	@Autowired
	private IRetainMessageStoreService messageStoreService;

	@Autowired
	private IDupPublishMessageStoreService dupPublishMessageStoreService;

	@Autowired
	private IDupPubRelMessageStoreService dupPubRelMessageStoreService;

	@Autowired
	private InternalCommunication internalCommunication;

	private static ConcurrentHashMap<MqttMessageType,Protocol>  protocols = new ConcurrentHashMap<>();


	@PostConstruct
	public void initProtocols(){
		protocols.put(MqttMessageType.CONNECT,connect());
		protocols.put(MqttMessageType.SUBSCRIBE,subscribe());
		protocols.put(MqttMessageType.UNSUBSCRIBE,unSubscribe());
		protocols.put(MqttMessageType.PUBLISH,publish());
		protocols.put(MqttMessageType.DISCONNECT,disConnect());
		protocols.put(MqttMessageType.PINGREQ,pingReq());
		protocols.put(MqttMessageType.PUBREL,pubRel());
		protocols.put(MqttMessageType.PUBACK,pubAck());
		protocols.put(MqttMessageType.PUBREC,pubRec());
		protocols.put(MqttMessageType.PUBCOMP,pubComp());

	}

	public static Map<MqttMessageType,Protocol> getProtocols(){
	    return protocols;
    }

	private Connect connect;

	private Subscribe subscribe;

	private UnSubscribe unSubscribe;

	private Publish publish;

	private DisConnect disConnect;

	private PingReq pingReq;

	private PubRel pubRel;

	private PubAck pubAck;

	private PubRec pubRec;

	private PubComp pubComp;

    private Connect connect() {
		if (connect == null) {
			connect = new Connect(sessionStoreService, subscribeStoreService, dupPublishMessageStoreService, dupPubRelMessageStoreService, authService);
		}
		return connect;
	}

    private Subscribe subscribe() {
		if (subscribe == null) {
			subscribe = new Subscribe(subscribeStoreService, messageIdService, messageStoreService);
		}
		return subscribe;
	}

    private UnSubscribe unSubscribe() {
		if (unSubscribe == null) {
			unSubscribe = new UnSubscribe(subscribeStoreService);
		}
		return unSubscribe;
	}

    private Publish publish() {
		if (publish == null) {
			publish = new Publish(sessionStoreService, subscribeStoreService, messageIdService, messageStoreService, dupPublishMessageStoreService, internalCommunication);
		}
		return publish;
	}

    private DisConnect disConnect() {
		if (disConnect == null) {
			disConnect = new DisConnect(sessionStoreService, subscribeStoreService, dupPublishMessageStoreService, dupPubRelMessageStoreService);
		}
		return disConnect;
	}

    private PingReq pingReq() {
		if (pingReq == null) {
			pingReq = new PingReq();
		}
		return pingReq;
	}

    private PubRel pubRel() {
		if (pubRel == null) {
			pubRel = new PubRel();
		}
		return pubRel;
	}

    private PubAck pubAck() {
		if (pubAck == null) {
			pubAck = new PubAck(messageIdService, dupPublishMessageStoreService);
		}
		return pubAck;
	}

    private PubRec pubRec() {
		if (pubRec == null) {
			pubRec = new PubRec(dupPublishMessageStoreService, dupPubRelMessageStoreService);
		}
		return pubRec;
	}

    private PubComp pubComp() {
		if (pubComp == null) {
			pubComp = new PubComp(messageIdService, dupPubRelMessageStoreService);
		}
		return pubComp;
	}

	public ISessionStoreService getSessionStoreService() {
		return sessionStoreService;
	}

}
