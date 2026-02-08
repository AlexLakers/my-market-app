package com.alex.market.mvc.security.config;

import com.alex.market.mvc.security.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerFormLoginAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerHttpBasicAuthenticationConverter;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http
                                                      /*ReactiveAuthenticationManager authenticationManager*/) throws Exception {

      /*  AuthenticationWebFilter authFilter = new AuthenticationWebFilter(authenticationManager);
        authFilter.setServerAuthenticationConverter(new ServerFormLoginAuthenticationConverter());*/

        http
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
              /*  .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)*/
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.pathMatchers("/cart/**,","/orders/**","/buy").hasAuthority(Role.USER.getAuthority()))
                .authorizeExchange(authorizeExchangeSpec ->authorizeExchangeSpec.pathMatchers("/**","/items/**","/static/**","/images/**","/login/**","/register").permitAll())
               /* .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.anyExchange().permitAll())*/
                .formLogin(login -> login
                        .loginPage("/login")
                        .authenticationSuccessHandler(new CustomAuthenticationSuccessHandler()/*new RedirectServerAuthenticationSuccessHandler("/")*/)
                )
                /*  .sessionManagement(sessionManagementSpec -> {I add it later if need
                  })*/
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new RedirectServerLogoutSuccessHandler())
                );
               // .addFilterAfter(cartWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }
}
