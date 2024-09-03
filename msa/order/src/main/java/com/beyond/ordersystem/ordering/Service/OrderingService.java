package com.beyond.ordersystem.ordering.Service;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.ordering.Controller.SseController;
import com.beyond.ordersystem.ordering.Repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.Repository.OrderingRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockInventoryService stockInventoryService;
//    private final StockDecreaseEventHandler stockDecreaseEventHandler;
    private final SseController sseController;
    private final RestTemplate restTemplate;
    private final ProductFeign productFeign;
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderingService(OrderingRepository orderingRepository,
                           OrderDetailRepository orderDetailRepository,
                           StockInventoryService stockInventoryService,
                           SseController sseController, RestTemplate restTemplate, ProductFeign productFeign) {
        this.orderingRepository = orderingRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.stockInventoryService = stockInventoryService;
//        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
        this.restTemplate = restTemplate;
        this.productFeign = productFeign;
//        this.kafkaTemplate = kafkaTemplate;
    }


    public Ordering orderRestTemplateCreate(List<OrderingSaveReqDto> dtos){

        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName(); // email 꺼내옴

        Ordering ordering = Ordering.builder()
                .memberEmail(memberEmail) // 위에서 찾은 member객체
                .orderDetails(new ArrayList<>()) // 아직 아무것도 안들어간 orderDetail 리스트
                .build();

        // 재고감소, 재고 저장에서 동시성이슈 생길 수 있음
        for (OrderingSaveReqDto dto : dtos){ // OrderingSaveReqDto의 orderDetailDto리스트 요소 하나씩 꺼내옴
            int quantity = dto.getProductCount();

//            ResponseEntity가 기본 응답 값이므로 바로 CommonResDto로 매핑
            CommonResDto commonResDto = productFeign.getProductById(dto.getProductId());
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDto productDto = objectMapper.convertValue(commonResDto.getResult(), ProductDto.class);

            System.out.println(productDto);
            // redis를 통한 재고관리 및 재고잔량 확인
            if (productDto.getName().contains("sale")) {
                int newQuantity = (stockInventoryService.decreaseStock(productDto.getId(), productDto.getStockQuantity())).intValue(); // Long -> int로 형변환
                if(newQuantity < 0){
                    throw new IllegalArgumentException("재고부족");
                }
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), dto.getProductCount()));
            }else{
                if(productDto.getStockQuantity() < quantity){
                    throw new IllegalArgumentException("재고부족");
                }
                productFeign.updateProductStock(new ProductUpdateStockDto(dto.getProductId(), dto.getProductCount()));
            }

            OrderDetail orderDetail = OrderDetail.builder() // 주문상세 OrderDetail 객체 조립
                    .productId(productDto.getId())
                    .ordering(ordering)
                    .quantity(quantity)
                    .build();
            // OrderDetail Repository를 통해서 저장하는게 아니라 OrderingRepository를 통해서 저장함
            ordering.getOrderDetails().add(orderDetail); // 여기서 이제 orderDetails 리스트에 orderDetail 하나씩 차례로 add해준다.
        }

        Ordering savedOrdering = orderingRepository.save(ordering); // 여기서 save해줘도 jpa에 의해서 선후관계 알아서 맞춰서 처리해주기 때문에 코드의 선후관계 안따져도 OK
        // 이제 Ordering 객체를 save 해줬으므로 Ordering 객체의 id값 나올 것 -> 이후 필요한 요소 jpa가 알아서 순서를 처리해준다.
        sseController.publishMessage(savedOrdering.fromEntity(), "admin@test.com");
        return savedOrdering;
    }

    public Ordering orderFeignClientCreate(List<OrderingSaveReqDto> dtos) {
        //Authentic 객체안에서 member객체 -> member 이메일 가져옴
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName(); // email 꺼내옴

        // Ordering(주문)객체 만듦 => 아직 save 전이라 id가 null인 것처럼 보여질수있음
        Ordering ordering = Ordering.builder()
                .memberEmail(memberEmail) // 위에서 찾은 member객체
                .orderDetails(new ArrayList<>()) // 아직 아무것도 안들어간 orderDetail 리스트
                .build();

        // 재고감소, 재고 저장에서 동시성이슈 생길 수 있음
        for (OrderingSaveReqDto dto : dtos){ // OrderingSaveReqDto의 orderDetailDto리스트 요소 하나씩 꺼내옴
            int quantity = dto.getProductCount();
            // Product API에 요청을 통해 product객체를 조회해야 함

            String productGetUrl = "http://product-service/product/"+ dto.getProductId();
            HttpHeaders httpHeaders = new HttpHeaders();
            String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
            httpHeaders.set("Authorization", token);
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<CommonResDto> productEntity = restTemplate.exchange(productGetUrl, HttpMethod.GET, entity, CommonResDto.class);
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDto productDto = objectMapper.convertValue(productEntity.getBody().getResult(), ProductDto.class);
            System.out.println(productDto);
            // redis를 통한 재고관리 및 재고잔량 확인
            if (productDto.getName().contains("sale")) {
                int newQuantity = (stockInventoryService.decreaseStock(dto.getProductId(), dto.getProductCount())).intValue(); // Long -> int로 형변환
                if(newQuantity < 0){
                    throw new IllegalArgumentException("재고부족");
                }
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), dto.getProductCount()));
                // rdb에 재고를 업데이트 -> 이벤트 기반의 아키텍처 구상
                // rabbitmq를 통해 비동기적으로 이벤트 처리(발생하는 액션기반)
            }else{
                if(productDto.getStockQuantity() < quantity){ // 재고감소 시키는 코드
                    throw new IllegalArgumentException("재고부족");
                }
//                 restTemplate를 통한 update 요청
//                product.updateStockQuantity(quantity);
                String updateUrl = "http://product-service/product/updatestock";
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity<>(
                        new ProductUpdateStockDto(dto.getProductId(), dto.getProductCount()), httpHeaders);
                restTemplate.exchange(updateUrl, HttpMethod.PUT, updateEntity, Void.class);



            }

            OrderDetail orderDetail = OrderDetail.builder() // 주문상세 OrderDetail 객체 조립
                    .productId(productDto.getId())
                    .ordering(ordering)
                    .quantity(quantity)
                    .build();
            // OrderDetail Repository를 통해서 저장하는게 아니라 OrderingRepository를 통해서 저장함
            ordering.getOrderDetails().add(orderDetail); // 여기서 이제 orderDetails 리스트에 orderDetail 하나씩 차례로 add해준다.
        }

        Ordering savedOrdering = orderingRepository.save(ordering); // 여기서 save해줘도 jpa에 의해서 선후관계 알아서 맞춰서 처리해주기 때문에 코드의 선후관계 안따져도 OK
        // 이제 Ordering 객체를 save 해줬으므로 Ordering 객체의 id값 나올 것 -> 이후 필요한 요소 jpa가 알아서 순서를 처리해준다.
        sseController.publishMessage(savedOrdering.fromEntity(), "admin@test.com");
        return savedOrdering;
    }

