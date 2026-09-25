import { Box, Chip, TextField } from "@mui/material";
import type { ChargerType, ConnectorType, StationFilters } from "../api/types";

interface Props {
  filters: StationFilters;
  onChange: (f: StationFilters) => void;
}

const CHARGER_TYPES: ChargerType[] = ["AC", "DC", "DC_FAST"];
const CONNECTORS: ConnectorType[] = ["CCS1", "CCS2", "CHADEMO", "TYPE2", "GBT", "NACS"];

export default function FilterBar({ filters, onChange }: Props) {
  const set = (patch: Partial<StationFilters>) => onChange({ ...filters, ...patch });

  return (
    <Box>
      {/* Tidy 2-column grid so the panel lines up cleanly */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 1.5,
        }}
      >
        <TextField
          select
          id="filter-charger"
          size="small"
          label="Charger"
          SelectProps={{ native: true }}
          InputLabelProps={{ shrink: true }}
          fullWidth
          value={filters.chargerType ?? ""}
          onChange={(e) => set({ chargerType: e.target.value as ChargerType | "" })}
        >
          <option value="">Any</option>
          {CHARGER_TYPES.map((t) => (
            <option key={t} value={t}>
              {t.replace("_", " ")}
            </option>
          ))}
        </TextField>

        <TextField
          select
          id="filter-connector"
          size="small"
          label="Connector"
          SelectProps={{ native: true }}
          InputLabelProps={{ shrink: true }}
          fullWidth
          value={filters.connector ?? ""}
          onChange={(e) => set({ connector: e.target.value as ConnectorType | "" })}
        >
          <option value="">Any</option>
          {CONNECTORS.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </TextField>

        <TextField
          size="small"
          type="number"
          label="Max ₱/kWh"
          fullWidth
          value={filters.priceMax ?? ""}
          onChange={(e) =>
            set({ priceMax: e.target.value === "" ? "" : Number(e.target.value) })
          }
        />

        <TextField
          size="small"
          type="number"
          label="Min kW"
          fullWidth
          value={filters.minKw ?? ""}
          onChange={(e) => set({ minKw: e.target.value === "" ? "" : Number(e.target.value) })}
        />
      </Box>

      <Box sx={{ mt: 1.5 }}>
        <Chip
          label="Available now"
          color={filters.availableOnly ? "success" : "default"}
          variant={filters.availableOnly ? "filled" : "outlined"}
          onClick={() => set({ availableOnly: !filters.availableOnly })}
        />
      </Box>
    </Box>
  );
}
