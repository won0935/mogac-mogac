package com.mogacmogac.userservice.interfaces.login;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Log4j2
@Controller
public class HomeController {
    @GetMapping("/")
    public String index(){
        return "/index";
    }

    @GetMapping("/user")
    public String user(Principal principal){
        log.info("user name :: "+principal.getName());
        return "/user";
    }
}