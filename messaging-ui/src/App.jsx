import { Controls } from "./components/Controls";
import { EventFeed } from "./components/EventFeed";
import { useSseEvents } from "./api/useSseEvents";
import { useRunStatus } from "./api/useRunState";

function App() {
  const{ connected, events, clear } = useSseEvents();
  const status = useRunStatus();

  return (
    <div style={{ padding: 18, fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, Arial" }}>
      <h1>Messaging Processing System</h1>

      <p>
        Status: <b>{status.status}</b> | RunId: <b>{status.runId}</b> | Mode: <b>{status.scenarioMode}</b>
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1.2fr", gap: 14 }}>
        <Controls />
        <EventFeed connected={connected} events={events} onClear={clear} />
      </div>
    </div>
  );
}

export default App