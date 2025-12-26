package org.main.engine;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.main.engine.listener.EngineEventPublisher;
import org.main.engine.processor.WorkerThreadPoolProcessor;

public class MainExecutorClass {

	public static void main(String[] args) throws InterruptedException, IOException {
		WorkerThreadPoolProcessor threadPool = new WorkerThreadPoolProcessor(4, 4, new EngineEventPublisher());
		
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