import { useEffect, useRef, useState } from "react";

export function useSseEvents(){
    const [connected, setConnected] = useState(false);
    const [events, setEvents] = useState([]);

    // track last time we actually received data from server
    const lastMessageAtRef = useRef(0); 
    const esRef = useRef(null);

    useEffect(() => {
        function connect(){
            // close any previous connection
            if(esRef.current){
                esRef.current.close();
                esRef.current = null;
            }

            const es = new EventSource("api/events/stream");
            esRef.current = es;

            es.onopen = () => { 
                // onopen means TCP opened, but not necessarily "healthy"
                // we'll mark connected after first message OR keep this as optimistic
                // For now: optimistic true, but watchdog will correct it.
                setConnected(true); 
            }

            const onAnyMessage = (msg) => {
                lastMessageAtRef.current = Date.now();
                setConnected(true);

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

            es.onerror = () => { setConnected(false); };
        }

        connect();

        // Watchdog: if no message for 5 seconds => disconnected

        const watchdog = setInterval(() => {
            const last = lastMessageAtRef.current;

            if(last == 0){ return; }
            if(Date.now - last > 5000){
                setConnected(false);
            }
        }, 1000);

        return () => {
            clearInterval(watchdog);
            if(esRef.current) esRef.current.close();
        };

    }, []);

    return { connected, events, clear: () => setEvents([]) };
}