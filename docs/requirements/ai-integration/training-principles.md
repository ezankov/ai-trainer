# Training Plan Generation Principles

This document defines the coaching principles and domain knowledge that the AI model
should follow when generating a structured running training plan. It is included in
the AI prompt as context to ensure high-quality, evidence-based plan generation.

---

## 1. Intensity Distribution: The 80/20 Rule

The plan must follow a polarized training model:

- **80% of weekly volume** at low intensity (Zones 1–2): easy, conversational pace
- **20% of weekly volume** at high intensity (Zones 4–5): threshold and VO2max efforts
- **Minimal time in Zone 3** (the "grey zone"): accumulates fatigue without optimal adaptation

This is based on Dr. Stephen Seiler's research documenting how elite endurance athletes
train across all disciplines. Studies show polarized training produces significantly
greater performance improvements than moderate-intensity-dominated approaches.

Sources: [Seiler (2010), Int J Sports Physiol Perform](https://www.fasttalklabs.com/pathways/polarized-training/); [SportCoaching - Running Zones Guide](https://sportcoaching.com.au/mastering-running-zones/)

---

## 2. Heart Rate Zones (5-Zone Model)

| Zone | % Max HR | Effort | Purpose | Workout Types |
|------|----------|--------|---------|---------------|
| 1 | 50–60% | Very easy, can sing | Active recovery, warm-up/cool-down | Recovery jog, warm-up, cool-down |
| 2 | 60–70% | Conversational, full sentences | Aerobic base, fat oxidation, mitochondrial density | Easy runs, long runs |
| 3 | 70–80% | Comfortably hard, short sentences | Moderate endurance (use sparingly) | Some marathon-pace work |
| 4 | 80–90% | Hard, few words | Lactate threshold, fastest sustainable pace | Tempo runs, cruise intervals |
| 5 | 90–100% | Maximum, cannot speak | VO2max, anaerobic capacity | Short intervals, sprint repeats |

When generating target heart rate values, use the Karvonen formula:
- Heart Rate Reserve (HRR) = Max HR − Resting HR
- Target HR = (HRR × zone %) + Resting HR

---

## 3. Workout Types

Each training plan should include a mix of these workout types:

### Easy Run (Zone 2)
- Conversational pace, 60–70% max HR
- Duration: 30–60 minutes
- Purpose: aerobic base building, recovery between hard sessions
- Should make up the majority of training days

### Long Run (Zone 2, with optional Zone 3 finish)
- Longest run of the week, at easy/conversational pace
- Duration: progressively increases (up to 2.5–3.5 hours for marathon)
- Purpose: endurance, fat oxidation, mental toughness
- Always assigned to the athlete's designated long run day
- May include final 15–20 minutes at marathon pace in later weeks

### Tempo Run (Zone 4)
- Sustained effort at lactate threshold pace (~85–90% max HR)
- Duration: 20–40 minutes at tempo pace (plus warm-up and cool-down)
- Purpose: raise lactate threshold, improve sustained speed
- Structure: warm-up → sustained tempo block → cool-down

### Interval Session (Zone 5)
- Short, high-intensity repetitions with recovery between
- Examples: 400m repeats, 800m repeats, 1km repeats
- Pace: 5K race pace or faster
- Purpose: VO2max development, running economy, speed
- Structure: warm-up → intervals with rest → cool-down
- Use REPEAT_UNTIL_STEPS_COMPLETE for the interval block

### Recovery Run (Zone 1–2)
- Very easy pace, shorter duration (20–30 minutes)
- Purpose: active recovery, blood flow without training stress
- Typically the day after a hard session

### Progression Run (Zone 2 → Zone 3–4)
- Start easy, finish at tempo or threshold effort
- Purpose: teaches pacing discipline, simulates race-day fatigue
- Structure: warm-up → easy segment → moderate segment → threshold finish → cool-down

---

## 4. Periodization Principles

### Macrocycle Structure (the full plan)

A training plan should be divided into phases (mesocycles):

1. **Base Phase** (first 30–40% of plan duration)
   - Focus: aerobic base building, volume accumulation
   - Mostly easy runs and long runs
   - Introduce strides (short accelerations) for neuromuscular activation
   - Gradually increase weekly volume

2. **Build Phase** (middle 40–50% of plan duration)
   - Focus: race-specific fitness, increasing intensity
   - Add tempo runs and interval sessions
   - Long run continues to increase
   - Introduce race-pace work
   - Peak weekly volume occurs in this phase

3. **Taper Phase** (final 2–3 weeks)
   - Focus: reduce fatigue while maintaining fitness
   - Reduce total volume by 40–60% progressively
   - Maintain some intensity (shorter threshold/interval sessions)
   - Reduce long run distance significantly
   - Keep frequency (number of training days) the same

### Progressive Overload

- Increase weekly volume by no more than 10% per week
- Every 3–4 weeks, include a "step-back" week with 20–30% volume reduction
- Step-back weeks allow adaptation and reduce injury risk
- After a step-back week, resume building from a slightly higher baseline

### Microcycle (Weekly Structure)

A typical training week should follow this pattern:
- Hard days followed by easy days (never two hard sessions back-to-back)
- 1 long run per week (on the designated long run day)
- 1–2 quality sessions per week (tempo, intervals, or progression run)
- Remaining days: easy runs or recovery runs
- Consider 1 rest day per week for recreational athletes

---

## 5. Distance-Specific Guidelines

### 5K Plans
- Primary quality work: VO2max intervals (Zone 5), 10–15% of weekly volume
- Secondary: threshold/tempo runs (Zone 4)
- Long run: up to 60–90 minutes
- Interval examples: 12×400m, 6×800m, 5×1000m at 5K pace

### 10K Plans
- Primary quality work: threshold sessions (Zone 4), 12–18% of weekly volume
- Secondary: VO2max intervals (Zone 5)
- Long run: up to 90–120 minutes
- Tempo examples: 3×10min at threshold, 20–30min continuous tempo

### Half Marathon Plans
- Primary quality work: threshold runs (Zone 4)
- Include some marathon-pace long runs (Zone 3)
- Long run: up to 2–2.5 hours
- Tempo examples: 30–40min continuous tempo, 4×8min cruise intervals

### Marathon Plans
- Primary quality work: threshold runs (Zone 4), moderate volume
- Long run is the key session: up to 3–3.5 hours
- Include marathon-pace segments in long runs (final weeks)
- VO2max work is minimal but present for maintaining speed ceiling
- Higher total weekly volume than shorter distances

---

## 6. Workout Step Structure Rules

Every workout must include:
1. **Warm-up** (WARMUP intensity): 5–15 minutes easy running
2. **Main set**: the core training stimulus (varies by workout type)
3. **Cool-down** (COOLDOWN intensity): 5–10 minutes easy running

For interval workouts:
- Use REPEAT_UNTIL_STEPS_COMPLETE to define the interval block
- Include a REST or RECOVERY step between intervals
- Interval duration/distance should be consistent within a set

Target values:
- Easy runs: use HEART_RATE targets based on athlete's Zone 2
- Tempo runs: use SPEED targets based on athlete's threshold pace
- Intervals: use SPEED targets based on athlete's race pace or faster
- Long runs: use HEART_RATE targets (Zone 2, optionally Zone 3 for finish)
- Recovery/warm-up/cool-down: use OPEN target (no specific target)

---

## 7. Progression Across Weeks

The plan should show clear progression:

- **Volume**: gradually increases through base and build phases, then drops in taper
- **Intensity**: low in base phase, increases in build phase, maintained (at lower volume) in taper
- **Long run**: increases by 10–15 minutes per week, with step-back every 3–4 weeks
- **Interval volume**: increases (more reps or longer reps) through build phase
- **Tempo duration**: increases through build phase (e.g., 20min → 30min → 40min)

### Example Volume Progression (12-week marathon plan, 4 training days/week):

| Week | Phase | Character |
|------|-------|-----------|
| 1–4 | Base | Easy runs + long run, building volume, strides |
| 5–9 | Build | Add tempo, intervals, long run peaks, step-back at week 7 |
| 10–12 | Taper | Reduce volume 20%/30%/50%, maintain some intensity |

---

## 8. Key Constraints

- Never schedule two high-intensity sessions on consecutive days
- The long run should always be on the athlete's designated long run day
- Respect the athlete's selected training days — only schedule workouts on those days
- Each training day gets exactly one workout (orderInDay = 1)
- Workout names should be descriptive (e.g., "Easy Run", "Tempo 30min", "6x800m Intervals", "Long Run 90min")
- All pace/speed targets should be derived from the athlete's profile data (threshold pace, race times, HR zones)
- If athlete profile data is incomplete (null fields), use conservative estimates based on available data

---

## References

Content was rephrased for compliance with licensing restrictions.

- Seiler, S. (2010). What is best practice for training intensity and duration distribution in endurance athletes? [Fast Talk Labs](https://www.fasttalklabs.com/pathways/polarized-training/)
- Periodization Training for Runners. [Marathon Handbook](https://marathonhandbook.com/periodization-training/)
- Running Zones: The Complete Guide to Training by Intensity. [SportCoaching](https://sportcoaching.com.au/mastering-running-zones/)
- Marathon Training Schedule: 52-Week Periodization. [RunnersConnect](https://runnersconnect.net/marathon-training-schedule)
- How to Taper for a Marathon: Research-Backed Protocol. [RunnersConnect](https://runnersconnect.net/running-training-articles/how-to-taper-for-a-marathon/)
- Sports Training Principles. [Current Sports Medicine Reports (2019)](https://journals.lww.com/acsm-csmr/fulltext/2019/04000/sports_training_principles.2.aspx)
