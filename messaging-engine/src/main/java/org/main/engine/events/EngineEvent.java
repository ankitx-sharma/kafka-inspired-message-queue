package org.main.engine.events;

import java.time.Instant;
import java.util.Map;

public record EngineEvent (
		EngineEventType type,
		String messageId,
		String message,
		Instant timestamp,
		Map<String, Object> meta
){}
