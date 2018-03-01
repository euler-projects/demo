package net.eulerframework.startweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("web")
public class TestWebController {

    @GetMapping("demo")
    public String demo(Model model) {
        model.addAttribute("a", "a");
        return "demo";
    }
}
