package org.main.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.main.api.dto.RunConfig;
import org.main.api.dto.RunStatusResponse;
import org.main.api.service.RunService;
import org.main.api.service.SseHub;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(RunController.class)
public class RunControllerTest {

	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	
	@MockBean RunService runService;
	@MockBean SseHub sseHub;
	
	@Test
	void start_shouldResolveConfigAndStartRun() throws Exception {
		String body = """
		{
			"scenario": null,
			"messageCount": 99,
			"workerThreads": 3,
			"queueCapacity": 7,
			"processingDelayMs": 250
		}	
		""";
		
		mockMvc.perform(post("/api/run/start")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
			.andExpect(status().isOk());
		
		ArgumentCaptor<RunConfig> captor = ArgumentCaptor.forClass(RunConfig.class);
		verify(runService).startRun(captor.capture());
		RunConfig cfg = captor.getValue();
		
		assertThat(cfg.threads()).isEqualTo(3);
		assertThat(cfg.queueCapacity()).isEqualTo(7);
		assertThat(cfg.processingDelayMs()).isEqualTo(250);
		assertThat(cfg.messageCount()).isEqualTo(99);
		assertThat(cfg.scenario()).isNull();
		
		verify(sseHub).broadcast(any());
	}
	
	@Test
	void stop_shouldCallService() throws Exception {
		mockMvc.perform(post("/api/run/stop"))
			.andExpect(status().isOk());
		
		verify(runService).stopRun();
	}
	
	@Test
	void reset_withoutBody_shouldNotFail() throws Exception {
		String body = """
				{
					"deleteDiskQueueFile": false
				}	
				""";
		
		mockMvc.perform(post("/api/run/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());
		
		verify(runService).reset(false);
	}
	
	@Test
	void status_shouldReturnJsonFromService() throws Exception {
		when(runService.getRunStatus()).thenReturn(new RunStatusResponse("RUNNING", "abc-123", "CUSTOM"));
		
		mockMvc.perform(get("/api/run/status"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("RUNNING"))
			.andExpect(jsonPath("$.runId").value("abc-123"))
			.andExpect(jsonPath("$.scenarioMode").value("CUSTOM"));
	}
}
