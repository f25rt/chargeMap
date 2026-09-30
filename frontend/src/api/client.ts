import axios from "axios";
import type {
  AdminStats,
  ManagedUser,
  NewSubmission,
  PageResponse,
  PointRules,
  PointsLedgerEntry,
  PricingTrendItem,
  Prize,
  ReportFeedItem,
  ReportResponse,
  StationDetail,
  StationFilters,
  StationSummary,
  Submission,
  SubmissionStatus,
  TokenResponse,
  UserProfile,
  ConnectVehicleRequest,
  ConnectedVehicle,
  VehicleLocation,
  VehicleSyncResult,
  Branch,
  AdminSummary,
  BranchPriceTrend,
  StationUpdateFrequency,
  Activity,
  UserActivity,
  ActivityCompletionStat,
  StationReview,
  LikeStatus,
  LikeTrendPoint,
  ChargingSession,
  LogSessionRequest,
  TelemetryRollup,
  StationTelemetry,
  GridTelemetry,
} from "./types";

const TOKEN_KEY = "chargemap.token";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// API base URL. Empty (default) = same-origin: Vite proxies /api in dev, and in
// production a reverse proxy / Render rewrite forwards /api to the backend. Set
// VITE_API_BASE_URL to the backend's full URL if you deploy the two on separate origins.
const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? "" });

http.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Guard against a misconfigured API rewrite: if an /api/* call gets HTML back (e.g. the
// SPA index.html because the /api rewrite didn't match), treat it as a routing error
// instead of silently accepting a non-JSON body.
http.interceptors.response.use((response) => {
  const url = response.config?.url ?? "";
  const contentType = String(response.headers?.["content-type"] ?? "");
  if (url.startsWith("/api/") && contentType.includes("text/html")) {
    return Promise.reject(
      Object.assign(new Error("API request was not routed to the backend"), {
        response: { status: 502, data: { code: "API_MISROUTED" } },
      }),
    );
  }
  return response;
});

/** Builds a query object from filters, dropping empty values. */
function filterParams(filters?: StationFilters): Record<string, unknown> {
  const params: Record<string, unknown> = {};
  if (!filters) return params;
  if (filters.chargerType) params.chargerType = filters.chargerType;
  if (filters.connector) params.connector = filters.connector;
  if (filters.minKw !== "" && filters.minKw != null) params.minKw = filters.minKw;
  if (filters.maxKw !== "" && filters.maxKw != null) params.maxKw = filters.maxKw;
  if (filters.priceMax !== "" && filters.priceMax != null) params.priceMax = filters.priceMax;
  if (filters.availableOnly) params.availableOnly = true;
  return params;
}

