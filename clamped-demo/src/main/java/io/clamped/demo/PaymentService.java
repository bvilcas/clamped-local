package io.clamped.demo;

import io.clamped.Clamped;
import io.clamped.Severity;

public class PaymentService {

    public static void processOrder(String orderId, String userId, double amount) {
        try {
            chargeCard(orderId, amount);
        } catch (Exception e) {
            Clamped.add(e, ctx -> ctx
                .tag("payment")
                .severity(Severity.HIGH)
                .meta("orderId", orderId)
                .meta("userId", userId)
                .meta("amount", amount));
        }
    }

    private static void chargeCard(String orderId, double amount) {
        throw new RuntimeException("Card declined: insufficient funds for order " + orderId);
    }
}
