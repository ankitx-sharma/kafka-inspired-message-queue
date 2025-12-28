package org.main.engine.processor;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.main.engine.dto.DiskRecord;
import org.main.engine.events.EngineEvent;
import org.main.engine.events.EngineEventType;
import org.main.engine.listener.EngineEventPublisher;
import org.main.engine.service.DiskQueue;
import org.main.engine.service.FileDiskQueue;

/**
* A small worker pool that executes tasks using a fixed {@link ThreadPoolExecutor}
* and spills overflow tasks to a disk-backed queue.
*
* <p>Behavior:
* <ul>
*   <li>If the disk queue is not empty, new tasks are appended to disk (disk priority).</li>
*   <li>If in-memory capacity is full, tasks are appended to disk.</li>
*   <li>A dedicated "drainer" thread moves tasks from disk to the executor whenever capacity exists.</li>
* </ul>
*
* <p>Capacity control is done via a {@link Semaphore} representing:
* (worker threads + in-memory queue capacity).
*/
public class WorkerThreadPoolProcessor {
	private final ThreadPoolExecutor executor;
	private final BlockingQueue<Runnable> queue;
	
	private DiskQueue fileQueue;
	private EngineEventPublisher eventPublisher;
	
	private final Semaphore permits;
	private final int capacity;
	private volatile long processingDelayMs;
	
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final Thread drainerThread;
	
	private final Lock lock = new ReentrantLock();
	private final Condition wakeUp = lock.newCondition();
	
	/**
     * Creates a worker pool with a fixed number of threads and a bounded in-memory queue.
     *
     * @param threads number of worker threads in the executor
     * @param queueCapacity max number of tasks that can wait in memory
     * @throws IOException if the disk queue cannot be created or opened
     */
	public WorkerThreadPoolProcessor(int threads, 
									int queueCapacity, 
									long processingDelayMs,
									EngineEventPublisher eventPublisher) throws IOException{
		if(processingDelayMs < 0) { throw new IllegalArgumentException("processingDelayMs must be more than 0"); }
		this.fileQueue = new FileDiskQueue("tasks.queue");
		this.queue = new LinkedBlockingQueue<>(queueCapacity);
		this.eventPublisher = eventPublisher;
		
		this.executor = new ThreadPoolExecutor(threads, 
											threads, 
											0L, TimeUnit.MILLISECONDS, 
											queue);
		
		this.capacity = threads + queueCapacity;
		this.permits = new Semaphore(capacity, true);
		this.processingDelayMs = processingDelayMs;
		
		this.drainerThread = new Thread(this::drainLoop, "disk-drainer");
		this.drainerThread.start();
	}
	
	/**
     * Submits a task for execution.
     *
     * <p>Rules:
     * <ul>
     *   <li>If disk is not empty, write the new task to disk to preserve ordering/backlog draining.</li>
     *   <li>If there is no capacity (threads + queue), write to disk.</li>
     *   <li>Otherwise execute directly in the thread pool.</li>
     * </ul>
     *
     * @param task the task payload/message
     * @throws IOException if writing to the disk queue fails
     * @throws InterruptedException if the caller thread is interrupted while waiting
     */
	public void submitTask(String task) throws IOException, InterruptedException{
		// Rule: If disk is NOT empty, always write new tasks to disk (disk priority)
		String id = nextId(task);
		publish(EngineEventType.SUBMITTED_TASK_FOR_EXECUTION, id, task, Map.of());
		
		if(!fileQueue.isEmpty()) {
			publish(EngineEventType.TASK_SPILLED_TO_DISK, id, task, Map.of("reason", "noCapacity"));
			
			fileQueue.append(id+"::"+task);
			signalDrainer();
			return;
		}
		
		// Otherwise try to submit directly
		if(!permits.tryAcquire()) {
			publish(EngineEventType.TASK_SPILLED_TO_DISK, id, task, Map.of("reason", "noCapacity"));
			
			fileQueue.append(id+"::"+task);
			signalDrainer();
			return;
		}
		
		executeUserTask(task);
	}
	
