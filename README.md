# Clamped! Local

Self-hosted error tracking platform for Java. Drop the SDK into your app, capture exceptions in one line, and triage them from a web dashboard or terminal CLI. All data stored in your own Postgres database.

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
- **Node.js LTS + npm** - only needed if you're working on the frontend (`clamped-ui/`), not for the packaged server/CLI/demo jars. Get it from [nodejs.org](https://nodejs.org), `winget install OpenJS.NodeJS.LTS` (Windows), or your platform's package manager (`brew install node`, `apt install nodejs npm`, etc.).

Once the jars are built and `~/.clamped/config.properties` is set up (step 4 below),
every runnable command is identical on Windows/macOS/Linux - only the build step
(step 3) differs by OS, since Maven's launcher is a real executable, not a portable one.

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

**Windows:**
```powershell
.\mvnw.cmd -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests
```

**macOS/Linux:**
```bash
./mvnw -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests
```

Both download their own copy of Maven 3.9.6 into `.mvn/` on first run - nothing to
install separately, and nothing from that folder is committed to git.

### 4. One-time: generate your local CLI/server config

The server, CLI, and demo app all read DB credentials from `~/.clamped/config.properties`
(same idea as `~/.aws/credentials`) when no environment variables are set. This script
derives that file from `.env` once, so you never have to re-export env vars per session:

**Windows** (requires script execution allowed once - `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`):
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

Open [http://localhost:8080](http://localhost:8080)

### 6. (Optional) Seed sample data

Two ways to get sample data in:

- **Run the demo app** - generates events through the actual SDK pipeline:
  ```bash
  java -jar clamped-demo/target/clamped-demo.jar
  ```
- **"Seed Sample Data" button / `/api/seed` endpoint** - preset data gated behind
  `DEMO_MODE` (returns 403 otherwise). Enable it as a JVM flag when starting the server:
  ```bash
  java -Ddemo.mode=true -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar
  ```
  Then click the nav button, or `curl -X POST http://localhost:8080/api/seed`.

### 7. (Optional) Run the frontend in dev mode

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

Once `~/.clamped/config.properties` exists (Quick Start step 4), the CLI jar needs no
wrapper - run it directly, same command on every OS:

```bash
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar list
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar list --status all
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar show 42
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar resolve 42
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar stats
java -jar clamped-cli/target/clamped-cli-1.0.0-SNAPSHOT.jar purge --before 30d
```

For a shorter `clamped list` instead of the full jar path, run the one-time installer,
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
| Clicking "Seed Sample Data" does nothing | Either the backend isn't running, or `DEMO_MODE` isn't set (endpoint returns 403, and the UI has no error handling for it) | Confirm the server is up, start it with `-Ddemo.mode=true` |
| `PSQLException: ... password is an empty string` | No `~/.clamped/config.properties` and no `CLAMPED_*`/`DATABASE_URL` env vars set | Run `scripts/setup-config.ps1` / `scripts/setup-config.sh` (Quick Start step 4) |
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
