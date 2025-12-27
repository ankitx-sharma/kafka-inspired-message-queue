package org.main.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.main.api.dto.EventDto;
import org.main.api.dto.RunConfig;
import org.main.api.dto.RunStatusResponse;
import org.main.engine.processor.MessagingEngine;
import org.springframework.stereotype.Service;

@Service
public class RunService {
	private enum RunState{ IDLE, RUNNING, STOPPING, STOPPED}
	private final static String NONE = "none";
	
	private volatile RunState runState = RunState.IDLE;
	private volatile String runId = null;
	private volatile String scenarioMode = NONE;
	
	private final SseHub sseHub;
	private volatile MessagingEngine engine;
	
	public RunService(SseHub sseHub) {
		this.sseHub = sseHub;
	}
	
	public synchronized void startRun(RunConfig preset) throws IOException {
		if(runState == RunState.RUNNING || runState == RunState.STOPPING) {
			throw new IllegalStateException("Run already active: "+ runId + " | " + runState );
		}
		
		if(engine != null) {
			try {
				engine.shutDownGracefully();
			} catch (InterruptedException | IOException ignored) {}
			engine = null;
		}
		
		runState = RunState.RUNNING;
		runId = "run-" + System.currentTimeMillis();
		scenarioMode = preset.scenario() == null ? NONE : preset.scenario().name();
		
		engine = new MessagingEngine(preset.threads(), preset.queueCapacity(), preset.processingDelayMs());
		
		engine.events().addListener(ev -> {
			sseHub.broadcast(new EventDto(
					ev.type().name(), 
					ev.messageId() + "|" + ev.message(), 
					ev.timestamp().toString()));
		});
		
		for(int i=0; i<=preset.messageCount(); i++) {
			try {
				engine.submitTask("hello- "+i);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void stopRun() {
		if(runState != RunState.RUNNING) { return; }
		
		runState = RunState.STOPPING;
		
		try {
			if(engine != null) { engine.shutDownGracefully(); }
		} catch (InterruptedException | IOException ignored) {
		} finally {
			engine = null;
			runState = RunState.STOPPED;
		}
	}
	
	public synchronized void reset(boolean deleteDiskQueueFile) throws IOException {
		stopRun();
		runId = null;
		scenarioMode = NONE;
		runState = RunState.IDLE;
		
		if(deleteDiskQueueFile) {
			Files.deleteIfExists(Path.of("tasks.queue"));
		}
	}
	
	public RunStatusResponse getRunStatus() {
		return new RunStatusResponse(runState.name(), runId, scenarioMode);
	}
	
	public MessagingEngine currentEngine() {
		return this.engine;
	}
}
