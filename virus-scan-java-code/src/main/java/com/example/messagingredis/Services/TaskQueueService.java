package com.example.messagingredis.Services;

import com.example.messagingredis.ClamAV.ScanRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class TaskQueueService {

    private final RedisTemplate<String, ScanRequest> taskRedisTemplate;

    @Value("${application.queue-scan-request}")
    private String queueKey;

    @Autowired
    public TaskQueueService(RedisTemplate<String, ScanRequest> taskRedisTemplate) {
        this.taskRedisTemplate = taskRedisTemplate;
    }

    // Add item to the queue (tail)
    public void enqueue(ScanRequest value) {
        taskRedisTemplate.opsForList().rightPush(queueKey, value);
    }

    // Remove item from the front (head)
    public ScanRequest dequeue() {
       return taskRedisTemplate.opsForList().leftPop(queueKey);
    }

    // Blocking dequeue with timeout
    public ScanRequest blockingDequeue(long timeoutSeconds) {
        return taskRedisTemplate.opsForList().leftPop(queueKey, Duration.ofSeconds(timeoutSeconds));
    }

    // Peek front of the queue
    public ScanRequest peek() {
        return taskRedisTemplate.opsForList().index(queueKey, 0);
    }

    // Get queue size
    public Long size() {
        return taskRedisTemplate.opsForList().size(queueKey);
    }

    // Clear queue
    public void clear() {
        taskRedisTemplate.delete(queueKey);
    }

    public boolean ping() {
        return "PONG".equals(Objects.requireNonNull(taskRedisTemplate.getConnectionFactory()).getConnection().ping());

    }

}
