export interface TrainingPlanSummary {
  id: string;
  eventName: string;
  distance: PlanDistance;
  duration: PlanDuration;
  raceDate: string;          // ISO date YYYY-MM-DD
  targetPaceSecondsPerKm: number;
  aiModel: AiModel;
  trainingDays: number[];    // Array of day-of-week values (1=Monday through 7=Sunday)
  longRunDay: number;        // Day-of-week value (1-7)
  state: PlanState;
  createdAt: string;         // ISO datetime
  updatedAt: string;         // ISO datetime
}

export interface TrainingPlanDetail extends TrainingPlanSummary {
  weeks: PlanWeek[];
}

export interface PlanWeek {
  weekNumber: number;
  workouts: PlanWorkoutEntry[];
}

export interface PlanWorkoutEntry {
  dayOfWeek: number;
  orderInDay: number;
  workout: WorkoutSummary;
}

export interface WorkoutSummary {
  id: string;
  name: string;
  sportType: string;
  subSport: string | null;
  numValidSteps: number;
  steps: WorkoutStep[];
}

export interface WorkoutStep {
  stepOrder: number;
  stepName: string | null;
  intensity: string;
  durationType: string;
  durationValue: number | null;
  targetType: string;
  targetValueLow: number | null;
  targetValueHigh: number | null;
  notes: string | null;
}

export interface CreatePlanRequest {
  eventName: string;
  distance: PlanDistance;
  duration: PlanDuration;
  raceDate: string;
  targetPaceSecondsPerKm: number;
  aiModel: AiModel;
  trainingDays: number[];
  longRunDay: number;
}

export type PlanState = 'NEW' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED';
export type PlanDistance = 'FIVE_K' | 'TEN_K' | 'HALF_MARATHON' | 'MARATHON';
export type PlanDuration = 'WEEKS_8' | 'WEEKS_10' | 'WEEKS_12';
export type AiModel = 'CHATGPT' | 'CLAUDE' | 'GEMINI' | 'KIRO' | 'DUMMY';
