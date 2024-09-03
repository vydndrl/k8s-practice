package com.beyond.ordersystem.ordering.Controller;

import com.beyond.ordersystem.ordering.dto.OrderingListResDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SseController implements MessageListener{

    // SseEmmiter는 연결된 사용자 정보를 의미
    // ConcurrentHashMap은 Thread-safe한 map (동시성 이슈 발생안함) 내부적으로 synchronized하게 작업되어있다.
    // String : 사용자 email(사용자 연결정보)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>(); // emitter 사용자정보 목록관리 목적
    // 여러번 구독을 방지하기 위한 ConcurrentHashSet 변수 생성
    private final Set<String> subscribeList = ConcurrentHashMap.newKeySet();

    @Qualifier("4")
    private final RedisTemplate<String, Object> sseRedisTemplate;

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public SseController(@Qualifier("4") RedisTemplate<String, Object> sseRedisTemplate, RedisMessageListenerContainer redisMessageListenerContainer) {
        this.sseRedisTemplate = sseRedisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    // email에 해당되는 메시지를 listen하는 listener를 추가한 것.
    public void subscribeChannel(String email){
        // 이미 구독한 email일 경우에는 더이상 구독하지 않는 분기처리
        if(!subscribeList.contains(email)){
            MessageListenerAdapter listenerAdapter = createListenerAdapter(this);
            redisMessageListenerContainer.addMessageListener(listenerAdapter, new PatternTopic(email));
            subscribeList.add(email);
        }

    }

    private MessageListenerAdapter createListenerAdapter(SseController sseController){
        return new MessageListenerAdapter(sseController, "onMessage");
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(){
        SseEmitter emitter = new SseEmitter(1440*60*1000L); //하루 정도로 emitter 유효시간 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        emitters.put(email, emitter); // emitter : 사용자 관련정보 들어있음(위치, 아이디 등)
        emitter.onCompletion(()->emitters.remove(email));
        emitter.onTimeout(()->emitters.remove(email));

        try{ // 응답성공했다는 응답 (eventName : 카테고리, object: 실제 메시지)
            emitter.send(SseEmitter.event().name("connect").data("connected!!!"));
        }catch(IOException e){
            e.printStackTrace();
        }
        // redis에 대해서도 subscribe 한다.
        subscribeChannel(email);
        return emitter;
    }

    public void publishMessage(OrderingListResDto dto, String email){
        SseEmitter emitter = emitters.get(email);
//        if(emitter != null){
//            try {
//                emitter.send(SseEmitter.event().name("ordered").data(dto)); // admin 유저에게 주문이 들어옴을 알려줌
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }else{
            // message 받는 사람의 정보를 해당서버가 갖고있지 않으면 redis에 publish한다
            // 받는 사람의 정보를 가진 서버가 subscribe할 수 있도록
            sseRedisTemplate.convertAndSend(email, dto);
//        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        message 내용 parsing (직접 parsing 해줘야 함)
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            OrderingListResDto dto = objectMapper.readValue(message.getBody(), OrderingListResDto.class);

            String email = new String(pattern, StandardCharsets.UTF_8);
            SseEmitter emitter = emitters.get(email);
            System.out.println(email);
            if(emitter != null){
                emitter.send(SseEmitter.event().name("ordered").data(dto)); // admin 유저에게 주문이 들어옴을 알려줌
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
