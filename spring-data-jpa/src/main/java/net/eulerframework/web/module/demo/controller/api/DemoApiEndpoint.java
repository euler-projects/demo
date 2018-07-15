package net.eulerframework.web.module.demo.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.core.annotation.ApiEndpoint;
import net.eulerframework.web.core.base.controller.ApiSupportWebController;
import net.eulerframework.web.module.authentication.entity.User;
import net.eulerframework.web.module.authentication.repository.UserRediesTemplate;

@ApiEndpoint
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoApiEndpoint extends ApiSupportWebController {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private UserRediesTemplate userRediesTemplate;
    
    @RequestMapping(value = "test") 
    public String test() {
        this.stringRedisTemplate.opsForValue().set("string", "Hello Ajax");
        User user = new User();
        user.setUsername("aaa");
        user.setPassword("bbb");
        this.userRediesTemplate.opsForValue().set("obj", user);
        return "Hello Ajax";
    }
}
