package org.main.project.kafka_style_sys.worker;

import java.io.IOException;
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

import org.main.project.kafka_style_sys.record.DiskRecord;
import org.main.project.kafka_style_sys.service.DiskQueue;
import org.main.project.kafka_style_sys.service.FileDiskQueue;

public class WorkerThreadPool {
	private final ThreadPoolExecutor executor;
	private final BlockingQueue<Runnable> queue;
	private DiskQueue fileQueue;
	
	private final Semaphore permits;
	private final int capacity;
	
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final Thread drainerThread;
	
	private final Lock lock = new ReentrantLock();
	private final Condition wakeUp = lock.newCondition();
	
	public WorkerThreadPool(int threads, int queueCapacity) throws IOException{
		this.fileQueue = new FileDiskQueue("tasks.queue");
		this.queue = new LinkedBlockingQueue<>(queueCapacity);
		
		this.executor = new ThreadPoolExecutor(threads, 
											threads, 
											0L, TimeUnit.MILLISECONDS, 
											queue);
		
		this.capacity = threads + queueCapacity;
		this.permits = new Semaphore(capacity, true);
		
		this.drainerThread = new Thread(this::drainLoop, "disk-drainer");
		this.drainerThread.start();
	}
	
	public void submitTask(String task) throws IOException, InterruptedException{
		// Rule: If disk is NOT empty, always write new tasks to disk (disk priority)
		if(!fileQueue.isEmpty()) {
			fileQueue.append(task);
			signalDrainer();
			return;
		}
		
		// Otherwise try to submit directly
		if(!permits.tryAcquire()) {
			System.out.println("No capacity -> write to disk: "+task);
			fileQueue.append(task);
			signalDrainer();
			return;
		}
		
		executeUserTask(task);
	}
	
	private void executeUserTask(String task) throws InterruptedException, IOException{
		try {
			executor.execute(() -> {
				try {
					System.out.println(task);
					Thread.sleep(10_000);
				}catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}finally {
					permits.release();
					signalDrainer(); // wake drainer because capacity might now exist
				}
			});
		}catch(RejectedExecutionException ex) {
			permits.release();
			fileQueue.append(task);
			signalDrainer();
			Thread.sleep(100);
		}
	}
	
	private void executeDiskTask(DiskRecord rec) throws InterruptedException{
		try {
			executor.execute(() ->{
				try {
					System.out.println(rec.message());
					Thread.sleep(10_000);
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
			});
		}catch(RejectedExecutionException ex) {
			permits.release();
			try {
				fileQueue.append(rec.message());
				signalDrainer();
			}catch(IOException io) {
				io.printStackTrace();
			}
			Thread.sleep(100);
		}
	}
	
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
	
	private void signalDrainer() {
		lock.lock();
		try {
			wakeUp.signalAll();
		}finally {
			lock.unlock();
		}
	}
	
	private void awaitSignal(long mills) throws InterruptedException{
		lock.lock();
		try {
			wakeUp.await(mills, TimeUnit.MILLISECONDS);
		}finally {
			lock.unlock();
		}
	}
	
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
}