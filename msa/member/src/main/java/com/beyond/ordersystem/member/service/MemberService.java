package com.beyond.ordersystem.member.service;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.MemberLoginDto;
import com.beyond.ordersystem.member.dto.MemberResDto;
import com.beyond.ordersystem.member.dto.MemberSaveReqDto;
import com.beyond.ordersystem.member.dto.ResetPassWordDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Service
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository){
        this.memberRepository = memberRepository;
    }

    public Member memberCreate(MemberSaveReqDto dto){
        if(memberRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new IllegalArgumentException("존재하는 이메일입니다.");
        }

        Member member = memberRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
        return member;
    }

    public Page<MemberResDto> memberList(Pageable pageable){
        Page<Member> members = memberRepository.findAll(pageable);
        Page<MemberResDto> memberListResDtos = members.map(a->a.FromEntity());
        return memberListResDtos;
    }

    public Member login(MemberLoginDto dto){
        // email 존재여부 체크
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()->new EntityNotFoundException("존재하는 이메일이 없습니다."));

        // password 일치여부
        if(!passwordEncoder.matches(dto.getPassword(), member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return member;
    }

    public MemberResDto myInfo(){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()->new EntityNotFoundException("존재하는 이메일이 없습니다."));
        return member.FromEntity();
    }

    public void resetPassword(ResetPassWordDto dto){
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()-> new EntityNotFoundException("존재하는 이메일이 없습니다."));
        // password 일치여부
        if(!passwordEncoder.matches(dto.getAsIsPassword(), member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        member.updatePassword(passwordEncoder.encode(dto.getToBePassword()));
    }
}
