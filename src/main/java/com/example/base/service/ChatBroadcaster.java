package com.example.base.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.example.base.entity.MessageEntity;

public class ChatBroadcaster {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    private static final Map<Integer, List<Consumer<MessageEntity>>> listeners = new ConcurrentHashMap<>();

    public static Registration register(Integer requestId, Consumer<MessageEntity> listener) {
        listeners.computeIfAbsent(requestId, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> {
            List<Consumer<MessageEntity>> list = listeners.get(requestId);
            if (list != null) {
                list.remove(listener);
            }
        };
    }

    public static void broadcast(Integer requestId, MessageEntity message) {
        List<Consumer<MessageEntity>> list = listeners.get(requestId);
        if (list == null) {
            return;
        }
        for (Consumer<MessageEntity> listener : list) {
            executor.execute(() -> listener.accept(message));
        }
    }

    @FunctionalInterface
    public interface Registration {
        void remove();
    }
}