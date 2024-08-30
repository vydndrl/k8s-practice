package beyond.ordersystem.ordering.service;

import beyond.ordersystem.common.service.StockInventoryService;
import beyond.ordersystem.member.domain.Member;
import beyond.ordersystem.member.repository.MemberRepository;
import beyond.ordersystem.ordering.controller.SseController;
import beyond.ordersystem.ordering.domain.OrderDetail;
import beyond.ordersystem.ordering.domain.OrderStatus;
import beyond.ordersystem.ordering.domain.Ordering;
import beyond.ordersystem.ordering.dto.OrderingSaveReqDto;
import beyond.ordersystem.ordering.dto.OrderingListResDto;
import beyond.ordersystem.ordering.dto.StockDecreaseEvent;
import beyond.ordersystem.ordering.repository.OrderDetailRepository;
import beyond.ordersystem.ordering.repository.OrderingRepository;
import beyond.ordersystem.product.domain.Product;
import beyond.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockInventoryService stockInventoryService;
//    private final StockDecreaseEventHandler stockDecreaseEventHandler;
    private final SseController sseController;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, ProductRepository productRepository, OrderDetailRepository orderDetailRepository, StockInventoryService stockInventoryService, SseController sseController) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.stockInventoryService = stockInventoryService;
//        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
    }

    // Order 생성
    //        방법 1. 쉬운방식
//    public Ordering orderingCreate(@ModelAttribute OrderingSaveReqDto dto) {
//        //    Ordering생성 : member_id, status
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("없음"));
//        Ordering ordering = orderingRepository.save(dto.toEntity(member));
//        //    OrderDetail생성 : order_id, product_id, quantity
//        for (OrderingSaveReqDto.OrderDto orderDto : dto.getOrderList()) {
//            Product product = productRepository.findById(orderDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("없음"));
//            int quantity = orderDto.getProductCount();
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//            orderDetailRepository.save(orderDetail);
//        }
//        return ordering;
//    }

    //  synchronized를 설정한다 하더라도, 재고 감소가 DB에 반영되는 시점은 트랜잭션이 커밋되고 종료되는 시점
    //      방법 2. JPA에 최적화된 방식
    public Ordering orderingCreate(List<OrderingSaveReqDto> dtos) {
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("없음"));
        // * 이 한 줄 외우기 ) 시큐리티컨텍스트 안에 어센티케이션이 있고 겟 네임을 하면 이메일이 나옴
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("member is not found"));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        for (OrderingSaveReqDto dto : dtos) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("없음"));
            int quantity = dto.getProductCount();

            if(product.getName().contains("sale")){
                // redis를 통한 재고 관리 및 재고잔량 확인
                int newQuantity = stockInventoryService.decreaseStock(dto.getProductId(), dto.getProductCount()).intValue();
                if (newQuantity < 0){
                    throw new IllegalArgumentException("재고가 부족합니다");
                }
                // rdb에 재고를 업데이트. rabbitmq를 통해 비동기적으로 이벤트 처리
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(), dto.getProductCount()));
            } else {
                if (product.getStockQuantity() < quantity){
                    throw new IllegalArgumentException("재고가 부족합니다");
                }
                product.updateStockQuantity(quantity);  // 변경감지로 인해 별도의 save 불필요
            }

            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(quantity)
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }
        Ordering savedOrdering = orderingRepository.save(ordering);
        sseController.publishMessage(savedOrdering.fromEntity(), "admin@test.com");
        return savedOrdering;
    }


    // Order 전체 조회
    public List<OrderingListResDto> orderingList(){
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderingListResDto> orderingListResDtos = new ArrayList<>();
        for (Ordering ordering : orderings){
            orderingListResDtos.add(ordering.fromEntity());
        }
        return orderingListResDtos;
    }

    public List<OrderingListResDto> myOrderingList(){
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 이메일입니다."));
        List<Ordering> orderings = orderingRepository.findByMemberId(member.getId());
        List<OrderingListResDto> orderingListResDtos = new ArrayList<>();
        for  (Ordering ordering : orderings){
            orderingListResDtos.add(ordering.fromEntity());
        }
        return orderingListResDtos;
    }

    public Ordering cancelOrdering(Long orderingId){
        Ordering ordering = orderingRepository.findById(orderingId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 주문 번호입니다"));
        ordering.updateStatus(OrderStatus.CANCELED);
        return ordering;
    }

}
