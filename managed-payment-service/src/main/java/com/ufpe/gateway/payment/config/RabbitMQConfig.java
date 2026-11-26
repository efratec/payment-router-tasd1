package com.ufpe.gateway.payment.config;

import com.ufpe.gateway.payment.effectors.AdaptationCommandDTO;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String TELEMETRY_EXCHANGE = "payment.telemetry.exchange";
    public static final String TELEMETRY_QUEUE = "payment.telemetry.queue";

    public static final String COMMAND_EXCHANGE = "payment.commands.exchange";
    public static final String COMMAND_QUEUE = "payment.commands.queue";

    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE);
    }

    @Bean
    public Queue telemetryQueue() {
        return new Queue(TELEMETRY_QUEUE, true);
    }

    @Bean
    public Binding telemetryBinding() {
        return BindingBuilder.bind(telemetryQueue()).to(telemetryExchange()).with("telemetry.key");
    }

    @Bean
    public TopicExchange commandExchange() {
        return new TopicExchange(COMMAND_EXCHANGE);
    }

    @Bean
    public Queue commandQueue() {
        return new Queue(COMMAND_QUEUE, true);
    }

    @Bean
    public Binding commandBinding() {
        return BindingBuilder.bind(commandQueue()).to(commandExchange()).with("command.key");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*");
        classMapper.setIdClassMapping(Map.of("adaptationCommandDTO", AdaptationCommandDTO.class));
        converter.setClassMapper(classMapper);
        return converter;
    }

}
