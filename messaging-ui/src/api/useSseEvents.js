import { useEffect, useState } from "react";

export function useSseEvents(){
    const [connected, setConnected] = useState(false);
    const [events, setEvents] = useState([]);

    useEffect(() => {
        const es = new EventSource("api/events/stream");

        es.onopen = () => { setConnected(true); }
        es.onmessage = (msg) => {
            try{
                const data = JSON.parse(msg.data);
                setEvents((prev) => [{ ...data, id: crypto.randomUUID() }, ...prev].slice(0,300));
            }catch {
                setEvents((prev) => [{ type: "raw", 
                                    message: msg.data, 
                                    timestamp: new Date().toISOString(),
                                    id: crypto.randomUUID() }, ...prev]);
            }
        };

        es.onerror = () => setConnected(false);

        return () => es.close();
    }, []);

    return { connected, events, clear: () => setEvents([]) };
}