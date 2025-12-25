package org.main.api.controller;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.main.api.dto.EventDto;
import org.main.api.dto.RunRequest;
import org.main.api.dto.StatsResponse;
import org.main.api.service.SseHub;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {
	private final SseHub sseHUb;
	
	// Dummy counters just so /stats changes (replace with real Engine metrics later)
	private final AtomicLong submitted = new AtomicLong(0);
	private final AtomicLong completed = new AtomicLong(0);
	private final AtomicLong spilledToDisk = new AtomicLong(0);
	
	public DemoController(SseHub sseHub) {
		this.sseHUb = sseHub;
	}
	
	@PostMapping("/run")
	public void runScenario(@RequestBody RunRequest request) {
		long add = request.messageCount() != null? request.messageCount() : 0;
		submitted.addAndGet(add);
		
		// Broadcast event (UI can show this immediately)
		sseHUb.broadcast(new EventDto(
				"run", 
				"started scenario=" + request.scenario()
                + " count=" + request.messageCount()
                + " threads=" + request.workerThreads()
                + " delayMs=" + request.processingDelayMs(), 
                Instant.now().toString()
         ));
		
		// TODO (next step): call your real MessagingEngine here:
        // engine.configureThreads(request.workerThreads());
        // engine.setProcessingDelay(request.processingDelayMs());
        // engine.submitBatch(...)
		
		// Dummy: simulate that some are completed/spilled (remove later)
		if(add > 0) {
			long dummyDone = Math.min(50, add);
			completed.addAndGet(dummyDone);
			if(add > 500) {
				spilledToDisk.addAndGet(add - 500);
			}
		}
	}
	
	public StatsResponse stats() {
		// TODO (next step): return real Engine stats snapshot
		return new StatsResponse(
				submitted.get(), 
				completed.get(), 
				spilledToDisk.get(), 
				0L, // inMemoryQueueSize
				0L, // diskSpoolSize
				0,  // activeThreads
				0	// poolSize
			);
	}
}
