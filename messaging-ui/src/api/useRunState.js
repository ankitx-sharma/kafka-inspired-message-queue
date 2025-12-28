import { useEffect, useState } from "react";
import { fetchStatus } from "./client";

export function useRunStatus(){
    const[ status, setStatus ] = useState({ status: "UNKNOWN", runId: "-", scenarioMode: "-"});

    useEffect(() => {
        let alive = true;

        async function poll (){
            try{
                const s = await fetchStatus();
                if(alive) setStatus(s);
            } catch {}
        }

        poll();
        const t = setInterval(poll, 5000);
        return () => {
            alive = false;
            clearInterval(t);
        }
    }, []);

    return status;
}