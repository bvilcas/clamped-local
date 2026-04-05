package io.clamped.demo;

import io.clamped.Clamped;
import io.clamped.Severity;

public class OrderService {

    public static void fetchOrder(long orderId) {
        try {
            queryDatabase(orderId);
        } catch (Exception e) {
            Clamped.add(e, ctx -> ctx
                .tag("database")
                .severity(Severity.HIGH)
                .meta("orderId", orderId)
                .meta("query", "SELECT * FROM orders WHERE id = " + orderId));
        }
    }

    private static void queryDatabase(long orderId) {
        throw new RuntimeException("Connection timeout after 30000ms — order " + orderId + " not found");
    }
}
