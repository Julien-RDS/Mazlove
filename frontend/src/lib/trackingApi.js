import { tokenStore, SESSION_EXPIRED_EVENT } from "../api";
export async function trackingApi(
  path,
  token,
  method = "GET",
  body,
  options = {},
) {
  let response;
  try {
    response = await fetch(`${import.meta.env.VITE_API_URL || ""}/api${path}`, {
      method,
      signal: options.signal,
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error(
      "Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.",
    );
  }
  const text = await response.text();
  let value = null;
  try {
    value = text ? JSON.parse(text) : null;
  } catch {}
  if (!response.ok) {
    if (response.status === 401 && token && tokenStore.get() === token) {
      tokenStore.clear();
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    const error = new Error(
      value?.message || "Action indisponible. Réessayez.",
    );
    error.status = response.status;
    throw error;
  }
  return value;
}
