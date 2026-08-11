package com.saadaoui.tunidesk.email.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(MailProperties properties) {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        if (properties.getPort() != null) {
            sender.setPort(properties.getPort());
        }
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProperties = new Properties();
        javaMailProperties.putAll(properties.getProperties());
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            javaMailProperties.put("mail.smtp.auth", "true");
            javaMailProperties.put("mail.smtp.starttls.enable", "true");
        }
        sender.setJavaMailProperties(javaMailProperties);

        return sender;
    }
}
