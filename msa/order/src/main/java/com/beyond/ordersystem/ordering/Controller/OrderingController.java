package com.beyond.ordersystem.ordering.Controller;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.ordering.Service.OrderingService;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderingListResDto;
import com.beyond.ordersystem.ordering.dto.OrderingSaveReqDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@RequestMapping("ordersystem/ordering/")
public class OrderingController {

    private final OrderingService orderingService;

    @Autowired
    public OrderingController(OrderingService orderingService){
        this.orderingService = orderingService;
    }

    @PostMapping("/order/create")
    public ResponseEntity<Object> orderingCreate (@RequestBody List<OrderingSaveReqDto> dto){
        Ordering ordering = orderingService.orderFeignClientCreate(dto);
        // 엔티티그대로 return할 경우 순환참조에 빠질수있음(OneToMany, ManyToOne 관계걸려있는 경우)
        // 엔티티 그대로 return하고 싶다면 createResDto 만들어서 리턴할 것.
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "정상완료", ordering.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/order/list")
    public ResponseEntity<Object> orderList(){
        List<OrderingListResDto> orderList = orderingService.orderList();
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "정상조회완료", orderList);
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }

    // 내 주문만 볼 수 있는 myOrders : order/myorders
    @GetMapping("order/my-orders")
    public ResponseEntity<Object> myOrders(){
        List<OrderingListResDto> myOrderList = orderingService.myOrders();
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "정상조회완료", myOrderList);
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }

    // admin 사용자가 주문취소 : order/{id}/cancel -> orderStatus만 변경
    @PatchMapping("order/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> orderCancel (@PathVariable Long id){
        Ordering ordering = orderingService.orderCancel(id);

        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "정상 취소", ordering.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }
}
