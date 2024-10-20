package com.example.security.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.security.entity.User;
import com.example.security.service.OtpService;
import com.example.security.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class SignUpController {

    private UserService userService;
    private OtpService otpService;

    public SignUpController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    @GetMapping("/signup")
    public String showSignUpForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/register")
    public String registerUserAccount(@ModelAttribute("user") User user, BindingResult result,
            RedirectAttributes redirectAttributes) {
        boolean emailExists = userService.checkIfEmailExists(user.getEmail());
        if (emailExists) {
            redirectAttributes.addFlashAttribute("error", "Email already exists. Please choose a different one.");
            return "redirect:/signup";
        }

        boolean usernameExists = userService.checkIfUsernameExists(user.getUsername());
        if (usernameExists) {
            redirectAttributes.addFlashAttribute("error", "Username already exists. Please choose a different one.");
            return "redirect:/signup";
        }

        if (result.hasErrors()) {
            return "signup";
        }

        String token = otpService.generateToken();
        otpService.storeToken(user.getEmail(), token, 15);

        user.setEnabled(false);
        user.setRoles(Set.of("USER_BASIC"));
        userService.saveUserWithRoles(user);

        // Send email with OTP
        // otpService.sendOtpEmail(user.getEmail(), token);

        log.info(token);

        return "redirect:/verify-otp?email=" + user.getEmail();
    }

    @GetMapping("/verify-otp")
    public String showOtpVerificationForm(@RequestParam String email, Model model) {
        model.addAttribute("email", email); // Add email to the model
        return "verify-otp"; // Name of the OTP verification template
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String email, @RequestParam String otp,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByEmail(email);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found.");
                return "redirect:/login";
            }

            String token = otpService.getToken(email);
            if (token != null && token.equals(otp)) {
                otpService.deleteToken(email);
                userService.updateUserEmailVerified(user); // Update user with new roles
                redirectAttributes.addFlashAttribute("verified", "Email verified successfully.");
                return "redirect:/login";
            }

            redirectAttributes.addFlashAttribute("error", "Invalid OTP.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getLocalizedMessage());
            return "redirect:/login";
        }
    }

}
