package beyond.ordersystem.ordering.domain;

import beyond.ordersystem.member.domain.Member;
import beyond.ordersystem.ordering.dto.OrderingListResDto;
import beyond.ordersystem.ordering.dto.OrderingSaveReqDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ordering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Builder.Default
//    @Column(columnDefinition = "ENUM('ORDERED', 'CANCELD') DEFAULT 'ORDERED'")
    private OrderStatus orderStatus = OrderStatus.ORDERED;


    // ordering에서 orderDetail을 접근하기 위한 변수
    // ordering.getOrderDetails() => 리턴타입 List
    @OneToMany(mappedBy = "ordering", cascade = CascadeType.PERSIST)
    // @Builder.Default : 빌더 패턴에서도 ArrayList로 초기화 되도록하는 설정
    @Builder.Default
    private List<OrderDetail> orderDetails = new ArrayList<>();


    public OrderingListResDto fromEntity(){
        List<OrderDetail> orderDetailList = this.getOrderDetails();
        List<OrderingListResDto.OrderDetailDto> orderDetailDtos = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList){
            orderDetailDtos.add(orderDetail.fromEntity());
        }
        return OrderingListResDto.builder()
                .id(this.id)
                .memberEmail(this.member.getEmail())
                .orderStatus(this.orderStatus)
                .orderDetailDtos(orderDetailDtos)
                .build();
    }

    public void updateStatus(OrderStatus orderStatus){
        this.orderStatus = orderStatus;
    }

}