	/**
     * Executes a user-submitted task directly via the executor.
     *
     * <p>On completion the permit is released. If execution is rejected, the task is written to disk.
     *
     * @param task the task payload/message
     * @throws InterruptedException if interrupted while handling backoff/sleep
     * @throws IOException if writing to the disk queue fails after rejection
     */
	private void executeUserTask(String task) throws InterruptedException, IOException{
		try {
			String id = nextId(task);
			publish(EngineEventType.STARTED_TASK_PROCESSING, id, task, Map.of("source", "memory"));
			
			executor.execute(() -> {
				try {
					System.out.println(task);
					Thread.sleep(this.processingDelayMs);
				}catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}finally {
					permits.release();
					signalDrainer(); // wake drainer because capacity might now exist
				}
			publish(EngineEventType.TASK_COMPLETED, id, task, Map.of("source", "memory"));
			});
		}catch(RejectedExecutionException ex) {
			permits.release();
			String id = nextId(task);
			publish(EngineEventType.TASK_SPILLED_TO_DISK, id, task, Map.of("reason", "noCapacity"));
			
			fileQueue.append(id+"::"+task);
			signalDrainer();
			Thread.sleep(100);
		}
	}
	
	/**
     * Executes a task read from disk.
     *
     * <p>After successful execution, the record is acknowledged on disk using {@code nextPos()}.
     * If execution is rejected, the message is appended back to disk.
     *
     * @param rec disk record containing the message and the next position for ack
     * @throws InterruptedException if interrupted while handling backoff/sleep
     */
	private void executeDiskTask(DiskRecord rec) throws InterruptedException{
		MessageData data = new MessageData(rec.message());
		try {
			publish(EngineEventType.STARTED_TASK_PROCESSING, data.id, data.payload, Map.of("source", "disk"));
			
			executor.execute(() ->{
				try {
					System.out.println(rec.message());
					Thread.sleep(this.processingDelayMs);
				}catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}finally {
					try {
						fileQueue.ack(rec.nextPos());
					}catch(IOException ex) {
						ex.printStackTrace();
					}
					permits.release();
					signalDrainer();
				}
			publish(EngineEventType.TASK_COMPLETED, data.id, data.payload, Map.of("source", "disk"));
			});
		}catch(RejectedExecutionException ex) {
			permits.release();
			try {
				publish(EngineEventType.TASK_SPILLED_TO_DISK, data.id, data.payload, Map.of("reason", "noCapacity"));
				
				fileQueue.append(rec.message());
				signalDrainer();
			}catch(IOException io) {
				io.printStackTrace();
			}
			Thread.sleep(100);
		}
	}
	
	/**
     * Background loop that drains tasks from disk into the executor when:
     * <ul>
     *   <li>disk is not empty</li>
     *   <li>and a permit (capacity) is available</li>
     * </ul>
     *
     * <p>Stops when {@code running} becomes false or on fatal disk errors.
     */
	private void drainLoop() {
		while(running.get()) {
			try {
				// If disk empty -> wait
				if(fileQueue.isEmpty()) {
					awaitSignal(200); // small timed wait to keep it simple
					continue;
				}
				
				// Disk has data. Only drain if we can get a permit.
				if(!permits.tryAcquire(200, TimeUnit.MILLISECONDS)) {
					continue;
				}
				
				// Now we have a permit -> safe to poll
				DiskRecord task = fileQueue.poll();				
				if(task == null) {
					// Disk got empty between checks
					permits.release();
					continue;
				}
				MessageData data = new MessageData(task.message());
				publish(EngineEventType.TASK_RECOVERED_FROM_DISK, data.id, data.payload, Map.of());
				
				executeDiskTask(task);
			}catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
                break;
			}catch(IOException ex) {
				// If disk queue fails, safest is to stop draining
                ex.printStackTrace();
                break;
			}
		}
	}
	
	 /**
     * Wakes up the drainer thread to re-check disk and available capacity.
     */
	private void signalDrainer() {
		lock.lock();
		try {
			wakeUp.signalAll();
		}finally {
			lock.unlock();
		}
	}
	
	/**
     * Waits for a wake-up signal (or timeout) to avoid busy-spinning when disk is empty.
     *
     * @param mills max time to wait in milliseconds
     * @throws InterruptedException if interrupted while waiting
     */
	private void awaitSignal(long mills) throws InterruptedException{
		lock.lock();
		try {
			wakeUp.await(mills, TimeUnit.MILLISECONDS);
		}finally {
			lock.unlock();
		}
	}
	
	/**
     * Shuts down the pool in a controlled way:
     * <ul>
     *   <li>wait until disk queue is empty</li>
     *   <li>stop and join the drainer thread</li>
     *   <li>shutdown the executor and await termination</li>
     *   <li>close the disk queue</li>
     * </ul>
     *
     * @throws InterruptedException if interrupted while waiting for shutdown steps
     * @throws IOException if closing the disk queue fails
     */
	public void shutdownGracefully() throws InterruptedException, IOException{
		while(!fileQueue.isEmpty()) {
			Thread.sleep(100);
		}
		running.set(false);
		signalDrainer();
		drainerThread.interrupt();
		drainerThread.join();
		
		executor.shutdown();
		if(!executor.awaitTermination(500_000, TimeUnit.MILLISECONDS)) {
			executor.shutdownNow();
		}
		fileQueue.close();
	}
	
	// Publisher methods added
	private void publish(EngineEventType type, 
						String id, 
						String payload, 
						Map<String, Object> meta) {
		if(eventPublisher == null) return;
		
		eventPublisher.publish(new EngineEvent(type, id, payload, Instant.now(), meta));
	}
	
	private String nextId(String message) {
		int idx = message.indexOf("-");
		String id = idx > 0 ? message.substring(idx+1) : "unknown";
		
		return "msg-" + id;
	}
	
	private class MessageData{
		String id;
		String payload;
		
		public MessageData(String message) {
			if(message == null) return;
			
			int idx = message.indexOf("::");
			this.id = idx > 0 ? message.substring(0, idx) : "unknown";
			this.payload = idx > 0 ? message.substring(idx + 2) : message;
		}
	}
}