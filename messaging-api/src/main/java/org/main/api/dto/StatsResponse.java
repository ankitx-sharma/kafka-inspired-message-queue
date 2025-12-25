package org.main.api.dto;

public record StatsResponse(
		long submitted,
		long completed,
		long spilledToDisk,
		long inMemoryQueueSize,
		long diskSpoolSize,
		int activeThreads,
		int poolSize
){}
