package org.main.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.main.api.dto.EventDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseHubTest {

	@Test
	void subscribe_addsEmitter() throws Exception {
		SseHub sseHub = new SseHub();
		SseEmitter emitter = sseHub.subscribe();
		assertThat(emitter).isNotNull();
	}
	
	@Test
	void broadcast_doesNotThrow_whenNoClients() {
		SseHub sseHub = new SseHub();
		assertThatCode(() -> sseHub.broadcast(new EventDto("t", "m", "ts")))
			.doesNotThrowAnyException();
	}
}
