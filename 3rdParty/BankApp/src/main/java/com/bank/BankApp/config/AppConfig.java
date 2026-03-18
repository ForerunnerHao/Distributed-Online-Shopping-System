package com.bank.BankApp.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableAsync
public class AppConfig {

/**
 * 创建并配置一个RestTemplate Bean实例
 * 该实例设置了连接超时时间和读取超时时间
 *
 * @parambuilder RestTemplateBuilder构建器，用于构建RestTemplate实例
 * @return 配置好的RestTemplate实例，包含3秒连接超时和5秒读取超时
 */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
    // 使用builder构建RestTemplate实例
    // 设置连接超时时间为3秒
    // 设置读取超时时间为5秒
    // 构建并返回配置完成的RestTemplate实例
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
