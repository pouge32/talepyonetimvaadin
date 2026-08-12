package com.example.base.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.example.base.entity.InternalCommentEntity;

@Component
public class TeamChatBroadcaster {
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    private final Map<Integer, List<Consumer<InternalCommentEntity>>> requestListeners = new ConcurrentHashMap<>();
    
    private final List<Consumer<InternalCommentEntity>> globalListeners = new CopyOnWriteArrayList<>();

    public synchronized void registerForRequest(Integer requestId, Consumer<InternalCommentEntity> listener) {
        requestListeners.computeIfAbsent(requestId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public synchronized void unregisterForRequest(Integer requestId, Consumer<InternalCommentEntity> listener) {
        List<Consumer<InternalCommentEntity>> list = requestListeners.get(requestId);
        if (list != null) list.remove(listener);
    }

    public synchronized void registerGlobal(Consumer<InternalCommentEntity> listener) {
        globalListeners.add(listener);
    }

    public synchronized void unregisterGlobal(Consumer<InternalCommentEntity> listener) {
        globalListeners.remove(listener);
    }

    public synchronized void broadcast(InternalCommentEntity comment) {
        List<Consumer<InternalCommentEntity>> list = requestListeners.get(comment.getRequest().getRequestId());
        if (list != null) {
            for (Consumer<InternalCommentEntity> listener : list) {
                executor.execute(() -> listener.accept(comment));
            }
        }
        for (Consumer<InternalCommentEntity> listener : globalListeners) {
            executor.execute(() -> listener.accept(comment));
        }
    }
}