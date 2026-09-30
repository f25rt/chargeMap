import { useEffect, useState } from "react";
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import PhotoCameraRoundedIcon from "@mui/icons-material/PhotoCameraRounded";
import EmojiEventsRoundedIcon from "@mui/icons-material/EmojiEventsRounded";
import { useRef } from "react";
import { Avatar } from "@mui/material";
import { LinearProgress, MenuItem } from "@mui/material";
import { api } from "../../api/client";
import type { ActivityCompletionStat, PointRules, Prize } from "../../api/types";

export default function RewardsPanel() {
  const [prizes, setPrizes] = useState<Prize[]>([]);
  const [rules, setRules] = useState<PointRules | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // New prize draft
  const [name, setName] = useState("");
  const [desc, setDesc] = useState("");
  const [cost, setCost] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  // Activities + completion metrics
  const [activityStats, setActivityStats] = useState<ActivityCompletionStat[]>([]);
  const [actTitle, setActTitle] = useState("");
  const [actGoal, setActGoal] = useState("3");
  const [actPrizeId, setActPrizeId] = useState("");
  const [actSaving, setActSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [p, r, stats] = await Promise.all([
        api.moderationPrizes(),
        api.pointRules(),
        api.activityMetrics(),
      ]);
      setPrizes(p);
      setRules(r);
      setActivityStats(stats);
    } finally {
      setLoading(false);
    }
  };

  const createActivity = async () => {
    if (!actTitle.trim() || actGoal === "") return;
    setActSaving(true);
    try {
      await api.createActivity({
        title: actTitle.trim(),
        goalCount: Number(actGoal),
        rewardPrizeId: actPrizeId || undefined,
      });
      setActTitle("");
      setActGoal("3");
      setActPrizeId("");
      await load();
    } finally {
      setActSaving(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const pickImage = (file: File | null) => {
    setImage(file);
    setImagePreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return file ? URL.createObjectURL(file) : null;
    });
  };

  const addPrize = async () => {
    if (!name.trim() || cost === "") return;
    setSaving(true);
    try {
      let imageId: string | undefined;
      if (image) imageId = await api.uploadImage(image);
      await api.createPrize({
        name: name.trim(),
        description: desc.trim(),
        pointCost: Number(cost),
        imageId,
      });
      setName("");
      setDesc("");
      setCost("");
      pickImage(null);
      await load();
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (p: Prize) => {
    const updated = p.active ? await api.deactivatePrize(p.id) : await api.activatePrize(p.id);
    setPrizes((prev) => prev.map((x) => (x.id === updated.id ? updated : x)));
  };

  const saveRules = async () => {
    if (!rules) return;
    setSaving(true);
    try {
      setRules(await api.updatePointRules(rules));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Stack alignItems="center" py={6}>
        <CircularProgress />
      </Stack>
    );
  }

  const ruleField = (label: string, key: keyof PointRules) => (
    <TextField
      size="small"
      type="number"
      label={label}
      value={rules?.[key] ?? 0}
      onChange={(e) => rules && setRules({ ...rules, [key]: Number(e.target.value) })}
      fullWidth
    />
  );

  const maxCompletions = activityStats.reduce((m, s) => Math.max(m, s.completions), 0) || 1;

  return (
    <Stack spacing={3}>
      {/* Task activities: create + completion study */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", lg: "1.4fr 1fr" },
          gap: 3,
          alignItems: "start",
        }}
      >
        <Card sx={{ p: 2 }}>
          <Stack direction="row" spacing={1} alignItems="center" mb={1}>
            <EmojiEventsRoundedIcon color="primary" />
            <Typography variant="subtitle1">Task activities</Typography>
          </Stack>
          <Divider sx={{ mb: 1.5 }} />
          <Typography variant="caption" color="text.secondary">
            Create an activity users can take on. When they complete the goal (that many
            approved station updates), the linked reward is granted.
          </Typography>
          <Stack spacing={1.5} mt={1.5}>
            <TextField
              size="small"
              label="Activity title"
              value={actTitle}
              onChange={(e) => setActTitle(e.target.value)}
              fullWidth
            />
            <Stack direction="row" spacing={1.5}>
              <TextField
                size="small"
                type="number"
                label="Goal (approved updates)"
                value={actGoal}
                onChange={(e) => setActGoal(e.target.value)}
                sx={{ width: 200 }}
              />
              <TextField
                select
                size="small"
                label="Reward"
                value={actPrizeId}
                onChange={(e) => setActPrizeId(e.target.value)}
                sx={{ flex: 1 }}
              >
                <MenuItem value="">No reward</MenuItem>
                {prizes.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.name} ({p.pointCost} pts)
                  </MenuItem>
                ))}
              </TextField>
            </Stack>
            <Button
              variant="contained"
              startIcon={<AddRoundedIcon />}
              onClick={createActivity}
              disabled={actSaving || !actTitle.trim() || actGoal === ""}
            >
              Create activity
            </Button>
          </Stack>
        </Card>

        <Card sx={{ p: 2 }}>
          <Typography variant="subtitle1" gutterBottom>
            Most-completed rewards
          </Typography>
          <Divider sx={{ mb: 1.5 }} />
          {activityStats.length === 0 ? (
            <Typography color="text.secondary" py={2} textAlign="center">
              No activities yet.
            </Typography>
          ) : (
            <Stack spacing={1.25}>
              {activityStats.map((s) => (
                <Box key={s.activityId}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2" fontWeight={600} noWrap>
                      {s.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {s.completions}
                    </Typography>
                  </Stack>
                  <LinearProgress
                    variant="determinate"
                    value={(s.completions / maxCompletions) * 100}
                    sx={{ mt: 0.5, height: 6, borderRadius: 3 }}
                  />
                </Box>
              ))}
            </Stack>
          )}
        </Card>
      </Box>

    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: { xs: "1fr", lg: "1.4fr 1fr" },
        gap: 3,
        alignItems: "start",
      }}
    >
      {/* Prizes */}
      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Prizes
        </Typography>
        <Divider sx={{ mb: 1.5 }} />

        <Stack spacing={1.5} mb={2}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems="flex-start">
            {/* Prize image picker */}
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              hidden
              onChange={(e) => pickImage(e.target.files?.[0] ?? null)}
            />
            <Box
              onClick={() => fileRef.current?.click()}
              sx={{
                width: { xs: "100%", sm: 72 },
                height: 72,
                flexShrink: 0,
                borderRadius: 2,
                border: "1.5px dashed",
                borderColor: "divider",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                overflow: "hidden",
                bgcolor: "action.hover",
              }}
            >
              {imagePreview ? (
                <Box
                  component="img"
                  src={imagePreview}
                  alt="Prize preview"
                  sx={{ width: "100%", height: "100%", objectFit: "cover" }}
                />
              ) : (
                <>
                  <PhotoCameraRoundedIcon sx={{ fontSize: 22, color: "text.secondary" }} />
                  <Typography variant="caption" color="text.secondary">
                    Image
                  </Typography>
                </>
              )}
            </Box>
            <Stack spacing={1.5} sx={{ flex: 1, width: "100%" }}>
              <TextField
                size="small"
                label="Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                fullWidth
              />
              <TextField
                size="small"
                label="Description"
                value={desc}
                onChange={(e) => setDesc(e.target.value)}
                fullWidth
              />
            </Stack>
          </Stack>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <TextField
              size="small"
              type="number"
              label="Cost"
              placeholder="pts"
              value={cost}
              onChange={(e) => setCost(e.target.value)}
              InputProps={{
                endAdornment: <InputAdornment position="end">pts</InputAdornment>,
              }}
              sx={{ flex: 1 }}
            />
            <Button
              variant="contained"
              startIcon={<AddRoundedIcon />}
              disabled={saving || !name.trim() || cost === ""}
              onClick={addPrize}
              sx={{ flexShrink: 0, minWidth: 110 }}
            >
              Add prize
            </Button>
          </Stack>
          {image && (
            <Typography variant="caption" color="text.secondary">
              {image.name} · <Button size="small" onClick={() => pickImage(null)}>remove</Button>
            </Typography>
          )}
        </Stack>

        <Stack divider={<Divider />}>
          {prizes.length === 0 ? (
            <Typography color="text.secondary" py={2} textAlign="center">
              No prizes yet.
            </Typography>
          ) : (
            prizes.map((p) => (
              <Stack
                key={p.id}
                direction="row"
                alignItems="center"
                justifyContent="space-between"
                sx={{ py: 1 }}
                spacing={1.5}
              >
                <Stack direction="row" spacing={1.5} alignItems="center" sx={{ minWidth: 0 }}>
                  <Avatar
                    variant="rounded"
                    src={p.imageId ? api.imageUrl(p.imageId) : undefined}
                    sx={{ width: 40, height: 40, bgcolor: "rgba(0,255,157,.12)", color: "primary.main" }}
                  >
                    <EmojiEventsRoundedIcon fontSize="small" />
                  </Avatar>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="body2" fontWeight={600} noWrap>
                      {p.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {p.description || "—"} · {p.pointCost} pts
                    </Typography>
                  </Box>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Chip
                    size="small"
                    label={p.active ? "Active" : "Inactive"}
                    color={p.active ? "success" : "default"}
                  />
                  <Button size="small" onClick={() => toggle(p)}>
                    {p.active ? "Deactivate" : "Activate"}
                  </Button>
                </Stack>
              </Stack>
            ))
          )}
        </Stack>
      </Card>

      {/* Point rules */}
      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          Point rules
        </Typography>
        <Divider sx={{ mb: 1.5 }} />
        <Stack spacing={1.5}>
          <Typography variant="caption" color="text.secondary">
            Points awarded per action
          </Typography>
          {ruleField("Station add", "pointsPerStationAdd")}
          {ruleField("Station update", "pointsPerStationUpdate")}
          {ruleField("Report", "pointsPerReport")}
          <Typography variant="caption" color="text.secondary" mt={1}>
            Daily caps
          </Typography>
          {ruleField("Cap: station add", "dailyCapStationAdd")}
          {ruleField("Cap: station update", "dailyCapStationUpdate")}
          {ruleField("Cap: report", "dailyCapReport")}
          <Button variant="contained" disabled={saving} onClick={saveRules}>
            Save rules
          </Button>
        </Stack>
      </Card>
    </Box>
    </Stack>
  );
}
