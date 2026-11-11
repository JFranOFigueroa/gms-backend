package com.gymback.security;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
@Configuration
public class SecurityConfig {
  @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
  @Bean public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret,IRefreshTokenRepository sessions) {
    SecretKeySpec key=secret(secret);
    NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
      JwtValidators.createDefaultWithIssuer("gym-back"),new SessionJwtValidator(sessions)));
    return decoder;
  }
  @Bean public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) { return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secret(secret))); }
  private SecretKeySpec secret(String secret) {
    if(secret==null || secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
  }
  @Bean FilterRegistrationBean<DeviceAuthenticationFilter> disableServletAutoRegistration(DeviceAuthenticationFilter filter){
    FilterRegistrationBean<DeviceAuthenticationFilter> registration=new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);return registration;
  }
  @Bean SecurityFilterChain filterChain(HttpSecurity http,DeviceAuthenticationFilter kioskFilter) throws Exception {
    return http.cors(cors->{}).csrf(csrf->csrf.disable())
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/api/v1/health","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout").permitAll()
        .requestMatchers(HttpMethod.GET,"/api/v1/public/gyms/*/landing","/api/v1/public/gyms/*/checkin-config","/api/v1/public/media/*").permitAll()
        .requestMatchers("/api/v1/kiosk/**").permitAll()
        .anyRequest().authenticated())
      .oauth2ResourceServer(o->o.jwt(j->{})).addFilterBefore(kioskFilter,BearerTokenAuthenticationFilter.class).build();
  }
  @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String origin) {
    CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization","Content-Type","X-CSRF-Token","Idempotency-Key"));c.setAllowCredentials(true);c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
  }
}
