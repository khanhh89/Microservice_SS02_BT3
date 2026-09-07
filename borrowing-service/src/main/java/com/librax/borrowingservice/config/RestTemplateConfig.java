package com.librax.borrowingservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // @LoadBalanced giúp RestTemplate tự resolve tên service (VD: "book-service")
    // sang IP:port thực tế thông qua Eureka, đồng thời hỗ trợ load balancing
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
