package org.main.project.inmemsys;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * InMemorySystem is a simple in-memory message buffer implemented using a
 * bounded BlockingQueue.
 *
 * <p>
 * It allows messages to be written to and read from an internal queue
 * without any persistent storage. The queue has a fixed capacity and
 * operates in a FIFO (First-In-First-Out) manner.
 * </p>
 *
 * <p>
 * This class is useful for lightweight message passing, prototyping,
 * or simulating producer–consumer behavior in a single-threaded or
 * multi-threaded environment.
 * </p>
 */
public class InMemorySystem {
	
	/**
     * Internal in-memory queue used to store messages.
     *
     * <p>
     * The queue has a fixed capacity of 10 elements. If the queue is full,
     * attempts to add a new message using {@code writeMessageToQueue()} will return
     * null to create backpressure to Producer.
     * </p>
     */
	private BlockingQueue<String> in_memory_queue = new LinkedBlockingQueue<>(10);
	
	/**
     * Reads and removes the next available message from the queue.
     *
     * @return the next message in the queue, or {@code null} if the queue is empty
     */
	public String readNextMessage() {
		if(in_memory_queue.isEmpty()) {
			return null;
		}
		return in_memory_queue.poll();
	}
	
	/**
     * Writes a message to the in-memory queue.
     *
     * @param message the message to be added to the queue
     * @returns false if the queue is full otherwise true
     */
	public boolean writeMessageToQueue(String message) {
		if(in_memory_queue.size() == 10) {
			return false;
		}
		in_memory_queue.add(message);
		return true;
	}
	
	/**
	 * Reads and removes the next message from the in-memory queue using
	 * the wait/notify mechanism.
	 *
	 * <p>
	 * If the queue is empty, the calling thread enters a waiting state
	 * until a producer thread adds a message and notifies waiting threads.
	 * The method uses a guarded block ({@code while} loop) to handle
	 * spurious wakeups correctly.
	 *
	 * <p>
	 * This method blocks until a message becomes available.
	 *
	 * @return the next message from the queue
	 * @throws InterruptedException if the waiting thread is interrupted
	 */
	public String readNextMessageWaitNotify() throws InterruptedException{
		synchronized (in_memory_queue) {
			while(in_memory_queue.isEmpty()) {
				System.out.println("Empty Queue wait");
				in_memory_queue.wait();
			}
			in_memory_queue.notify();
			return in_memory_queue.poll();
		}
	}
	
	/**
	 * Writes a message to the in-memory queue using the wait/notify mechanism.
	 *
	 * <p>
	 * If the queue has reached its maximum capacity (10 elements),
	 * the calling thread waits until a consumer removes an element
	 * and notifies waiting threads.
	 *
	 * <p>
	 * This method blocks until space becomes available in the queue.
	 *
	 * @param message the message to be added to the queue
	 * @throws InterruptedException if the waiting thread is interrupted
	 */
	public synchronized void writeMessageToQueueWaitNotify(String message) throws InterruptedException{
		synchronized (in_memory_queue) {
			while(in_memory_queue.size() == 10) {
				System.out.println("Queue full wait");
				in_memory_queue.wait();
			}

			in_memory_queue.add(message);
			in_memory_queue.notify();
		}
	}	
}
