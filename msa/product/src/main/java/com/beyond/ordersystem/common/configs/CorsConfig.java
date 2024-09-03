package com.beyond.ordersystem.common.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS(Cross-Origin Resource Sharing) 설정을 구성하는 방법
// CORS : 웹 어플리케이션이 다른 도메인에서 리소스를 요청할 때 발생하는 보안 문제를 해결하기 위해 사용

@Configuration
public class CorsConfig implements WebMvcConfigurer { // WebMvcConfigurer : Spring MVC 설정을 사용자 정의하기 위한 인터페이스

    // addCorsMapping : WebMvcConfigurer 인터페이스 메서드, CORS 매핑을 추가하는 역할
    // CorsRegistry : 객체를 사용하여 CORS 설정 정의
    @Override
    public void addCorsMappings(CorsRegistry corsRegistry){ // 허용 정책
        corsRegistry.addMapping("/**")
                .allowedOrigins("http://localhost:8081") // 허용 url 명시
                .allowedMethods("*") // get요청, post요청 등
                .allowedHeaders("*")
                .allowCredentials(true); // 보안처리 할 것인지
    }
}
