package com.datdofishing.order.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Job chạy mỗi phút. ShedLock đảm bảo chỉ 1 trong 3 instance chạy mỗi lần. */
@Component
public class StalePendingOrderJob {

    private static final Logger log = LoggerFactory.getLogger(StalePendingOrderJob.class);

    private final OrderService orders;
    private final Duration timeout;

    public StalePendingOrderJob(OrderService orders, @Value("${datdofishing.order.pending-timeout-minutes}") long minutes) {
        this.orders = orders;
        this.timeout = Duration.ofMinutes(minutes);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @SchedulerLock(name = "cancelStalePendingOrders", lockAtMostFor = "PT50S", lockAtLeastFor = "PT10S")
    public void run() {
        int n = orders.cancelStalePending(Instant.now().minus(timeout));
        if (n > 0) {
            log.info("Đã hủy {} đơn PENDING quá hạn", n);
        }
    }
}
