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
} from "./types";

const TOKEN_KEY = "chargemap.token";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// Same-origin base; Vite proxies /api to the backend in dev.
const http = axios.create({ baseURL: "" });

http.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
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

  operatorUpdatePricing: (stationId: string, pricePerKwh: number, pricingModel = "PER_KWH") =>
    http
      .put<StationDetail>(`/api/operator/stations/${stationId}/pricing`, {
        pricePerKwh,
        pricingModel,
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
};
