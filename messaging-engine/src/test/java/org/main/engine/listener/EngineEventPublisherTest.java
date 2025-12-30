package org.main.engine.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.main.engine.events.EngineEvent;
import org.main.engine.events.EngineEventType;

public class EngineEventPublisherTest {

	@Test
	void publish_shouldNotifyListeners() {
		EngineEventPublisher publisher = new EngineEventPublisher();
		
		AtomicInteger count = new AtomicInteger(0);
		
		publisher.addListener(ev -> count.incrementAndGet());
		publisher.addListener(ev -> count.incrementAndGet());
		
		publisher.publish(new EngineEvent(
							EngineEventType.TASK_COMPLETED, 
							"msg-1", 
							"payload", 
							Instant.now(), 
							Map.of()
		));
		
		assertEquals(2, count.get());
	}
	
	@Test
	void removeListener_shouldStopReceivingEvents() {
		EngineEventPublisher publisher = new EngineEventPublisher();
		
		AtomicInteger count = new AtomicInteger(0);
		var listener = (Consumer<EngineEvent>) ev -> count.incrementAndGet();
		
		publisher.addListener(listener);
		publisher.publish(new EngineEvent(EngineEventType.RUN_IDLE, 
							"run", "idle", Instant.now(), Map.of()));
		assertEquals(1, count.get());
		
		publisher.removeListener(listener);
		publisher.publish(new EngineEvent(EngineEventType.RUN_IDLE, 
				"run", "idle", Instant.now(), Map.of()));
		assertEquals(1, count.get());
	}
}
