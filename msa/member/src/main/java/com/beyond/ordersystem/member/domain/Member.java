package com.beyond.ordersystem.member.domain;

import com.beyond.ordersystem.common.domain.Address;
import com.beyond.ordersystem.common.domain.BaseTimeEntity;
import com.beyond.ordersystem.member.dto.MemberResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Embedded
    private Address address;



    @Enumerated(EnumType.STRING)
//    @Builder.Default // 이렇게하면 build할 때 초기화된 값(OREDERED)으로 세팅됨
    private Role role;

    public MemberResDto FromEntity(){
        MemberResDto memberResDto = MemberResDto.builder()
                                            .id(this.id)
                                            .name(this.name)
                                            .email(this.email)
                                            .address(this.address)
                                            .role(this.role)
                                            .build();
        return memberResDto;
    }

    public void updatePassword(String newPassword){
        this.password = newPassword;
    }
}
