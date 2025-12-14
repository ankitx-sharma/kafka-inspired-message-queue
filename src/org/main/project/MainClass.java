package org.main.project;

import org.main.project.inmemsys.InMemorySystem;

/**
 * MainClass demonstrates a simple producer-consumer pattern using two threads
 * that communicate through an in-memory message system.
 *
 * <p>
 * The class creates:
 * <ul>
 *   <li>A writer thread (producer) that attempts to write messages into an in-memory queue</li>
 *   <li>A reader thread (consumer) that continuously reads messages from the same queue</li>
 * </ul>
 *
 * <p>
 * The communication between threads is handled through {@link InMemorySystem},
 * which internally manages a bounded queue.
 *
 * <p>
 * This example intentionally uses polling with retries and sleep intervals
 * to demonstrate basic concurrency behavior without advanced synchronization
 * mechanisms such as BlockingQueue methods.
 */
public class MainClass {
	/**
     * Shared in-memory messaging system used by both writer and reader threads.
     */
	private static InMemorySystem in_memory_message = new InMemorySystem();
	
	/**
     * Application entry point.
     *
     * <p>
     * Starts two threads:
     * <ul>
     *   <li><b>Writer Thread:</b> Attempts to write 25 messages into the queue.
     *       If the queue is full, it waits and retries.</li>
     *   <li><b>Reader Thread:</b> Attempts to read 25 messages from the queue.
     *       If the queue is empty, it waits and retries.</li>
     * </ul>
     *
     * <p>
     * The main thread waits for both threads to finish execution using {@code join()}.
     *
     * @param args command-line arguments (not used)
     * @throws Exception if the thread execution is interrupted
     */
//	public static void main(String[] args) throws Exception{
//		
//		 /**
//         * Writer thread (Producer).
//         *
//         * <p>
//         * Generates 25 messages and attempts to write them to the in-memory queue.
//         * If the queue is full, the thread waits for one second before retrying.
//         */
//		Thread writeThread = new Thread(() -> {
//			for(int i=0;i<25;i++) {
//				while(!in_memory_message.writeMessageToQueue("Message: "+i)) {
//					try {
//						System.out.println("message queue is full");
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}});
//		
//		/**
//         * Reader thread (Consumer).
//         *
//         * <p>
//         * Attempts to read 25 messages from the in-memory queue.
//         * If the queue is empty, the thread waits for three seconds before retrying.
//         */
//		Thread readThread = new Thread(() -> {
//			for(int i=0;i<25;i++) {
//				String message = null;
//				while((message = in_memory_message.readNextMessage()) == null) {
//					try {
//						System.out.println("message queue is empty");
//						Thread.sleep(3000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				System.out.println(message);
//			}
//		});
//		
//		// Start both threads
//		writeThread.start();
//		readThread.start();
//		
//		// Wait for both threads to complete execution
//		writeThread.join();
//		readThread.join();
//	}
	
	/**
	 * Application entry point that demonstrates a producer–consumer
	 * interaction using explicit wait/notify synchronization.
	 *
	 * <p>
	 * The method creates two threads:
	 * <ul>
	 *   <li><b>Writer Thread (Producer):</b>
	 *       Generates 25 messages and writes them to an in-memory queue
	 *       using {@code writeMessageToQueueWaitNotify}. If the queue is full,
	 *       the thread blocks until space becomes available.</li>
	 *
	 *   <li><b>Reader Thread (Consumer):</b>
	 *       Reads 25 messages from the in-memory queue using
	 *       {@code readNextMessageWaitNotify}. If the queue is empty,
	 *       the thread blocks until a message is produced.</li>
	 * </ul>
	 *
	 * <p>
	 * Both threads coordinate through intrinsic locks and the
	 * {@code wait()/notify()} mechanism implemented inside the
	 * {@link InMemorySystem} class.
	 *
	 * <p>
	 * The main thread starts both worker threads and waits for their
	 * completion using {@code Thread.join()} to ensure orderly shutdown.
	 *
	 * @param args command-line arguments (not used)
	 * @throws Exception if the main thread is interrupted while waiting
	 *         for the worker threads to complete
	 */
	public static void main(String[] args) throws Exception{
		
		Thread writeThread = new Thread(() -> {
			for(int i=0;i<25;i++) {
				try{
					in_memory_message.writeMessageToQueueWaitNotify("Message: "+i);
				}catch(InterruptedException ex) {
					System.out.println("Thread interrupted");
				}
			}});
		
		Thread readThread = new Thread(() -> {
			for(int i=0;i<25;i++) {
				try {
					System.out.println(in_memory_message.readNextMessageWaitNotify());
				}catch(InterruptedException ex) {
					System.out.println("Thread interrupted");
				}
			}
		});
		
		// Start both threads
		writeThread.start();
		readThread.start();
		
		// Wait for both threads to complete execution
		writeThread.join();
		readThread.join();
	}
}