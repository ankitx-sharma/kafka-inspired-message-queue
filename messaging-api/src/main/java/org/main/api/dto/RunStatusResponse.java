package org.main.api.dto;

public record RunStatusResponse(
	String status,
	String runId,
	String scenarioMode
) {}