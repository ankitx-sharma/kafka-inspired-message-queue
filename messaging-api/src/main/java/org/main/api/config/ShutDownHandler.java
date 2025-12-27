package org.main.api.config;

import org.main.api.service.RunService;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class ShutDownHandler {
	private final RunService runService;
	
	public ShutDownHandler(RunService runService) {
		this.runService = runService;
	}
	
	@PreDestroy
	public void onShutdown() {
		runService.stopRun();
	}
}
