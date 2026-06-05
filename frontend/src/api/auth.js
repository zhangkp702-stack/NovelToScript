async function request(url, method, body) {
  const response = await fetch(url, {
    method,
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : {},
    body: body ? JSON.stringify(body) : undefined
  });

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();
  return { response, payload };
}

export function register(payload) {
  return request("/api/auth/register", "POST", payload);
}

export function login(payload) {
  return request("/api/auth/login", "POST", payload);
}

export function currentUser() {
  return request("/api/auth/me", "GET");
}

export function logout() {
  return request("/api/auth/logout", "POST");
}
