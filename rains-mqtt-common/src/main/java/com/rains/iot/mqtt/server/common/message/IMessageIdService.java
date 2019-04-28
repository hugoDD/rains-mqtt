/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.rains.iot.mqtt.server.common.message;

/**
 * 分布式生成报文标识符
 */
public interface IMessageIdService {

	/**
	 * 获取报文标识符
	 */
	int getNextMessageId();

	/**
	 * 释放报文标识符
	 */
	void releaseMessageId(int messageId);
}
