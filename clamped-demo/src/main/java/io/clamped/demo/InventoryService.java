package io.clamped.demo;

import io.clamped.Clamped;
import io.clamped.Severity;

public class InventoryService {

    public static void checkStock(String sku, int stock) {
        if (stock < 0) {
            Clamped.flag("Negative stock detected", ctx -> ctx
                .tag("inventory")
                .severity(Severity.MEDIUM)
                .meta("sku", sku)
                .meta("stock", stock)
                .meta("warehouseId", "WH-01"));
        } else if (stock == 0) {
            Clamped.flag("Out of stock", ctx -> ctx
                .tag("inventory")
                .severity(Severity.LOW)
                .meta("sku", sku));
        }
    }
}
