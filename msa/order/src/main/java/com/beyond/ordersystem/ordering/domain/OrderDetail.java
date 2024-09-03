package com.beyond.ordersystem.ordering.domain;

import com.beyond.ordersystem.ordering.dto.OrderingListResDto;
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
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_id")
    private Ordering ordering; // 실질적으로는 DB에 ordering_id로 들어감

    private Long productId;

    public OrderingListResDto.OrderDetailDto fromEntity(){
        return OrderingListResDto.OrderDetailDto.builder()
                .id(this.id)
                .count(this.quantity)
                .build();
    }
}


