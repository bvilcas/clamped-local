package io.clamped.demo;

import io.clamped.Clamped;
import io.clamped.ClampedConfig;
import io.clamped.Severity;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Fake e-commerce app that simulates real-world errors.
 * Sends events to local Postgres via the Clamped SDK.
 *
 * Run: java -jar clamped-demo/target/clamped-demo.jar
 */
public class DemoApp {

    public static void main(String[] args) throws Exception {
        Properties config = loadConfig();

        Clamped.init(ClampedConfig.builder()
            .jdbcUrl(config.getProperty("jdbcUrl"))
            .username(config.getProperty("username"))
            .password(config.getProperty("password"))
            .appName("shop-service")
            .environment("production")
            .flushIntervalSeconds(1)
            .autoCreateTables(true)
            .build());

        System.out.println("Running demo app — simulating errors...");

        PaymentService.processOrder("ORD-1001", "usr-441", 149.99);
        PaymentService.processOrder("ORD-1002", "usr-882", 79.50);
        InventoryService.checkStock("WIDGET-42", -3);
        InventoryService.checkStock("GADGET-7", 0);
        AuthService.login("usr-999", "expired-token-abc");
        AuthService.login("usr-123", null);
        OrderService.fetchOrder(99999);
        OrderService.fetchOrder(88888);
        CacheService.warmUp();

        System.out.println("Flushing events...");
        Thread.sleep(2000);
        Clamped.shutdown();
        System.out.println("Done. Check http://localhost:8080");
    }

    private static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        var path = Paths.get(System.getProperty("user.home"), ".clamped", "config.properties");
        try (var fis = new FileInputStream(path.toFile())) {
            props.load(fis);
        }
        return props;
    }
}
