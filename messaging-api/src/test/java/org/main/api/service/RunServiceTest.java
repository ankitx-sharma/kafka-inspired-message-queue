package org.main.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.main.api.dto.RunConfig;
import org.main.api.dto.RunStatusResponse;

public class RunServiceTest {
	private final SseHub sseHub = mock(SseHub.class);
	private final RunService runService = new RunService(sseHub);
	
	@AfterEach
	void cleanUp() {
		runService.stopRun();
		try {
			Files.deleteIfExists(Path.of("tasks.queue"));
		} catch(IOException ignored) {}
	}
	
	@Test
	void startRun_shouldSetStatusToRunning_AndCreateRunId() throws IOException {
		RunConfig cfg = new RunConfig(1, 1, 0L, 0L, null);
		runService.startRun(cfg);
		
		RunStatusResponse status = runService.getRunStatus();
		assertThat(status.status()).isEqualTo("RUNNING");
        assertThat(status.runId()).startsWith("run-");
        assertThat(status.scenarioMode()).isEqualTo("none");
        assertThat(runService.currentEngine()).isNotNull();

        verify(sseHub, atLeast(0)).broadcast(any());;
	}
	
	@Test
	void startRun_whenNotRunning_shouldDoNothing() {
		runService.stopRun();
		
		RunStatusResponse status = runService.getRunStatus();
		assertThat(status.status()).isEqualTo("IDLE");
		assertThat(runService.currentEngine()).isNull();
	}
	
	@Test
	void stopRun_shouldSetStatusToStopped() throws IOException {
		RunConfig cfg = new RunConfig(1, 1, 0L, 0L, null);
		runService.startRun(cfg);
		
		runService.stopRun();
		
		RunStatusResponse status = runService.getRunStatus();
		assertThat(status.status()).isEqualTo("STOPPED");
		assertThat(runService.currentEngine()).isNull();
	}
	
	@Test
	void reset_true_shouldDeleteQueueFile() throws IOException{
		Files.writeString(Path.of("tasks.queue"), "dummy");
		
		assertThat(Files.exists(Path.of("tasks.queue"))).isTrue();
		
		runService.reset(true);
		
		RunStatusResponse status = runService.getRunStatus();
		assertThat(status.status()).isEqualTo("IDLE");
        assertThat(status.runId()).isNull();
        assertThat(status.scenarioMode()).isEqualTo("none");
        
        assertThat(Files.exists(Path.of("tasks.queue"))).isFalse();
	}
	
	@Test
	void reset_false_shouldNotDeleteQueueFile() throws IOException{
		Files.writeString(Path.of("tasks.queue"), "dummy");
		
		assertThat(Files.exists(Path.of("tasks.queue"))).isTrue();
		
		runService.reset(false);
		
		RunStatusResponse status = runService.getRunStatus();
		assertThat(status.status()).isEqualTo("IDLE");
        assertThat(status.runId()).isNull();
        assertThat(status.scenarioMode()).isEqualTo("none");
        
        assertThat(Files.exists(Path.of("tasks.queue"))).isTrue();
        Files.delete(Path.of("tasks.queue"));
	}
}
