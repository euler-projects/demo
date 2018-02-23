package net.eulerframework.startweb.controller;

import net.eulerframework.startweb.entity.User;
import net.eulerframework.startweb.repository.UserRepository;
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

    @GetMapping("demoUser")
    public List<User> demoUser() {
        return this.testRepository.findAll();
    }

}
