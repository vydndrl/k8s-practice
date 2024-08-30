package beyond.ordersystem.ordering.controller;

import beyond.ordersystem.ordering.dto.OrderingListResDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SseController {
    // SseEmitter는 연결된 사용자 정보를 의미
    // ConcurrentHashMap은 Thread-safe한 map(동시성 이슈 발생 X)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    @Qualifier("4")
    private final RedisTemplate<String, Object> sseRedisTemplate;

    public SseController(@Qualifier("4") RedisTemplate<String, Object> sseRedisTemplate) {
        this.sseRedisTemplate = sseRedisTemplate;
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(){
        SseEmitter emitter = new SseEmitter(14400*60*1000L);  // 30분 정도로 emitter
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        emitters.put(email, emitter);
        emitter.onCompletion(()->emitters.remove(email));
        emitter.onTimeout(()->emitters.remove(email));
        try{
            emitter.send(SseEmitter.event().name("connect").data("connected!!!"));
        }catch(IOException e){
            e.printStackTrace();
        }
        return emitter;
    }


    public void publishMessage(OrderingListResDto dto, String email){
        SseEmitter emitter = emitters.get(email);
        if(emitter != null){
            try {
                emitter.send(SseEmitter.event().name("ordered").data(dto));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            sseRedisTemplate.convertAndSend(email, dto);
        }
    }

}
