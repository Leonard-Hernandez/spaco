package com.lsoftware.spaco.entity.identity.gateway;

import com.lsoftware.spaco.entity.identity.model.UserIdentity;

/**
 * Identity provider (authco). Access tokens only carry `sub`, so profile data comes from userinfo.
 */
public interface IdentityGateway {
    UserIdentity userInfo(String accessToken);
}
