package beyond.ordersystem.member.controller;

import beyond.ordersystem.common.auth.JwtTokenProvider;
import beyond.ordersystem.common.dto.CommonErrorDto;
import beyond.ordersystem.common.dto.CommonResDto;
import beyond.ordersystem.member.domain.Member;
import beyond.ordersystem.member.dto.*;
import beyond.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    @Qualifier("2")
    private final RedisTemplate<String, Object> redisTemplate;   // "1"번 DB을 접근할 수 있는 접근 객체

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider, @Qualifier("2") RedisTemplate<String, Object> redisTemplate) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/member/create")
    public ResponseEntity<?> createMember(@Valid @RequestBody MemberReqDto dto){
        Member member = memberService.memberCreate(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "member is successfully created", member.getId()), HttpStatus.CREATED);

    }

    // todo ) admin만 회원 목록 전체 조회 가능
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/member/list")
    public ResponseEntity<Object> memberList(Pageable pageable){
        Page<MemberResDto> dtos = memberService.memberList(pageable);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "members are found", dtos), HttpStatus.OK);
    }

    // todo ) 본인은 본인 회원 정보만 조회 가능
    @GetMapping("/member/myInfo")
    public ResponseEntity<?> myInfo(){
        MemberResDto dto = memberService.myInfo();
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "member is found", dto), HttpStatus.OK);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginDto dto) {
        // 1. email, password가 일치하는 지 검증
        Member member = memberService.login(dto);

        // 2. 일치할 경우 accessToken 생성
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().toString());

        // redis에 email과 rt를 key:value하여 저장
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 240, TimeUnit.HOURS);  // 240시간
        // 3. 생성된 토큰을 CommonResDto에 담아 사용자에게 return
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put ("id", member.getId());
        loginInfo.put ("token", jwtToken);
        loginInfo.put ("refreshToken", refreshToken);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "login is successful", loginInfo), HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> generateNewAccessToken(@RequestBody MemberRefreshDto dto){
        String rt = dto.getRefreshToken();
        Claims claims = null;
        try{
            // 코드를 통해 rt 검증
            claims = Jwts.parser().setSigningKey(secretKeyRt).parseClaimsJws(rt).getBody();
        } catch (Exception e){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST, "invalid refresh token"), HttpStatus.BAD_REQUEST);
        }

        String email = claims.getSubject();
        String role = claims.get("role").toString();

        // redis를 조회하여 rt 추가 검증
        Object obj = redisTemplate.opsForValue().get(email);
        if(obj == null || !obj.toString().equals(rt)) {
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST, "invalid refresh token"), HttpStatus.BAD_REQUEST);
        }
        String newAt = jwtTokenProvider.createToken(email, role);

        Map<String, Object> info = new HashMap<>();
        info.put ("token", newAt);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "AT is renewed", info), HttpStatus.OK);
    }

    @PatchMapping("/member/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody MemberPasswordUpdateDto dto){
        memberService.resetPassword(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "password is successfully changed", dto.getEmail()), HttpStatus.OK);
    }



}
