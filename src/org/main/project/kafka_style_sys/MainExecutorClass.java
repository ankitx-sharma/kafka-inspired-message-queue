package org.main.project.kafka_style_sys;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.main.project.kafka_style_sys.service.ReadLogService;
import org.main.project.kafka_style_sys.worker.WorkerThreadPool;

public class MainExecutorClass {

	public static void main(String[] args) throws InterruptedException, IOException {
		WorkerThreadPool threadPool = new WorkerThreadPool(4, 4);
		ReadLogService read = new ReadLogService();
		
		for(int i=0; i<10; i++) {
			threadPool.submitTask(Task.generateTask());
		}
		
//		System.out.println(read.readMessage(0));
//		System.out.println(read.readMessage(0));
//		System.out.println(read.readMessage(0));
//		System.out.println(read.readMessage(0));
		
		
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