package org.main.engine.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.main.engine.events.EngineEvent;

public class EngineEventPublisher {
	private final List<Consumer<EngineEvent>> listeners = new CopyOnWriteArrayList<>();
	
	public void addListener(Consumer<EngineEvent> listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Consumer<EngineEvent> listener) {
		listeners.remove(listener);
	}
	
	public void publish(EngineEvent event) {
		for(var listener: listeners) {
			listener.accept(event);
		}
	}
}
