package com.beyond.ordersystem.ordering.domain;

import com.beyond.ordersystem.ordering.dto.OrderingListResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Ordering {
    // id, member(1:n관계, ManyToOne), orderStatus(ORDERED, CANCLED), orderdetail(OneToMany)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default // 이렇게하면 build할 때 초기화된 값(ORDERED)으로 세팅됨
    private OrderStatus orderStatus = OrderStatus.ORDERED;

    @OneToMany(mappedBy = "ordering", cascade = CascadeType.PERSIST)
    // Builder 패턴에서도 ArrayList로 초기화되도록 하는 설정
//    @Builder.Default
    private List<OrderDetail> orderDetails;

    public OrderingListResDto fromEntity(){
        List<OrderDetail> orderDetailList = this.getOrderDetails();
        List<OrderingListResDto.OrderDetailDto> orderDetailDtos = new ArrayList<>();

        for(OrderDetail orderDetail : orderDetailList){
            orderDetailDtos.add(orderDetail.fromEntity());
        }

        OrderingListResDto orderingListResDto = OrderingListResDto.builder()
                .id(this.id)
                .memberEmail(this.memberEmail)
                .orderStatus(this.orderStatus)
                .orderDetailDtos(orderDetailDtos)
                .build();
        return orderingListResDto;
    }

    public void updateSatus(OrderStatus orderStatus){
        this.orderStatus = orderStatus;
    }

}
