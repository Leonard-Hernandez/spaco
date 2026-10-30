package com.lsoftware.spaco.infrastructure.config.security;

import com.lsoftware.spaco.entity.member.model.MemberRole;
import com.lsoftware.spaco.infrastructure.config.tenant.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String MCP_SESSION_ID = "Mcp-Session-Id";
    private static final String MCP_PROTOCOL_VERSION = "Mcp-Protocol-Version";

    private final PanelAuthorizationManager panel;

    @Value("${spaco.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${spaco.security.issuer}")
    private String issuer;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/media/**").permitAll()
                        // Booking needs no account; the booking link is the credential to cancel it
                        .requestMatchers(HttpMethod.POST, "/api/public/appointments").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/public/appointments/*").permitAll()
                        // Clinical data: owner and estheticians, never the front desk
                        .requestMatchers("/api/panel/clinical/**").access(panel.roles(MemberRole.ESTHETICIAN))
                        .requestMatchers("/api/panel/admin/**").access(panel.roles())
                        .requestMatchers("/api/panel/**").access(panel.member())
                        // /api/me (patient portal) and /mcp check who the caller is themselves
                        .anyRequest().authenticated()
                )
                // RFC 9728: /.well-known/oauth-protected-resource[/<path>] and the resource_metadata
                // of every 401 come from Spring Security; MCP clients find authco there
                .oauth2ResourceServer(rs -> rs
                        .jwt(Customizer.withDefaults())
                        .protectedResourceMetadata(m -> m.protectedResourceMetadataCustomizer(meta -> meta
                                .authorizationServer(issuer)
                                .scope("openid").scope("profile").scope("email")
                                .resourceName("Spaco")
                                .tlsClientCertificateBoundAccessTokens(false))))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::strip).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", TenantFilter.HEADER,
                MCP_SESSION_ID, MCP_PROTOCOL_VERSION));
        cfg.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE, MCP_SESSION_ID));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
