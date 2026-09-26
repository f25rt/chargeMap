import { useEffect, useRef, useState, type ReactNode } from "react";
import { Box, Paper } from "@mui/material";

/**
 * A draggable bottom sheet with three snap points (peek / half / full), mimicking the
 * iOS Maps / Apple Maps pattern. Drag the grabber (or the header area) to resize.
 * Heights are expressed as fractions of the available shell height.
 */
const SNAPS = [0.2, 0.55, 0.92]; // peek, half, full

interface Props {
  children: ReactNode;
  bottomInset: number; // px reserved for the bottom nav
  /** Fired with the snap index (0=peek,1=half,2=full) whenever it changes. */
  onSnapChange?: (snapIndex: number) => void;
  /** Fired with the live height fraction (0..1) as the sheet resizes/drags. */
  onFractionChange?: (fraction: number) => void;
}

export default function BottomSheet({
  children,
  bottomInset,
  onSnapChange,
  onFractionChange,
}: Props) {
  const [snap, setSnap] = useState(0); // start at "peek" (compact)
  const containerRef = useRef<HTMLDivElement>(null);
  const drag = useRef<{ startY: number; startFrac: number } | null>(null);
  const [frac, setFrac] = useState(SNAPS[1]);

  useEffect(() => {
    setFrac(SNAPS[snap]);
    onSnapChange?.(snap);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [snap]);

  useEffect(() => {
    onFractionChange?.(frac);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [frac]);

  const onPointerDown = (e: React.PointerEvent) => {
    (e.target as HTMLElement).setPointerCapture?.(e.pointerId);
    drag.current = { startY: e.clientY, startFrac: frac };
  };

  const onPointerMove = (e: React.PointerEvent) => {
    if (!drag.current) return;
    const shell = containerRef.current?.parentElement;
    const h = shell?.clientHeight ?? window.innerHeight;
    const deltaFrac = (drag.current.startY - e.clientY) / h;
    const next = Math.min(0.94, Math.max(0.08, drag.current.startFrac + deltaFrac));
    setFrac(next);
  };

  const onPointerUp = () => {
    if (!drag.current) return;
    drag.current = null;
    // Snap to the nearest configured point.
    let nearest = 0;
    let best = Infinity;
    SNAPS.forEach((s, i) => {
      const d = Math.abs(s - frac);
      if (d < best) {
        best = d;
        nearest = i;
      }
    });
    setSnap(nearest);
  };

  return (
    <Paper
      ref={containerRef}
      elevation={0}
      sx={{
        position: "absolute",
        left: 0,
        right: 0,
        bottom: 0,
        height: `calc(${frac * 100}% )`,
        borderTopLeftRadius: 32,
        borderTopRightRadius: 32,
        boxShadow: "0 -10px 40px rgba(20,45,30,.16)",
        transition: drag.current ? "none" : "height .32s cubic-bezier(.32,.72,0,1)",
        display: "flex",
        flexDirection: "column",
        zIndex: 1100,
        overflow: "hidden",
      }}
    >
      {/* Grab handle — the whole strip is the drag target for easy thumb use. */}
      <Box
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        sx={{
          py: 1.25,
          display: "flex",
          justifyContent: "center",
          cursor: "grab",
          touchAction: "none",
          flexShrink: 0,
        }}
      >
        <Box sx={{ width: 40, height: 5, borderRadius: 3, bgcolor: "#c7ccd1" }} />
      </Box>

      <Box
        sx={{
          flex: 1,
          overflowY: "auto",
          px: 2,
          pb: `calc(${bottomInset}px + 12px)`,
          WebkitOverflowScrolling: "touch",
        }}
      >
        {children}
      </Box>
    </Paper>
  );
}
