import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import CancelRoundedIcon from "@mui/icons-material/CancelRounded";
import { api } from "../../api/client";
import type { Submission, SubmissionStatus } from "../../api/types";

const TABS: { label: string; value: SubmissionStatus | "ALL" }[] = [
  { label: "Pending", value: "PENDING" },
  { label: "Approved", value: "APPROVED" },
  { label: "Rejected", value: "REJECTED" },
  { label: "All", value: "ALL" },
];

const statusColor = (s: SubmissionStatus) =>
  s === "APPROVED" ? "success" : s === "REJECTED" ? "error" : "warning";

export default function SubmissionsPanel() {
  const [filter, setFilter] = useState<SubmissionStatus | "ALL">("PENDING");
  const [subs, setSubs] = useState<Submission[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [rejectFor, setRejectFor] = useState<string | null>(null);
  const [rejectText, setRejectText] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      setSubs(await api.moderationSubmissions(filter === "ALL" ? undefined : filter));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const approve = async (id: string) => {
    setBusyId(id);
    try {
      await api.moderationApprove(id);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const reject = async (id: string) => {
    if (!rejectText.trim()) return;
    setBusyId(id);
    try {
      await api.moderationReject(id, rejectText.trim());
      setRejectFor(null);
      setRejectText("");
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <Box>
      <Stack direction="row" spacing={1} mb={2} flexWrap="wrap" useFlexGap>
        {TABS.map((t) => (
          <Chip
            key={t.value}
            label={t.label}
            color={filter === t.value ? "primary" : "default"}
            variant={filter === t.value ? "filled" : "outlined"}
            onClick={() => setFilter(t.value)}
          />
        ))}
      </Stack>

      {loading ? (
        <Stack alignItems="center" py={6}>
          <CircularProgress />
        </Stack>
      ) : subs.length === 0 ? (
        <Typography color="text.secondary" py={4} textAlign="center">
          No {filter === "ALL" ? "" : filter.toLowerCase()} submissions.
        </Typography>
      ) : (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", md: "1fr 1fr", xl: "1fr 1fr 1fr" },
            gap: 2,
          }}
        >
          {subs.map((s) => (
            <Card key={s.id} sx={{ overflow: "hidden" }}>
              {s.imageId && (
                <Box
                  component="img"
                  src={api.imageUrl(s.imageId)}
                  sx={{ width: "100%", height: 160, objectFit: "cover" }}
                />
              )}
              <Box sx={{ p: 2 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Typography variant="subtitle1" sx={{ minWidth: 0 }} noWrap>
                    {s.name}
                  </Typography>
                  <Chip label={s.status} size="small" color={statusColor(s.status)} />
                </Stack>
                <Typography variant="body2" color="text.secondary">
                  {s.address ?? "No address"}
                </Typography>
                <Stack direction="row" spacing={1} mt={1} flexWrap="wrap" useFlexGap>
                  {s.proposedPricePerKwh != null && (
                    <Chip size="small" variant="outlined" label={`₱${s.proposedPricePerKwh}/kWh`} />
                  )}
                  {s.connectorType && <Chip size="small" variant="outlined" label={s.connectorType} />}
                  {s.powerKw != null && <Chip size="small" variant="outlined" label={`${s.powerKw} kW`} />}
                  {!s.ocrLooksLikeStation && (
                    <Chip size="small" color="warning" label="OCR: unsure" />
                  )}
                </Stack>

                {s.comments.length > 0 && (
                  <Box sx={{ mt: 1.5 }}>
                    <Divider sx={{ mb: 1 }} />
                    {s.comments.map((c, i) => (
                      <Typography key={i} variant="caption" display="block" color="text.secondary">
                        <strong>{c.authorName}</strong>: {c.text}
                      </Typography>
                    ))}
                  </Box>
                )}

                {s.status === "PENDING" && (
                  <>
                    <Stack direction="row" spacing={1} mt={1.5}>
                      <Button
                        fullWidth
                        variant="contained"
                        color="success"
                        startIcon={<CheckCircleRoundedIcon />}
                        disabled={busyId === s.id}
                        onClick={() => approve(s.id)}
                      >
                        Approve
                      </Button>
                      <Button
                        fullWidth
                        variant="outlined"
                        color="error"
                        startIcon={<CancelRoundedIcon />}
                        disabled={busyId === s.id}
                        onClick={() => setRejectFor(rejectFor === s.id ? null : s.id)}
                      >
                        Reject
                      </Button>
                    </Stack>
                    {rejectFor === s.id && (
                      <Stack direction="row" spacing={1} mt={1}>
                        <TextField
                          size="small"
                          fullWidth
                          label="Reason"
                          value={rejectText}
                          onChange={(e) => setRejectText(e.target.value)}
                        />
                        <Button
                          variant="contained"
                          color="error"
                          disabled={!rejectText.trim() || busyId === s.id}
                          onClick={() => reject(s.id)}
                        >
                          Confirm
                        </Button>
                      </Stack>
                    )}
                  </>
                )}
                {s.status === "APPROVED" && (
                  <Alert severity="success" sx={{ mt: 1.5 }}>
                    Approved — live station created.
                  </Alert>
                )}
              </Box>
            </Card>
          ))}
        </Box>
      )}
    </Box>
  );
}
