package com.beyond.ordersystem.common.service;

import com.beyond.ordersystem.member.domain.Role;
import com.beyond.ordersystem.member.dto.MemberSaveReqDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// CommandLineRunner를 상속함으로서 해당 컴포넌트가 스프링빈으로 등록되는 시점에서 run 메서드 실행
@Component
public class InitialDataLoader implements CommandLineRunner {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public void run(String... args) throws Exception{
        if(memberRepository.findByEmail("admin@naver.com").isEmpty()){
            memberService.memberCreate(MemberSaveReqDto.builder()
                    .name("admin")
                    .email("admin@naver.com")
                    .password("12341234")
                    .role(Role.ADMIN)
                    .build());
        }
    }
}
