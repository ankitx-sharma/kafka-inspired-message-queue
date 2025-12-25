package org.main.api.controller;

import org.main.api.service.SseHub;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class StreamController {
	private final SseHub ssehub;
	
	public StreamController(SseHub ssehub) {
		this.ssehub = ssehub;
	}
	
	@GetMapping("/stream")
	public SseEmitter stream() {
		return ssehub.subscribe();
	}
}
