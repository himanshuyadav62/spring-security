package com.example.security.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class HomeController {
    
    @GetMapping("/")    
    public SecurityContext getMethodName() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context;  
    }

    
    

}
