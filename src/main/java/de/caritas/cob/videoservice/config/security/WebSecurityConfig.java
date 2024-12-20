package de.caritas.cob.videoservice.config.security;

import de.caritas.cob.videoservice.api.authorization.Authority.AuthorityValue;
import de.caritas.cob.videoservice.filter.HttpTenantFilter;
import de.caritas.cob.videoservice.filter.StatelessCsrfFilter;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configuration class to provide the Keycloak security configuration. */
@KeycloakConfiguration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {

  private static final String UUID_PATTERN =
      "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b";

  public static final List<String> WHITE_LIST =
      List.of(
          "/videocalls/docs",
          "/videocalls/docs/**",
          "/videocalls/event/stop",
          "/v2/api-docs",
          "/configuration/ui",
          "/swagger-resources/**",
          "/configuration/security",
          "/swagger-ui",
          "/swagger-ui/**",
          "/webjars/**",
          "/actuator/health",
          "/actuator/health/**");

  @Autowired AuthorisationService authorisationService;
  @Autowired JwtAuthConverterProperties jwtAuthConverterProperties;

  @Autowired(required = false)
  @Nullable
  private HttpTenantFilter httpTenantFilter;

  @Value("${csrf.cookie.property}")
  private String csrfCookieProperty;

  @Value("${csrf.header.property}")
  private String csrfHeaderProperty;

  @Value("${multitenancy.enabled}")
  private boolean multitenancy;

  /**
   * Configure spring security filter chain: disable default Spring Boot CSRF token behavior and add
   * custom {@link StatelessCsrfFilter}, set all sessions to be fully stateless, define necessary
   * Keycloak roles for specific REST API paths
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    var httpSecurity =
        http.csrf()
            .disable()
            .addFilterBefore(
                new StatelessCsrfFilter(csrfCookieProperty, csrfHeaderProperty), CsrfFilter.class);

    if (multitenancy) {
      httpSecurity =
          httpSecurity.addFilterAfter(httpTenantFilter, BearerTokenAuthenticationFilter.class);
    }

    http.sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .requestMatchers(WHITE_LIST.toArray(String[]::new))
        .permitAll()
        .requestMatchers("/videocalls/new")
        .hasAuthority(AuthorityValue.CONSULTANT)
        .requestMatchers("/videocalls/stop/{sessionId:" + UUID_PATTERN + "}")
        .hasAnyAuthority(AuthorityValue.CONSULTANT)
        .requestMatchers("/videocalls/join/{sessionId:" + UUID_PATTERN + "}")
        .hasAnyAuthority(AuthorityValue.CONSULTANT)
        .requestMatchers("/videocalls/event/stop/*")
        .hasAnyAuthority(AuthorityValue.JITSI_TECHNICAL)
        .requestMatchers("/videocalls/reject")
        .hasAnyAuthority(AuthorityValue.USER, AuthorityValue.CONSULTANT)
        .requestMatchers("/videocalls/*/jwt")
        .permitAll()
        .anyRequest()
        .denyAll()
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

    httpSecurity.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthConverter());
    return httpSecurity.build();
  }

  /**
   * Configure trailing slash match for all endpoints (needed as Spring Boot 3.0.0 changed default
   * behaviour for trailing slash match) https://www.baeldung.com/spring-boot-3-migration (section
   * 3.1)
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.setUseTrailingSlashMatch(true);
  }

  @Bean
  public JwtAuthConverter jwtAuthConverter() {
    return new JwtAuthConverter(jwtAuthConverterProperties, authorisationService);
  }
}
