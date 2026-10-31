import React, { lazy, Suspense, Component } from "react";
import "./tracking.css";
import { useTheme } from "../../lib/theme";
const Map = lazy(() => import("./TrackingMap"));
class MapBoundary extends Component {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  render() {
    return this.state.failed ? (
      <p role="status">
        La carte n’a pas pu être chargée. Les informations du trajet restent
        disponibles. Rechargez la page pour réessayer.
      </p>
    ) : (
      this.props.children
    );
  }
}
export default function LazyTrackingMap(props) {
  // Le fond de carte (clair ou sombre) est choisi à la création : changer de thème recrée la carte.
  const theme = useTheme();
  return (
    <MapBoundary>
      <Suspense
        fallback={
          <div className="tracking-map map-placeholder" role="status">
            Chargement de la carte…
          </div>
        }
      >
        <Map key={theme} {...props} />
      </Suspense>
    </MapBoundary>
  );
}
