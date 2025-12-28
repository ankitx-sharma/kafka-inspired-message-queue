export const SCENARIO_PRESETS = {
    A: {workerThreads: 4, queueCapacity: 50, processingDelayMs: 50, messageCount: 30},
    B: {workerThreads: 2, queueCapacity: 5, processingDelayMs: 50, messageCount: 200},
    C: {workerThreads: 2, queueCapacity: 10, processingDelayMs: 3000, messageCount: 60},
    D: {workerThreads: 1, queueCapacity: 3, processingDelayMs: 2000, messageCount: 120},
};