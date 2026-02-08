package com.alex.market.mvc.security.config;

import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.security.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CartWebFilter cartWebFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {
        http
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorizeExchangeSpec ->authorizeExchangeSpec.pathMatchers("/static/**","/images/**","/login/**","/register").permitAll())
                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.pathMatchers("/**", "/items/**").hasAuthority(Role.USER.getAuthority()))
                .formLogin(login -> login
                        .loginPage("/login")
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                )
                /*  .sessionManagement(sessionManagementSpec -> {sessionManagementSpec
                  })*/
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new RedirectServerLogoutSuccessHandler())
                )
                .addFilterAfter(cartWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }
}
