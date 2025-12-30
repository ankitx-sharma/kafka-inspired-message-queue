package org.main.engine.processor;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.main.engine.events.EngineEvent;
import org.main.engine.events.EngineEventType;
import org.main.engine.listener.EngineEventPublisher;

public class WorkerThreadPoolProcessorTest {
	private WorkerThreadPoolProcessor engine;
	
	@Test
	void cleanUp() throws Exception {
		if(engine != null) {
			assertTimeoutPreemptively(Duration.ofSeconds(5), () -> engine.shutdownGracefully());
		}
		
		Files.deleteIfExists(Path.of("tasks.queue"));
	}
	
	@Test
	void submitTask_shouldProcessFromMemory_andEmitEvents() throws Exception {
		EngineEventPublisher publisher = new EngineEventPublisher();
		List<EngineEventType> types = new CopyOnWriteArrayList<>();
		
		publisher.addListener(ev -> types.add(ev.type()));
		
		engine = new WorkerThreadPoolProcessor(1, 10, 0L, publisher);
		
		engine.submitTask("hello-1");
		
		assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
			while(!types.contains(EngineEventType.TASK_COMPLETED)) {
				Thread.sleep(10);
			}
		});
		
		assertTrue(types.contains(EngineEventType.SUBMITTED_TASK_FOR_EXECUTION));
		assertTrue(types.contains(EngineEventType.STARTED_TASK_PROCESSING));
		assertTrue(types.contains(EngineEventType.TASK_COMPLETED));
	}
	
	@Test
	void whenNoCapacity_shouldSpillToDisk_thenRecoverAndProcess() throws Exception {
		EngineEventPublisher publisher = new EngineEventPublisher();
		List<EngineEvent> events = new CopyOnWriteArrayList<>();
		publisher.addListener(events::add);
		
		WorkerThreadPoolProcessor engine = new WorkerThreadPoolProcessor(
				1, 1, 200L, publisher);
		
		// Submit 3 quickly -> third should spill to disk (noCapacity)
        engine.submitTask("msg-1");
        engine.submitTask("msg-2");
        engine.submitTask("msg-3");
        
        // Wait until we get 3 TASK_COMPLETED events
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
        	while(events.stream()
        			.filter(e -> e.type() == EngineEventType.TASK_COMPLETED).count() < 3) {
        		Thread.sleep(20);
        	}
        });
        
        long spilled = events.stream().filter(e -> e.type() == EngineEventType.TASK_SPILLED_TO_DISK).count();
        long recovered = events.stream().filter(e -> e.type() == EngineEventType.TASK_RECOVERED_FROM_DISK).count();
        
        assertTrue(spilled >=1, "Expected at least one TASK_SPILLED_TO_DISK");
        assertTrue(recovered >=1, "Expected at least one TASK_RECOVERED_FROM_DISK");
        
	}
}