export const api = {
  listStations: (page = 0, size = 50, filters?: StationFilters) =>
    http
      .get<PageResponse<StationSummary>>("/api/stations", {
        params: { page, size, ...filterParams(filters) },
      })
      .then((r) => r.data),

  nearby: (lat: number, lng: number, radius: number, filters?: StationFilters) =>
    http
      .get<StationSummary[]>("/api/stations/nearby", {
        params: { lat, lng, radius, ...filterParams(filters) },
      })
      .then((r) => r.data),

  cheapest: (lat: number, lng: number, radius: number, filters?: StationFilters) =>
    http
      .get<StationSummary[]>("/api/stations/cheapest", {
        params: { lat, lng, radius, ...filterParams(filters) },
      })
      .then((r) => r.data),

  available: (lat: number, lng: number, radius: number, filters?: StationFilters) =>
    http
      .get<StationSummary[]>("/api/stations/available", {
        params: { lat, lng, radius, ...filterParams(filters) },
      })
      .then((r) => r.data),

  search: (q: string) =>
    http.get<StationSummary[]>("/api/stations/search", { params: { q } }).then((r) => r.data),

  station: (id: string) =>
    http.get<StationDetail>(`/api/stations/${id}`).then((r) => r.data),

  register: (email: string, name: string, password: string) =>
    http.post<UserProfile>("/api/users", { email, name, password }).then((r) => r.data),

  login: (email: string, password: string) =>
    http.post<TokenResponse>("/api/auth/login", { email, password }).then((r) => r.data),

  me: () => http.get<UserProfile>("/api/users/me").then((r) => r.data),

  report: (stationId: string, status: string, chargerId?: string) =>
    http
      .post<ReportResponse>(`/api/stations/${stationId}/reports`, { status, chargerId })
      .then((r) => r.data),

  favorites: () =>
    http.get<StationSummary[]>("/api/users/me/favorites").then((r) => r.data),

  addFavorite: (stationId: string) =>
    http.post(`/api/users/me/favorites/${stationId}`).then((r) => r.data),

  removeFavorite: (stationId: string) =>
    http.delete(`/api/users/me/favorites/${stationId}`).then((r) => r.data),

  // ----- Operator -----
  operatorStations: () =>
    http.get<StationDetail[]>("/api/operator/stations").then((r) => r.data),

  operatorSetChargerStatus: (stationId: string, chargerId: string, status: string) =>
    http
      .put<StationDetail>(`/api/operator/stations/${stationId}/chargers/${chargerId}/status`, {
        status,
      })
      .then((r) => r.data),

  operatorUpdatePricing: (
    stationId: string,
    pricePerKwh: number,
    pricingModel = "PER_KWH",
    tariff?: { offPeakPricePerKwh?: number; peakStartHour?: number; peakEndHour?: number },
  ) =>
    http
      .put<StationDetail>(`/api/operator/stations/${stationId}/pricing`, {
        pricePerKwh,
        pricingModel,
        offPeakPricePerKwh: tariff?.offPeakPricePerKwh,
        peakStartHour: tariff?.peakStartHour,
        peakEndHour: tariff?.peakEndHour,
      })
      .then((r) => r.data),

  // ----- Admin -----
  adminStats: () => http.get<AdminStats>("/api/admin/stats").then((r) => r.data),

  adminReports: (limit = 50) =>
    http.get<ReportFeedItem[]>("/api/admin/reports", { params: { limit } }).then((r) => r.data),

  adminStations: () => http.get<StationDetail[]>("/api/admin/stations").then((r) => r.data),

  adminDisableStation: (id: string) =>
    http.post<StationDetail>(`/api/admin/stations/${id}/disable`).then((r) => r.data),

  adminEnableStation: (id: string) =>
    http.post<StationDetail>(`/api/admin/stations/${id}/enable`).then((r) => r.data),

  adminVerifyStation: (id: string) =>
    http.post<StationDetail>(`/api/admin/stations/${id}/verify`).then((r) => r.data),

  /** Admin/operator direct edit of a station's descriptive details (applied immediately). */
  adminUpdateStationDetails: (
    id: string,
    d: { name?: string; operator?: string; address?: string; area?: string },
  ) => http.put<StationDetail>(`/api/moderation/stations/${id}/details`, d).then((r) => r.data),

  adminUpdateStationLocation: (id: string, lat: number, lng: number) =>
    http.put<StationDetail>(`/api/moderation/stations/${id}/location`, { lat, lng }).then((r) => r.data),

  adminPricingTrends: (days = 90) =>
    http.get<PricingTrendItem[]>("/api/admin/pricing-trends", { params: { days } }).then((r) => r.data),

  // ----- Moderation (operator + admin) -----
  moderationUsers: () =>
    http.get<ManagedUser[]>("/api/moderation/users").then((r) => r.data),

  moderationSuspend: (id: string, reason: string) =>
    http.post<ManagedUser>(`/api/moderation/users/${id}/suspend`, { reason }).then((r) => r.data),

  moderationUnsuspend: (id: string) =>
    http.post<ManagedUser>(`/api/moderation/users/${id}/unsuspend`).then((r) => r.data),

  // ----- Submissions (user) -----
  createSubmission: (s: NewSubmission) => {
    const fd = new FormData();
    fd.append("image", s.image);
    fd.append("name", s.name);
    fd.append("lat", String(s.lat));
    fd.append("lng", String(s.lng));
    if (s.operator) fd.append("operator", s.operator);
    if (s.address) fd.append("address", s.address);
    if (s.area) fd.append("area", s.area);
    if (s.ocrText) fd.append("ocrText", s.ocrText);
    fd.append("ocrLooksLikeStation", String(!!s.ocrLooksLikeStation));
    if (s.pricePerKwh != null && s.pricePerKwh !== undefined)
      fd.append("pricePerKwh", String(s.pricePerKwh));
    if (s.connectorType) fd.append("connectorType", s.connectorType);
    if (s.chargerType) fd.append("chargerType", s.chargerType);
    if (s.powerKw != null && s.powerKw !== undefined) fd.append("powerKw", String(s.powerKw));
    return http.post<Submission>("/api/submissions", fd).then((r) => r.data);
  },

  mySubmissions: () => http.get<Submission[]>("/api/submissions/me").then((r) => r.data),

  /** Propose edits to an existing station; queued for admin approval before going live. */
  proposeStationEdit: (
    stationId: string,
    changes: {
      name?: string;
      operator?: string;
      address?: string;
      area?: string;
      pricePerKwh?: number;
      connectorType?: string;
      chargerType?: string;
      powerKw?: number;
    },
  ) =>
    http.post<Submission>(`/api/submissions/stations/${stationId}/edits`, changes).then((r) => r.data),

  // ----- Submission moderation (operator + admin) -----
  moderationSubmissions: (status?: SubmissionStatus) =>
    http
      .get<Submission[]>("/api/moderation/submissions", { params: status ? { status } : {} })
      .then((r) => r.data),

  moderationComment: (id: string, text: string) =>
    http.post<Submission>(`/api/moderation/submissions/${id}/comment`, { text }).then((r) => r.data),

  moderationApprove: (id: string) =>
    http.post<Submission>(`/api/moderation/submissions/${id}/approve`).then((r) => r.data),

  moderationReject: (id: string, text: string) =>
    http.post<Submission>(`/api/moderation/submissions/${id}/reject`, { text }).then((r) => r.data),

  imageUrl: (imageId: string) => `/api/images/${imageId}`,

  /** Uploads an image (prize artwork, etc.) and returns its stored id. Operator/admin only. */
  uploadImage: (file: File) => {
    const fd = new FormData();
    fd.append("image", file);
    return http.post<{ imageId: string }>("/api/moderation/images", fd).then((r) => r.data.imageId);
  },

  // ----- Points + prizes (user) -----
  prizes: () => http.get<Prize[]>("/api/prizes").then((r) => r.data),

  myPoints: () => http.get<PointsLedgerEntry[]>("/api/users/me/points").then((r) => r.data),

  // ----- Rewards management (operator + admin) -----
  moderationPrizes: () => http.get<Prize[]>("/api/moderation/prizes").then((r) => r.data),

  createPrize: (p: { name: string; description?: string; pointCost: number; imageId?: string }) =>
    http.post<Prize>("/api/moderation/prizes", p).then((r) => r.data),

  updatePrize: (id: string, p: { name: string; description?: string; pointCost: number; imageId?: string }) =>
    http.put<Prize>(`/api/moderation/prizes/${id}`, p).then((r) => r.data),

  deactivatePrize: (id: string) =>
    http.post<Prize>(`/api/moderation/prizes/${id}/deactivate`).then((r) => r.data),

  activatePrize: (id: string) =>
    http.post<Prize>(`/api/moderation/prizes/${id}/activate`).then((r) => r.data),

  pointRules: () => http.get<PointRules>("/api/moderation/point-rules").then((r) => r.data),

  updatePointRules: (rules: PointRules) =>
    http.put<PointRules>("/api/moderation/point-rules", rules).then((r) => r.data),

  adjustPoints: (userId: string, delta: number, reason: string) =>
    http.post(`/api/moderation/users/${userId}/points`, { delta, reason }).then((r) => r.data),

  // ----- Vehicle Sync & Live Telemetry (Phase 1) -----
  connectVehicle: (req: ConnectVehicleRequest) =>
    http.post<ConnectedVehicle>("/api/vehicle/connect", req).then((r) => r.data),

  listVehicles: () => http.get<ConnectedVehicle[]>("/api/vehicle").then((r) => r.data),

  syncVehicles: () => http.post<VehicleSyncResult>("/api/vehicle/sync").then((r) => r.data),

  vehicleStatus: (vehicleId?: string) =>
    http
      .get<ConnectedVehicle>("/api/vehicle/status", {
        params: vehicleId ? { vehicleId } : {},
      })
      .then((r) => r.data),

  vehicleLocation: (vehicleId?: string) =>
    http
      .get<VehicleLocation>("/api/vehicle/location", {
        params: vehicleId ? { vehicleId } : {},
      })
      .then((r) => r.data),

  disconnectVehicle: (id: string) =>
    http.delete(`/api/vehicle/${id}`).then((r) => r.data),

  // ----- Super admin: branches + admin oversight -----
  branches: () => http.get<Branch[]>("/api/superadmin/branches").then((r) => r.data),

  createBranch: (b: { name: string; description?: string; area?: string }) =>
    http.post<Branch>("/api/superadmin/branches", b).then((r) => r.data),

  updateBranch: (id: string, b: { name?: string; description?: string; area?: string }) =>
    http.put<Branch>(`/api/superadmin/branches/${id}`, b).then((r) => r.data),

  setBranchImage: (id: string, kind: "profile" | "banner", file: File) => {
    const fd = new FormData();
    fd.append("kind", kind);
    fd.append("image", file);
    return http.put<Branch>(`/api/superadmin/branches/${id}/image`, fd).then((r) => r.data);
  },

  assignAdminToBranch: (branchId: string, userId: string) =>
    http.post(`/api/superadmin/branches/${branchId}/admins/${userId}`).then((r) => r.data),

  assignStationToBranch: (branchId: string, stationId: string) =>
    http.post(`/api/superadmin/branches/${branchId}/stations/${stationId}`).then((r) => r.data),

  admins: () => http.get<AdminSummary[]>("/api/superadmin/admins").then((r) => r.data),

  createAdmin: (a: { email: string; name: string; password: string; branchId?: string }) =>
    http.post<AdminSummary>("/api/superadmin/admins", a).then((r) => r.data),

  reassignAdminBranch: (adminId: string, branchId: string | null) =>
    http.put<AdminSummary>(`/api/superadmin/admins/${adminId}/branch`, { branchId }).then((r) => r.data),

  disableAdmin: (adminId: string) =>
    http.post<AdminSummary>(`/api/superadmin/admins/${adminId}/disable`).then((r) => r.data),

  enableAdmin: (adminId: string) =>
    http.post<AdminSummary>(`/api/superadmin/admins/${adminId}/enable`).then((r) => r.data),

  // ----- Metrics -----
  branchPriceTrend: (days = 7) =>
    http
      .get<BranchPriceTrend[]>("/api/moderation/metrics/branch-price-trend", { params: { days } })
      .then((r) => r.data),

  stationUpdateFrequency: (stationId: string, days = 30) =>
    http
      .get<StationUpdateFrequency>(`/api/moderation/metrics/station/${stationId}/update-frequency`, {
        params: { days },
      })
      .then((r) => r.data),

  // ----- Activities -----
  moderationActivities: () => http.get<Activity[]>("/api/moderation/activities").then((r) => r.data),

  createActivity: (a: { title: string; description?: string; goalCount: number; rewardPrizeId?: string }) =>
    http.post<Activity>("/api/moderation/activities", a).then((r) => r.data),

  activityMetrics: () =>
    http.get<ActivityCompletionStat[]>("/api/moderation/activities/metrics").then((r) => r.data),

  myActivities: () => http.get<UserActivity[]>("/api/activities").then((r) => r.data),

  chooseActivity: (id: string) =>
    http.post<UserActivity>(`/api/activities/${id}/choose`).then((r) => r.data),

  // ----- Self profile (admin + user) -----
  updateName: (name: string) =>
    http.put<UserProfile>("/api/users/me", { name }).then((r) => r.data),

  changePassword: (currentPassword: string, newPassword: string) =>
    http.post("/api/users/me/password", { currentPassword, newPassword }).then((r) => r.data),

  // ----- Station reviews + likes -----
  stationReviews: (stationId: string) =>
    http.get<StationReview[]>(`/api/stations/${stationId}/reviews`).then((r) => r.data),

  createReview: (stationId: string, text: string, image: File | null) => {
    const fd = new FormData();
    if (text) fd.append("text", text);
    if (image) fd.append("image", image);
    return http.post<StationReview>(`/api/stations/${stationId}/reviews`, fd).then((r) => r.data);
  },

  editReview: (reviewId: string, text: string, image: File | null) => {
    const fd = new FormData();
    fd.append("text", text);
    if (image) fd.append("image", image);
    return http.put<StationReview>(`/api/reviews/${reviewId}`, fd).then((r) => r.data);
  },

  deleteReview: (reviewId: string) => http.delete(`/api/reviews/${reviewId}`).then((r) => r.data),

  likeStatus: (stationId: string) =>
    http.get<LikeStatus>(`/api/stations/${stationId}/like`).then((r) => r.data),

  toggleLike: (stationId: string) =>
    http.post<LikeStatus>(`/api/stations/${stationId}/like`).then((r) => r.data),

  branchLikeTrend: (days = 7) =>
    http
      .get<LikeTrendPoint[]>("/api/moderation/metrics/branch-like-trend", { params: { days } })
      .then((r) => r.data),

  // ----- Charging sessions + driver telemetry rollup -----
  logSession: (req: LogSessionRequest) =>
    http.post<ChargingSession>("/api/sessions", req).then((r) => r.data),

  mySessions: () => http.get<ChargingSession[]>("/api/sessions").then((r) => r.data),

  myTelemetry: () => http.get<TelemetryRollup>("/api/sessions/telemetry").then((r) => r.data),

  // ----- Simulated station/grid telemetry (DEMO) -----
  stationTelemetry: (stationId: string) =>
    http.get<StationTelemetry>(`/api/stations/${stationId}/telemetry`).then((r) => r.data),

  gridTelemetry: () => http.get<GridTelemetry>("/api/admin/telemetry/grid").then((r) => r.data),
};
