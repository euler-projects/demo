package net.eulerframework.web.demo.controller;

import net.eulerframework.web.core.conf.AppConfiguration;
import net.eulerframework.web.core.conf.reader.AppConfigReader;
import net.eulerframework.web.demo.entity.User;
import net.eulerframework.web.demo.repository.UserRepository;
import net.eulerframework.web.demo.vo.TestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("configReader")
    public AppConfigReader configReader() {
        return AppConfiguration.getReader();
    }

    @PostMapping("jsonTest")
    public TestVO post(@RequestBody TestVO data) {
        return data;
    }

}
