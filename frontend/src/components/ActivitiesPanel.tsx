import { useEffect, useState } from "react";
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  LinearProgress,
  Stack,
  Typography,
} from "@mui/material";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import AssignmentTurnedInRoundedIcon from "@mui/icons-material/AssignmentTurnedInRounded";
import { api } from "../api/client";
import type { UserActivity } from "../api/types";

/** User-facing task activities: browse, choose (start), and track progress. */
export default function ActivitiesPanel() {
  const [activities, setActivities] = useState<UserActivity[] | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = async () => {
    try {
      setActivities(await api.myActivities());
    } catch {
      setActivities([]);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const choose = async (id: string) => {
    setBusyId(id);
    try {
      await api.chooseActivity(id);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  if (activities === null) {
    return (
      <Stack alignItems="center" py={3}>
        <CircularProgress size={24} />
      </Stack>
    );
  }

  if (activities.length === 0) {
    return (
      <Box sx={{ textAlign: "center", py: 4, color: "text.secondary" }}>
        <AssignmentTurnedInRoundedIcon sx={{ fontSize: 40, opacity: 0.4 }} />
        <Typography sx={{ mt: 1 }}>No activities available yet.</Typography>
      </Box>
    );
  }

  return (
    <Stack spacing={1.5}>
      <Typography variant="body2" color="text.secondary">
        Take on an activity, then update station prices and photos. Once your updates are
        approved and you hit the goal, you earn the reward.
      </Typography>
      {activities.map((a) => {
        const pct = a.goalCount > 0 ? Math.min(100, (a.progress / a.goalCount) * 100) : 0;
        return (
          <Card key={a.activityId} sx={{ p: 1.75 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="subtitle2">{a.title}</Typography>
                {a.description && (
                  <Typography variant="caption" color="text.secondary">
                    {a.description}
                  </Typography>
                )}
              </Box>
              {a.status === "COMPLETED" ? (
                <Chip
                  size="small"
                  color="success"
                  icon={<CheckCircleRoundedIcon />}
                  label="Completed"
                />
              ) : a.status === "IN_PROGRESS" ? (
                <Chip size="small" color="primary" label="In progress" />
              ) : (
                <Button size="small" variant="contained" disabled={busyId === a.activityId} onClick={() => choose(a.activityId)}>
                  Start
                </Button>
              )}
            </Stack>

            {a.status !== "NOT_STARTED" && (
              <Box sx={{ mt: 1 }}>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="caption" color="text.secondary">
                    Progress
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {a.progress}/{a.goalCount}
                  </Typography>
                </Stack>
                <LinearProgress
                  variant="determinate"
                  value={pct}
                  color={a.status === "COMPLETED" ? "success" : "primary"}
                  sx={{ mt: 0.5, height: 6, borderRadius: 3 }}
                />
                {a.status === "COMPLETED" && a.grantedPrizeId && (
                  <Typography variant="caption" color="success.main">
                    Reward granted 🎉
                  </Typography>
                )}
              </Box>
            )}
          </Card>
        );
      })}
    </Stack>
  );
}
