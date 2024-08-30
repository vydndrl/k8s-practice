package beyond.ordersystem.ordering.controller;

import beyond.ordersystem.common.dto.CommonResDto;
import beyond.ordersystem.ordering.domain.Ordering;
import beyond.ordersystem.ordering.dto.OrderingListResDto;
import beyond.ordersystem.ordering.dto.OrderingSaveReqDto;
import beyond.ordersystem.ordering.service.OrderingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Transactional
public class OrderingController {
    private final OrderingService orderingService;

    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping("/ordering/create")
    public ResponseEntity<?> createOrdering(@RequestBody List<OrderingSaveReqDto> dtos){
        Ordering ordering = orderingService.orderingCreate(dtos);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "주문완료",  ordering.getId()), HttpStatus.CREATED);
        // ordering.getId() 하는 이유 : 엔티티 그대로 리턴하면 순환참조에 빠질 수 있음
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ordering/list")
    public ResponseEntity<?> orderingList(){
        List<OrderingListResDto> orderList = orderingService.orderingList();
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "OK", orderList), HttpStatus.OK);
    }

    @GetMapping("/ordering/myorders")
    public ResponseEntity<?> myOrders(){
        List<OrderingListResDto> orderList = orderingService.myOrderingList();
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "OK", orderList), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/ordering/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id){
        orderingService.cancelOrdering(id);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "OK", "주문이 삭제되었습니다."), HttpStatus.OK);
    }



}
