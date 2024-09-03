//package com.beyond.ordersystem.ordering.Service;
//
//import com.beyond.ordersystem.common.configs.RabbitMqConfig;
//import com.beyond.ordersystem.common.service.StockInventoryService;
//import com.beyond.ordersystem.ordering.dto.StockDecreaseEvent;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.persistence.EntityNotFoundException;
//
//@Component
//public class StockDecreaseEventHandler {
//
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
////    @Autowired
////    private ProductRepository productRepository;
//
//    public void publish(StockDecreaseEvent event){
//        rabbitTemplate.convertAndSend(RabbitMqConfig.STOCK_DECREASE_QUEUE, event);
//    }
//
//    // 트랜잭션이 완료된 이후에 그 다음 메시지 수신하므로 동시성이슈 없음
//    @Transactional
//    @RabbitListener(queues = RabbitMqConfig.STOCK_DECREASE_QUEUE) // 큐가 어떤 큐를 바라고 있을지? -> orderService와 별도의 트랜잭션으로 돎
//    public void listen(Message message) throws JsonProcessingException {
//        String messageBody = new String(message.getBody()); // {"productId":1,"productCount":1} json 형태로 메시지 출력
//        // message를 가져다가 재고 업데이트
//        // json 메시지를 ObjectMapper로 직접 parsing
//        ObjectMapper objectMapper = new ObjectMapper();
//        try{
//            // 형변환
//            StockDecreaseEvent stockDecreaseEvent = objectMapper.readValue(messageBody, StockDecreaseEvent.class);
//            // product 찾아옴
////            Product product = productRepository.findById(stockDecreaseEvent.getProductId())
////                    .orElseThrow(()-> new EntityNotFoundException("product 없음"));
////            product.updateStockQuantity(stockDecreaseEvent.getProductCount());
//
//        }catch (JsonProcessingException e){
//            throw new RuntimeException(e);
//        }
//    }
//}
