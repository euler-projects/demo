package net.eulerframework.web.module.demo.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class Beans {

    @Bean
    public JedisPoolConfig getJedisPoolConfig() {
        JedisPoolConfig JedisPoolConfig = new JedisPoolConfig();
        JedisPoolConfig.setMaxIdle(1000);
        JedisPoolConfig.setMaxTotal(100);
        JedisPoolConfig.setMinIdle(100);
        return JedisPoolConfig;
    }
    
    @Bean
    public RedisStandaloneConfiguration getRedisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setDatabase(0);
        redisStandaloneConfiguration.setHostName("127.0.0.1");
        redisStandaloneConfiguration.setPassword(RedisPassword.none());
        redisStandaloneConfiguration.setPort(6379);
        return redisStandaloneConfiguration;
    }

    @Bean
    public JedisConnectionFactory getJedisConnectionFactory(
            RedisStandaloneConfiguration redisStandaloneConfiguration, 
            JedisPoolConfig jedisPoolConfig) {
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }
    
    @Bean("stringRedisSerializer")
    public StringRedisSerializer getStringRedisSerializer() {
        return new StringRedisSerializer();
    }
    
    @Bean
    public StringRedisTemplate getStringRedisTemplate(
            JedisConnectionFactory jedisConnectionFactory,
            StringRedisSerializer stringRedisSerializer) {
        StringRedisTemplate stringRedisTemplate= new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(jedisConnectionFactory);
        //redisTemplate.setStringSerializer(stringRedisSerializer);
        return stringRedisTemplate;
    }
    
//    @Bean
//    public RedisTemplate<?, ?> getRedisTemplate(
//            JedisConnectionFactory jedisConnectionFactory,
//            StringRedisSerializer stringRedisSerializer) {
//        RedisTemplate<?, ?> redisTemplate= new RedisTemplate<Object, Object>();
//        redisTemplate.setConnectionFactory(jedisConnectionFactory);
//        redisTemplate.setStringSerializer(stringRedisSerializer);
//        return redisTemplate;
//    }

    private final static String EMAIL_SIGN = 
            "<p>------------------</p>"
            + "<p>Best Regards</p>";

    private final static String RESET_PASSWORD_EMAIL_CONTENT = 
            "<p>尊敬的先生/女士，</p>"
            + "<p>您可使用下面的链接重置密码，十分钟内有效：</p>"
            + "<p><a href=\"${resetPasswordUrl}\">${resetPasswordUrl}</a></p>"
            + "<br>"
            + "<p>Dear Sir/Madam,</p>"
            + "<p>You can use the following link to reset your password in 10 minutes:</p>"
            + "<p><a href=\"${resetPasswordUrl}\">${resetPasswordUrl}</a></p>"
            + EMAIL_SIGN;

    @Bean
    public String resetPasswordEmailContent() {
        return RESET_PASSWORD_EMAIL_CONTENT;
    }

    @Bean
    public String resetPasswordEmailSubject() {
        return "[Euler Projects] Please reset your password";
    }
}
