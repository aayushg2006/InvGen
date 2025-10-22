package com.invoice.generator.dto;

import lombok.Data;

@Data
public class RegisterDto {

    private String fullName;
    private String username; // This will be the user's email
    private String password;
    private String shopName;
    private String shopGstin;

}