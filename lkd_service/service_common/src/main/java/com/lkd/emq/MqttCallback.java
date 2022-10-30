package com.lkd.emq;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * mqtt消息回调类，用来解析处理收到的mqtt消息和根据配置订阅关心的主题
 */
@Component
@Slf4j
public class MqttCallback implements MqttCallbackExtended {
    //需要订阅的topic配置
    @Value("${mqtt.consumer.consumerTopics}")
    private List<String> consumerTopics;

    @Autowired
    private MqttService mqttService;

    private MqttClient mqttClient;

    /**
     * 连接丢失处理
     *
     * @param throwable the reason behind the loss of connection.
     */
    @Override
    public void connectionLost(Throwable throwable) {
        log.error("emq error.", throwable);
    }

    /**
     * 消息到达处理
     *
     * @param topic   name of the topic on the message was published to
     * @param message the actual message.
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.info("topic"+topic+":"+ new String(message.getPayload()));
        //处理消息
        mqttService.processMessage(topic, message);
        //处理成功后确认消息
        mqttClient.messageArrivedComplete(message.getId(), message.getQos());
    }

    /**
     * 交付完成处理
     *
     * @param iMqttDeliveryToken the delivery token associated with the message.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.info("deliveryComplete---------" + iMqttDeliveryToken.isComplete());
    }

    /**
     * 连接完成处理
     *
     * @param b If true, the connection was the result of automatic reconnect.
     * @param s The server URI that the connection was made to.
     */
    @Override
    public void connectComplete(boolean b, String s) {
        //和EMQ连接成功后根据配置自动订阅topic
        if (consumerTopics != null && consumerTopics.size() > 0) {
            consumerTopics.forEach(t -> {
                try {
                    log.info(">>>>>>>>>>>>>>subscribe topic:" + t);
                    mqttClient.subscribe(t, 2);//订阅主题,设置qos
                } catch (MqttException e) {
                    log.error("emq connect error", e);
                }
            });
        }
    }

    /**
     * 设置mqttClient
     * @param mqttClient
     */
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }
}