import { Box, Button, Chip, Divider, MenuItem, Stack, TextField, Typography } from "@mui/material";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import type { ChargerType, ConnectorType, StationFilters } from "../api/types";

interface Props {
  filters: StationFilters;
  onChange: (f: StationFilters) => void;
}

const CHARGER_TYPES: ChargerType[] = ["AC", "DC", "DC_FAST"];
const CONNECTORS: ConnectorType[] = ["CCS1", "CCS2", "CHADEMO", "TYPE2", "GBT", "NACS"];

/** Small uppercase section label. */
function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <Typography
      sx={{
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: 0.5,
        textTransform: "uppercase",
        color: "text.secondary",
        mb: 0.75,
      }}
    >
      {children}
    </Typography>
  );
}

export default function FilterBar({ filters, onChange }: Props) {
  const set = (patch: Partial<StationFilters>) => onChange({ ...filters, ...patch });

  const activeCount = Object.entries(filters).filter(
    ([, v]) => v !== "" && v != null && v !== false,
  ).length;

  const clearAll = () =>
    onChange({
      chargerType: "",
      connector: "",
      priceMax: "",
      minKw: "",
      maxKw: "",
      availableOnly: false,
    });

  return (
    <Box>
      {/* Header */}
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1.5}>
        <Typography sx={{ fontSize: 15, fontWeight: 700 }}>Filters</Typography>
        {activeCount > 0 && (
          <Button size="small" onClick={clearAll} sx={{ textTransform: "none" }}>
            Clear all
          </Button>
        )}
      </Stack>

      {/* Connector & charger type */}
      <SectionLabel>Charger</SectionLabel>
      <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1.5 }}>
        <TextField
          select
          id="filter-charger"
          size="small"
          label="Type"
          fullWidth
          value={filters.chargerType ?? ""}
          onChange={(e) => set({ chargerType: e.target.value as ChargerType | "" })}
        >
          <MenuItem value="">Any</MenuItem>
          {CHARGER_TYPES.map((t) => (
            <MenuItem key={t} value={t}>
              {t.replace("_", " ")}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          id="filter-connector"
          size="small"
          label="Connector"
          fullWidth
          value={filters.connector ?? ""}
          onChange={(e) => set({ connector: e.target.value as ConnectorType | "" })}
        >
          <MenuItem value="">Any</MenuItem>
          {CONNECTORS.map((c) => (
            <MenuItem key={c} value={c}>
              {c}
            </MenuItem>
          ))}
        </TextField>
      </Box>

      <Divider sx={{ my: 2 }} />

      {/* Power range */}
      <SectionLabel>Power output (kW)</SectionLabel>
      <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1.5 }}>
        <TextField
          size="small"
          type="number"
          label="Min kW"
          fullWidth
          value={filters.minKw ?? ""}
          onChange={(e) => set({ minKw: e.target.value === "" ? "" : Number(e.target.value) })}
        />
        <TextField
          size="small"
          type="number"
          label="Max kW"
          fullWidth
          value={filters.maxKw ?? ""}
          onChange={(e) => set({ maxKw: e.target.value === "" ? "" : Number(e.target.value) })}
        />
      </Box>

      <Divider sx={{ my: 2 }} />

      {/* Price */}
      <SectionLabel>Price</SectionLabel>
      <TextField
        size="small"
        type="number"
        label="Max ₱/kWh"
        fullWidth
        value={filters.priceMax ?? ""}
        onChange={(e) => set({ priceMax: e.target.value === "" ? "" : Number(e.target.value) })}
      />

      <Divider sx={{ my: 2 }} />

      {/* Availability */}
      <SectionLabel>Availability</SectionLabel>
      <Chip
        icon={<BoltRoundedIcon />}
        label="Available now"
        color={filters.availableOnly ? "success" : "default"}
        variant={filters.availableOnly ? "filled" : "outlined"}
        onClick={() => set({ availableOnly: !filters.availableOnly })}
        sx={{ fontWeight: 600 }}
      />
    </Box>
  );
}
