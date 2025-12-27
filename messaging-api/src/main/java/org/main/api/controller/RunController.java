package org.main.api.controller;

import java.io.IOException;
import java.time.Instant;

import org.main.api.config.RunConfigResolver;
import org.main.api.dto.EventDto;
import org.main.api.dto.ResetRequest;
import org.main.api.dto.RunConfig;
import org.main.api.dto.RunRequest;
import org.main.api.dto.RunStatusResponse;
import org.main.api.dto.StatsResponse;
import org.main.api.service.RunService;
import org.main.api.service.SseHub;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/run")
public class RunController {
	private final SseHub sseHUb;
	private final RunService runService;
	
	public RunController(SseHub sseHub, RunService runService) {
		this.sseHUb = sseHub;
		this.runService = runService;
	}
	
	@PostMapping("/start")
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
		
		runService.startRun(config);
	}
	
	@PostMapping("/stop")
	public void stop() {
		runService.stopRun();
	}
	
	@PostMapping("/reset")
	public void reset(@RequestBody(required = false) ResetRequest resetReq) {
		boolean deleteDiskQueueFile = resetReq!=null & resetReq.deleteDiskQueueFile();
		try {
			runService.reset(deleteDiskQueueFile);
		} catch (IOException ignored) {}
	}
	
	@GetMapping("/status")
	public RunStatusResponse status() {
		return runService.getRunStatus();
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
