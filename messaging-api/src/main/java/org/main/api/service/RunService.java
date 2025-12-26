package org.main.api.service;

import java.io.IOException;

import org.main.api.dto.EventDto;
import org.main.api.dto.ScenarioPresetDto;
import org.main.engine.processor.MessagingEngine;
import org.springframework.stereotype.Service;

@Service
public class RunService {
	private final SseHub sseHub;
	private volatile MessagingEngine engine;
	
	public RunService(SseHub sseHub) {
		this.sseHub = sseHub;
	}
	
	public synchronized MessagingEngine startNewEngine(ScenarioPresetDto preset) throws IOException {
		if(engine != null) {
			try {
				engine.shutDownGracefully();
			} catch (InterruptedException | IOException ignored) {}
		}
		
		engine = new MessagingEngine(preset.threads(), preset.queueCapacity(), preset.processingDelayMs());
		
		engine.events().addListener(ev -> {
			sseHub.broadcast(new EventDto(
					ev.type().name(), 
					ev.messageId() + "|" + ev.message(), 
					ev.timestamp().toString()));
		});
		
		return engine;
	}
	
	public MessagingEngine currentEngine() {
		return this.engine;
	}
}
