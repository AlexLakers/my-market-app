package com.alex.market.mvc.config;

import com.alex.market.mvc.ApiClient;
import com.alex.market.mvc.client.api.DefaultApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PaymentClientConfig {

    private final ConfigProperties configProperties;

    @Bean
    public ApiClient paymentApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(configProperties.getPaymentServiceUrl());
        return apiClient;
    }

    @Bean
    public DefaultApi paymentApi(ApiClient paymentApiClient) {
        return new DefaultApi(paymentApiClient);
    }
}
