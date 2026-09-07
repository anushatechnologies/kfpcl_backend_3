package com.project.kfpcl_exports.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPrincipal {
    private final Long userId;
    private final String phoneNumber;
    private final String accessToken;
}
