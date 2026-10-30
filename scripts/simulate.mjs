const [id, scenario = "HAPPY_PATH", speed = 10] = process.argv.slice(2);
const base = process.env.API_URL || "http://localhost:3001";
const login = await fetch(base + "/api/dev/login-as", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email: "sophie@test.com" }),
});
if (!login.ok) throw new Error("Activez le profil dev et le seed.");
const { token } = await login.json();
const call = async (path, body) => {
  const r = await fetch(base + path, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok) throw new Error(await r.text());
};
const s = await (
  await fetch(base + `/api/sessions/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
).json();
if (s.status === "ACCEPTED") {
  await call("/api/consents");
  await call(`/api/sessions/${id}/depart`, {
    travelMode: "DRIVING",
    lat: s.meetingPoint.lat + 0.015,
    lng: s.meetingPoint.lng - 0.025,
  });
}
await call(
  `/api/dev/tracking/${id}/simulate?scenario=${scenario}&speedFactor=${speed}`,
);
console.log("Simulation lancée.");
