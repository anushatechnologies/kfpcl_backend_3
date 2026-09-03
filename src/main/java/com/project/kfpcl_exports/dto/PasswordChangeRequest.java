package com.project.kfpcl_exports.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
}
