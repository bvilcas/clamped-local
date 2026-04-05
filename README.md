# Clamped! Local

Self-hosted error tracking platform for Java. Drop the SDK into your app, capture exceptions in one line, and triage them from a web dashboard or terminal CLI. All data stored in your own Postgres database.

No cloud. No DSN keys. No pricing tiers. Just a JAR and a dashboard you own.

---

## Features

- **One-line error capture**: `Clamped.add(e)` in any catch block
- **Automatic deduplication**: same error 1000 times = 1 row + occurrence counter
- **Web dashboard**: filterable events table, stats page, bulk actions, CSV export
- **CLI**: triage events directly from the terminal without opening a browser
- **Zero overhead**: async queue, never blocks your app
- **Graceful degradation**: if Postgres is down, events queue in memory and drop silently

---

## Quick Start

### 1. Start Postgres

```bash
docker compose up -d
```

### 2. Build

```bash
./mvnw.cmd -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests
```

### 3. Start the server

```bash
java -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar
```

Open [http://localhost:8080](http://localhost:8080)

### 4. (Optional) Seed sample data

Click **Seed Sample Data** in the nav, or run the demo app:

```bash
java -jar clamped-demo/target/clamped-demo.jar
```

---

## SDK Integration

### Add the dependency

```xml
<dependency>
    <groupId>io.clamped</groupId>
    <artifactId>clamped-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Initialize once at startup

```java
Clamped.init(ClampedConfig.builder()
    .jdbcUrl("jdbc:postgresql://localhost:5432/clamped_db")
    .username("postgres")
    .password("secret")
    .appName("my-service")
    .environment("production")
    .autoCreateTables(true)
    .captureUncaughtExceptions(true)
    .build());
```

### Capture exceptions

```java
try {
    processPayment(order);
} catch (PaymentException e) {
    Clamped.add(e);                         // simplest form
    Clamped.add(e, "payment-flow");         // with a tag
    Clamped.add(e, ctx -> ctx               // with full context
        .tag("payment-flow")
        .severity(Severity.HIGH)
        .meta("orderId", order.getId())
    );
}
```

### Capture warnings (non-exception events)

```java
Clamped.flag("Negative stock detected", ctx -> ctx
    .tag("inventory")
    .severity(Severity.MEDIUM)
    .meta("sku", item.getSku())
);
```

---

## CLI

```bash
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar list
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar list --status all
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar show 42
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar resolve 42
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar stats
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar purge --before 30d
```

---

## Architecture

```
Your Java App
    └── Clamped.add(e)
            └── async queue
                    └── background flush (every 5s)
                            └── Postgres (clamped_events)
                                    ├── clamped-server  →  Web Dashboard (localhost:8080)
                                    └── clamped-cli     →  Terminal
```

---

## Stack

| Layer | Technology |
|---|---|
| SDK | Java, JDBC only (no Spring) |
| Server | Spring Boot, JDBC |
| Frontend | Vue 3, Vuetify, Chart.js |
| Database | PostgreSQL |
| CLI | Java, JDBC only |

---

## License

MIT
