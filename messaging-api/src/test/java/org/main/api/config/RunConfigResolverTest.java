package org.main.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.main.api.dto.RunConfig;
import org.main.api.dto.RunRequest;
import org.main.api.dto.Scenario;

public class RunConfigResolverTest {

	@Test
	void resolve_shouldUseDefaultsWhenNulls() {
		RunRequest req = new RunRequest(null, null, null, null, null);
		RunConfig cfg = RunConfigResolver.resolve(req);
		
		assertThat(cfg.threads()).isEqualTo(2);
		assertThat(cfg.queueCapacity()).isEqualTo(10);
		assertThat(cfg.processingDelayMs()).isEqualTo(1000);
		assertThat(cfg.messageCount()).isEqualTo(10);
		assertThat(cfg.scenario()).isNull();
	}
	
	void resolve_shouldUsePresetA() {
		RunRequest req = new RunRequest("A", 999L, 9, 9, 9L);
		RunConfig cfg = RunConfigResolver.resolve(req);
		
		assertThat(cfg.scenario()).isEqualTo(Scenario.A);
		assertThat(cfg.threads()).isEqualTo(4);
		assertThat(cfg.queueCapacity()).isEqualTo(50);
		assertThat(cfg.processingDelayMs()).isEqualTo(50);
		assertThat(cfg.messageCount()).isEqualTo(30);
	}
}
