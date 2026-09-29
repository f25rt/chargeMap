import { useEffect, useState } from "react";
import { Alert, Button, Snackbar } from "@mui/material";
import { registerSW } from "virtual:pwa-register";

/**
 * Registers the service worker and surfaces two lightweight prompts:
 *  - "New version available" with a Reload action (registerType: "prompt").
 *  - "Ready to work offline" confirmation the first time the SW caches the app.
 *
 * Mounted once near the app root. Renders nothing until there's something to say.
 */
export default function PwaUpdater() {
  const [needRefresh, setNeedRefresh] = useState(false);
  const [offlineReady, setOfflineReady] = useState(false);
  const [updateSW, setUpdateSW] = useState<((reload?: boolean) => Promise<void>) | null>(null);

  useEffect(() => {
    const update = registerSW({
      immediate: true,
      onNeedRefresh() {
        setNeedRefresh(true);
      },
      onOfflineReady() {
        setOfflineReady(true);
      },
    });
    setUpdateSW(() => update);
  }, []);

  const close = () => {
    setNeedRefresh(false);
    setOfflineReady(false);
  };

  return (
    <>
      <Snackbar
        open={needRefresh}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          severity="info"
          variant="filled"
          sx={{ alignItems: "center" }}
          action={
            <Button
              color="inherit"
              size="small"
              onClick={() => updateSW?.(true)}
              sx={{ fontWeight: 700 }}
            >
              Reload
            </Button>
          }
        >
          A new version of ChargeMap is available.
        </Alert>
      </Snackbar>

      <Snackbar
        open={offlineReady}
        autoHideDuration={4000}
        onClose={close}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert severity="success" variant="filled" onClose={close}>
          ChargeMap is ready to work offline.
        </Alert>
      </Snackbar>
    </>
  );
}
