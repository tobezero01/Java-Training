package com.ducnhu.common.cache;

import com.ducnhu.common.dto.Intent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}") private String redisHost;
    @Value("${spring.redis.port}") private int redisPort;
    @Value("${spring.redis.password}") private String redisPassword;
    @Value("${spring.redis.database}") private int redisDb;
    @Value("${spring.redis.timeout}") private Duration redisTimeout;
    @Value("${spring.redis.ssl}") private boolean redisSsl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            configuration.setPassword(RedisPassword.of(redisPassword));
        }
        configuration.setDatabase(redisDb);

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder().commandTimeout(redisTimeout);
        if (redisSsl) builder.useSsl();

        return new LettuceConnectionFactory(configuration, builder.build());
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public ObjectMapper cacheObjectMapper() {
        ObjectMapper objectMapper =  new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        objectMapper.registerModule(hibernate6Module);

        return objectMapper;
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper cacheObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        StringRedisSerializer stringSer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        template.setKeySerializer(stringSer);
        template.setHashKeySerializer(stringSer);

        template.setValueSerializer(jsonSer);
        template.setHashValueSerializer(jsonSer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean("intentRedisTemplate")
    public RedisTemplate<String, Intent> intentRedisTemplate(
            RedisConnectionFactory cf
    ) {
        RedisTemplate<String, Intent> tpl = new RedisTemplate<String, Intent>();
        tpl.setConnectionFactory(cf);

        StringRedisSerializer keySer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer valSer = new Jackson2JsonRedisSerializer<>(Intent.class);

        tpl.setKeySerializer(keySer);
        tpl.setValueSerializer(valSer);
        tpl.setHashKeySerializer(keySer);
        tpl.setHashValueSerializer(valSer);

        tpl.afterPropertiesSet();
        return tpl;
    }

}
