package net.eulerframework.web.demo.controller;

import net.eulerframework.web.conf.WebConfig;
import net.eulerframework.web.demo.entity.User;
import net.eulerframework.web.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("demo")
public class TestController {

    @Autowired
    private UserRepository testRepository;

    @GetMapping("demo")
    public String demo() {
        return "Hello Spring Boot!!！";
    }

    @GetMapping("demoUser")
    public List<User> demoUser() {
        return this.testRepository.findAll();
    }

    @GetMapping("config")
    public int config() {
        return WebConfig.getValue();
    }

}
