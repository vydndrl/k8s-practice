package com.beyond.ordersystem.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j // 로그남길 수 있는 'log'객체 자동생성
@Component
public class JwtAuthFilter extends GenericFilter { // 필터구현을 위해 GenericFilter 클래스 상속받음
    // jwt(json web token)를 통한 인증토큰필터

    @Value("${jwt.secretKey}")
    private String secretKey; // yml 파일에서 jwt.secretKey 값을 일거와서 secretKey 필드에 주입

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 토큰은 헤더부에 들어간다.
        // Http요청의 헤더에서 'Authorization'값을 읽어온다. 이 값은 JWT 토큰이 포함된 헤더
        String bearerToken = ((HttpServletRequest)request).getHeader("Authorization");

        try{ // AccessToken 검증 및 파싱
            if(bearerToken != null){
                // token은 관례적으로 Bearer로 시작하는 문구를 넣어서 요청
                if(!bearerToken.substring(0,7).equals("Bearer ")){
                    throw new AuthenticationServiceException("Bearer 형식이 아닙니다.");
                }
                String token = bearerToken.substring(7); // 7번째 자리 이후 글자만 잘라낸다.
                // token 검증 및 claims 추출
                // token 생성시에 사용한 secret키 값을 넣어 토큰 검증에 사용한다. (비밀키를 넣어서 암호화 해서 만들어진 암호와 비교해봄)
                // secretKey 를 사용하여 JWT 토큰을 파싱하고 토큰의 클레임(claims)을 추출한다.
                Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody(); // payLoad에 있는 정보 (이메일, 권한 등..) => 이 한줄로 검증코드가 끝남
                // Authentication 객체 생성 + UserDetails객체도 팔요 (인증정보 들어있음 - 이메일, 권한 등..)

                // 클레임에서 role을 추출하여 GrantedAuthority 리스트에 추가한다.
                // GrantedAuthority : Spring Security에서 사용자의 권한을 표현하는 인터페이스
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_"+claims.get("role")));
                // UserDetails (Subject() : email)
                // User 클래스 : UserDetails 인터페이스 구현하는 클래스, 생성자에 사용자명, 비밀번호, 사용자 권한 리스트 전달
                UserDetails userDetails = new User(claims.getSubject(), "", authorities); // subject : 일반적으로 이메일 혹은 id같은 고유 식별자
                // Authentication 객체 만드는 이유 : 전역적으로 만들어서 사용하기 위해 만듦 (이 사용자가 누군지 식별하기 위해)
                // Authentication 객체 : 사용자의 인증정보. Spring Security는 현재 사용자가 누구인지 식별할 수 있다.
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,"",userDetails.getAuthorities());
                // 현재 스레드의 SecurityContext가져옴(현재 인증된 사용자와 관련된 보안정보를 저장하는 컨텍스트)
                SecurityContextHolder.getContext().setAuthentication(authentication); // 생성된 Authentication객체를 SecurityContext에 설정한다.
                // 이를 통해 현제 스레드에서 요청을 처리하는 동안 애플리케이션은 사용자의 인증정보를 전역적으로 사용할 수 있게 된다.
            }
            // filterchain에서 그 다음 filtering으로 넘어가도록 하는 메서드
            chain.doFilter(request,response);
        }catch(Exception e){
            log.error(e.getMessage());
            // 예외가 발생하면 로그를 남기고, HTTP응답상태를 401 Unauthorized로 설정한다.
            // 응답의 콘텐츠 타입을 JSON으로 설정하고 token error 메시지를 클라이언트에 반환한다
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write("token error");
        }
    }
}
