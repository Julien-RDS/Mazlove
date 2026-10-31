import React from "react";
import ReactDOM from "react-dom/client";
// Polices auto-hébergées (pas d'appel à Google Fonts : rien ne part chez un tiers, RGPD oblige).
import "@fontsource/fraunces/500.css";
import "@fontsource/fraunces/600.css";
import "@fontsource/atkinson-hyperlegible/400.css";
import "@fontsource/atkinson-hyperlegible/700.css";
import "./theme.css";
import "./index.css";
import { initTheme } from "./lib/theme";
import CuddleApp from "./components/CuddleApp";
const AdminSosPage = React.lazy(
  () => import("./components/tracking/AdminSosPage"),
);
const PublicTripPage = React.lazy(
  () => import("./components/tracking/PublicTripPage"),
);
const Demo =
  import.meta.env.VITE_DEMO === "true"
    ? React.lazy(() => import("./dev/DemoSplitScreen"))
    : null;
initTheme();
const path = window.location.pathname;
const page =
  path === "/admin/sos" ? (
    <AdminSosPage />
  ) : path.startsWith("/t/") ? (
    <PublicTripPage token={path.slice(3)} />
  ) : path === "/demo" && Demo ? (
    <React.Suspense fallback={<p>Chargement…</p>}>
      <Demo />
    </React.Suspense>
  ) : (
    <CuddleApp />
  );

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <React.Suspense fallback={<p role="status">Chargement…</p>}>
      {page}
    </React.Suspense>
  </React.StrictMode>,
);
