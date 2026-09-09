package com.project.kfpcl_exports.buyer.dto;

import com.project.kfpcl_exports.buyer.enums.BusinessType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian mobile number")
    private String mobileNumber;

    // Alias in case frontend sends 'phoneNumber'
    public void setPhoneNumber(String phoneNumber) {
        if (this.mobileNumber == null || this.mobileNumber.isBlank()) {
            this.mobileNumber = phoneNumber;
        }
    }

    @NotBlank(message = "Email address is required")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Please enter a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name cannot exceed 150 characters")
    private String companyName;

    @NotNull(message = "Business type is required (WHOLESALER, TRADER, RETAILER)")
    private BusinessType businessType;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    // Optional GSTIN
    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
             message = "Invalid GSTIN format (e.g. 22AAAAA0000A1Z5)")
    private String gstin;

    // Optional GSTIN Photo (Max 500 KB)
    private MultipartFile gstinPhoto;

    // Required PAN number
    @NotBlank(message = "PAN card number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
             message = "Invalid PAN number format (must be 10 characters, e.g. ABCDE1234F)")
    private String panNumber;

    // Required PAN image (Max 1 MB)
    @NotNull(message = "PAN card image file is required")
    private MultipartFile panCardImage;
}
