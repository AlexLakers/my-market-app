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
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.session.InMemoryWebSessionStore;
import reactor.core.publisher.Mono;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final CartWebFilter cartWebFilter;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http){


        http
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                 .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/orders/**", "/cart/**", "/buy").hasAuthority(Role.USER.getAuthority())
                        .pathMatchers("/**", "/items/**", "/static/**", "/images/**", "/logout/**", "/login/**", "/register").permitAll()
                        .anyExchange().denyAll()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                )
                .oauth2Client(withDefaults())
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
                /*.exceptionHandling(handling -> handling
                        .accessDeniedHandler((exchange, denied) ->
                                Mono.error(new AccessDeniedException("Access Denied"))))*/
                .addFilterAfter(cartWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }


    @Bean
    ReactiveOAuth2AuthorizedClientManager auth2AuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .refreshToken()
                .build()
        );

        return manager;
    }
}
