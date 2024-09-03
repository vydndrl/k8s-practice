package com.beyond.ordersystem.member.dto;

import com.beyond.ordersystem.common.domain.Address;
import com.beyond.ordersystem.member.domain.Role;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberResDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Address address;
    private int orderCount;
}
