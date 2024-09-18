package de.caritas.cob.videoservice.api.authorization;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;

/** Definition of all authorities and of the role-authority-mapping. */
@Getter
public enum Authority {
  CONSULTANT(UserRole.CONSULTANT, "AUTHORIZATION_CONSULTANT_DEFAULT"),
  USER(UserRole.USER, "AUTHORIZATION_USER_DEFAULT"),

  JITSI_TECHNICAL(UserRole.JITSI_TECHNICAL, "AUTHORIZATION_JITSI_TECHNICAL_DEFAULT");

  private final UserRole role;
  private final List<String> authorities;

  Authority(final UserRole role, final String authorityName) {
    this.role = role;
    this.authorities = Lists.newArrayList(authorityName);
  }

  /**
   * Finds a {@link Authority} instance by given roleName.
   *
   * @param roleName the role name to search for
   * @return the {@link Authority} instance
   */
  public static Authority fromRoleName(String roleName) {
    return Stream.of(values())
        .filter(authority -> authority.role.getValue().equals(roleName))
        .findFirst()
        .orElse(null);
  }

  public static class AuthorityValue {

    private AuthorityValue() {}

    public static final String PREFIX = "AUTHORIZATION_";
    public static final String CONSULTANT = PREFIX + "CONSULTANT_DEFAULT";
    public static final String USER = PREFIX + "USER_DEFAULT";
    public static final String JITSI_TECHNICAL = PREFIX + "JITSI_TECHNICAL_DEFAULT";
  }
}
