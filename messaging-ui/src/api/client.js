export async function startRun(payload){
    const res = await fetch("api/run/start", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(payload),
    });

    if(!res.ok) throw Error("Start failed"); 
}

export async function stopRun(){
    const res = await fetch("api/run/stop", {method: "POST"});
    if(!res.ok) throw new Error("Stop failed");
}

export async function resetRun(deleteDiskQueueFile = false){
    const res = await fetch("api/run/reset", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({deleteDiskQueueFile}),
    });

    if(!res.ok) throw new Error("Reset failed");
}

export async function fetchStatus(){
    const res = await fetch("api/run/status");
    if(!res.ok) throw new Error("Fetch Status failed");
    return await res.json();
}