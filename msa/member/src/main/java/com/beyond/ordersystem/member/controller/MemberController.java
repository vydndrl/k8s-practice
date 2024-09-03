package com.beyond.ordersystem.member.controller;

import com.beyond.ordersystem.common.auth.JwtTokenProvider;
import com.beyond.ordersystem.common.dto.CommonErrorDto;
import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.*;
import com.beyond.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
//@RequestMapping("/ordersystem/member")
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("2")
    private final RedisTemplate<String, Object> redisTemplate;

    // yml 파일에서 secretKeyRt(refresh token) 가져옴
    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    @Autowired
    public MemberController(MemberService memberService,
                            JwtTokenProvider jwtTokenProvider,
                            @Qualifier("2") RedisTemplate<String, Object> redisTemplate){
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    // @Valid 어노테이션: Spring MVC와 Spring Boot에서 사용되는 유효성 검증 어노테이션으로, 메서드의 매개변수나 필드에 적용하여 객체의 유효성을 자동으로 검증
    @PostMapping("member/create")
    public ResponseEntity<Object> memberCreate(@Valid @RequestBody MemberSaveReqDto dto){
        Member member = memberService.memberCreate(dto);
        // body에 들어가는 HttpStatus상태
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "memberCreate 성공", member.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED); //header에 들어가는 상태
    }

    // @PreAuthorize 어노테이션은 Spring Security에서 메서드 레벨에서 접근 제어를 구현하는 데 사용
    // Spring Security는 기본적으로 ROLE_ 접두사를 자동으로 추가
    // 회원관리 목적 -> admin만 회원목록 전체조회 가능하도록 함 (메서드 접근 권한 제어)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("member/list")
    public ResponseEntity<Object> memberList(Pageable pageable){
        Page<MemberResDto> memberResDtos = memberService.memberList(pageable);
        // body에 들어가는 HttpStatus상태
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "memberList 조회 성공", memberResDtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK); //header에 들어가는 상태
    }

    // 본인은 본인회원정보만 조회가능하도록 함 (자기자신의 정보만 조회, 단건 조회, 토큰에서 member 확인하면 됨 ->매개변수 필요X)
    // member/myinfo
    @GetMapping("member/myinfo")
    public ResponseEntity<Object> myInfo(){
        MemberResDto memberResDto = memberService.myInfo();
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "myInfo 조회성공", memberResDto);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    // 로그인하면 -> 토큰발급 -> redis에 (key : email, value : refreshToken) 형태로 저장됨
    @PostMapping("/doLogin")
    public ResponseEntity<Object> doLogin(@RequestBody MemberLoginDto dto){
        // email, password가 일치하는지 검증
        Member member = memberService.login(dto);

        // 일치할 경우 accessToken 생성
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString()); // payload에 패스워드 넣으면 안됨(복호화될 수 있는 요소이기 때문)
        // refresh token 추가
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().toString()); // payload에 패스워드 넣으면 안됨(복호화될 수 있는 요소이기 때문)

        // redis에 email과 rt를 key:value로 하여 저장
        // set(key : member의 email, value : refresh token)
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 240, TimeUnit.HOURS); // 토큰 유효시간 : 240시간

        // 생성된 토큰을 CommonResDto에 담아 사용자에게 return
        Map<String, Object> logInfo = new HashMap<>();
        logInfo.put("id", member.getId());
        logInfo.put("token", jwtToken);
        logInfo.put("refresh token", refreshToken);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "login is successful", logInfo);

        return new ResponseEntity<>(commonResDto ,HttpStatus.OK);
    }

    // refresh Token 검증
    @PostMapping("/refresh-token")
    public ResponseEntity<Object> generateNewAccessToken(@RequestBody MemberRefreshDto dto){
        String rt = dto.getRefreshToken();
        Claims claims = null;
        try{
            // 코드를 통해 rt검증
            // JwtAuthFilter에서 검증하는 코드 긁어옴
            claims = Jwts.parser().setSigningKey(secretKeyRt).parseClaimsJws(rt).getBody(); // payLoad에 있는 정보 (이메일, 권한 등..) => 이 한줄로 검증코드가 끝남
        }catch (Exception e){
               return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(),"invalid refresh token"), HttpStatus.BAD_REQUEST);
        }

        String email = claims.getSubject();
        String role = claims.get("role").toString();

        // redis를 조회하여 rt 추가 검증(이메일로 조회해서 rt와 비교) - 토큰탈취 방지하는 안정장치, 메모리에 저장하면 날라갈수도있다
        Object obj = redisTemplate.opsForValue().get(email);
        if(obj == null || !obj.toString().equals(rt)){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(),"invalid refresh token"), HttpStatus.BAD_REQUEST);
        }

        // 새로운 access Token 받아냄
        String newAt = jwtTokenProvider.createToken(email, role);

        // 생성된 토큰을 CommonResDto에 담아 사용자에게 return
        Map<String, Object> info = new HashMap<>();
        info.put("token", newAt);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "at is renewed", info);

        return new ResponseEntity<>(commonResDto ,HttpStatus.OK);
    }

    @PatchMapping("member/reset-password")
    public ResponseEntity<Object> resetPassword(@RequestBody ResetPassWordDto dto){
        memberService.resetPassword(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "비밀번호가 성공적으로 변경되었습니다.","ok");
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

}
