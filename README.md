# Disk-Backed Message Processing System (Kafka-Inspired)

<img src="assets/ui_screen.png" alt="Project Title" width="1000" align="center">

## Overview
This project implements a **Kafka-inspired message processing system** in pure Java.  
It focuses on **backpressure**, **bounded concurrency**, **disk persistence**, and **graceful shutdown**.

The purpose of this project is to explore and demonstrate real-world system design problems:
- How to handle producer–consumer imbalance
- How to prevent unbounded memory growth
- How to use disk as a safety buffer under load
- How to shut down concurrent systems without data loss

A **simple in-memory implementation** is included for comparison.

---

## Technology Stack
**Backend**
- Java
- Maven
- RESTful API design

**Frontend**
- JavaScript / TypeScript
- Modern frontend tooling (via npm)

**Infrastructure**
- Docker
- Docker Compose

---

## Prerequisites
Ensure the following tools are installed:
- Java 17 or higher
- Maven
- Node.js 18 or higher
- npm
- Docker & Docker Compose

## Running the Project
**Option 1: Run with Docker (Recommended)**

This is the easiest and most consistent way to run the entire system.
```
docker-compose up --build
```
**Once started:**
- Backend API: http://localhost:8080
- Frontend UI: http://localhost:5173

<br>

**Option 2: Run Locally (Without Docker)**

Start the Backend
```
cd messaging-api
mvn clean package
java -jar target/*.jar
```

Start the Frontend
```
cd messaging-ui
npm install
npm run dev
```

---

## Project Structure
```
.
├── messaging-api/        # Backend REST API (Java)
├── messaging-engine/     # Backend Message Processing Engine (Java)
├── messaging-ui/         # Frontend Web UI
├── docker-compose.yml    # Unified setup for local development
├── .project
└── README.md


messaging-engine/
└── src/
  └── org.main.project
  ├── kafka_style_sys
  │ ├── MainExecutorClass.java
  │ ├── worker
  │ │ └── WorkerThreadPool.java
  │ ├── service
  │ │ ├── DiskQueue.java
  │ │ ├── FileDiskQueue.java
  │ │ └── IndexMapService.java
  │ └── record
  │ └── DiskRecord.java
  │
  └── simple_msg_sys
  ├── MainClass.java
  └── inmemsys
  └── InMemorySystem.java
```

---

## API Usage

The backend exposes REST endpoints that can be accessed via:
- The provided Web UI
- Tools like Postman or curl
- Custom client applications

Typical use cases include:
- Sending messages
- Retrieving messages
- Testing API behavior and flows

## Key Concepts
- Backpressure using `Semaphore`
- Bounded `ThreadPoolExecutor`
- Disk-backed queue with file persistence
- Explicit locking with `ReentrantLock` and `Condition`
- Graceful shutdown without task loss
- Producer / consumer decoupling

---

## Architecture Overview
1. Producer submits a task
2. A `Semaphore` enforces global capacity
3. If memory is available → task goes to the executor
4. If memory is full → task is written to disk
5. Drainer thread moves tasks from disk back into memory
6. Workers process tasks and release permits

### Design Decisions

#### Disk-Backed Queue (Overflow Buffer)
- Disk acts as a pressure buffer, not the primary queue (RAM-first, disk-overflow)
- Append-only write path for high throughput and low fragmentation
- Uses a record format (length-prefix + payload) to support deterministic replay
- **Crash-safe recovery**:
  - On startup, the system replays unread records from the last known read offset
  - Partial/corrupt trailing records are detected and ignored safely
- **Thread-safe by design**:
  - Single-writer append path (or explicit locking if multiple producers)
  - Separate read pointer for the drainer
- **Guarantees (typical configuration)**:
  - Preserves FIFO ordering per queue
  - Supports at-least-once delivery (exactly-once requires idempotency at consumer)

#### Backpressure via Semaphore (Producer Throttling)
- Every enqueue must acquire a permit
- A permit is released only after the task is fully processed (completion-ack)
- Prevents unbounded memory growth and naturally throttles producers under load
- Works as a single, central capacity control across:
  - in-memory queue
  - disk buffer
  - executor backlog
- Failure handling:
  - If enqueue fails after acquiring a permit, the permit is released immediately
  - Timeout-based acquisition can be used to avoid blocking forever

#### Bounded ThreadPoolExecutor (Controlled Concurrency)
- Fixed worker count (predictable throughput)
- Fixed in-memory queue size (bounded latency and RAM)
- Avoids dangerous unbounded executors (newCachedThreadPool, unbounded queues)
- Rejection strategy is explicit and intentional:
  - AbortPolicy for strict fail-fast
  - or a custom handler that routes overflow to disk buffer
- Supports clean instrumentation:
  - active threads, queue depth, completed task count, rejection count

#### Drainer Thread (Disk → Memory / Executor)
- Independent from workers (separation of concerns)
- Moves tasks from disk buffer into the bounded in-memory pipeline
- Uses Condition.await() / signaling instead of polling:
  - Signals when disk has new data
  - Signals when capacity permits become available
- CPU-efficient:
  - Sleeps when there is nothing to drain or no capacity
  - Wakes up only on meaningful events (new record / permit released / shutdown)
- Safe batching (optional):
  - Reads in small batches for throughput while respecting capacity bounds
  - Avoids starving producers or executor queue

#### Graceful Shutdown (Deterministic, No Surprise Loss)
Shutdown sequence (recommended):
1. Stop accepting new tasks (flip an accepting=false gate)
2. Signal drainer to stop waiting and enter shutdown mode
3. Drain disk queue (respecting permits and executor capacity)
4. Stop drainer thread (join with timeout, then hard-stop fallback if needed)
5. Shut down executor:
    - shutdown() then awaitTermination(...)
    - shutdownNow() only as last resort
6. Flush/close file resources safely:
    - ensure buffers are flushed (e.g., FileChannel.force(true) if required)
    - persist read-offset/checkpoint
7. Final consistency check:
    - permits should return to full capacity (no leaks)
    - disk segments should be fully acknowledged or remain replayable

Result: **deterministic shutdown**, predictable behavior under load, and no silent data loss
(when using at-least-once semantics + replayable disk buffer).

---

## Possible Extensions

This project is intentionally minimal and can be extended in many directions, such as:
- Persistent storage (e.g. PostgreSQL, MongoDB)
- Authentication and authorization
- Message queues or async processing
- Rate limiting or throttling
- Monitoring and logging
- WebSocket support

## Contributing
Contributions are welcome.
To contribute:
1. Fork the repository
2. Create a feature branch
3. Make your changes with clear commits
4. Open a pull request with a concise description

Please ensure:
- Code is clean and readable
- Changes are well-scoped
- Commits are meaningful

## Feedback and Support

If you find an issue, have a suggestion, or want to discuss improvements, please open an issue in the repository.
- Automated testing (unit and integration)
- UI/UX improvements
