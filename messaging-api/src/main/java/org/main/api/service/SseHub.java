package org.main.api.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.main.api.dto.EventDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseHub {
	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
	
	public SseEmitter subscribe() {
		return null;
	}
	
	public void broadcast(EventDto event) {
		
	}
	
	private void sendToOne(SseEmitter emitter, EventDto event) {
		
	}
}
