# In-Memory Message Queue (Producer–Consumer Demo)

A small Java project that demonstrates the **producer–consumer pattern** using an **in-memory, bounded queue**.  
It includes two approaches:

- **Non-blocking / polling** (commented out in `MainClass`)
- **Blocking with `wait()/notify()`** (active `main` method)

---

## What’s included

### `InMemorySystem`
A lightweight in-memory message buffer backed by a **bounded `BlockingQueue<String>`** with a **fixed capacity of 10** (FIFO order).

It supports:

- **Non-blocking API**
  - `boolean writeMessageToQueue(String message)`  
    Returns `false` when the queue is full (backpressure).
  - `String readNextMessage()`  
    Returns `null` when the queue is empty.

- **Blocking API (wait/notify)**
  - `void writeMessageToQueueWaitNotify(String message)`  
    Blocks when the queue is full until space is available.
  - `String readNextMessageWaitNotify()`  
    Blocks when the queue is empty until a message is available.

### `MainClass`
Demonstrates two threads communicating through `InMemorySystem`:

- **Writer thread (Producer)**: writes 25 messages (`Message: 0` … `Message: 24`)
- **Reader thread (Consumer)**: reads 25 messages and prints them

The currently active `main()` uses the **wait/notify** methods.

---

## How it works (High Level)

- The queue capacity is **10**
- Producer adds messages until the queue is full
- Consumer removes messages until the queue is empty
- In the wait/notify version:
  - producer calls `wait()` if full
  - consumer calls `wait()` if empty
  - after producing/consuming, the thread calls `notify()` to wake a waiting thread

---

## Running the project

### Prerequisites
- Java 8+ (or any modern JDK)

### Compile
```bash
javac InMemorySystem.java MainClass.java
```

### Run
```bash
java MainClass
```
