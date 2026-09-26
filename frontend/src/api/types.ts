// Types mirroring the ChargeMap backend DTOs.

export type AvailabilitySummary = "AVAILABLE" | "OCCUPIED" | "UNKNOWN";
export type ChargerType = "AC" | "DC" | "DC_FAST";
export type ChargerStatus = "AVAILABLE" | "OCCUPIED" | "BROKEN" | "CLOSED" | "UNKNOWN";
export type ConnectorType = "CCS1" | "CCS2" | "CHADEMO" | "TYPE2" | "GBT" | "NACS";
export type DataSource = "OPEN_DATASET" | "OPERATOR" | "OPERATOR_API" | "USER_SUBMISSION";
export type Confidence = "HIGH" | "MEDIUM" | "LOW";

export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface StationSummary {
  id: string;
  name: string;
  operator: string | null;
  area: string | null;
  location: GeoPoint | null;
  distanceMeters: number | null;
  pricePerKwh: number | null;
  availableCount: number;
  totalChargers: number;
  availabilitySummary: AvailabilitySummary;
  availabilityUpdatedAt: string | null;
  dataSource: DataSource | null;
  confidence: Confidence | null;
  lastUpdated: string | null;
}

export interface Charger {
  chargerId: string;
  connectorType: ConnectorType;
  chargerType: ChargerType;
  powerKw: number;
  status: ChargerStatus;
  statusUpdatedAt: string | null;
}

