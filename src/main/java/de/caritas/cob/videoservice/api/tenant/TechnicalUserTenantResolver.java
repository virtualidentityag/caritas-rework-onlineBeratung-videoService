package de.caritas.cob.videoservice.api.tenant;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TechnicalUserTenantResolver implements TenantResolver {

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return isTechnicalUserRole() ? Optional.of(0L) : Optional.empty();
  }

  private boolean isTechnicalUserRole() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      Jwt jwt = (Jwt) authentication.getPrincipal();
      return getRealmRoles(jwt).contains("technical");
    }
    return false;
  }

  private Collection<String> getRealmRoles(Jwt jwt) {

    if (jwt != null) {
      var claims = jwt.getClaims();
      if (claims.containsKey("realm_access")) {
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess.containsKey("roles")) {
          return (List<String>) realmAccess.get("roles");
        }
      }
    }
    return Lists.newArrayList();
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
