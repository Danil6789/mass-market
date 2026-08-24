package com.marketplace.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mock-payment configuration for the saga. Bound to {@code app.payment.*} in
 * {@code application.yml}.
 *
 * <p>The {@code success-rate} is interpreted as the probability that the
 * mock payment succeeds on a given call. {@code delay-ms} simulates the
 * latency of a real payment processor.</p>
 */
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    /** Probability in [0, 1] that the mock payment succeeds. */
    private double successRate = 0.95;

    /** Artificial latency to simulate a real PSP round-trip. */
    private long delayMs = 200;

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }
}
