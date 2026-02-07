package com.restrictedfileshare.service;

import com.restrictedfileshare.config.JwtUtil;
import com.restrictedfileshare.dto.*;
import com.restrictedfileshare.exception.BadRequestException;
import com.restrictedfileshare.exception.ResourceNotFoundException;
import com.restrictedfileshare.exception.UnauthorizedException;
import com.restrictedfileshare.model.User;
import com.restrictedfileshare.model.VerificationToken;
import com.restrictedfileshare.repository.UserRepository;
import com.restrictedfileshare.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.verification.cooldown.minutes}")
    private int verificationCooldownMinutes;

    public ApiResponse<String> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerified(false);
        user.setEnabled(true);

        userRepository.save(user);

        String token = sendVerificationEmail(user);

        return new ApiResponse<>(
                true,
                "Registration successful. Verification email sent.",
                token
        );
    }

    private String sendVerificationEmail(User user) {
        LocalDateTime cooldownTime = LocalDateTime.now().minusMinutes(verificationCooldownMinutes);
        if (tokenRepository.existsRecentToken(user.getId(), cooldownTime)) {
            throw new BadRequestException(
                    "Please wait " + verificationCooldownMinutes +
                            " minutes before requesting another verification email"
            );
        }

        tokenRepository.invalidateUserTokens(user.getId());

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        token.setCreatedAt(LocalDateTime.now());
        token.setUsed(false);

        tokenRepository.save(token);

        String verificationLink = "http://localhost:8080/api/auth/verify?token=" + tokenValue;
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);

        return tokenValue;
    }

    public ApiResponse<String> verifyEmail(String tokenValue) {
        VerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (token.isUsed()) {
            throw new BadRequestException("Token already used");
        }

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        User user = token.getUser();
        user.setVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return new ApiResponse<>(
                true,
                "Email verified successfully",
                null
        );
    }

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isVerified()) {
            String token = sendVerificationEmail(user);
            throw new UnauthorizedException(
                    "Email not verified. A new verification link has been sent to your email."
            );
        }

        String jwtToken = jwtUtil.generateToken(user.getEmail());
        LoginResponse response = new LoginResponse(jwtToken, user.getEmail(), "Login successful");

        return new ApiResponse<>(true, "Login successful", response);
    }

    public ApiResponse<String> resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new BadRequestException("User is already verified");
        }

        String token = sendVerificationEmail(user);

        return new ApiResponse<>(
                true,
                "Verification email resent successfully",
                token
        );
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}