package com.lkd.emq;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * mqtt配置类:配置绑定
 */
@Data
@Slf4j
@Component
@Configuration
public class MqttConfig {
    @Value("${mqtt.client.username}")
    private String username;
    @Value("${mqtt.client.password}")
    private String password;
    @Value("${mqtt.client.serverURI}")
    private String serverURI;
    @Value("${mqtt.client.clientId}")
    private String clientId;
    @Value("${mqtt.client.keepAliveInterval}")
    private int keepAliveInterval;
    @Value("${mqtt.client.connectionTimeout}")
    private int connectionTimeout;

    //private List<String> consumerTopics;

    @Autowired
    private MqttCallback mqttCallback;

    @Bean
    public MqttClient mqttClient() {
        try {
            MqttClientPersistence persistence = mqttClientPersistence();
            MqttClient client = new MqttClient(serverURI, clientId, persistence);
            //设置手动消息接收确认
            client.setManualAcks(true);
            mqttCallback.setMqttClient(client);//设置消息回调的中的客户端
            client.setCallback(mqttCallback);//设置回调侦听器
            client.connect(mqttConnectOptions());//连接
            //client.subscribe(subTopic);
            return client;
        } catch (MqttException e) {
            log.error("emq connect error",e);
            return null;
        }
    }

    /**
     * mqtt连接选项(参数)
     * @return
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);//用户名
        options.setPassword(password.toCharArray());//密码
        options.setCleanSession(true);//是否清除之前的连接信息
        options.setAutomaticReconnect(true);//自动连接
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        return options;
    }

    /**
     * Mqtt客户端持久化
     * @return
     */
    public MqttClientPersistence mqttClientPersistence() {
        return new MemoryPersistence();//内存持久化
    }
}
