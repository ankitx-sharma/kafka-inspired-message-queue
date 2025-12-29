package org.main.api.controller;

import org.junit.jupiter.api.Test;
import org.main.api.service.SseHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StreamController.class)
public class StreamControllerTest {

	@Autowired MockMvc mockMvc;
	@MockBean SseHub sseHub;
	
	@Test
	void stream_shouldSubscribe() throws Exception {
		when(sseHub.subscribe()).thenReturn(new SseEmitter());
		
		mockMvc.perform(get("/api/events/stream"))
			.andExpect(status().isOk());
		
		verify(sseHub).subscribe();
	}
}
