package com.campusconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// No default Spring Security user: every login goes through our JWT AuthService.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CampusConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusConnectApplication.class, args);
    }
}
