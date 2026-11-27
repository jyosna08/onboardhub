package com.onboardhub.auth.security;

import com.onboardhub.auth.entity.User;

public interface TokenService {
    String generateToken(User user);
}
