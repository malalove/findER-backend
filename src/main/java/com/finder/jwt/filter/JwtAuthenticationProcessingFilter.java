package com.finder.jwt.filter;

import com.finder.domain.Users;
import com.finder.jwt.service.JwtService;
import com.finder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {
    private static final HashSet<String> NO_CHECK_URL_SET = new HashSet(Set.of("/api/login", "/api/signup"));

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (NO_CHECK_URL_SET.contains(request.getRequestURI()) || request.getRequestURI().startsWith("/api/emailValidation")) {
            filterChain.doFilter(request, response);

            return;
        }

        // 사용자 요청 헤더에서 RefreshToken 추출
        String refreshToken = jwtService.extractRefreshToken(request)
                .filter(jwtService::isRefreshTokenValid)
                .orElse(null);

        // AccessToken 갱신 요청
        if (refreshToken != null) {
            checkRTAndReIssueAT(response, refreshToken);

            return; // RefreshToken을 보낸 경우 AccessToken을 재발급 후 즉시 응답
        }

        // 일반적인 요청
        if (refreshToken == null) {
            checkAccessTokenAndAuthentication(request, response, filterChain);
        }
    }

    // RefreshToken을 가진 사용자가 있다면 AccessToken과 RefreshToken을 재발급 후 응답 헤더 설정
    public void checkRTAndReIssueAT(HttpServletResponse response, String refreshToken) {
        userRepository.findByRefreshToken(refreshToken)
                .ifPresentOrElse(user -> {
                    String newRefreshToken = reIssueRT(user);
                    jwtService.setAccessAndRefreshHeader(response, jwtService.createAccessToken(user.getEmail()),
                            newRefreshToken);
                }, () -> {
                    throw new RuntimeException("유효하지 않은 Refresh Token 입니다.");
                });
    }

    // RefreshToken 재발급 & 데이터베이스 내 RefreshToken 갱신
    private String reIssueRT(Users user) {
        String newRefreshToken = jwtService.createRefreshToken();
        user.updateRefreshToken(newRefreshToken);
        userRepository.saveAndFlush(user);

        return newRefreshToken;
    }

    // Access Token 검증 & 인증 처리 & 필터 진행
    public void checkAccessTokenAndAuthentication(HttpServletRequest request, HttpServletResponse response,
                                                  FilterChain filterChain) throws ServletException, IOException {
        Optional<String> accessToken = jwtService.extractAccessToken(request);

        // JWT 토큰 존재 여부 판단
        if (!accessToken.isPresent()) {
            log.error("유효한 JWT 토큰이 없습니다");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        // JWT 토큰 검증
        if (!accessToken.map(jwtService::isAccessTokenValid).orElse(false)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        // 인증 처리
        accessToken.flatMap(jwtService::extractEmail)
                .ifPresent(email -> userRepository.findByEmail(email)
                        .ifPresent(this::saveAuthentication));

        filterChain.doFilter(request, response);
    }

    // 인증 허용
    public void saveAuthentication(Users myUser) {
        String password = myUser.getPassword();

        // Spring Security에서 사용자 정보를 처리하는 User 객체 생성
        UserDetails userDetailsUser = org.springframework.security.core.userdetails.User.builder()
                .username(myUser.getEmail())
                .password(password)
                .roles(myUser.getRole().name())
                .build();

        // Spring Security에서 사용자의 인증 정보를 나타내는 Authentication 객체 생성
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetailsUser, null,
                        authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

        // SecurityContextHolder에 Authentication 객체를 설정하여 인증 처리
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}