package com.eshop.common.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Production-grade Redis Configuration
 * 
 * Features:
 * 1. Connection pooling (Lettuce)
 * 2. Cluster support with topology refresh
 * 3. Proper serialization with type info
 * 4. Timeout configuration
 * 5. Health check
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:5000}")
    private long timeout;

    @Value("${spring.redis.cluster.enabled:false}")
    private boolean clusterEnabled;

    @Value("${spring.redis.cluster.nodes:}")
    private List<String> clusterNodes;

    // Connection pool settings
    @Value("${spring.redis.lettuce.pool.max-active:16}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    @Value("${spring.redis.lettuce.pool.max-wait:2000}")
    private long maxWait;

    /**
     * Connection pool configuration
     */
    @Bean
    public GenericObjectPoolConfig<?> poolConfig() {
        GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWait(Duration.ofMillis(maxWait));
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        return config;
    }

    /**
     * Lettuce client configuration with connection pool
     */
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration() {
        // Socket options
        SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(timeout))
            .keepAlive(true)
            .build();

        // Timeout options
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
            .timeoutCommands(true)
            .fixedTimeout(Duration.ofMillis(timeout))
            .build();

        if (clusterEnabled) {
            // Cluster topology refresh for automatic failover detection
            ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30))
                .enableAllAdaptiveRefreshTriggers()
                .build();

            ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(timeoutOptions)
                .topologyRefreshOptions(topologyRefreshOptions)
                .autoReconnect(true)
                .build();

            return LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig())
                .clientOptions(clusterClientOptions)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();
        } else {
            ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(timeoutOptions)
                .autoReconnect(true)
                .build();

            return LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig())
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();
        }
    }

    /**
     * Redis connection factory
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        if (clusterEnabled && !clusterNodes.isEmpty()) {
            log.info("Configuring Redis Cluster with nodes: {}", clusterNodes);
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
            if (password != null && !password.isEmpty()) {
                clusterConfig.setPassword(password);
            }
            return new LettuceConnectionFactory(clusterConfig, lettuceClientConfiguration());
        } else {
            log.info("Configuring Redis Standalone: {}:{}", host, port);
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
            standaloneConfig.setDatabase(database);
            if (password != null && !password.isEmpty()) {
                standaloneConfig.setPassword(password);
            }
            return new LettuceConnectionFactory(standaloneConfig, lettuceClientConfiguration());
        }
    }

    /**
     * ObjectMapper for Redis serialization
     * Includes type info for proper deserialization
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8 time support
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Allow unknown properties for forward compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Include type information in JSON for proper deserialization
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        return mapper;
    }

    /**
     * RedisTemplate for object serialization
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key serializer - always String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value serializer - JSON with type info
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setEnableDefaultSerializer(false);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * StringRedisTemplate for simple string operations
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
