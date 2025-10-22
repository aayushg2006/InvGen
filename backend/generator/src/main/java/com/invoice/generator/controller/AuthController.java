package com.invoice.generator.controller;

import com.invoice.generator.dto.RegisterDto;
import com.invoice.generator.model.User;
import com.invoice.generator.service.AuthServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.invoice.generator.dto.AuthResponseDto;
import com.invoice.generator.dto.LoginDto;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthServiceImpl authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterDto registerDto) {
        try {
            // Step 1: Capture the User object returned by the service
            User newUser = authService.register(registerDto);
            
            // Step 2: Use the object in your response for a more useful message
            return new ResponseEntity<>("User '" + newUser.getUsername() + "' registered successfully!", HttpStatus.CREATED);
            
        } catch (Exception e) {
            return new ResponseEntity<>("Error during registration: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginDto loginDto) {
        String token = authService.login(loginDto);
        return new ResponseEntity<>(new AuthResponseDto(token), HttpStatus.OK);
    }

    // We will add the /login endpoint here later
}