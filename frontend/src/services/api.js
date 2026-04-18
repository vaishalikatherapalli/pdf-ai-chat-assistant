const BASE_URL = 'http://localhost:8080/api';

export async function streamChat(message, onToken, onDone, onError) {
    const response = await fetch(`${BASE_URL}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }),
    });

    if (!response.ok) {
        onError('Server error: ' + response.statusText);
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop(); // keep incomplete line in buffer

        for (const line of lines) {
            const clean = line.replace(/\r/g, '');
            if (!clean.startsWith('data:')) continue;
            // strip "data:" and one optional space per SSE spec
            const raw = clean.slice(5).replace(/^ /, '');
            try {
                const msg = JSON.parse(raw);
                if (msg.done) { onDone(); return; }
                if (msg.error) { onError('Stream error'); return; }
                if (msg.t !== undefined) onToken(msg.t);
            } catch (e) { /* incomplete chunk, skip */ }
        }
    }
    onDone();
}

export async function uploadPdf(file, onSuccess, onError) {
    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await fetch(`${BASE_URL}/pdf/upload`, {
            method: 'POST',
            body: formData,
        });
        const data = await res.json();
        if (res.ok) onSuccess(data.message);
        else onError(data.error);
    } catch (e) {
        onError(e.message);
    }
}

export async function listPdfs() {
    const res = await fetch(`${BASE_URL}/pdf/list`);
    return res.json();
}

export async function deletePdf(filename) {
    await fetch(`${BASE_URL}/pdf/${encodeURIComponent(filename)}`, { method: 'DELETE' });
}
