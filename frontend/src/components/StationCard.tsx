import { Box, Card, CardActionArea, Stack, Typography } from "@mui/material";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";
import AccessTimeRoundedIcon from "@mui/icons-material/AccessTimeRounded";
import type { StationSummary } from "../api/types";
import { statusDot, statusLabel } from "../theme";

interface Props {
  station: StationSummary;
  favorite?: boolean;
  selected?: boolean;
  onClick: () => void;
  /** Estimated drive time to this station (shown only when the user set their battery range). */
  etaText?: string | null;
  /** Whether the station is reachable within the user's battery range. */
  reachable?: boolean;
}

export default function StationCard({
  station,
  favorite,
  selected,
  onClick,
  etaText,
  reachable = true,
}: Props) {
  const dot = statusDot(station.availabilitySummary);

  return (
    <Card
      sx={{
        boxShadow: selected ? "0 6px 20px rgba(31,157,87,.18)" : "0 2px 10px rgba(20,45,30,.05)",
        outline: selected ? "1.5px solid" : "none",
        outlineColor: "primary.main",
      }}
    >
      <CardActionArea onClick={onClick} sx={{ px: 1.5, py: 1.1, borderRadius: 3.5 }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <Box sx={{ flex: 1, minWidth: 0 }}>
            {/* Name + favorite */}
            <Stack direction="row" spacing={0.5} alignItems="center" sx={{ minWidth: 0 }}>
              {favorite && <FavoriteRoundedIcon sx={{ fontSize: 14, color: "#dc2626" }} />}
              <Typography sx={{ fontSize: 14.5, fontWeight: 600 }} noWrap>
                {station.name}
              </Typography>
            </Stack>

            {/* Status line: dot + availability + distance */}
            <Stack direction="row" spacing={0.5} alignItems="center" mt={0.25}>
              <Box sx={{ width: 6, height: 6, borderRadius: "50%", bgcolor: dot }} />
              <Typography sx={{ fontSize: 12, fontWeight: 600, color: dot }}>
                {statusLabel(station.availabilitySummary)}
              </Typography>
              <Typography sx={{ fontSize: 12 }} color="text.secondary" noWrap>
                · {station.availableCount}/{station.totalChargers} free
                {station.distanceMeters != null &&
                  ` · ${(station.distanceMeters / 1000).toFixed(1)} km`}
              </Typography>
            </Stack>

            {/* ETA line — only when the user has entered their battery range. */}
            {etaText && (
              <Stack direction="row" spacing={0.375} alignItems="center" mt={0.25}>
                <AccessTimeRoundedIcon
                  sx={{ fontSize: 12, color: reachable ? "success.main" : "warning.main" }}
                />
                <Typography
                  sx={{ fontSize: 12, fontWeight: 600 }}
                  color={reachable ? "success.main" : "warning.main"}
                >
                  {etaText} away
                </Typography>
                {!reachable && (
                  <Typography sx={{ fontSize: 11 }} color="warning.main" noWrap>
                    · out of range
                  </Typography>
                )}
              </Stack>
            )}
          </Box>

          {/* Price */}
          <Box sx={{ textAlign: "right", flexShrink: 0 }}>
            <Typography sx={{ fontSize: 15, fontWeight: 700, color: "primary.main", lineHeight: 1 }}>
              {station.pricePerKwh != null ? `₱${station.pricePerKwh}` : "—"}
            </Typography>
            <Typography sx={{ fontSize: 10 }} color="text.secondary">
              /kWh
            </Typography>
          </Box>
        </Stack>
      </CardActionArea>
    </Card>
  );
}
