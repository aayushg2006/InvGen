package com.invoice.generator.service;

import com.invoice.generator.dto.LoginDto;
import com.invoice.generator.dto.RegisterDto;
import com.invoice.generator.model.Shop;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ShopRepository;
import com.invoice.generator.repository.UserRepository;
import com.invoice.generator.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired // This is the corrected injection for PasswordEncoder
    private PasswordEncoder passwordEncoder; 
    
    @Transactional
    public User register(RegisterDto registerDto) {
        // Step 1: Create and save the new shop
        Shop newShop = new Shop();
        newShop.setShopName(registerDto.getShopName());
        newShop.setGstin(registerDto.getShopGstin());
        Shop savedShop = shopRepository.save(newShop);

        // Step 2: Create the new user
        User newUser = new User();
        newUser.setFullName(registerDto.getFullName());
        newUser.setUsername(registerDto.getUsername());
        
        // Step 3: HASH the password before saving it
        newUser.setPassword(passwordEncoder.encode(registerDto.getPassword())); 
        
        newUser.setRole("OWNER");
        newUser.setShop(savedShop);

        // Step 4: Save the new user with the hashed password
        return userRepository.save(newUser);
    }

    public String login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtil.generateToken(userDetails);
    }
}