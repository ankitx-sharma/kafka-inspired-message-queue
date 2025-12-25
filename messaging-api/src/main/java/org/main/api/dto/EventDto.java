package org.main.api.dto;

public record EventDto(
		String type,
		String message,
		String timestamp)
{}
