package de.caritas.cob.videoservice.api.tenant;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import de.caritas.cob.videoservice.api.service.TenantService;
import de.caritas.cob.videoservice.filter.SubdomainExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SubdomainTenantResolver implements TenantResolver {

  private final @NonNull SubdomainExtractor subdomainExtractor;

  private final @NonNull TenantService tenantService;

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return resolveTenantFromSubdomain();
  }

  private Optional<Long> resolveTenantFromSubdomain() {
    Optional<String> currentSubdomain = subdomainExtractor.getCurrentSubdomain();
    if (currentSubdomain.isPresent()) {
      return of(getTenantIdBySubdomain(currentSubdomain.get()));
    } else {
      return empty();
    }
  }

  private Long getTenantIdBySubdomain(String currentSubdomain) {
    return tenantService.getRestrictedTenantDataBySubdomain(currentSubdomain).getId();
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
