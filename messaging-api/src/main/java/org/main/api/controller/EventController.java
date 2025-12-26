package org.main.api.controller;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.main.api.dto.EventDto;
import org.main.api.dto.RunRequest;
import org.main.api.dto.StatsResponse;
import org.main.api.service.SseHub;
import org.main.engine.processor.MessagingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EventController {
	private final MessagingEngine messagingEngine;
	private final SseHub sseHUb;
	
	public EventController(SseHub sseHub, MessagingEngine messagingEngine) {
		this.sseHUb = sseHub;
		this.messagingEngine = messagingEngine;
	}
	
	@PostMapping("/run")
	public void runScenario(@RequestBody RunRequest request) {
		long count = request.messageCount() != null? request.messageCount() : 0;
		
		// Broadcast event (UI can show this immediately)
		sseHUb.broadcast(new EventDto(
				"run", 
				"started scenario=" + request.scenario()
                + " count=" + count
                + " threads=" + request.workerThreads()
                + " delayMs=" + request.processingDelayMs(), 
                Instant.now().toString()
         ));
		
		for(int i=0; i<=count; i++) {
			try {
				messagingEngine.submitTask("hello- "+i);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public StatsResponse stats() {
		// TODO (next step): return real Engine stats snapshot
		return new StatsResponse(
				0, 
				0, 
				0, 
				0L, // inMemoryQueueSize
				0L, // diskSpoolSize
				0,  // activeThreads
				0	// poolSize
			);
	}
}
