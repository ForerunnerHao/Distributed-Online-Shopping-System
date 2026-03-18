package com.email.EmailService.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * RabbitMQ configuration
 */
@Configuration
public class RabbitMQConfig {
    
    @Value("${messaging.exchange.store}")
    private String exchangeName;
    
    @Value("${messaging.queue.email}")
    private String queueName;
    
    @Value("${messaging.routingKeys}")
    private String routingKeysStr;
    
    /**
     * Create topic exchange
     */
    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(exchangeName);
    }
    
    /**
     * Create email queue
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(queueName).build();
    }
    
    /**
     * Bind queue to exchange with routing keys
     */
    @Bean
    public List<Binding> emailBindings() {
        return List.of(
                BindingBuilder.bind(emailQueue()).to(emailExchange()).with("order.#"),  // Orders
                BindingBuilder.bind(emailQueue()).to(emailExchange()).with("delivery.#"),  // Deliveries
                BindingBuilder.bind(emailQueue()).to(emailExchange()).with("refund.#")  // Refunds
        );
    }
    
    /**
     * Message converter to JSON
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}

