package com.finder.service;

import com.finder.domain.Role;
import com.finder.domain.Users;
import com.finder.dto.SignUpDto;
import com.finder.repository.UserRepository;
import com.finder.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RedisUtil redisUtil;

    @Transactional
    public String createUser(SignUpDto signUpDto) {
        Users user = Users.builder()
                .email(signUpDto.getEmail())
                .password(signUpDto.getPassword())
                .name(signUpDto.getName())
                .role(Role.USER)
                .build();

        user.passwordEncode(passwordEncoder);
        userRepository.save(user);

        return "사용자 생성 완료";
    }

    @Transactional
    public String logout(String accessToken, String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 존재하지 않습니다."));
        user.updateRefreshToken(null);
        redisUtil.setBlackList(accessToken, "accessToken", 3);

        return "로그아웃 완료";
    }

    public Boolean emailValidation(String email) {
        return (userRepository.findByEmail(email).isPresent()) ? false : true;
    }
}