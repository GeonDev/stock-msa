package com.stock.discovery.security;

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

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        //csrf 설정은 무시
        http.csrf((auth) -> auth.disable());
        //모든 경로에 대하여 시큐리티 값이  필요함
        http.authorizeHttpRequests((auth) -> auth.anyRequest().authenticated());
        //로그인은 베이직
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }


    @Bean
    public UserDetailsService userDetailsService() {

        String username = System.getenv().getOrDefault("CONFIG_SERVER_USER", "admin");
        String password = System.getenv().getOrDefault("CONFIG_SERVER_PASSWORD", "1234");

        UserDetails user = User.builder()
                .username(username)
                .password(bCryptPasswordEncoder().encode(password))
                .roles("ADMIN")
                .build();

        //inMemory에 ID/PW 저장
        return new InMemoryUserDetailsManager(user);
    }
}

