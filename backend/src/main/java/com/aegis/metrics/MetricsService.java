package com.aegis.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter toolCallTotal;
    private final Counter agentFallbackTotal;
    private final Counter rateLimitBlockTotal;
    private final Counter actionBlockedTotal;
    
    // Timers
    private final Timer agentLatencyTimer;
    
    // Distribution summaries
    private final DistributionSummary agentLatencyMs;
    
    // Circuit breaker state
    private final ConcurrentHashMap<String, AtomicInteger> circuitBreakerFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> circuitBreakerOpenTime = new ConcurrentHashMap<>();
    
    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.toolCallTotal = Counter.builder("tool_call_total")
            .description("Total number of tool calls")
            .tag("tool", "unknown")
            .tag("ok", "false")
            .register(meterRegistry);
            
        this.agentFallbackTotal = Counter.builder("agent_fallback_total")
            .description("Total number of agent fallbacks")
            .tag("tool", "unknown")
            .register(meterRegistry);
            
        this.rateLimitBlockTotal = Counter.builder("rate_limit_block_total")
            .description("Total number of rate limit blocks")
            .register(meterRegistry);
            
        this.actionBlockedTotal = Counter.builder("action_blocked_total")
            .description("Total number of blocked actions")
            .tag("policy", "unknown")
            .register(meterRegistry);
        
        // Initialize timers
        this.agentLatencyTimer = Timer.builder("agent_latency_timer")
            .description("Agent execution time")
            .register(meterRegistry);
            
        // Initialize distribution summary
        this.agentLatencyMs = DistributionSummary.builder("agent_latency_ms")
            .description("Agent latency in milliseconds")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }
    
    /**
     * Record a tool call
     */
    public void recordToolCall(String tool, boolean success) {
        Counter.builder("tool_call_total")
            .tag("tool", tool)
            .tag("ok", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record an agent fallback
     */
    public void recordAgentFallback(String tool) {
        Counter.builder("agent_fallback_total")
            .tag("tool", tool)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record a rate limit block
     */
    public void recordRateLimitBlock() {
        rateLimitBlockTotal.increment();
    }
    
    /**
     * Record a blocked action
     */
    public void recordActionBlocked(String policy) {
        Counter.builder("action_blocked_total")
            .tag("policy", policy)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record agent latency
     */
    public void recordAgentLatency(long latencyMs) {
        agentLatencyMs.record(latencyMs);
        agentLatencyTimer.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record agent execution time using Timer.Sample
     */
    public Timer.Sample startAgentTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Stop agent timer and record the duration
     */
    public void stopAgentTimer(Timer.Sample sample, String agentName) {
        sample.stop(Timer.builder("agent_execution_time")
            .tag("agent", agentName)
            .register(meterRegistry));
    }
    
    /**
     * Check if circuit breaker is open for a service
     */
    public boolean isCircuitBreakerOpen(String serviceName) {
        AtomicInteger failures = circuitBreakerFailures.get(serviceName);
        if (failures == null) {
            return false;
        }
        
        if (failures.get() >= 3) {
            Long openTime = circuitBreakerOpenTime.get(serviceName);
            if (openTime != null && System.currentTimeMillis() - openTime < 30000) { // 30 seconds
                return true;
            } else {
                // Reset circuit breaker after 30 seconds
                failures.set(0);
                circuitBreakerOpenTime.remove(serviceName);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Record a circuit breaker failure
     */
    public void recordCircuitBreakerFailure(String serviceName) {
        AtomicInteger failures = circuitBreakerFailures.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        int currentFailures = failures.incrementAndGet();
        
        if (currentFailures >= 3) {
            circuitBreakerOpenTime.put(serviceName, System.currentTimeMillis());
        }
    }
    
    /**
     * Record a circuit breaker success
     */
    public void recordCircuitBreakerSuccess(String serviceName) {
        AtomicInteger failures = circuitBreakerFailures.get(serviceName);
        if (failures != null) {
            failures.set(0);
        }
        circuitBreakerOpenTime.remove(serviceName);
    }
}
