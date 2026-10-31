/** Browser-managed lock survives route changes and is released automatically if a tab crashes. */
export function providerLease(
  name,
  {
    start,
    blocked,
    transferred,
    wait = false,
    locks = navigator.locks,
    channelFactory = (n) => new BroadcastChannel(n),
  },
) {
  let disposed = false,
    stopEngine,
    release,
    acquired = false,
    channel;
  const abort = new AbortController();
  try {
    channel = channelFactory(name);
  } catch {}
  const finish = () => {
    stopEngine?.();
    stopEngine = null;
    release?.();
    release = null;
    acquired = false;
  };
  if (!locks) {
    channel?.close();
    blocked(
      "Ce navigateur ne permet pas de sécuriser le partage entre onglets. Utilisez un navigateur récent en HTTPS.",
      "unsupported",
    );
    return { stop: () => {}, takeover: () => {} };
  }
  channel?.addEventListener("message", (e) => {
    if (e.data === "release" && acquired) {
      finish();
      transferred();
    }
  });
  if (wait) channel?.postMessage("release");
  const timeout = wait ? setTimeout(() => abort.abort(), 5000) : null;
  locks
    .request(
      name,
      wait ? { signal: abort.signal } : { ifAvailable: true },
      async (lock) => {
        clearTimeout(timeout);
        if (disposed) return;
        if (!lock) {
          blocked(
            "Le GPS est actif dans un autre onglet. Vous pouvez reprendre le suivi ici.",
          );
          return;
        }
        acquired = true;
        await new Promise((resolve) => {
          release = resolve;
          stopEngine = start();
        });
      },
    )
    .catch((e) => {
      if (!disposed)
        blocked(
          e.name === "AbortError"
            ? "Fermez l’autre onglet de suivi, puis réessayez."
            : "Le partage GPS n’a pas pu démarrer.",
        );
    });
  return {
    stop: () => {
      disposed = true;
      clearTimeout(timeout);
      abort.abort();
      finish();
      channel?.close();
    },
  };
}
