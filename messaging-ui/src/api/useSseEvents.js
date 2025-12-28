import { useEffect, useRef, useState } from "react";

export function useSseEvents(){
    const [events, setEvents] = useState([]);
    const esRef = useRef(null);

    useEffect(() => {
        const es = new EventSource("api/events/stream");
        esRef.current = es;

        const onAnyMessage = (msg) => {
            try{
                const data = JSON.parse(msg.data);
                if(data.type == "heartbeat") return; 

                setEvents((prev) => [{ ...data, id: crypto.randomUUID() }, ...prev].slice(0,300));
            }catch {
                setEvents((prev) => [{ type: "raw", 
                                    message: msg.data, 
                                    timestamp: new Date().toISOString(),
                                    id: crypto.randomUUID() }, ...prev]);
            }
        };

        es.onmessage = onAnyMessage;
        es.addEventListener("message", onAnyMessage);

        es.onerror = () => { 
            console.log("[SSE error]", e);
        };

        return () => {
            es.close();
            esRef.current = null;
        };

    }, []);

    return { events, clear: () => setEvents([]) };
}