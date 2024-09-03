package com.beyond.ordersystem.common.configs;

import com.beyond.ordersystem.common.auth.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity // Spring Security 활성화
@EnableGlobalMethodSecurity(prePostEnabled = true) // pre:사전검증 post:사후검증 (메서드 수준에서 보안설정을 활성화)
public class SecurityConfigs {

    @Autowired
    private JwtAuthFilter jwtAuthFilter; // 해당토큰이 정상인지 아닌지 검증하는 필터 (체인 내에서 코드 작성하면 길고 복잡해지므로 따로 파일로 분리해서 작성)
    // Spring Security를 사용하여 웹 애플리케이션 보안설정을 구성하는 방법
    // JWT(Json Web Token)인증을 사용하여 세션리스(stateless)인증을 구현
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        // 필터 체인
        return httpSecurity
                // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화
                .csrf().disable()
                .cors().and() // CORS활성화 (같은도메인끼리 통신해야 한다. 다른 도메인에서 서버로 호출하는 것을 금지)
                // eg) localhost : 8080 과 localhost:8081 통신불가(오류반환)
                .httpBasic().disable()
                .authorizeRequests()
                    .antMatchers("/member/create", "/", "/doLogin", "/refresh-token","/product/list", "/member/reset-password")// 예외 url : 회원가입, 홈, 로그인
                    .permitAll() // 나머지는 인증처리 (위의 url패턴에 대해 모든 사용자에게 접근을 허용)
                .anyRequest().authenticated() // 그 외의 모든 요청은 인증된 사용자만 접근할 수 있다.
                .and()
                // 세션로그인이 아닌 stateless한 token을 사용하겠다라는 의미 (세션을 사용하지 않으면 JWT 토큰을 사용하여 stateless 인증 구현)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 사용자로부터 받아온 토큰이 정상인지 아닌지를 검증하는 코드
                // 로그인시 사용자는 서버로부터 토큰을 발급받고, 매요청마다 해당 토큰을 http header에 넣어 요청
                // 아래 코드는 사용자로부터 받아온 토큰이 정상인지 아닌지를 검증하는 코드 (JWT토큰 유효성 검사)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // user이름과 비밀번호 비교하는 필터
                .build();
    }
}
