const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function futureTime(offsetMs = 60000) {
  return new Date(Date.now() + offsetMs);
}

function validProfile(uid = ALICE_UID, ts = pastTime(5000)) {
  return {
    userId: uid,
    displayName: "Maya Lin",
    age: 34,
    biologicalSex: "FEMALE",
    bloodType: "O+",
    allergies: ["Penicillin"],
    medications: ["Vitamin D3"],
    conditions: ["Mild Asthma"],
    wellnessGoals: ["Better Sleep", "Hydration"],
    onboardingCompleted: true,
    disclaimerAccepted: true,
    emergencyContactName: "Daniel Lin",
    emergencyContactPhone: "+1-555-0199",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validConsultation(uid = ALICE_UID, ts = pastTime(5000)) {
  return {
    userId: uid,
    consultationType: "SYMPTOM_CHECK",
    title: "Persistent Tension Headache",
    bodyArea: "Head & Neuro",
    symptomSummary: "Dull ache behind eyes since this morning with mild fatigue.",
    severity: 5,
    duration: "Today",
    associatedSymptoms: ["Fatigue", "Light sensitivity"],
    hadPhotoAttachment: false,
    urgencyLevel: "SELF_CARE",
    aiSummary: "Your symptoms suggest a mild tension headache. Stay hydrated and rest.",
    possibleCauses: ["Tension headache", "Dehydration or eye strain"],
    careSuggestions: ["Drink 2 glasses of water", "Rest in a dim quiet room for 20 minutes"],
    redFlags: ["Sudden severe thunderclap headache", "Vision loss or neck stiffness"],
    doctorQuestions: ["Could screen posture be triggering these headaches?"],
    followUpStatus: "PENDING",
    followUpNote: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated denial across all collections
test("Unauthenticated users cannot read or write any user health data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("consultations").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("checkIns").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("careTasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("medications").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("medicationLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vitals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("mentalAssessments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bodyMeasurements").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cycleLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("aiSafetyReports").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("labBiomarkers").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").set(validProfile())
  );
});

// 2. Health Profile CRUD, onboarding/emergency fields, and cross-user isolation
test("HealthProfile: owner create/update/read succeed; ghost fields & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main");

  await assertSucceeds(profileRef.set(validProfile()));
  await assertSucceeds(profileRef.get());

  // Partial update preserving createdAt and userId (including 18+ gate, US/CA region & Care Team)
  await assertSucceeds(
    profileRef.update({
      age: 35,
      allergies: ["Penicillin", "Pollen"],
      emergencyContactName: "Sam Lin",
      emergencyContactPhone: "555-234-5678",
      onboardingCompleted: true,
      disclaimerAccepted: true,
      ageConfirmed18Plus: true,
      preferredHealthRegion: "CA",
      preferredEmergencyCountry: "CA",
      coverageProvider: "OHIP (Ontario)",
      coverageMemberId: "1234-567-890-AB",
      primaryDoctorName: "Dr. Marc Tremblay",
      preferredPharmacy: "Shoppers Drug Mart #0824",
      updatedAt: pastTime(1000),
    })
  );

  // Reject oversized coverageMemberId (> 40 chars)
  await assertFails(
    profileRef.update({
      coverageMemberId: "X".repeat(50),
      updatedAt: pastTime(800),
    })
  );

  // Reject ghost field
  await assertFails(
    profileRef.update({
      isVerified: true,
      updatedAt: pastTime(500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    profileRef.update({
      createdAt: pastTime(500),
      updatedAt: pastTime(500),
    })
  );

  // Reject cross-user access
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").update({
      displayName: "Hijacked",
      updatedAt: pastTime(500),
    })
  );
});

