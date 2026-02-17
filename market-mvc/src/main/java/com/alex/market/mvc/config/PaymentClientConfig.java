package com.alex.market.mvc.config;

import com.alex.market.mvc.ApiClient;
import com.alex.market.mvc.client.api.DefaultApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
@RequiredArgsConstructor
public class PaymentClientConfig {

    private final ConfigProperties configProperties;

    @Bean
    public WebClient webClient(ReactiveOAuth2AuthorizedClientManager auth2AuthorizedClientManager) {
        var oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(auth2AuthorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId(configProperties.getOauth2RegistrationId());

        return WebClient.builder()
                .baseUrl(configProperties.getPaymentServiceUrl())
                .filter(oauth2Client)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Bean
    public ApiClient paymentApiClient(WebClient webClient) {
        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(configProperties.getPaymentServiceUrl());
        return apiClient;
    }

    @Bean
    public DefaultApi paymentApi(ApiClient paymentApiClient) {
        return new DefaultApi(paymentApiClient);
    }
}
