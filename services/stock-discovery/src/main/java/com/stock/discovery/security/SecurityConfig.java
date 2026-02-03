package com.stock.discovery.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${EUREKA_USER:admin}")
    private String eurekaUser;

    @Value("${EUREKA_PASSWORD:1234}")
    private String eurekaPassword;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        //csrf 설정은 무시
        http.csrf((auth) -> auth.disable());
        // actuator/health 는 인증 없이 접근 가능하도록 설정
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated());
        //로그인은 베이직
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }


    @Bean
    public UserDetailsService userDetailsService() {

        UserDetails user = User.builder()
                .username(eurekaUser)
                .password(bCryptPasswordEncoder().encode(eurekaPassword))
                .roles("ADMIN")
                .build();

        //inMemory에 ID/PW 저장
        return new InMemoryUserDetailsManager(user);
    }
}

