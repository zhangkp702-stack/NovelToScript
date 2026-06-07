const SESSION_ID_KEY = "ntc_session_id";
const SESSION_ID_HEADER = "X-Session-Id";

function buildHeaders(extra = {}) {
  const headers = { ...extra };
  const sessionId = localStorage.getItem(SESSION_ID_KEY);
  if (sessionId) {
    headers[SESSION_ID_HEADER] = sessionId;
  }
  return headers;
}

async function request(url, method, body) {
  const headers = buildHeaders(body ? { "Content-Type": "application/json" } : {});
  const response = await fetch(url, {
    method,
    credentials: "include",
    headers,
    body: body ? JSON.stringify(body) : undefined
  });

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();
  return { response, payload };
}

function parseSseChunk(buffer) {
  const events = [];
  const blocks = buffer.split("\n\n");
  const remainder = blocks.pop() ?? "";

  for (const block of blocks) {
    if (!block.trim()) {
      continue;
    }
    let eventName = "message";
    const dataLines = [];
    for (const line of block.split("\n")) {
      if (line.startsWith("event:")) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
    }
    events.push({ event: eventName, data: dataLines.join("\n") });
  }

  return { events, remainder };
}

export function generateScript(payload) {
  return request("/api/scripts/generate", "POST", payload);
}

export function saveScript(payload) {
  return request("/api/scripts", "POST", payload);
}

export function listScripts(workTitle = "") {
  const query = new URLSearchParams();
  if (workTitle) {
    query.set("workTitle", workTitle);
  }
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return request(`/api/scripts${suffix}`, "GET");
}

export function getScript(id) {
  return request(`/api/scripts/${id}`, "GET");
}

export function listWorks() {
  return request("/api/scripts/works", "GET");
}

export function deleteWork(workTitle = "") {
  const query = new URLSearchParams();
  if (workTitle) {
    query.set("workTitle", workTitle);
  }
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return request(`/api/scripts/works${suffix}`, "DELETE");
}

export async function generateScriptStream(payload, handlers = {}, signal) {
  const response = await fetch("/api/scripts/generate/stream", {
    method: "POST",
    credentials: "include",
    headers: buildHeaders({
      "Content-Type": "application/json",
      Accept: "text/event-stream"
    }),
    body: JSON.stringify(payload),
    signal
  });

  if (!response.ok) {
    const contentType = response.headers.get("content-type") || "";
    const errorPayload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();
    return { response, payload: errorPayload };
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error("浏览器不支持流式响应");
  }

  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  function dispatchEvents(events) {
    for (const item of events) {
      if (item.event === "open" && handlers.onOpen) {
        handlers.onOpen(item.data);
      } else if (item.event === "token" && handlers.onToken) {
        handlers.onToken(item.data);
      } else if (item.event === "warn" && handlers.onWarn) {
        handlers.onWarn(item.data);
      } else if (item.event === "done" && handlers.onDone) {
        handlers.onDone(item.data);
      } else if (item.event === "error" && handlers.onError) {
        handlers.onError(item.data);
      }
    }
  }

  function consumeBuffer(chunk = "") {
    buffer += chunk;
    const parsed = parseSseChunk(buffer);
    buffer = parsed.remainder;
    dispatchEvents(parsed.events);
  }

  while (true) {
    const { done, value } = await reader.read();
    if (value) {
      consumeBuffer(decoder.decode(value, { stream: true }));
    }
    if (done) {
      break;
    }
  }

  consumeBuffer(decoder.decode());
  if (buffer.trim()) {
    consumeBuffer("\n\n");
  }

  return { response };
}
