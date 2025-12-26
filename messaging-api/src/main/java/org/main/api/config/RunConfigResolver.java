package org.main.api.config;

import org.main.api.dto.RunConfig;
import org.main.api.dto.RunRequest;
import org.main.api.dto.Scenario;
import org.main.api.dto.ScenarioPresets;

public final class RunConfigResolver {
	private RunConfigResolver() {}
	
	private static final int DEFAULT_THREADS = 2;
    private static final int DEFAULT_QUEUE_CAP = 10;
    private static final long DEFAULT_DELAY_MS = 1000;
    private static final long DEFAULT_COUNT = 10;
    
    public static RunConfig resolve(RunRequest request) {
    	Scenario scenario = parseScenario(request.scenario());
    	
    	if(scenario !=null ) {
    		var preset = ScenarioPresets.from(scenario);
    		return new RunConfig(preset.threads(), 
			    				preset.queueCapacity(), 
			    				preset.processingDelayMs(), 
			    				preset.messageCount(), 
			    				scenario);
    	}else {
    		int threads = request.workerThreads() != null ? request.workerThreads() : DEFAULT_THREADS;
            int queueCap = request.queueCapacity() != null ? request.queueCapacity() : DEFAULT_QUEUE_CAP;
            long delay = request.processingDelayMs() != null ? request.processingDelayMs() : DEFAULT_DELAY_MS;
            long count = request.messageCount() != null ? request.messageCount() : DEFAULT_COUNT;
            
            return new RunConfig(threads, queueCap, delay, count, scenario);
    	}
    }
    
    private static Scenario parseScenario(String raw) {
    	if(raw == null || 
    	  (raw !=null && raw.trim().isEmpty())) {
    		return null;
    	}
    	
    	return Scenario.valueOf(raw.trim().toUpperCase()); 
    }
}
