package com.example.rideshare.service;

import com.example.rideshare.model.dto.LoginRequest;
import com.example.rideshare.model.dto.LoginResponse;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password}")
    private String adminPassword;

    public Optional<LoginResponse> login(LoginRequest request) {
        String login = request.getUsername().trim();
        String password = request.getPassword();

        if (adminUsername.equals(login) && adminPassword.equals(password)) {
            String token = jwtService.generateToken(login, "ADMIN", null);
            return Optional.of(new LoginResponse(token, "ADMIN", login, null));
        }

        Optional<User> userOpt = userRepository.findByEmail(login);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return Optional.empty();
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }

        String token = jwtService.generateToken(user.getEmail(), "USER", user.getId());
        return Optional.of(new LoginResponse(token, "USER", user.getName(), user.getId()));
    }
}
