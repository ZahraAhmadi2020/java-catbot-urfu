package com.urfu.catbot.telegram.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String CAT_QUEUE = "cat.commands";
  public static final String CAT_EXCHANGE = "cat.exchange";
  public static final String ROUTING_KEY = "cat.command";

  @Bean
  public Queue catQueue() {
    return new Queue(CAT_QUEUE, true);
  }

  @Bean
  public TopicExchange catExchange() {
    return new TopicExchange(CAT_EXCHANGE);
  }

  @Bean
  public Binding binding(Queue catQueue, TopicExchange catExchange) {
    return BindingBuilder.bind(catQueue).to(catExchange).with(ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    return template;
  }
}
