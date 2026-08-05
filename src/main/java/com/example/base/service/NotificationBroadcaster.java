package com.example.base.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

@Service
public class NotificationBroadcaster {
    
    private final Map<Integer, CopyOnWriteArraySet<Consumer<Void>>> listeners = new ConcurrentHashMap<>();

    public void register(Integer userId, Consumer<Void> listener) {
        listeners.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(listener);
    }

    public void unregister(Integer userId, Consumer<Void> listener) {
        CopyOnWriteArraySet<Consumer<Void>> userListeners = listeners.get(userId);
        if (userListeners != null) {
            userListeners.remove(listener);
        }
    }

    public void broadcast(Integer userId) {
        CopyOnWriteArraySet<Consumer<Void>> userListeners = listeners.get(userId);
        if (userListeners != null) {
            for (Consumer<Void> listener : userListeners) {
                listener.accept(null); 
            }
        }
    }
}