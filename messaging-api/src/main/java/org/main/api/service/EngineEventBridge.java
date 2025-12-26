package org.main.api.service;

import org.main.api.dto.EventDto;
import org.main.engine.processor.MessagingEngine;
import org.springframework.stereotype.Service;

@Service
public class EngineEventBridge {
	
	public EngineEventBridge(MessagingEngine engine, SseHub sseHub) {
		engine.events().addListener(ev ->
			sseHub.broadcast(new EventDto(
					ev.type().name(), 
					ev.messageId() + "|" +ev.message(), 
					ev.timestamp().toString()
			))
		);
	}
}
