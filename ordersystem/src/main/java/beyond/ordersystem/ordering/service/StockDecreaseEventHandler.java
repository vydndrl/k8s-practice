//package beyond.ordersystem.ordering.service;
//
//import beyond.ordersystem.common.configs.RabbitMqConfig;
//import beyond.ordersystem.ordering.domain.Ordering;
//import beyond.ordersystem.ordering.dto.StockDecreaseEvent;
//import beyond.ordersystem.product.domain.Product;
//import beyond.ordersystem.product.repository.ProductRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.persistence.EntityNotFoundException;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//@Component
//public class StockDecreaseEventHandler {
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//    private final ProductRepository productRepository;
//
//    public StockDecreaseEventHandler(ProductRepository productRepository) {
//        this.productRepository = productRepository;
//    }
//
//    public void publish(StockDecreaseEvent e){
//        rabbitTemplate.convertAndSend(RabbitMqConfig.STOCK_DECREASE_QUEUE, e);
//    }
//
//    // 트랜잭션이 완료된 이후에 그 다음 메시지 수신하므로, 동시성 이슈 발생 X
//    @Transactional
//    @RabbitListener(queues = RabbitMqConfig.STOCK_DECREASE_QUEUE)
//    public void listen(Message message){
//        String messageBody = new String(message.getBody());
//        // json 메시지를 ObjectMapper로 Parsing
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            StockDecreaseEvent stockDecreaseEvent = objectMapper.readValue(messageBody, StockDecreaseEvent.class);
//            Product product = productRepository.findById(stockDecreaseEvent.getProductId()).orElseThrow(()->new EntityNotFoundException("product is not found"));
//            product.updateStockQuantity(stockDecreaseEvent.getProductCount());
//        } catch (IOException e) {
//            e.printStackTrace();
//
//        }
//        System.out.println(messageBody);
//    }
//
//
//}
