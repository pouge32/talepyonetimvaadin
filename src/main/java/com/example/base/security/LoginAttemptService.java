package com.example.base.security;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    private final ConcurrentHashMap<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        Attempt attempt = attemptsCache.getOrDefault(key, new Attempt());
        attempt.increment();

        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            attempt.setLockTime(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
        }
        attemptsCache.put(key, attempt);
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attemptsCache.get(key);
        if (attempt != null && attempt.getAttempts() >= MAX_ATTEMPTS) {
            if (attempt.getLockTime() != null && LocalDateTime.now().isBefore(attempt.getLockTime())) {
                return true; 
            } else {
                attemptsCache.remove(key);
                return false;
            }
        }
        return false;
    }

    private static class Attempt {
        private int attempts = 0;
        private LocalDateTime lockTime;

        public void increment() { this.attempts++; }
        public int getAttempts() { return attempts; }
        public LocalDateTime getLockTime() { return lockTime; }
        public void setLockTime(LocalDateTime lockTime) { this.lockTime = lockTime; }
    }
}