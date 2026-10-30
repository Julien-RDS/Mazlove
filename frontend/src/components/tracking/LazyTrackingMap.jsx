import React, { lazy, Suspense, Component } from "react";
import "./tracking.css";
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
  return (
    <MapBoundary>
      <Suspense
        fallback={
          <div className="tracking-map map-placeholder" role="status">
            Chargement de la carte…
          </div>
        }
      >
        <Map {...props} />
      </Suspense>
    </MapBoundary>
  );
}
