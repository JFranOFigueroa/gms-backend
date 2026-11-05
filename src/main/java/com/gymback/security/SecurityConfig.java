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
import org.springframework.web.cors.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
@Configuration
public class SecurityConfig {
  @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
  @Bean public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
    SecretKeySpec key=secret(secret);
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
  @Bean public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) { return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secret(secret))); }
  private SecretKeySpec secret(String secret) {
    if(secret==null || secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
  }
  @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.cors(cors->{}).csrf(csrf->csrf.disable())
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/api/v1/health","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout").permitAll()
        .requestMatchers(HttpMethod.GET,"/api/v1/public/gyms/*/landing","/api/v1/public/media/*").permitAll()
        .anyRequest().authenticated())
      .oauth2ResourceServer(o->o.jwt(j->{})).build();
  }
  @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String origin) {
    CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization","Content-Type"));c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
  }
}
