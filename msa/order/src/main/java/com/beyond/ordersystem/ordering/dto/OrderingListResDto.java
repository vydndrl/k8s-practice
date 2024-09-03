package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.ordering.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderingListResDto {
    // 주문목록 조회(전체) : {id, memberEmail, orderStatus,
    // [{id(주문상세), productName, count}, {id(주문상세), productName, count}]}
    private Long id; // 주문 id
    private String memberEmail; // 주문회원 이메일
    private OrderStatus orderStatus;
    private List<OrderDetailDto> orderDetailDtos;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderDetailDto{
        private Long id; // 주문상세 id
        private String productName; // 상품명
        private Integer count;
    }

}
