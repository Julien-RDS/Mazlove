export async function trackingApi(path, token, method = "GET", body) {
  const r = await fetch(`${import.meta.env.VITE_API_URL || ""}/api${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await r.text();
  let value;
  try {
    value = data ? JSON.parse(data) : null;
  } catch {
    value = null;
  }
  if (!r.ok)
    throw new Error(value?.message || "Action indisponible. Réessayez.");
  return value;
}
