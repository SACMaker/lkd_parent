package com.lkd.emq;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * mqtt服务
 */
public interface MqttService {
    /**
     * 处理消息
     * @param topic
     * @param message
     */
    void processMessage(String topic, MqttMessage message);
}
