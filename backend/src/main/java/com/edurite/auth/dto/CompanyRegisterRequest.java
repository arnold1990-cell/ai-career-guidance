package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRegisterRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 120) String registrationNumber,
        @Size(max = 120) String industry,
        @NotBlank @Email String officialEmail,
        @Size(max = 30) String mobileNumber,
        @NotBlank @Size(max = 150) String contactPersonName,
        @Size(max = 255) String address,
        @Size(max = 255) String website,
        String description,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
