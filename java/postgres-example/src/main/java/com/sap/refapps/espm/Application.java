package com.sap.refapps.espm;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.sap.refapps.espm.config.SaleApplicationContextInitializer;

@SpringBootApplication
@EnableCircuitBreaker
public class Application {

    public static void main(String[] args) {
    	new SpringApplicationBuilder(Application.class)
		.initializers(new SaleApplicationContextInitializer())
		.run(args);

    }

    @Bean
    @LoadBalanced
    public RestTemplate rest(RestTemplateBuilder builder) {
      return builder.build();
    }
    
}
