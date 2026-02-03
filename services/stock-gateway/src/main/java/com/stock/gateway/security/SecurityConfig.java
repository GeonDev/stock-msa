package com.stock.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${GATEWAY_USER:admin}")
    private String gatewayUser;

    @Value("${GATEWAY_PASSWORD:1234}")
    private String gatewayPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // CSRF 비활성화
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        // Actuator 엔드포인트 보호
        http.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").authenticated()
                .anyExchange().permitAll() // 그 외 Gateway 라우팅은 인증 없이 통과 (필요 시 수정)
        );

        // HTTP Basic 인증 사용
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username(gatewayUser)
                .password(passwordEncoder().encode(gatewayPassword))
                .roles("ADMIN")
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}

