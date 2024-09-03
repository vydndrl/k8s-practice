package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.ordering.domain.Ordering;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// memberId받아서 회원을 식별할 필요가 없어졌으므로 OrderDetailDto를 꺼낸다.
public class OrderingSaveReqDto {
    private Long productId; // 상품id
    private Integer productCount; // 주문상품개수


}
