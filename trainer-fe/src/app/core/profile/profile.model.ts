export interface ProfileRequest {
  dateOfBirth: string;
  weightKg: number;
  restingHR: number;
  maxHR: number;
  lthr?: number | null;
  thresholdPaceSecondsPerKm?: number | null;
  vo2Max?: number | null;
  fiveKSeconds?: number | null;
  tenKSeconds?: number | null;
  halfMarathonSeconds?: number | null;
  marathonSeconds?: number | null;
}

export interface ProfileResponse {
  id: number;
  dateOfBirth: string;
  weightKg: number;
  restingHR: number;
  maxHR: number;
  lthr: number | null;
  thresholdPaceSecondsPerKm: number | null;
  vo2Max: number | null;
  fiveKSeconds: number | null;
  tenKSeconds: number | null;
  halfMarathonSeconds: number | null;
  marathonSeconds: number | null;
  hrProfile: HrProfileResponse | null;
  paceProfile: PaceProfileResponse | null;
}

export interface HrProfileResponse {
  zones: HrZoneResponse[];
}

export interface HrZoneResponse {
  zoneNumber: number;
  name: string;
  lowerBound: number;
  upperBound: number;
}

export interface PaceProfileResponse {
  zones: PaceZoneResponse[];
}

export interface PaceZoneResponse {
  zoneNumber: number;
  name: string;
  lowerBound: number;
  upperBound: number;
}
