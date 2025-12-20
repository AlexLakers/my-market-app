package com.alex.market.config;

import com.alex.market.filter.CartFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CartFilterConfig {

    @Bean
    public FilterRegistrationBean<CartFilter> authFilterRegistration() {
        FilterRegistrationBean<CartFilter> bean =
                new FilterRegistrationBean<>(new CartFilter());
        bean.setOrder(1);
        bean.addUrlPatterns("/*");
        return bean;
    }
}
