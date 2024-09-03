package com.beyond.ordersystem.common.configs;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}") // application.yml파일의 spring > redis > host의 정보를 소스코드의 변수로 가져오는 것
    public String host;

    @Value("${spring.redis.port}") // application.yml파일의 spring > redis > port의 정보를 소스코드의 변수로 가져오는 것
    public int port;

    @Bean
    @Qualifier("2") // default : 1 (Bean의 식별자)
    // Qualifier는 주로 여러 Bean이 정의된 경우 특정 Bean을 주입받기 위해 사용된다.
    // RedisConnectionFactory는 Redis서버와의 연결을 설정하는 역할 (정보성 요소)
    // LettuceConnectionFactory는 RedisConnectionFactory의 구현체로서 실질적인 역할 수행
    public RedisConnectionFactory redisConnectionFactory(){
//        return new LettuceConnectionFactory(host, port);

        // 유연하게 사용
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1); // 1번 DB 사용
//        configuration.setPassword("1234");
        return new LettuceConnectionFactory(configuration);
    }

    // redisTemplate은 redis와 상호작용할 때 redis key,value의 형식을 정의 (실질적으로 데이터를 넣는 곳)
    @Bean
    @Qualifier("2")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("2") RedisConnectionFactory redisConnectionFactory){
        // Object : 보통 json형태의 데이터가 들어올 것
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // String 형태를 직렬화 시키겠다. (String으로 직렬화)
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer()); //json으로 직렬화
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("3") // default : 1
    // RedisConnectionFactory는 Redis서버와의 연결을 설정하는 역할 (정보성 요소)
    // LettuceConnectionFactory는 RedisConnectionFactory의 구현체로서 실질적인 역할 수행
    public RedisConnectionFactory stockFactory(){
//        return new LettuceConnectionFactory(host, port);

        // 유연하게 사용
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(2); // 2번 DB 사용
//        configuration.setPassword("1234");
        return new LettuceConnectionFactory(configuration);
    }

    // redisTemplate은 redis와 상호작용할 때 redis key,value의 형식을 정의 (실질적으로 데이터를 넣는 곳)
    // RedisTemplate : Redis와 상호작용하기 위한 템플릿으로, 데이터의 직렬화 방식과 연결팩토리를 설정한다.
    // key는 문자열로 직렬화하고 값은 JSON 형태로 직렬화 한다.
    @Bean
    @Qualifier("3")
    public RedisTemplate<String, Object> stockRedisTemplate(@Qualifier("3") RedisConnectionFactory redisConnectionFactory){
        // Object : 보통 json형태의 데이터가 들어올 것
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // String 형태를 직렬화 시키겠다. (String으로 직렬화), Redis의 키를 문자열로 직렬화
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer()); //json으로 직렬화, Redis의 값을 JSON형태로 직렬화
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("4") // default : 1
    public RedisConnectionFactory sseFactory(){
        // 유연하게 사용
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(3);
//        configuration.setPassword("1234");
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("4")
    public RedisTemplate<String, Object> sseRedisTemplate(@Qualifier("4") RedisConnectionFactory sseFactory){
        // Object : 보통 json형태의 데이터가 들어올 것
        // key : email, value : json형태의 dto
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // String 형태를 직렬화 시키겠다. (String으로 직렬화), Redis의 키를 문자열로 직렬화

        // dto의 구조가 객체안에 객체가 있으므로(직렬화 이슈) 아래와 같이 serializer 커스텀
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        serializer.setObjectMapper(objectMapper);

        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setConnectionFactory(sseFactory);
        return redisTemplate;
    }

    // 리스너 객체 생성
    @Bean
    @Qualifier("4")
    public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("4")RedisConnectionFactory sseFactory){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(sseFactory);
        return container;
    }



    // redisTemplate.opsForValue().set(key, value)
    // redisTemplate.opsForValue().get(key)
    // redisTemplate.opsForValue().increment 또는 decrement
}
