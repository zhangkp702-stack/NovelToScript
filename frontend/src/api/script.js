const SESSION_ID_KEY = "ntc_session_id";
const SESSION_ID_HEADER = "X-Session-Id";

async function request(url, method, body) {
  const headers = body ? { "Content-Type": "application/json" } : {};
  const sessionId = localStorage.getItem(SESSION_ID_KEY);
  if (sessionId) {
    headers[SESSION_ID_HEADER] = sessionId;
  }
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

export function generateScript(payload) {
  return request("/api/scripts/generate", "POST", payload);
}
