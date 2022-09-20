package com.chw.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author CHW
 * @Date 2022/9/17
 **/
@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue delCacheQueue(){
        return new Queue("delCache");
    }
}
