package com.example.base.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import com.example.base.entity.GlobalChatMessageEntity;

@Component
public class GlobalChatBroadcaster {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final List<Consumer<GlobalChatMessageEntity>> listeners = new CopyOnWriteArrayList<>();

    public synchronized void register(Consumer<GlobalChatMessageEntity> listener) {
        listeners.add(listener);
    }

    public synchronized void unregister(Consumer<GlobalChatMessageEntity> listener) {
        listeners.remove(listener);
    }

    public synchronized void broadcast(GlobalChatMessageEntity message) {
        for (Consumer<GlobalChatMessageEntity> listener : listeners) {
            executor.execute(() -> listener.accept(message));
        }
    }
}