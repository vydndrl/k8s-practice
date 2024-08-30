package beyond.ordersystem.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockInventoryService {
    @Qualifier("3")
    private final RedisTemplate<String, Object> redisTemplate;

    public StockInventoryService(@Qualifier("3") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 상품 등록시 increaseStock 호출
    public Long increaseStock(Long productId, int quantity){
        // 레디스가 음수까지 내려갈 경우, 추후 재고 update 상황에서 increase 값이 정확하지 않을 수 있으므로 음수이면 0으로 setting로직이 필요

        // 아래 메서드의 리턴값은 잔량값을 리턴
        return redisTemplate.opsForValue().increment(String.valueOf(productId), quantity);
    }
    // 주문 등록시 decreaseStock 호출
    public Long decreaseStock(Long productId, int quantity){
        Object remains = redisTemplate.opsForValue().get(String.valueOf(productId));
        int longRemains = Integer.parseInt(remains.toString());
        if (longRemains < quantity){
            return -1L;
        } else {
            // 남아있는 잔량을 리턴
            return redisTemplate.opsForValue().decrement(String.valueOf(productId), quantity);
        }
    }
}
