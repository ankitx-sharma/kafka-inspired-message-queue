package org.main.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MessagingAPIApplication {
	public static void main(String[] args) {
		SpringApplication.run(MessagingAPIApplication.class, args);
	}
}
