package com.example.demo.config;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayPalConfig {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.sandbox:true}")
    private boolean sandbox;

    @Bean
    public PayPalHttpClient payPalHttpClient() {
        PayPalEnvironment environment = sandbox
                ? new PayPalEnvironment.Sandbox(clientId, clientSecret)
                : new PayPalEnvironment.Live(clientId, clientSecret);

        return new PayPalHttpClient(environment);
    }
}
