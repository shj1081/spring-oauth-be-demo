package dev.hyzoon.oauth_test.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // RedisTemplate = Redis에 데이터를 저장, 조회, 삭제하는 등의 작업을 수행하는 핵심 도구
    // connectionFactory = Spring Boot가 application.yml의 설정(host, port 등)을 바탕으로 자동으로 생성해주는 Redis 연결 객체
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        // Redis 서버와의 연결을 설정
        redisTemplate.setConnectionFactory(connectionFactory);

        // 직렬화(Serialization) 설정
        // Redis에 데이터를 저장할 때, Key와 Value는 바이트 배열(byte[])로 변환되어 저장
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
