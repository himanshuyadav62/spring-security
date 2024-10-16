package com.example.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.security.entity.User;
import com.example.security.service.UserService;

@Controller
public class SignUpController {

   
    private UserService userService;

    public SignUpController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String showSignUpForm(Model model) {
        model.addAttribute("user", new User()); 
        return "signup";
    }

    @PostMapping("/register")
    public String registerUserAccount(@ModelAttribute("user") User user, BindingResult result) {
        if (result.hasErrors()) {
            return "signup";
        }
        userService.saveUser(user);
        return "redirect:/login?registered"; // Redirect to login page after successful signup
    }
}
