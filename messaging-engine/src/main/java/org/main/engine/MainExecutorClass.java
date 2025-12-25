package org.main.engine;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.main.engine.worker.WorkerThreadPool;

public class MainExecutorClass {

	public static void main(String[] args) throws InterruptedException, IOException {
		WorkerThreadPool threadPool = new WorkerThreadPool(4, 4);
		
		for(int i=0; i<20; i++) {
			threadPool.submitTask(Task.generateTask());
		}
		
		
		
		threadPool.shutdownGracefully();
	}
}

class Task{
	private static String message = "Message: ";
	private static AtomicInteger counter = new AtomicInteger(0);
	
	public static String generateTask(){
		return (message + 
				Thread.currentThread().getName() + " " +
				counter.getAndIncrement());
	}
}