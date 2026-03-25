package uz.xorazmdelivery.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uz.xorazmdelivery.security.JwtAuthFilter;
import uz.xorazmdelivery.security.JwtAuthEntryPoint;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint authEntryPoint;

    @Value("${cors.allowed-origins:http://localhost:3000,https://xorazmdelivery.uz}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/v1/auth/send-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/auth/verify-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/price/calculate").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/payments/callback/**").permitAll()

                // Actuator
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

                // WebSocket
                .requestMatchers("/ws/**").permitAll()

                // Admin only
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                // Everything else — authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // HSTS
        http.headers(h -> h.httpStrictTransportSecurity(hsts ->
            hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));

        return http.build();
    }
}
