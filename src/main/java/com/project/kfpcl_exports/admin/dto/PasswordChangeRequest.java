package com.project.kfpcl_exports.admin.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
}
