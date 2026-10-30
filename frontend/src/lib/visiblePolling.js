/** One request at a time; pause background tabs, abort on disposal, coalesce refreshes. */
export function startVisiblePolling(
  load,
  delay,
  { onError = () => {}, onVisibility, visibility = document } = {},
) {
  let stopped = false,
    running = false,
    queued = false,
    timer,
    controller;
  const refresh = async () => {
    clearTimeout(timer);
    if (stopped || visibility.hidden) return;
    if (running) {
      queued = true;
      return;
    }
    running = true;
    controller = new AbortController();
    try {
      await load(controller.signal);
    } catch (error) {
      if (!stopped && error.name !== "AbortError") onError(error);
    } finally {
      running = false;
      if (!stopped && !visibility.hidden) {
        if (queued) {
          queued = false;
          refresh();
        } else timer = setTimeout(refresh, delay);
      }
    }
  };
  const visible = () => {
    onVisibility?.(!visibility.hidden);
    if (visibility.hidden) clearTimeout(timer);
    else refresh();
  };
  visibility.addEventListener("visibilitychange", visible);
  refresh();
  return {
    refresh,
    stop: () => {
      stopped = true;
      clearTimeout(timer);
      controller?.abort();
      visibility.removeEventListener("visibilitychange", visible);
    },
  };
}
