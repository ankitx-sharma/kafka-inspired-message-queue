package org.main.engine.processor;

import java.io.IOException;

import org.main.engine.listener.EngineEventPublisher;

public class MessagingEngine {
	private final WorkerThreadPoolProcessor threadProcessor;
	private final EngineEventPublisher eventPublisher;
	
	public MessagingEngine(int threadCount, int queueCapactiy) throws IOException {
		this.eventPublisher = new EngineEventPublisher();
		this.threadProcessor = new WorkerThreadPoolProcessor(threadCount, queueCapactiy, eventPublisher);
	}
	
	public void submitTask(String message) throws IOException, InterruptedException {
		this.threadProcessor.submitTask(message);
	}
	
	public EngineEventPublisher getEventPublisher() {
		return this.eventPublisher;
	}
	
	public void shutDownGracefully() throws InterruptedException, IOException {
		this.threadProcessor.shutdownGracefully();
	}
}