export interface Pricing {
  pricePerKwh: number;
  pricingModel: string;
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

export interface StationDetail extends StationSummary {
  address: string | null;
  openingHours: string | null;
  phone: string | null;
  amenities: string[];
  rating: number | null;
  chargers: Charger[];
  currentPricing: Pricing | null;
  lastVerified: string | null;
  disabled: boolean;
  likeCount: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  role: string;
  branchId: string | null;
  vehicleCount: number;
  favoriteCount: number;
  suspended: boolean;
  pointsBalance: number;
  lifetimePoints: number;
  level: string;
  stationsAdded: number;
  updatesMade: number;
  createdAt: string | null;
}

export interface ReportResponse {
  message: string;
  reportId: string;
  availabilitySummary: AvailabilitySummary;
  availableCount: number;
  totalChargers: number;
  lastUpdated: string | null;
}

export interface AdminStats {
  totalStations: number;
  totalChargers: number;
  availableChargers: number;
  totalUsers: number;
  totalReports: number;
  staleStations: number;
}

export interface ReportFeedItem {
  reportId: string;
  stationId: string;
  stationName: string;
  chargerId: string | null;
  status: ChargerStatus;
  createdAt: string;
}

export interface PricingTrendItem {
  stationId: string;
  stationName: string;
  priceChanges: number;
  currentPrice: number | null;
  lastChangedAt: string | null;
}

export interface ManagedUser {
  id: string;
  email: string;
  name: string;
  role: "USER" | "OPERATOR" | "ADMIN";
  suspended: boolean;
  suspendedReason: string | null;
  pointsBalance: number;
  lifetimePoints: number;
  level: string;
  stationsAdded: number;
  updatesMade: number;
  createdAt: string | null;
}

export interface Prize {
  id: string;
  name: string;
  description: string | null;
  imageId: string | null;
  pointCost: number;
  active: boolean;
}

export interface PointsLedgerEntry {
  taskType: "STATION_ADD" | "STATION_UPDATE" | "REPORT" | "ADMIN_ADJUST";
  points: number;
  reason: string | null;
  createdAt: string;
}

export interface PointRules {
  pointsPerStationAdd: number;
  pointsPerStationUpdate: number;
  pointsPerReport: number;
  dailyCapStationAdd: number;
  dailyCapStationUpdate: number;
  dailyCapReport: number;
}

export type SubmissionStatus = "PENDING" | "APPROVED" | "REJECTED";

export type SubmissionType = "NEW" | "EDIT";

export interface SubmissionComment {
  authorName: string;
  role: string;
  text: string;
  createdAt: string;
}

export interface Submission {
  id: string;
  submittedBy: string | null;
  status: SubmissionStatus;
  type: SubmissionType;
  targetStationId: string | null;
  name: string;
  operator: string | null;
  address: string | null;
  area: string | null;
  location: GeoPoint | null;
  imageId: string | null;
  ocrLooksLikeStation: boolean;
  proposedPricePerKwh: number | null;
  connectorType: ConnectorType | null;
  chargerType: ChargerType | null;
  powerKw: number | null;
  comments: SubmissionComment[];
  createdStationId: string | null;
  createdAt: string | null;
  reviewedAt: string | null;
}

export interface NewSubmission {
  image: File;
  name: string;
  operator?: string;
  address?: string;
  area?: string;
  lat: number;
  lng: number;
  ocrText?: string;
  ocrLooksLikeStation?: boolean;
  pricePerKwh?: number | null;
  connectorType?: ConnectorType | "";
  chargerType?: ChargerType | "";
  powerKw?: number | null;
}

export interface StationFilters {
  chargerType?: ChargerType | "";
  connector?: ConnectorType | "";
  minKw?: number | "";
  maxKw?: number | "";
  priceMax?: number | "";
  availableOnly?: boolean;
}

// ----- Vehicle Sync & Live Telemetry (Phase 1) -----

export type VehicleManufacturer =
  | "TESLA"
  | "FORD"
  | "BMW"
  | "HYUNDAI"
  | "KIA"
  | "MERCEDES"
  | "MOCK";

export type VehicleChargingStatus = "IDLE" | "CHARGING" | "COMPLETE" | "DISCONNECTED";

export type BatteryTier = "EXCELLENT" | "GOOD" | "LOW" | "CRITICAL" | "UNKNOWN";

export interface VehicleTelemetry {
  batteryPercentage: number | null;
  batteryTier: BatteryTier;
  rangeKm: number | null;
  chargingStatus: VehicleChargingStatus | null;
  chargingSpeedKw: number | null;
  batteryHealthPercent: number | null;
  odometerKm: number | null;
  lastUpdated: string | null;
}

export interface ConnectedVehicle {
  id: string;
  manufacturer: VehicleManufacturer;
  model: string | null;
  year: number | null;
  nickname: string | null;
  connected: boolean;
  locationConsent: boolean;
  chargeTargetPercent: number;
  lastSyncAt: string | null;
  telemetry: VehicleTelemetry | null;
}

export interface VehicleLocation {
  latitude: number | null;
  longitude: number | null;
  lastUpdate: string | null;
}

export interface VehicleSyncResult {
  success: boolean;
  lastSync: string | null;
  vehiclesSynced: number;
}

export interface ConnectVehicleRequest {
  manufacturer: VehicleManufacturer;
  nickname?: string;
  locationConsent?: boolean;
  chargeTargetPercent?: number;
  authCode?: string;
}

// ----- Branches, Super Admin, Metrics, Activities -----

export interface Branch {
  id: string;
  name: string;
  description: string | null;
  area: string | null;
  profileImageId: string | null;
  bannerImageId: string | null;
  createdAt: string | null;
}

export interface AdminSummary {
  id: string;
  name: string;
  email: string;
  role: string;
  branchId: string | null;
  adminDisabled: boolean;
}

export interface BranchPriceTrend {
  branchId: string | null;
  branchName: string;
  priceChanges: number;
}

export interface StationUpdateFrequency {
  stationId: string;
  stationName: string;
  approvedUpdates: number;
  priceChanges: number;
  windowDays: number;
}

export interface Activity {
  id: string;
  title: string;
  description: string | null;
  goalType: string;
  goalCount: number;
  rewardPrizeId: string | null;
  active: boolean;
  createdAt: string | null;
}

export interface UserActivity {
  activityId: string;
  title: string;
  description: string | null;
  goalCount: number;
  rewardPrizeId: string | null;
  status: "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";
  progress: number;
  grantedPrizeId: string | null;
}

export interface ActivityCompletionStat {
  activityId: string;
  title: string;
  rewardPrizeId: string | null;
  completions: number;
}

// ----- Station reviews + likes -----

export interface StationReview {
  id: string;
  stationId: string;
  userId: string;
  authorName: string;
  text: string | null;
  imageId: string | null;
  mine: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface LikeStatus {
  likeCount: number;
  likedByMe: boolean;
}

export interface LikeTrendPoint {
  date: string;
  likes: number;
  unlikes: number;
  net: number;
}
