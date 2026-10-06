package com.campusconnect.service;

import com.campusconnect.dto.CreateUserRequest;
import com.campusconnect.dto.UserDto;
import com.campusconnect.exception.ApiException;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list(Role role) {
        List<User> users = role == null ? userRepository.findAllByOrderByRoleAscNameAsc()
                : userRepository.findByRoleOrderByNameAsc(role);
        return users.stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
        }
        User user = new User(req.name().trim(), email, passwordEncoder.encode(req.password()), req.role(),
                AuthService.blankToNull(req.department()));
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto setActive(Long id, boolean active, User actor) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getId().equals(actor.getId()) && !active) {
            throw ApiException.badRequest("CANNOT_DISABLE_SELF", "You can't disable your own account.");
        }
        user.setActive(active);
        return UserDto.from(user);
    }
}
