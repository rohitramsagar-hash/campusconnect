package com.campusconnect.controller;

import com.campusconnect.dto.CreateUserRequest;
import com.campusconnect.dto.UserDto;
import com.campusconnect.dto.UserStatusRequest;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> list(@RequestParam(name = "role", required = false) Role role) {
        return userService.list(role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }

    @PatchMapping("/{id}/status")
    public UserDto setActive(@AuthenticationPrincipal User admin, @PathVariable("id") Long id,
                             @Valid @RequestBody UserStatusRequest req) {
        return userService.setActive(id, req.active(), admin);
    }
}
