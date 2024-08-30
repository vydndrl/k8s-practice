package beyond.ordersystem.member.service;

import beyond.ordersystem.member.domain.Member;
import beyond.ordersystem.member.dto.MemberLoginDto;
import beyond.ordersystem.member.dto.MemberPasswordUpdateDto;
import beyond.ordersystem.member.dto.MemberReqDto;
import beyond.ordersystem.member.dto.MemberResDto;
import beyond.ordersystem.member.repository.MemberRepository;

import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import javax.xml.bind.ValidationException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

//    @Bean
//    public PasswordEncoder passwordEncoder(){
//        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
//    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member memberCreate(MemberReqDto dto){
        if (memberRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        Member savedMember = memberRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
        return savedMember;
    }

    public Page<MemberResDto> memberList(Pageable pageable){
        Page<Member> members = memberRepository.findAll(pageable);
//        return members.map((a->a.fromEntity()));
        return members.map((Member::fromEntity));
    }

    public MemberResDto myInfo(){
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 이메일입니다."));
        return member.fromEntity();
    }

    public Member login(MemberLoginDto dto){
        // email 존재 여부 검증
            Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이메일입니다."));

        // password 일치 여부 검증
        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }
        return member;
    }

    @Transactional
    public void resetPassword(MemberPasswordUpdateDto dto){
        // email 존재 여부 검증
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이메일입니다."));

        // password 일치 여부 검증
        if (!passwordEncoder.matches(dto.getAsIsPassword(), member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        member.updatePassword(passwordEncoder.encode(dto.getToBePassword()));
    }


}
