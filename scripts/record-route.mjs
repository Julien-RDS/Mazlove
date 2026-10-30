// Record once, commit the OSRM response. Tests never call the network.
import { writeFile, mkdir } from "node:fs/promises";
const routes = [
  ["chatelet", "2.319,48.874;2.347,48.8584"],
  ["bastille", "2.355,48.876;2.369,48.853"],
];
await mkdir(new URL("./routes/", import.meta.url), { recursive: true });
for (const [name, coords] of routes) {
  const r = await fetch(
    `https://router.project-osrm.org/route/v1/driving/${coords}?geometries=geojson&overview=full`,
  );
  if (!r.ok) throw new Error(`OSRM : ${r.status}`);
  const data = await r.json();
  if (data.code !== "Ok") throw new Error("Itinéraire absent");
  await writeFile(
    new URL(`./routes/${name}.geojson`, import.meta.url),
    JSON.stringify(
      {
        type: "Feature",
        properties: {
          source: "OSRM",
          recordedAt: new Date().toISOString(),
          distance: data.routes[0].distance,
          duration: data.routes[0].duration,
        },
        geometry: data.routes[0].geometry,
      },
      null,
      2,
    ),
  );
}
