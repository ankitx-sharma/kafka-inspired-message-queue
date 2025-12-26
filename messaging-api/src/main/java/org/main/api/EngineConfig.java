package org.main.api;

import java.io.IOException;

import org.main.engine.processor.MessagingEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

	@Bean
	public MessagingEngine messagingEngine() throws IOException {
		return new MessagingEngine(4, 100);
	}
}
