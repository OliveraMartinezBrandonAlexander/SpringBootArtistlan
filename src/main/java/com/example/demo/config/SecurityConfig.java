package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/paginado").hasRole("ADMIN")
                        // Publicos
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/existe").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*/categoria").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/2fa/verify-login", "/auth/2fa/verify-login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/2fa/resend", "/auth/2fa/resend").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artistas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/obras/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/servicios/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Privados en fase 2A
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/2fa/request-activation",
                                "/auth/2fa/request-activation",
                                "/api/auth/2fa/verify-activation",
                                "/auth/2fa/verify-activation",
                                "/api/auth/2fa/disable",
                                "/auth/2fa/disable")
                        .authenticated()
                        .requestMatchers("/api/moderacion/**").hasAnyRole("ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/rol").hasRole("ADMIN")

                        // Gradual: no bloquear otros modulos aun
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                escribirError(response, HttpServletResponse.SC_FORBIDDEN, "No tienes permisos para realizar esta accion"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticacion gestionada por JWT de Artistlan");
        };
    }

    private void escribirError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "message", message
        )));
    }
}