//    public Ordering orderFeignKafkaCreate(List<OrderingSaveReqDto> dtos) {
////Authentic 객체안에서 member객체 -> member 이메일 가져옴
//        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName(); // email 꺼내옴
//
//        // Ordering(주문)객체 만듦 => 아직 save 전이라 id가 null인 것처럼 보여질수있음
//        Ordering ordering = Ordering.builder()
//                .memberEmail(memberEmail) // 위에서 찾은 member객체
//                .orderDetails(new ArrayList<>()) // 아직 아무것도 안들어간 orderDetail 리스트
//                .build();
//
//        // 재고감소, 재고 저장에서 동시성이슈 생길 수 있음
//        for (OrderingSaveReqDto dto : dtos){ // OrderingSaveReqDto의 orderDetailDto리스트 요소 하나씩 꺼내옴
//            int quantity = dto.getProductCount();
//            // Product API에 요청을 통해 product객체를 조회해야 함
//
//            String productGetUrl = "http://product-service/product/"+ dto.getProductId();
//            HttpHeaders httpHeaders = new HttpHeaders();
//            String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
//            httpHeaders.set("Authorization", token);
//            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
//            ResponseEntity<CommonResDto> productEntity = restTemplate.exchange(productGetUrl, HttpMethod.GET, entity, CommonResDto.class);
//            ObjectMapper objectMapper = new ObjectMapper();
//            ProductDto productDto = objectMapper.convertValue(productEntity.getBody().getResult(), ProductDto.class);
//            System.out.println(productDto);
//            // redis를 통한 재고관리 및 재고잔량 확인
//            if (productDto.getName().contains("sale")) {
//                int newQuantity = (stockInventoryService.decreaseStock(dto.getProductId(), dto.getProductCount())).intValue(); // Long -> int로 형변환
//                if(newQuantity < 0){
//                    throw new IllegalArgumentException("재고부족");
//                }
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), dto.getProductCount()));
//                // rdb에 재고를 업데이트 -> 이벤트 기반의 아키텍처 구상
//                // rabbitmq를 통해 비동기적으로 이벤트 처리(발생하는 액션기반)
//            }else{
//                if(productDto.getStockQuantity() < quantity) { // 재고감소 시키는 코드
//                    throw new IllegalArgumentException("재고부족");
//                }
//                ProductUpdateStockDto productUpdateStockDto = new ProductUpdateStockDto(dto.getProductId(), dto.getProductCount());
//                kafkaTemplate.send("product-update-topic", productUpdateStockDto);
//            }
//
//            OrderDetail orderDetail = OrderDetail.builder() // 주문상세 OrderDetail 객체 조립
//                    .productId(productDto.getId())
//                    .ordering(ordering)
//                    .quantity(quantity)
//                    .build();
//            // OrderDetail Repository를 통해서 저장하는게 아니라 OrderingRepository를 통해서 저장함
//            ordering.getOrderDetails().add(orderDetail); // 여기서 이제 orderDetails 리스트에 orderDetail 하나씩 차례로 add해준다.
//        }
//
//        Ordering savedOrdering = orderingRepository.save(ordering); // 여기서 save해줘도 jpa에 의해서 선후관계 알아서 맞춰서 처리해주기 때문에 코드의 선후관계 안따져도 OK
//        // 이제 Ordering 객체를 save 해줬으므로 Ordering 객체의 id값 나올 것 -> 이후 필요한 요소 jpa가 알아서 순서를 처리해준다.
//        sseController.publishMessage(savedOrdering.fromEntity(), "admin@test.com");
//        return savedOrdering;
//    }




    // 주문 목록조회
    public List<OrderingListResDto> orderList(){
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderingListResDto> orderingListResDtos = new ArrayList<>();

        for(Ordering ordering : orderings){
            orderingListResDtos.add(ordering.fromEntity());
        }
        return orderingListResDtos;
    }

    // 내 주문만 조회
    // 주문 목록조회
    public List<OrderingListResDto> myOrders(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Ordering> orderingList = orderingRepository.findByMemberEmail(email);

        List<OrderingListResDto> orderingListResDtos = new ArrayList<>();

        for(Ordering ordering : orderingList){
            orderingListResDtos.add(ordering.fromEntity());
        }
        return orderingListResDtos;
    }

    public Ordering orderCancel(Long id){
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException("주문번호 없음"));
        ordering.updateSatus(OrderStatus.CANCELED); // 더티체킹(Transactional) -> 수정이므로 save 안해줘도 됨.
        return ordering;
    }
}
