package org.main.api.dto;

public record RunRequest(
		String scenario,
		Long messageCount,
		Integer workerThreads,
		Long processingDelayMs
) {}
