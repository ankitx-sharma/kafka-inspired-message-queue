package org.main.api.dto;

public record ScenarioPreset(
		Scenario scenario,
		int threads,
		int queueCapacity,
		long processingDelayMs,
		long messageCount
){}