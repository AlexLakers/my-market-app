package com.alex.market.mvc.security.config;

import com.alex.market.mvc.filter.CartWebFilter;
import com.alex.market.mvc.security.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.InMemoryReactiveSessionRegistry;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.session.InMemoryWebSessionStore;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final CartWebFilter cartWebFilter;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {


        http
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                /* .csrf(ServerHttpSecurity.CsrfSpec::disable)*/
                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.pathMatchers("/orders/**", "/cart/**", "/buy").hasAuthority(Role.USER.getAuthority()))
                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.pathMatchers("/**", "/items/**", "/static/**", "/images/**", "/login/**", "/register").permitAll())
                .formLogin(login -> login
                        .loginPage("/login")
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                )
                .sessionManagement(sessionManagementSpec -> {
                    sessionManagementSpec.concurrentSessions(concurrency -> {
                        concurrency.maximumSessions(SessionLimit.of(3))
                                .sessionRegistry(new InMemoryReactiveSessionRegistry())
                                .maximumSessionsExceededHandler(new InvalidateLeastUsedServerMaximumSessionsExceededHandler(new InMemoryWebSessionStore()));
                    });
                })
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutHandler(new WebSessionServerLogoutHandler())
                        .logoutSuccessHandler(new RedirectServerLogoutSuccessHandler())
                )
                .exceptionHandling(handling -> handling
                        .accessDeniedHandler((exchange, denied) ->
                                Mono.error(new AccessDeniedException("Access Denied"))))
                .addFilterAfter(cartWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }
}
