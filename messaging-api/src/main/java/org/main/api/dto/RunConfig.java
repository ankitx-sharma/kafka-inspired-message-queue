package org.main.api.dto;

public record RunConfig(
	int threads,
	int queueCapacity,
	long processingDelayMs,
	long messageCount,
	Scenario scenario
) {}