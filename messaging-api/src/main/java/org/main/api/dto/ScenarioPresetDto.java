package org.main.api.dto;

public record ScenarioPresetDto(
		Scenario scenario,
		int threads,
		int queueCapacity,
		long processingDelayMs,
		long messageCount
){}