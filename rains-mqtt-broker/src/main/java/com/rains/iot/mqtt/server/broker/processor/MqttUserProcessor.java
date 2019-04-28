package com.rains.iot.mqtt.server.broker.processor;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.RemotingContext;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import io.netty.handler.codec.mqtt.MqttMessage;

import java.util.concurrent.Executor;

public class MqttUserProcessor extends SyncUserProcessor<MqttMessage> {



    @Override
    public Object handleRequest(BizContext bizCtx, MqttMessage request) throws Exception {
        bizCtx.getConnection().getChannel();
        return null;
    }

    @Override
    public String interest() {
        return null;
    }
}