// 3. Consultations, Follow-Up Outcomes & Chat Messages
test("Consultations & Messages: valid payloads, follow-up updates succeed; invalid bounds & orphan messages fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const consultRef = aliceDb.collection("users").doc(ALICE_UID).collection("consultations").doc("consult_1");

  // Orphan message before consultation exists must fail
  await assertFails(
    consultRef.collection("messages").doc("msg_orphan").set({
      userId: ALICE_UID,
      consultationId: "consult_1",
      role: "USER",
      content: "Orphan message",
      hadPhotoAttachment: false,
      createdAt: pastTime(1000),
    })
  );

  // Valid consultation create (no `id` field)
  await assertSucceeds(consultRef.set(validConsultation()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("consultations").get());

  // Valid symptom outcome follow-up update
  await assertSucceeds(
    consultRef.update({
      followUpStatus: "IMPROVING",
      followUpNote: "Pain eased after rest and hydration.",
      followUpUpdatedAt: pastTime(800),
      updatedAt: pastTime(800),
    })
  );

  // Invalid follow-up enum fails
  await assertFails(
    consultRef.update({
      followUpStatus: "CURED_FOREVER",
      updatedAt: pastTime(500),
    })
  );

  // Reject @DocumentId `id` field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("consultations").doc("consult_2").set({
      ...validConsultation(),
      id: "consult_2",
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("consultations").doc("consult_3").set(
      validConsultation(ALICE_UID, futureTime())
    )
  );

  // Valid follow-up chat messages
  await assertSucceeds(
    consultRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      consultationId: "consult_1",
      role: "USER",
      content: "Should I take ibuprofen if I have mild asthma?",
      hadPhotoAttachment: false,
      createdAt: pastTime(1500),
    })
  );
  await assertSucceeds(consultRef.collection("messages").orderBy("createdAt").get());

  // Mutating aiSummary fails
  await assertFails(
    consultRef.update({ aiSummary: "Tampered", updatedAt: pastTime(500) })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("consultations").get()
  );
});

