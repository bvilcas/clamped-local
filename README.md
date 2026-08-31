# Clamped! Local

Self-hosted error tracking platform for Java. Drop the SDK into your app, capture exceptions in one line, and triage them from a web dashboard or terminal CLI. All data stored in your own Postgres database.

---

## Features

- **One-line error capture**: `Clamped.add(e)` in any catch block
- **Automatic deduplication**: same error 1000 times = 1 row + occurrence counter
- **Web dashboard**: filterable events table, stats page, bulk actions, CSV export, resolve with notes, collapsible nav
- **CLI**: triage events directly from the terminal without opening a browser
- **Zero overhead**: async queue, never blocks your app
- **Error Handling**: if Postgres is down, events queue in memory and drop seamlessly

---

## Prerequisites

Install these once before following Quick Start:

- **Java 25 JDK** (e.g. [Eclipse Adoptium](https://adoptium.net/)) - required by the parent `pom.xml`. Check with `java -version`.
- **Docker Desktop** - runs Postgres via `docker-compose.yml`.
- **Node.js LTS + npm** - only needed if you're working on the frontend (`clamped-ui/`), not for the packaged server/CLI/demo jars. Get it from [nodejs.org](https://nodejs.org), `winget install OpenJS.NodeJS.LTS` (Windows), or your platform's package manager (`brew install node`, `apt install nodejs npm`, etc.).

## Quick Start

### 1. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` and set your own `POSTGRES_PASSWORD` (and matching `CLAMPED_PASSWORD` -
keep the two the same). `.env` is gitignored and never committed.

### 2. Start Postgres

```bash
docker compose up -d
```

Compose reads `.env` automatically. If you skip step 1, this fails with a
message: set `POSTGRES_PASSWORD` rather than using a default.

### 3. Build

**Windows:**
```powershell
.\mvnw.cmd -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests
```

**macOS/Linux:**
```bash
./mvnw -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests
```

### 4. Set up local CLI/server config

The server, CLI, and demo app all read DB credentials (.env) from `~/.clamped/config.properties`. 

**Windows** (requires script execution allowed once: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`):
```powershell
.\scripts\setup-config.ps1
```

**macOS/Linux:**
```bash
./scripts/setup-config.sh
```

### 5. Start the server

```bash
java -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar
```

This starts the backend API. Don't open a browser yet as the dashboard UI isn't
available until step 7.

### 6. (Optional) Seed sample data

Two ways to get sample data in:

- **Run the demo app** - generates events through the actual SDK pipeline:
  ```bash
  java -jar clamped-demo/target/clamped-demo.jar
  ```
- **"Seed Sample Data" button / `/api/seed` endpoint** - preset data gated behind
  `DEMO_MODE`. Also appears as a button on the UI. Enable as a JVM flag when starting the server:
  ```bash
  java "-Ddemo.mode=true" -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar
  ```
  Then click the nav button, or `curl -X POST http://localhost:8080/api/seed`.

-> Refresh the page if no sample data appears.

### 7. Run the frontend to see the dashboard

The frontend runs via Vite (requires Node.js... see Prerequisites):

```bash
cd clamped-ui
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) and keep it running alongside the
server from step 5 (Vite calls the API through port `:8080`).

---

### Add the dependency

```xml
<dependency>
    <groupId>io.clamped</groupId>
    <artifactId>clamped-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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

For a shorter command instead of the full jar path, run the one-time installer,
which puts a `clamped` command on your `PATH` (works from any shell):

```powershell
.\scripts\install-cli.ps1   # Windows
```
```bash
./scripts/install-cli.sh    # macOS/Linux
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Clicking "Seed Sample Data" does nothing | Either the backend isn't running, or `DEMO_MODE` isn't set (endpoint returns 403, and the UI has no error handling for it) | Confirm the server is up, start it with `"-Ddemo.mode=true"` (quoted) |
| `Could not find or load main class .mode=true` | PowerShell splits an unquoted `-Ddemo.mode=true` into two arguments | Quote it: `java "-Ddemo.mode=true" -jar ...` |
| `PSQLException: ... password is an empty string` | No `~/.clamped/config.properties` and no `CLAMPED_*`/`DATABASE_URL` env vars set | Run `scripts/setup-config.ps1` / `scripts/setup-config.sh` (Quick Start step 4) |
| `npm` / `node` not recognized | Node.js isn't installed | See Prerequisites |
| `localhost:8080` shows a blank/404 page instead of the dashboard | The built frontend was never copied into `clamped-server/src/main/resources/static/` (gitignored, empty by default) | Run the frontend separately with `npm run dev` in `clamped-ui/` (step 7), or build+copy `clamped-ui/dist` into that folder |

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
