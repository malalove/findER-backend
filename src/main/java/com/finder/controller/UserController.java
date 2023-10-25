package com.finder.controller;

import com.finder.dto.SignUpDto;
import com.finder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    // 회원 가입
    @PostMapping("/signup")
    public ResponseEntity signup(@RequestBody SignUpDto signUpDto) {
        return ResponseEntity.ok(userService.createUser(signUpDto));
    }

    // 이메일 중복 검증
    @GetMapping("/emailValidation")
    public ResponseEntity<Boolean> emailValidation(@RequestParam String email) {
        return ResponseEntity.ok(userService.emailValidation(email));
    }

    // 로그아웃
    @DeleteMapping("/logout")
    public ResponseEntity logout(HttpServletRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.logout(request.getHeader("Authorization"), userDetails.getUsername()));
    }
}