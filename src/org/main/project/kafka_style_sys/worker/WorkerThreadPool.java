package org.main.project.kafka_style_sys.worker;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.main.project.kafka_style_sys.service.ReadLogService;
import org.main.project.kafka_style_sys.service.WriteLogService;

public class WorkerThreadPool {
	private final ThreadPoolExecutor executor;
	private final BlockingQueue<Runnable> queue;
	private volatile boolean readFromFile = false;
	private WriteLogService writer = new WriteLogService();
	private ReadLogService reader = new ReadLogService();
	private AtomicInteger currentFileNo = new AtomicInteger(0);
	
	public WorkerThreadPool(int threads, int queueCapacity) {
		this.queue = new LinkedBlockingQueue<>(queueCapacity);
		
		this.executor = new ThreadPoolExecutor(threads, 
											threads, 
											0L, TimeUnit.MILLISECONDS, 
											queue, 
											new ThreadPoolExecutor.AbortPolicy());
	}
	
	public void submitTask(String task) throws IOException{
		if(readFromFile) {
			task = readTaskFromFile(task);
			System.out.println("Reading from file");
		}
		
		executeTheTask(task);
	}
	
	private void executeTheTask(String task) throws IOException{
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					System.out.println(task);
					try {
						Thread.sleep(10_000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}catch(RejectedExecutionException ex) {
			writer.appendMessageToLog(task);
			readFromFile = true;
		}
	}
	
	private String readTaskFromFile(String task) throws IOException{
		writer.appendMessageToLog(task);
		String message = reader.readMessage(currentFileNo.get());
		if(message.equals("eof")) {
			message = reader.readMessage(currentFileNo.incrementAndGet());
		}
		
		return message;
	}
	
	public void shutdownGracefully() throws InterruptedException{
		executor.shutdown();
		if(!executor.awaitTermination(500_000, TimeUnit.MILLISECONDS)) {
			executor.shutdownNow();
		}
	}
}