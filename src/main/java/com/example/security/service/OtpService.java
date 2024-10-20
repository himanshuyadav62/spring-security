package com.example.security.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OtpService {

    private final Random random = new Random();
    private final RedisTemplate<String, String> redisTemplate;

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Method to generate an OTP or token
    public String generateToken() {
        int otp = 100000 + random.nextInt(900000); // Generate a 6-digit OTP
        return String.valueOf(otp);
    }

    // Store the token and set an expiry time
    public void storeToken(String email, String token, long expiryTimeMinutes) {
        redisTemplate.opsForValue().set(email + ":token", token, expiryTimeMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(email + ":count", "0", expiryTimeMinutes, TimeUnit.MINUTES); // Initialize access count
    }

    // Retrieve the token and check access count
    public String getToken(String email) {
        String token = redisTemplate.opsForValue().get(email + ":token");
        if (token == null) {
            throw new IllegalArgumentException("Token does not exist for this email.");
        }

        // Get the current count and check limits
        String countStr = redisTemplate.opsForValue().get(email + ":count");


        int currentCount = (countStr != null) ? Integer.parseInt(countStr) : 0;

        log.info("Current count: " + currentCount + "Count string: " + countStr);

        if (currentCount >= 5) {
            throw new IllegalArgumentException("Maximum access limit reached for this token.");
        }

        // Increment the access count
        redisTemplate.opsForValue().increment(email + ":count", 1);
        return token;
    }

    // Delete the token and count
    public void deleteToken(String email) {
        redisTemplate.delete(email + ":token");
        redisTemplate.delete(email + ":count");
    }
}
