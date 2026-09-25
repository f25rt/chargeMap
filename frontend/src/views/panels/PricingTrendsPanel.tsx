import { useEffect, useState } from "react";
import {
  Box,
  Card,
  Chip,
  CircularProgress,
  Divider,
  LinearProgress,
  Stack,
  Typography,
} from "@mui/material";
import TrendingUpRoundedIcon from "@mui/icons-material/TrendingUpRounded";
import { api } from "../../api/client";
import type { PricingTrendItem } from "../../api/types";

/** Admin analytics: stations ranked by how often their price changed. */
export default function PricingTrendsPanel() {
  const [items, setItems] = useState<PricingTrendItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .adminPricingTrends(90)
      .then(setItems)
      .finally(() => setLoading(false));
  }, []);

  const max = items.reduce((m, i) => Math.max(m, i.priceChanges), 0) || 1;

  return (
    <Card sx={{ p: 2 }}>
      <Stack direction="row" spacing={1} alignItems="center" mb={1}>
        <TrendingUpRoundedIcon color="primary" />
        <Typography variant="subtitle1">Pricing trends (90 days)</Typography>
      </Stack>
      <Divider sx={{ mb: 1 }} />

      {loading ? (
        <Stack alignItems="center" py={4}>
          <CircularProgress />
        </Stack>
      ) : items.length === 0 ? (
        <Typography variant="body2" color="text.secondary" py={2}>
          No price changes recorded in the last 90 days.
        </Typography>
      ) : (
        <Stack spacing={1.5} sx={{ maxHeight: 420, overflowY: "auto" }}>
          {items.map((i) => (
            <Box key={i.stationId}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="body2" fontWeight={600} noWrap sx={{ minWidth: 0 }}>
                  {i.stationName}
                </Typography>
                <Chip
                  size="small"
                  label={`${i.priceChanges} change${i.priceChanges === 1 ? "" : "s"}`}
                  color={i.priceChanges >= 3 ? "warning" : "default"}
                />
              </Stack>
              <LinearProgress
                variant="determinate"
                value={(i.priceChanges / max) * 100}
                sx={{ mt: 0.5, height: 6, borderRadius: 3 }}
              />
              <Typography variant="caption" color="text.secondary">
                {i.currentPrice != null ? `Now ₱${i.currentPrice}/kWh` : "Price unknown"}
                {i.lastChangedAt
                  ? ` · last change ${new Date(i.lastChangedAt).toLocaleDateString()}`
                  : ""}
              </Typography>
            </Box>
          ))}
        </Stack>
      )}
    </Card>
  );
}
