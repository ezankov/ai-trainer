# Training Plan Generation Principles

This document defines the coaching principles and domain knowledge that the AI model
should follow when generating a structured running training plan. It is included in
the AI prompt as context to ensure high-quality, evidence-based plan generation.

---

## 1. Intensity Distribution: The 80/20 Rule

The plan must follow a polarized training model:

- **80% of weekly volume** at low intensity (Zones 1–2): easy, conversational pace.
- **20% of weekly volume** at high intensity (Zones 4–5): threshold and VO2max efforts.
- **Minimal time in Zone 3** (the "grey zone"): accumulates fatigue without optimal adaptation, except when used for race-specific marathon pace simulation.

This is based on Dr. Stephen Seiler's research documenting how elite endurance athletes train across all disciplines. Studies show polarized training produces significantly greater performance improvements than moderate-intensity-dominated approaches.

Sources: [Seiler (2010), Int J Sports Physiol Perform](https://www.fasttalklabs.com/pathways/polarized-training/); [SportCoaching - Running Zones Guide](https://sportcoaching.com.au/mastering-running-zones/)

---

## 2. Intensity & Training Zones (5-Zone Model)

To maintain physiological precision, target intensity metrics must be explicitly calibrated depending on whether they use Heart Rate Reserve (HRR / Karvonen Formula) or Maximum Heart Rate (% Max HR).

**Primary method: Heart Rate Reserve (Karvonen Formula).** Unless the athlete has lactate testing data or a verified Max HR from a field test, always default to the HRR method as the authoritative zone calculator. The % Max HR column below is provided as a secondary reference only and must not be mixed with HRR-derived targets within the same plan.

| Zone | % HRR (Karvonen) | % Max HR (reference only) | Effort | Purpose | Workout Types |
|------|------------------|--------------------------|--------|---------|---------------|
| **1** | < 60% | 50–60% | Very easy, can sing | Active recovery, structural adaptation, metabolic flushing | Recovery runs, warm-up, cool-down |
| **2** | 60–70% | 60–70% | Conversational, full sentences | Aerobic base building, lipid oxidation, mitochondrial density | Easy runs, aerobic development long runs |
| **3** | 70–80% | 70–80% | Comfortably hard, short sentences | Aerobic-anaerobic transition zone (use selectively) | Aerobic progression runs, marathon-pace blocks |
| **4** | 80–90% | 80–90% | Hard, controlled, few words | Lactate threshold, maximal steady-state, clearance capacity | Tempo runs, cruise intervals |
| **5** | 90–100% | > 90% | Maximal effort, cannot speak | VO2max development, neuromuscular recruitment, anaerobic capacity | Short intervals, track repeats, hill sprints |

### Formula Application Protocol

- **Heart Rate Reserve (HRR) Calculation (Karvonen):**
  - HRR = Max HR − Resting HR
  - Target HR = (HRR × zone %) + Resting HR
- Ensure the model applies percentages matching the selected derivation method (do not mix % Max HR boundaries with the Karvonen formula).

> **Note:** HRR-derived targets will always produce a higher absolute heart rate than a flat % Max HR target at the same zone level for athletes with a meaningful resting HR — this is the physiological advantage of the Karvonen method and the reason it is preferred.

---

## 3. Workout Types & Structure

Each training plan should include a structured mix of these workout types. Every workout step must carry an explicit intensity constraint.

### Easy Run (Zone 2)
- **Intensity:** Strictly bounded to Zone 2 (HRR or Pace).
- **Duration:** 30–60 minutes.
- **Purpose:** Primary driver of aerobic base building and cardiovascular capillary density.
- **Volume:** Forms the clear majority of training frequency.

### Long Run (Zone 2, with targeted Zone 3 variations)
- **Intensity:** Predominantly Zone 2. May include continuous blocks of Zone 3 (Marathon Pace) in the final 1/3 of the run during the late Build phase.
- **Duration:** Progressively scales, but is strictly **capped at a maximum of 2.5 to 3 hours**. This cap is **time-based**, not distance-based. Do not extend duration to reach a target mileage.
- **Purpose:** Glycogen depletion adaptation, structural durability, mental stamina.
- Always assigned to the athlete's designated long run day.

### Tempo Run (Zone 4)
- **Intensity:** Sustained effort precisely at Lactate Threshold (Zone 4).
- **Structure:** Warm-up → 20–40 min continuous tempo segment → Cool-down.
- **Purpose:** Elevate functional threshold pace, improve metabolic byproduct buffering.

### Interval Session (Zone 5)
- **Intensity:** Zone 5 efforts matched to specific target distances (e.g., 3K to 5K pace for shorter intervals; 5K to 10K pace for longer intervals).
- **Structure:** Warm-up → Structured interval repetition block → Cool-down.
- **Execution:** Always utilize `REPEAT_UNTIL_STEPS_COMPLETE` for the core block. Each hard repetition must be followed by a designated `REST` or `RECOVERY` step (active walking or light jogging).
- **Examples:** 12×400m, 6×800m, 5×1000m at 5K pace.

### Recovery Run (Zone 1)
- **Intensity:** Strictly confined to Zone 1.
- **Duration:** Shorter blocks (20–30 minutes).
- **Purpose:** Active recovery, facilitating localized blood flow and structural repair without adding neuromuscular or mechanical stress.
- Typically scheduled the day after a hard session.

### Progression Run (Zone 2 → Zone 3–4)
- **Structure:** Clear step-wise acceleration profile: Warm-up → Easy base segment → Moderate aerobic segment → Threshold finish → Cool-down.
- **Purpose:** Teaches mechanical discipline under metabolic fatigue, simulates late-race pacing requirements.

---

## 4. Periodization & Volume Progression Principles

### Macrocycle Structure (the full plan)

A training plan should be divided into phases (mesocycles):

1. **Base Phase** (first 30–40% of plan duration)
   - Focus: Capillary and mitochondrial development, volume accumulation.
   - Primary Workouts: Easy runs, progressive long runs, and short neuromuscular strides (accelerations).
   - Gradually increase weekly volume.

2. **Build Phase** (middle 40–50% of plan duration)
   - Focus: Race-specific metabolic adaptations, peak volume, and structured intensity.
   - Primary Workouts: Higher frequency of Zone 4/5 quality sessions. Weekly volume peaks in the final third of this phase.
   - Add tempo runs and interval sessions.
   - Introduce race-pace work.

3. **Taper Phase** (final 2–3 weeks)
   - Focus: Systematic reduction of accumulated fatigue while maintaining enzymatic and physiological fitness.
   - Protocol: Reduce total volume by 40–60% progressively. **Maintain training frequency and baseline intensity** via highly abbreviated, high-quality interval or tempo elements.

### Taper Phase — Week-by-Week Session Protocol

The taper requires careful management of quality sessions. Do not simply reduce volume uniformly; follow this session-specific structure:

| Taper Week | Volume Reduction | Quality Sessions | Long Run |
|------------|-----------------|-----------------|----------|
| **Week 1 of taper** | 20–30% reduction | Maintain all session types; shorten each quality session by ~20% | Reduce by 20–25% of peak long run duration |
| **Week 2 of taper** | 40–50% reduction | Shorten quality sessions by 30–40%; eliminate one quality session if schedule had two | Reduce to 50–60% of peak long run duration |
| **Race week** | 60–70% reduction | Easy runs only with 2–3 short strides (10–15 sec accelerations) at race pace; no structured tempo or intervals | No long run; final easy shakeout run of 20–30 min, 2 days before race |

> **Key principle:** Intensity (pace targets) must not be reduced during taper — only duration and volume decrease. The athlete should arrive at race day feeling sharp, not detrained.

### Progressive Overload Constraints

- **Weekly Volume Increase:** Increase total weekly volume by no more than 5–10% per week.
- **The Long Run Proportionality Rule:** The Long Run must not exceed 30–35% of total weekly volume (with flexible variance allowed only for lower-volume 5K/10K developmental plans).
- **Step-Back (Recovery) Weeks:** Every 3–4 weeks, integrate a dedicated recovery cycle with a 20–30% drop in total volume to facilitate adaptation. Resume progression from the baseline established immediately prior to the step-back week.

### Microcycle Design (Weekly Schedule Layout)

- **Spacing:** Quality or high-intensity sessions must never be scheduled on back-to-back days (minimum 24–48 hours of recovery or low-intensity running between hard efforts).
- **Distribution:** Maximum of 1 long run and 1–2 quality sessions (Tempo, Intervals, Progression) per weekly block.
- **Rest Days:** Ensure a minimum of 1 full rest day per week for recreational or intermediate athletes.

---

## 5. Distance-Specific Optimization Guidelines

### 5K Plans
- **Primary Stimulus:** VO2max intervals (Zone 5), 10–15% of weekly volume. Target intensities optimized to 3K–5K goal paces.
- **Secondary Stimulus:** Lactate threshold support (Zone 4).
- **Long Run Cap:** 60–90 minutes.
- **Interval examples:** 12×400m, 6×800m, 5×1000m at 5K pace.

### 10K Plans
- **Primary Stimulus:** Continuous or broken threshold sets (Zone 4), 12–18% of weekly volume.
- **Secondary Stimulus:** VO2max development (Zone 5) optimized to 5K pace.
- **Long Run Cap:** 90–120 minutes.
- **Tempo examples:** 3×10min at threshold, 20–30min continuous tempo.

### Half Marathon Plans
- **Primary Stimulus:** Prolonged lactate threshold intervals and cruise tempos (Zone 4).
- **Secondary Stimulus:** Selective inclusion of Zone 3 (Marathon Pace) segments within the weekly long run.
- **Long Run Cap:** 2 to 2.5 hours.
- **Tempo examples:** 30–40min continuous tempo, 4×8min cruise intervals.

### Marathon Plans
- **Primary Stimulus:** High-volume aerobic development. The weekly Long Run is the priority workout of the macrocycle.
- **Secondary Stimulus:** Extended continuous threshold blocks (Zone 4) and targeted race-pace (Zone 3) simulation intervals. VO2max work is strictly managed to maintain anaerobic ceiling without inducing excessive systemic fatigue.
- **Long Run Cap:** Strictly **capped at 2.5 to 3 hours** maximum (time-based).
- Include marathon-pace segments in long runs during the final weeks of the Build phase.

---

## 6. Target Selection and Intensity Guardrails

Every step generated in a workout must follow strict constraint formatting:

1. **Warm-up / Cool-down Steps:** Must **never** use an unconstrained "OPEN" target. Always bind these segments to explicit **Zone 1 to Zone 2** heart rate ranges to enforce proper preparation and recovery.
2. **Recovery Runs:** Must be explicitly locked to **Zone 1** heart rate targets.
3. **Easy Runs / Long Runs:** Bounded to **Zone 2** heart rate targets (with intentional exceptions for marathon-pace sections which use SPEED targets).
4. **Tempo / Threshold Workouts:** Target metrics must use precise **SPEED** targets calculated from the runner's Threshold Pace, rather than heart rate, to account for cardiovascular drift during prolonged efforts.
5. **High-Intensity Intervals:** Target metrics must use **SPEED** targets corresponding to race pace or faster.

---

## 7. Mathematical Fallback Protocols for Incomplete Profiles

If an athlete profile contains missing or incomplete data fields, the model must follow structured fallback logic to avoid generation failures or dangerous intensity estimates:

- **Missing Maximum Heart Rate (Max HR):** Calculate using the Gellish formula:
  - Max HR = 207 − (0.7 × Age)
- **Missing Threshold Pace:** Extrapolate a baseline threshold pace using the most recent race performance data via standard Riegel prediction models.
- **Missing Resting HR:** Assume a default resting HR of 60 bpm for zone calculations.
- **Completely Unknown Fitness Baseline:** If the runner's fitness baseline is completely absent, default to a highly conservative approach:
  - Cap total starting weekly volume at 24 kilometers.
  - Limit the initial Long Run to a maximum duration of 45–60 minutes.
  - Focus exclusively on Zone 2 work for the first 2–3 weeks before introducing any quality sessions.

---

## 8. Safety Flags & Escalation Principles

The model must identify scenarios that carry elevated injury or health risk and flag them in the generated plan. These are coaching knowledge thresholds, not medical diagnoses.

Flag and include an advisory note when any of the following conditions are detected:

- **Very high weekly volume jump:** If the plan's Week 1 target volume exceeds the athlete's apparent current fitness level by more than 30%, apply a bridging ramp-up block before the official Base Phase begins.
- **Older athlete with no recent fitness baseline:** Athletes aged 60+ with no recent training history or race result. Apply the conservative fitness baseline from Section 7 and note that medical clearance is advisable.
- **Extreme pace targets:** If the target race pace implies a performance level significantly beyond what the athlete's profile data suggests (e.g., target pace 30%+ faster than predicted from race times), flag the mismatch and generate the plan at a more realistic intensity.

> **Principle:** The model's role is to generate an appropriate plan and surface risks transparently. It does not diagnose, prescribe, or override medical advice.

---

## 9. Key Operational Constraints

- **Single Workout Per Day:** Each training day gets exactly one workout (`orderInDay = 1`).
- **Schedule Integrity:** Strictly honor the athlete's designated training days and preferred Long Run day. Never schedule workouts on unselected days.
- **No Consecutive Quality Days:** Never place two high-intensity (Zone 4 or Zone 5) sessions on back-to-back calendar days.
- **Descriptive Naming:** All workout names must be unambiguous and descriptive (e.g., `"Easy Run 45min"`, `"Tempo 30min"`, `"6x800m Intervals"`, `"Long Run 2h00"`).
- **Pace/Speed Derivation:** All pace/speed targets must be derived from the athlete's profile data (threshold pace, race times, HR zones). Never use arbitrary values.

---

## 10. Progression Across Weeks

The plan should show clear progression:

- **Volume:** Gradually increases through base and build phases, then drops in taper.
- **Intensity:** Low in base phase, increases in build phase, maintained (at lower volume) in taper.
- **Long run:** Increases by 10–15 minutes per week, with step-back every 3–4 weeks.
- **Interval volume:** Increases (more reps or longer reps) through build phase.
- **Tempo duration:** Increases through build phase (e.g., 20min → 30min → 40min).

### Example Volume Progression (12-week marathon plan, 4 training days/week):

| Week | Phase | Character |
|------|-------|-----------|
| 1–4 | Base | Easy runs + long run, building volume, strides |
| 5–9 | Build | Add tempo, intervals, long run peaks, step-back at week 7 |
| 10–12 | Taper | Reduce volume 20%/30%/50%, maintain intensity at shorter durations |

---

## References

Content was rephrased for compliance with licensing restrictions.

- Seiler, S. (2010). What is best practice for training intensity and duration distribution in endurance athletes? [Fast Talk Labs](https://www.fasttalklabs.com/pathways/polarized-training/)
- Periodization Training for Runners. [Marathon Handbook](https://marathonhandbook.com/periodization-training/)
- Running Zones: The Complete Guide to Training by Intensity. [SportCoaching](https://sportcoaching.com.au/mastering-running-zones/)
- Marathon Training Schedule: 52-Week Periodization. [RunnersConnect](https://runnersconnect.net/marathon-training-schedule)
- How to Taper for a Marathon: Research-Backed Protocol. [RunnersConnect](https://runnersconnect.net/running-training-articles/how-to-taper-for-a-marathon/)
- Sports Training Principles. [Current Sports Medicine Reports (2019)](https://journals.lww.com/acsm-csmr/fulltext/2019/04000/sports_training_principles.2.aspx)
