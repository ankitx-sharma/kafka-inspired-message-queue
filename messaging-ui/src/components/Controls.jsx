import { useState } from "react";
import { SCENARIO_PRESETS } from "../api/scenarioPresets";
import { startRun, stopRun, resetRun } from "../api/client";

export function Controls() {
    const [custom, setCustom] = useState({
        workerThreads: 2,
        queueCapacity: 10,
        processingDelayMs: 1000,
        messageCount: 20,
    });

    const [error, setError] = useState("");

    async function startCustom() {
        setError("");
        try{
            await startRun({...custom});
        } catch (e) {
            setError(String(e));
        }
    }

    async function startScenario(input){
        setError("");
        try{
            await startRun({ scenario: input});
        } catch (e){
            setError(String(e));
        } 
    }

    async function stop() {
        setError("");
        try{
            await stopRun();
        } catch (e) {
            setError(String(e));
        }
    }

    async function reset(deleteDiskQueueFile) {
        setError("");
        try{
            await resetRun(deleteDiskQueueFile);
        } catch (e) {
            setError(String(e));
        }
    }

    function applyScenarioToFields(input) {
        const preset = SCENARIO_PRESETS[input];
        if(!preset) return;
        setCustom(preset);
    }

    return (
        <div style={{ border: "1px solid #ddd", padding: 12, borderRadius: 12}}>
            <h2>Controls</h2>

            <div style={{ display: "flex", gap: 8, flexWrap: "wrap"}}>
                <button onClick={() => { applyScenarioToFields("A"); startScenario("A"); }}>
                    Scenario A
                </button>
                <button onClick={() =>{ applyScenarioToFields("B"); startScenario("B"); }}>
                    Scenario B
                </button>
                <button onClick={() => { applyScenarioToFields("C"); startScenario("C"); }}>
                    Scenario C
                </button>
                <button onClick={() => { applyScenarioToFields("D"); startScenario("D"); }}>
                    Scenario D
                </button>
                <button onClick={stop}>Stop Program</button>
                <button onClick={() => reset(false)}>Reset Program</button>
                <button onClick={() => reset(true)}>Reset Program and Clear Disk</button>
            </div>

            <p style={{ marginTop: 10 }}>
                Tip: Use Scenario buttons for presets, or fill custom values below and start custom run.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, maxWidth: 520 }}>
                <label>
                    workerThreads
                    <input type="number" 
                        value={custom.workerThreads}
                        onChange={(e) => setCustom({...custom, workerThreads: Number(e.target.value) })}
                    />
                </label>

                <label>
                    queueCapacity
                    <input type="number"
                        value={custom.queueCapacity}
                        onChange={(e) => setCustom({...customElements, queueCapacity: Number(e.target.value)})}
                    />
                </label>

                <label>
                    processingDelayMs
                    <input type="number"
                        value={custom.processingDelayMs}
                        onChange={(e) => setCustom({...custom, processingDelayMs: Number(e.target.value)})}
                    />
                </label>

                <label>
                    messageCount
                    <input type="number"
                        value={custom.messageCount}
                        onChange={(e) => setCustom({...custom, messageCount: Number(e.target.value)})}
                    />
                </label>
            </div>

            <div style={{ marginTop: 10 }}>
                <button onClick={startCustom}>Start Process</button>
            </div>

            {error ? <p style={{ color: "crimson" }}>{error}</p> : null}
        </div>
    );
}