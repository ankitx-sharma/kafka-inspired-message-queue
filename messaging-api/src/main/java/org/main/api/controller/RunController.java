package org.main.api.controller;

import java.io.IOException;
import java.time.Instant;

import org.main.api.config.RunConfigResolver;
import org.main.api.dto.EventDto;
import org.main.api.dto.RunConfig;
import org.main.api.dto.RunRequest;
import org.main.api.dto.StatsResponse;
import org.main.api.service.RunService;
import org.main.api.service.SseHub;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EventController {
	private final SseHub sseHUb;
	private final RunService runService;
	
	public EventController(SseHub sseHub, RunService runService) {
		this.sseHUb = sseHub;
		this.runService = runService;
	}
	
	@PostMapping("/run")
	public void runScenario(@RequestBody RunRequest request) throws IOException {
		RunConfig config = RunConfigResolver.resolve(request);
		
		sseHUb.broadcast(new EventDto(
					"run", 
					"started scenario=" + config.scenario()
	                + " count=" + config.messageCount()
	                + " threads=" + config.threads()
	                + " delayMs=" + config.processingDelayMs(), 
	                Instant.now().toString()
	    ));
		
		var engine = runService.startNewEngine(config);
		
		for(int i=0; i<=config.messageCount(); i++) {
			try {
				engine.submitTask("hello- "+i);
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
