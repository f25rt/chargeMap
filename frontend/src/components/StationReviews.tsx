import { useEffect, useRef, useState } from "react";
import {
  Avatar,
  Box,
  Button,
  IconButton,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import PhotoCameraRoundedIcon from "@mui/icons-material/PhotoCameraRounded";
import DeleteOutlineRoundedIcon from "@mui/icons-material/DeleteOutlineRounded";
import EditRoundedIcon from "@mui/icons-material/EditRounded";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import { api } from "../api/client";
import type { StationReview } from "../api/types";
import { useAuth } from "../auth/AuthContext";

function timeAgo(iso: string | null): string {
  if (!iso) return "";
  const mins = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const h = Math.round(mins / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.round(h / 24)}d ago`;
}

interface Props {
  stationId: string;
  onRequireLogin: () => void;
}

/** Reviews list + composer with optional photo; edit/delete on the caller's own reviews. */
export default function StationReviews({ stationId, onRequireLogin }: Props) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState<StationReview[] | null>(null);
  const [text, setText] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editText, setEditText] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  const load = async () => {
    try {
      setReviews(await api.stationReviews(stationId));
    } catch {
      setReviews([]);
    }
  };

  useEffect(() => {
    setReviews(null);
    setText("");
    pickImage(null);
    setEditingId(null);
    if (stationId) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stationId]);

  const pickImage = (f: File | null) => {
    setImage(f);
    setPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return f ? URL.createObjectURL(f) : null;
    });
  };

  const submit = async () => {
    if (!user) return onRequireLogin();
    if (!text.trim() && !image) return;
    setBusy(true);
    try {
      await api.createReview(stationId, text.trim(), image);
      setText("");
      pickImage(null);
      await load();
    } finally {
      setBusy(false);
    }
  };

  const saveEdit = async (id: string) => {
    setBusy(true);
    try {
      await api.editReview(id, editText.trim(), null);
      setEditingId(null);
      await load();
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: string) => {
    setBusy(true);
    try {
      await api.deleteReview(id);
      await load();
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box>
      <Typography sx={{ fontSize: 13, fontWeight: 700, mb: 1 }}>REVIEWS</Typography>

      {/* Composer */}
      <Box sx={{ mb: 2 }}>
        <TextField
          size="small"
          fullWidth
          multiline
          minRows={1}
          placeholder={user ? "Share your experience…" : "Sign in to leave a review"}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onFocus={() => {
            if (!user) onRequireLogin();
          }}
        />
        <input
          ref={fileRef}
          type="file"
          accept="image/*"
          hidden
          onChange={(e) => pickImage(e.target.files?.[0] ?? null)}
        />
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
          <Button
            size="small"
            variant="outlined"
            startIcon={<PhotoCameraRoundedIcon />}
            onClick={() => (user ? fileRef.current?.click() : onRequireLogin())}
            sx={{ textTransform: "none" }}
          >
            Photo
          </Button>
          {preview && (
            <Box sx={{ position: "relative" }}>
              <Box
                component="img"
                src={preview}
                sx={{ width: 40, height: 40, borderRadius: 1, objectFit: "cover" }}
              />
              <IconButton
                size="small"
                onClick={() => pickImage(null)}
                sx={{ position: "absolute", top: -8, right: -8, bgcolor: "background.paper", p: 0.25 }}
              >
                <CloseRoundedIcon sx={{ fontSize: 14 }} />
              </IconButton>
            </Box>
          )}
          <Box sx={{ flex: 1 }} />
          <Button
            size="small"
            variant="contained"
            onClick={submit}
            disabled={busy || (!text.trim() && !image)}
          >
            Post
          </Button>
        </Stack>
      </Box>

      {/* List */}
      {reviews === null ? (
        <Typography variant="caption" color="text.secondary">
          Loading…
        </Typography>
      ) : reviews.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
          No reviews yet. Be the first to share.
        </Typography>
      ) : (
        <Stack spacing={1.5}>
          {reviews.map((r) => (
            <Stack key={r.id} direction="row" spacing={1.25}>
              <Avatar sx={{ width: 32, height: 32, bgcolor: "primary.main", fontSize: 13 }}>
                {r.authorName?.charAt(0).toUpperCase() ?? "?"}
              </Avatar>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Stack direction="row" alignItems="center" spacing={0.75}>
                  <Typography sx={{ fontSize: 13, fontWeight: 600 }}>{r.authorName}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {timeAgo(r.createdAt)}
                    {r.updatedAt && r.updatedAt !== r.createdAt ? " · edited" : ""}
                  </Typography>
                </Stack>
                {editingId === r.id ? (
                  <Box sx={{ mt: 0.5 }}>
                    <TextField
                      size="small"
                      fullWidth
                      multiline
                      value={editText}
                      onChange={(e) => setEditText(e.target.value)}
                    />
                    <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}>
                      <Button size="small" variant="contained" onClick={() => saveEdit(r.id)} disabled={busy}>
                        Save
                      </Button>
                      <Button size="small" onClick={() => setEditingId(null)}>
                        Cancel
                      </Button>
                    </Stack>
                  </Box>
                ) : (
                  <>
                    {r.text && (
                      <Typography sx={{ fontSize: 13.5, mt: 0.25 }} color="text.primary">
                        {r.text}
                      </Typography>
                    )}
                    {r.imageId && (
                      <Box
                        component="img"
                        src={api.imageUrl(r.imageId)}
                        sx={{ mt: 0.75, width: "100%", maxWidth: 220, borderRadius: 2, objectFit: "cover" }}
                      />
                    )}
                    {r.mine && (
                      <Stack direction="row" spacing={0.5} sx={{ mt: 0.25 }}>
                        <IconButton
                          size="small"
                          onClick={() => {
                            setEditingId(r.id);
                            setEditText(r.text ?? "");
                          }}
                        >
                          <EditRoundedIcon sx={{ fontSize: 16 }} />
                        </IconButton>
                        <IconButton size="small" color="error" onClick={() => remove(r.id)}>
                          <DeleteOutlineRoundedIcon sx={{ fontSize: 16 }} />
                        </IconButton>
                      </Stack>
                    )}
                  </>
                )}
              </Box>
            </Stack>
          ))}
        </Stack>
      )}
    </Box>
  );
}
