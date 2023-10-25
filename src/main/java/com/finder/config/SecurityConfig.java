package com.finder.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finder.jwt.service.JwtService;
import com.finder.jwt.service.LoginService;
import com.finder.jwt.filter.CustomJsonUsernamePasswordAuthenticationFilter;
import com.finder.jwt.filter.JwtAuthenticationProcessingFilter;
import com.finder.jwt.handler.LoginFailureHandler;
import com.finder.jwt.handler.LoginSuccessHandler;
import com.finder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig { // Spring Security 설정 파일
    private final LoginService loginService;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .formLogin().disable() // FormLogin 사용 X
                .httpBasic().disable() // HttpBasic 사용 X
                .csrf().disable() // CSRF 보안 사용 X
                .headers().frameOptions().disable()
                .and()

                // 세션을 사용하지 않으므로 stateless로 설정
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // URL 별 권한 관리 옵션
                .authorizeRequests()
                .antMatchers("/","/css/**","/images/**","/js/**","/favicon.ico").permitAll()
                .antMatchers("/api/signup", "/api/emailValidation").permitAll()
                .antMatchers("/login/oauth2/code/**").permitAll()
                .anyRequest().authenticated();

        // LogoutFilter -> JwtAuthenticationProcessingFilter -> CustomJsonUsernamePasswordAuthenticationFilter
        http.addFilterAfter(customJsonUsernamePasswordAuthenticationFilter(), LogoutFilter.class);
        http.addFilterBefore(jwtAuthenticationProcessingFilter(), CustomJsonUsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // AuthenticationManager 설정 후 빈 등록
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(loginService);

        return new ProviderManager(provider);
    }

    // 로그인 성공 시 호출되는 LoginSuccessJWTProviderHandler 빈 등록
    @Bean
    public LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler(jwtService);
    }

    // 로그인 실패 시 호출되는 LoginFailureHandler 빈 등록
    @Bean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler();
    }

    // UsernamePasswordAuthenticationFilter를 커스텀한 필터 빈 등록
    @Bean
    public CustomJsonUsernamePasswordAuthenticationFilter customJsonUsernamePasswordAuthenticationFilter() {
        CustomJsonUsernamePasswordAuthenticationFilter customJsonUsernamePasswordLoginFilter
                = new CustomJsonUsernamePasswordAuthenticationFilter(objectMapper);
        customJsonUsernamePasswordLoginFilter.setAuthenticationManager(authenticationManager());
        customJsonUsernamePasswordLoginFilter.setAuthenticationSuccessHandler(loginSuccessHandler());
        customJsonUsernamePasswordLoginFilter.setAuthenticationFailureHandler(loginFailureHandler());

        return customJsonUsernamePasswordLoginFilter;
    }

    // JWT 필터 빈 등록
    @Bean
    public JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter() {
        JwtAuthenticationProcessingFilter jwtAuthenticationFilter = new JwtAuthenticationProcessingFilter(jwtService, userRepository);

        return jwtAuthenticationFilter;
    }
}