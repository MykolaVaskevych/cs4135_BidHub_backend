package com.bidhub.order.messaging.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bidhub.outbox.publisher")
public class OutboxPublisherProperties {

    private boolean enabled = true;
    private long pollMs = 1000L;
    private int batchSize = 50;
    private int maxAttempts = 5;
    private long processingTimeoutMs = 60_000L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getPollMs() { return pollMs; }
    public void setPollMs(long pollMs) { this.pollMs = pollMs; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getProcessingTimeoutMs() { return processingTimeoutMs; }
    public void setProcessingTimeoutMs(long processingTimeoutMs) { this.processingTimeoutMs = processingTimeoutMs; }
}
