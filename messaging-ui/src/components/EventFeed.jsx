export function EventFeed({ connected, events, onClear }){
    return (
        <div style={{ border: "1px solid #ddd", padding: 12, borderRadius: 12}}>
            <h2>Live Event Feed</h2>
            <p>
                SSE: <b>{connected ? "connected"  : "disconnected"}</b>
                {" "}
                <button style={{ marginLeft: 10 }} onClick={onClear}>Clear</button>
            </p>

            <div style={{ height: 420, overflow: "auto", background: "#fafafa", padding: 10, borderRadius: 10 }}>
                {events.length === 0 ? (
                    <div style={{ color: "#666" }}>No events yet. Start a scenario.</div>
                ) : (
                    events.map((e) => (
                        <div key={e.id} style={{ background: "white", border: "1px solid #eee", padding: 10, borderRadius: 10, marginBottom: 10 }}>
                            <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                                <b>{e.type}</b>
                                <span style={{ color: "#666", fontSize: 12 }}>{e.timestamp}</span>
                            </div>
                            <div style={{ marginTop: 6 }}>{e.message}</div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}