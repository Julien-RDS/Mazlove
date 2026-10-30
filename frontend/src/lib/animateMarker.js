export function animateMarker(
  marker,
  from,
  to,
  duration,
  reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches,
) {
  let frame, start;
  if (reduced) {
    marker.setLngLat(to);
    return () => {};
  }
  const ms = Math.max(500, Math.min(5000, duration));
  const step = (now) => {
    start ??= now;
    const t = Math.min(1, (now - start) / ms);
    marker.setLngLat([
      from[0] + (to[0] - from[0]) * t,
      from[1] + (to[1] - from[1]) * t,
    ]);
    if (t < 1) frame = requestAnimationFrame(step);
  };
  frame = requestAnimationFrame(step);
  return () => cancelAnimationFrame(frame);
}
