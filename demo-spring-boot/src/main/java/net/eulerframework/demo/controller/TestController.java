package net.eulerframework.demo.controller;

import net.eulerframework.demo.entity.User;
import net.eulerframework.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("test")
public class TestController {

    @Autowired
    private UserRepository testRepository;

    @GetMapping("demo")
    public String demo() {
        return "Hello Spring Boot!!";
    }

    @GetMapping("demoUser")
    public List<User> demoUser() {
        return this.testRepository.findAll();
    }

}
