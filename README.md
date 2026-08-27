# Clamped! Local

Self-hosted error tracking platform for Java. Drop the SDK into your app, capture exceptions in one line, and triage them from a web dashboard or terminal CLI. All data stored in your own Postgres database.

No cloud. No DSN keys. No pricing tiers. Just a JAR and a dashboard you own.

---

## Features

- **One-line error capture**: `Clamped.add(e)` in any catch block
- **Automatic deduplication**: same error 1000 times = 1 row + occurrence counter
- **Web dashboard**: filterable events table, stats page, bulk actions, CSV export, resolve with notes, collapsible nav, dark/light mode
- **CLI**: triage events directly from the terminal without opening a browser
- **Zero overhead**: async queue, never blocks your app
- **Graceful degradation**: if Postgres is down, events queue in memory and drop silently

---

## Prerequisites

Install these once per machine before following Quick Start:

- **Java 11 JDK** (e.g. [Eclipse Adoptium](https://adoptium.net/)) - required by the parent `pom.xml`. Check with `java -version`.
- **Docker Desktop** - runs Postgres via `docker-compose.yml`.
- **PowerShell script execution allowed for your user** - `run.ps1` is a `.ps1` file, and PowerShell blocks those by default. Run once:
  ```powershell
  Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
  ```
  (Maven's `mvnw.cmd` is unaffected by this - it's a `.cmd` file, not `.ps1`.)
- **Node.js LTS + npm** - only needed if you're working on the frontend (`clamped-ui/`), not for the packaged server/CLI/demo jars. Get it from [nodejs.org](https://nodejs.org) or `winget install OpenJS.NodeJS.LTS`.

## Quick Start

### 1. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` and set your own `POSTGRES_PASSWORD` (and matching `CLAMPED_PASSWORD` -
keep the two in sync). `.env` is gitignored and never committed.

### 2. Start Postgres

```bash
docker compose up -d
```

Compose reads `.env` automatically. If you skip step 1, this fails loudly with a
message telling you to set `POSTGRES_PASSWORD` rather than silently using a
default.

### 3. Build

```bash
./mvnw.cmd -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests
```

### 4. Start the server

Plain `java -jar` does **not** read `.env` (only Docker Compose does), so use
`run.ps1`, which loads `.env` into the process environment first:

```powershell
.\run.ps1 server
```

Open [http://localhost:8080](http://localhost:8080)

### 5. (Optional) Seed sample data

Two ways to get sample data in:

- **Run the demo app** - generates real events through the actual SDK pipeline:
  ```powershell
  .\run.ps1 demo
  ```
- **"Seed Sample Data" button / `/api/seed` endpoint** - canned data, but gated behind
  `DEMO_MODE` (returns 403 otherwise). Set it before starting the server:
  ```powershell
  $env:DEMO_MODE = "true"
  .\run.ps1 server
  ```
  Then click the nav button, or `curl -X POST http://localhost:8080/api/seed`.

### 6. (Optional) Run the frontend in dev mode

The server jar only serves a built frontend from `clamped-server/src/main/resources/static/`,
which is gitignored and empty until you build `clamped-ui` into it. For active frontend
work, run Vite's dev server instead (requires Node.js - see Prerequisites):

```bash
cd clamped-ui
npm install
npm run dev
```

Vite serves its own port (check the terminal output, typically `5173`) and proxies API
calls through to the backend on `:8080` - keep both running side by side.

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

```powershell
.\run.ps1 cli list
.\run.ps1 cli list --status all
.\run.ps1 cli show 42
.\run.ps1 cli resolve 42
.\run.ps1 cli stats
.\run.ps1 cli purge --before 30d
```

(`run.ps1` just loads `.env` then runs `java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar` with your args appended - see [Quick Start](#quick-start).)

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `running scripts is disabled on this system` | PowerShell's default execution policy blocks `.ps1` files | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` (see Prerequisites) |
| `-classpath requires class path specification` from `mvnw.cmd` | Vendored Maven distro under `.mvn/` is incomplete/corrupt | Delete `.mvn/apache-maven-*` and re-run `mvnw.cmd` - it downloads a fresh copy |
| `PSQLException: ... password is an empty string` | Server started without `CLAMPED_*` env vars set (plain `java -jar` does **not** read `.env`) | Use `.\run.ps1 server` instead, which loads `.env` first |
| `Port 8080 was already in use` | A previous server instance is still running (didn't crash - just failed its DB connection and kept Tomcat up) | Find and stop it: `netstat -ano \| findstr :8080`, then `taskkill /PID <pid> /F` |
| Clicking "Seed Sample Data" does nothing | Either the backend isn't running, or `DEMO_MODE` isn't set (endpoint returns 403, and the UI has no error handling for it) | Confirm the server is up, set `$env:DEMO_MODE="true"` before starting it |
| `npm` / `node` not recognized | Node.js isn't installed | See Prerequisites |
| `localhost:8080` shows a blank/404 page instead of the dashboard | The built frontend was never copied into `clamped-server/src/main/resources/static/` (gitignored, empty by default) | Run the frontend separately with `npm run dev` in `clamped-ui/` (step 6), or build+copy `clamped-ui/dist` into that folder |

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
