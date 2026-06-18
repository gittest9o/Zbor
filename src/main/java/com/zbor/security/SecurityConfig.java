package com.zbor.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TelegramAuthFilter telegramAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Stateless API — CSRF не нужен
            .csrf(AbstractHttpConfigurer::disable)

            // Без сессий — каждый запрос проверяется по initData
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // Авторизация и регистрация — без токена (AuthController сам валидирует)
                .requestMatchers("/auth", "/auth/**").permitAll()

                // Просмотр ивентов публичный — читать может кто угодно
                .requestMatchers(HttpMethod.GET, "/events", "/events/**").permitAll()

                // Создание и редактирование ивентов — только авторизованные
                .requestMatchers(HttpMethod.POST, "/events", "/events/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/events/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/events/**").authenticated()

                // Health check — без авторизации
                .requestMatchers("/actuator/health").permitAll()

                // Всё остальное (/users/me и т.д.) — нужен валидный X-Telegram-Init-Data
                .anyRequest().authenticated()
            )

            // Наш фильтр встаёт ДО стандартного фильтра Spring Security
            .addFilterBefore(telegramAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> null;
    }
}