// 4. Daily Check-Ins (Mood, Sleep, Physical Activity & Hydration)
test("DailyCheckIn: create, update, list succeed for owner and enforce bounds including physical activity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const checkInsCol = aliceDb.collection("users").doc(ALICE_UID).collection("checkIns");
  const checkInRef = checkInsCol.doc("2026-11-19");

  const created = pastTime(4000);
  await assertSucceeds(
    checkInRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-19",
      moodLevel: 4,
      energyLevel: 4,
      sleepHours: 7.5,
      hydrationGlasses: 6,
      activityLevel: "MODERATE",
      activityMinutes: 35,
      notes: "Feeling rested after morning walk.",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Backward-compatible legacy document without activityLevel/activityMinutes still succeeds
  await assertSucceeds(
    checkInsCol.doc("2026-11-18").set({
      userId: ALICE_UID,
      dateKey: "2026-11-18",
      moodLevel: 3,
      energyLevel: 3,
      sleepHours: 7.0,
      hydrationGlasses: 5,
      notes: "Legacy entry.",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching HealthCloudRepository.observeCheckIns
  await assertSucceeds(checkInsCol.orderBy("dateKey", "desc").limit(31).get());

  // Partial update including physical activity fields succeeds
  await assertSucceeds(
    checkInRef.update({
      hydrationGlasses: 8,
      activityLevel: "ACTIVE",
      activityMinutes: 50,
      updatedAt: pastTime(1000),
    })
  );

  // Out-of-bounds mood fails
  await assertFails(
    checkInRef.update({
      moodLevel: 9,
      updatedAt: pastTime(500),
    })
  );

  // Invalid activityLevel enum fails
  await assertFails(
    checkInRef.update({
      activityLevel: "ULTRA_EXTREME",
      updatedAt: pastTime(500),
    })
  );

  // Out-of-bounds activityMinutes (> 600 or negative) fail
  await assertFails(
    checkInRef.update({
      activityMinutes: 900,
      updatedAt: pastTime(500),
    })
  );
  await assertFails(
    checkInRef.update({
      activityMinutes: -10,
      updatedAt: pastTime(500),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("checkIns").get());
});

// 5. Care Checklist Tasks
test("CareTask: create, toggle, delete succeed and reject cross-user delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const tasksCol = aliceDb.collection("users").doc(ALICE_UID).collection("careTasks");
  const created = pastTime(3000);

  await assertSucceeds(
    tasksCol.doc("task_1").set({
      userId: ALICE_UID,
      title: "Drink warm ginger tea",
      category: "RECOVERY",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    tasksCol.doc("task_1").update({
      completed: true,
      updatedAt: pastTime(1000),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("careTasks").doc("task_1").delete()
  );
  await assertSucceeds(tasksCol.doc("task_1").delete());
});

// 6. Medications & Medication Adherence Logs
test("Medications & Logs: owner CRUD and adherence upsert succeed; invalid bounds & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const medsCol = aliceDb.collection("users").doc(ALICE_UID).collection("medications");
  const logsCol = aliceDb.collection("users").doc(ALICE_UID).collection("medicationLogs");
  const created = pastTime(4000);

  await assertSucceeds(
    medsCol.doc("med_1").set({
      userId: ALICE_UID,
      name: "Vitamin D3",
      dosage: "2000 IU",
      scheduleSlot: "MORNING",
      reminderHour: 8,
      reminderMinute: 30,
      instructions: "Take with breakfast",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(medsCol.orderBy("createdAt", "desc").get());

  // Toggle paused
  await assertSucceeds(
    medsCol.doc("med_1").update({
      active: false,
      updatedAt: pastTime(1000),
    })
  );

  // Invalid hour fails
  await assertFails(
    medsCol.doc("med_1").update({
      reminderHour: 29,
      updatedAt: pastTime(500),
    })
  );

  // Adherence log create & update
  await assertSucceeds(
    logsCol.doc("2026-11-20_med_1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-20",
      medicationId: "med_1",
      medicationName: "Vitamin D3",
      status: "TAKEN",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    logsCol.doc("2026-11-20_med_1").update({
      status: "SKIPPED",
      updatedAt: pastTime(1000),
    })
  );

  await assertSucceeds(logsCol.orderBy("dateKey", "desc").limit(200).get());

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("medications").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("medicationLogs").get());
  await assertSucceeds(medsCol.doc("med_1").delete());
  await assertSucceeds(logsCol.doc("2026-11-20_med_1").delete());
});

// 7. Vitals & Biometrics Readings
test("Vitals: blood pressure (dual value) and heart rate (single value) succeed; out-of-range & update fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const vitalsCol = aliceDb.collection("users").doc(ALICE_UID).collection("vitals");
  const ts = pastTime(3000);

  // Blood pressure with optional secondaryValue
  await assertSucceeds(
    vitalsCol.doc("vital_bp_1").set({
      userId: ALICE_UID,
      vitalType: "BLOOD_PRESSURE",
      primaryValue: 118,
      secondaryValue: 76,
      unit: "mmHg",
      note: "Resting morning reading",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Heart rate omitting optional secondaryValue
  await assertSucceeds(
    vitalsCol.doc("vital_hr_1").set({
      userId: ALICE_UID,
      vitalType: "HEART_RATE",
      primaryValue: 68,
      unit: "bpm",
      note: "",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(vitalsCol.orderBy("recordedAt", "desc").limit(100).get());

  // Immutable vitals (updates denied)
  await assertFails(
    vitalsCol.doc("vital_hr_1").update({
      primaryValue: 72,
      updatedAt: pastTime(500),
    })
  );

  // Out-of-bounds value fails
  await assertFails(
    vitalsCol.doc("vital_bad").set({
      userId: ALICE_UID,
      vitalType: "HEART_RATE",
      primaryValue: -5,
      unit: "bpm",
      note: "",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vitals").get());
  await assertSucceeds(vitalsCol.doc("vital_bp_1").delete());
});

// 8. Mental Wellbeing Assessments (PHQ-9 & GAD-7)
test("MentalAssessments: PHQ-9/GAD-7 create, list, AI reflection update succeed; score tampering & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("mentalAssessments");
  const ts = pastTime(3000);

  // Valid PHQ-9 create
  await assertSucceeds(
    col.doc("phq9_1").set({
      userId: ALICE_UID,
      assessmentType: "PHQ9",
      totalScore: 8,
      maxScore: 27,
      severityBand: "MILD",
      itemScores: [1, 1, 2, 1, 1, 1, 1, 0, 0],
      crisisFlagged: false,
      aiReflection: "",
      aiCopingSteps: [],
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid GAD-7 create
  await assertSucceeds(
    col.doc("gad7_1").set({
      userId: ALICE_UID,
      assessmentType: "GAD7",
      totalScore: 6,
      maxScore: 21,
      severityBand: "MILD",
      itemScores: [1, 1, 1, 1, 0, 1, 1],
      crisisFlagged: false,
      aiReflection: "Mild anxiety signals noted.",
      aiCopingSteps: ["Practice 4-4-4-4 box breathing for 3 minutes"],
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List query matching HealthCloudRepository.observeMentalAssessments
  await assertSucceeds(col.orderBy("recordedAt", "desc").limit(50).get());

  // Updating AI reflection & coping steps succeeds
  await assertSucceeds(
    col.doc("phq9_1").update({
      aiReflection: "Your score suggests mild low mood over the past two weeks. Gentle structure and rest can help.",
      aiCopingSteps: ["Take a 15-minute morning daylight walk", "Protect a consistent sleep wind-down window"],
      updatedAt: pastTime(1000),
    })
  );

  // Score tampering on update fails
  await assertFails(
    col.doc("phq9_1").update({
      totalScore: 0,
      updatedAt: pastTime(500),
    })
  );

  // Invalid score out of bounds fails
  await assertFails(
    col.doc("phq9_bad").set({
      userId: ALICE_UID,
      assessmentType: "PHQ9",
      totalScore: 99,
      maxScore: 27,
      severityBand: "SEVERE",
      itemScores: [3, 3, 3, 3, 3, 3, 3, 3, 3],
      crisisFlagged: true,
      aiReflection: "",
      aiCopingSteps: [],
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("mentalAssessments").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("mentalAssessments").doc("phq9_1").delete());

  // Owner delete succeeds
  await assertSucceeds(col.doc("phq9_1").delete());
});

// 9. Body Measurements (Weight, Height, BMI, Waist)
test("BodyMeasurements: create, list, delete succeed; out-of-bounds, update & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("bodyMeasurements");
  const ts = pastTime(3000);

  await assertSucceeds(
    col.doc("body_1").set({
      userId: ALICE_UID,
      weightKg: 68.5,
      heightCm: 170.0,
      bmi: 23.7,
      waistCm: 78.0,
      note: "Morning weigh-in",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List query matching HealthCloudRepository.observeBodyMeasurements
  await assertSucceeds(col.orderBy("recordedAt", "desc").limit(100).get());

  // Immutable entries (updates denied)
  await assertFails(
    col.doc("body_1").update({
      weightKg: 67.0,
      updatedAt: pastTime(500),
    })
  );

  // Out-of-bounds weight fails
  await assertFails(
    col.doc("body_bad").set({
      userId: ALICE_UID,
      weightKg: 5.0,
      heightCm: 170.0,
      bmi: 1.7,
      waistCm: 0,
      note: "",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bodyMeasurements").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bodyMeasurements").doc("body_1").delete());

  // Owner delete succeeds
  await assertSucceeds(col.doc("body_1").delete());
});

// 10. Optional Cycle Tracking Logs
test("CycleLogs: owner create, update, list, delete succeed; invalid flow, dateKey tampering & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("cycleLogs");
  const ts = pastTime(3000);

  await assertSucceeds(
    col.doc("2026-11-21").set({
      userId: ALICE_UID,
      dateKey: "2026-11-21",
      flowLevel: "MEDIUM",
      symptoms: ["Cramps", "Fatigue"],
      moodTag: "Calm",
      notes: "Warm tea helped.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List query matching HealthCloudRepository.observeCycleLogs
  await assertSucceeds(col.orderBy("dateKey", "desc").limit(120).get());

  // Partial update preserving dateKey & createdAt succeeds
  await assertSucceeds(
    col.doc("2026-11-21").update({
      flowLevel: "LIGHT",
      symptoms: ["Mild Cramps"],
      moodTag: "Rested",
      notes: "Feeling better this evening.",
      updatedAt: pastTime(1000),
    })
  );

  // Invalid flow enum fails
  await assertFails(
    col.doc("2026-11-21").update({
      flowLevel: "EXTREME",
      updatedAt: pastTime(500),
    })
  );

  // Mismatched dateKey on create fails
  await assertFails(
    col.doc("2026-11-22").set({
      userId: ALICE_UID,
      dateKey: "2026-11-21",
      flowLevel: "LIGHT",
      symptoms: [],
      moodTag: "",
      notes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cycleLogs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cycleLogs").doc("2026-11-21").delete());

  // Owner delete succeeds
  await assertSucceeds(col.doc("2026-11-21").delete());
});

// 11. Generative AI Safety Reports (Play Policy Flagging)
test("AiSafetyReports: owner create, read, list, delete succeed; updates, invalid enums & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("aiSafetyReports");
  const ts = pastTime(3000);

  await assertSucceeds(
    col.doc("report_1").set({
      userId: ALICE_UID,
      featureArea: "SYMPTOM_TRIAGE",
      reason: "INACCURATE",
      contentSnippet: "Sample flagged AI output excerpt for review.",
      userNote: "Suggested taking ibuprofen despite allergy listed.",
      createdAt: ts,
    })
  );

  await assertSucceeds(col.doc("report_1").get());
  await assertSucceeds(col.get());

  // Immutable: updates must fail
  await assertFails(
    col.doc("report_1").update({
      userNote: "Tampered note",
    })
  );

  // Invalid featureArea enum fails
  await assertFails(
    col.doc("report_bad_enum").set({
      userId: ALICE_UID,
      featureArea: "UNKNOWN_MODULE",
      reason: "INACCURATE",
      contentSnippet: "Test",
      userNote: "",
      createdAt: ts,
    })
  );

  // Oversized snippet (> 400 chars) fails
  await assertFails(
    col.doc("report_too_long").set({
      userId: ALICE_UID,
      featureArea: "FOLLOW_UP_CHAT",
      reason: "UNSAFE_ADVICE",
      contentSnippet: "x".repeat(450),
      userNote: "",
      createdAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("aiSafetyReports").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("aiSafetyReports").doc("report_1").delete());

  // Owner delete succeeds
  await assertSucceeds(col.doc("report_1").delete());

  // New P1 AI feature areas succeed
  await assertSucceeds(
    col.doc("report_lab").set({
      userId: ALICE_UID,
      featureArea: "LAB_BIOMARKER_SCAN",
      reason: "INACCURATE",
      contentSnippet: "HbA1c parsed unit mismatch.",
      userNote: "",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    col.doc("report_reassess").set({
      userId: ALICE_UID,
      featureArea: "PROACTIVE_REASSESSMENT",
      reason: "MISSED_URGENCY",
      contentSnippet: "Follow-up reassessment note.",
      userNote: "",
      createdAt: ts,
    })
  );

  // New Phase 3 AI feature areas succeed
  await assertSucceeds(
    col.doc("report_soap").set({
      userId: ALICE_UID,
      featureArea: "SOAP_PREVISIT_BRIEF",
      reason: "INACCURATE",
      contentSnippet: "SOAP Assessment omitted recent BP reading.",
      userNote: "",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    col.doc("report_chat_action").set({
      userId: ALICE_UID,
      featureArea: "CHAT_ACTION_SUGGESTION",
      reason: "INACCURATE",
      contentSnippet: "Suggested wrong medication slot.",
      userNote: "",
      createdAt: ts,
    })
  );
});

// 12. Structured Lab Biomarkers (HbA1c, LDL, Vitamin D, TSH)
test("LabBiomarkers: owner create, list, delete succeed; updates, invalid status/value & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("labBiomarkers");
  const ts = pastTime(3000);

  await assertSucceeds(
    col.doc("lab_hba1c_1").set({
      userId: ALICE_UID,
      markerName: "HbA1c",
      category: "METABOLIC",
      value: 5.4,
      unit: "%",
      referenceRangeText: "4.0 - 5.6 %",
      status: "OPTIMAL",
      panelDateKey: "2026-11-24",
      labNote: "Reflects average blood glucose over the past 2-3 months.",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List query matching LabBiomarkerRepository.observeLabBiomarkers
  await assertSucceeds(col.orderBy("recordedAt", "desc").limit(100).get());

  // Immutable: updates must fail
  await assertFails(
    col.doc("lab_hba1c_1").update({
      value: 5.2,
      updatedAt: pastTime(500),
    })
  );

  // Invalid status enum fails
  await assertFails(
    col.doc("lab_bad_status").set({
      userId: ALICE_UID,
      markerName: "Vitamin D",
      category: "VITAMINS",
      value: 34.0,
      unit: "ng/mL",
      referenceRangeText: "30 - 100 ng/mL",
      status: "PERFECT",
      panelDateKey: "2026-11-24",
      labNote: "",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Negative value fails
  await assertFails(
    col.doc("lab_bad_value").set({
      userId: ALICE_UID,
      markerName: "TSH",
      category: "THYROID",
      value: -1.5,
      unit: "mIU/L",
      referenceRangeText: "0.4 - 4.0 mIU/L",
      status: "LOW",
      panelDateKey: "2026-11-24",
      labNote: "",
      recordedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("labBiomarkers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("labBiomarkers").doc("lab_hba1c_1").delete());

  // Owner delete succeeds
  await assertSucceeds(col.doc("lab_hba1c_1").delete());
});

