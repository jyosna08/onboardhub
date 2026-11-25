package com.onboardhub.auth.mapper;

import com.onboardhub.auth.dto.UserResponse;
import com.onboardhub.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
