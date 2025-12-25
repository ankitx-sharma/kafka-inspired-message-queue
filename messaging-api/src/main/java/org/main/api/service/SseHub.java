package org.main.api.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.main.api.dto.EventDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseHub {
	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
	
	public SseEmitter subscribe() {
		// 0L = no timeout (browser/proxies may still close; UI should reconnect)
		SseEmitter emitter = new SseEmitter(0L);
		
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));
		
		sendToOne(emitter, new EventDto("system", "System connected", Instant.now().toString()));
		return emitter;
	}
	
	public void broadcast(EventDto event) {
		for(SseEmitter emitter: emitters) {
			try {
				emitter.send(SseEmitter.event()
						.name("message")
						.data(event, MediaType.APPLICATION_JSON));
			}catch(IOException ex) {
				emitters.remove(emitter);
			}
		}
	}
	
	private void sendToOne(SseEmitter emitter, EventDto event) {
		try {
			emitter.send(SseEmitter.event()
					.name("message")
					.data(event, MediaType.APPLICATION_JSON));
		}catch(IOException ex) {
			emitters.remove(emitter);
		}
	}
}
