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
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc("any_user").get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Donor",
      email: "alice@example.com",
      phone: "+1234567890",
      bloodGroup: "O+",
      city: "Springfield",
      district: "Central",
      radiusKm: 25,
      notificationsEnabled: true,
      isAvailable: true,
      isVerifiedDonor: false,
      role: "donor",
      donationCount: 0,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create profile with another user ID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob Impersonator",
      email: "bob@example.com",
      phone: "+1234567890",
      bloodGroup: "A+",
      city: "Springfield",
      district: "Central",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create blood request with status pending", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("bloodRequests").doc("req_1").set({
      id: "req_1",
      requesterId: ALICE_UID,
      requesterName: "Alice Requester",
      bloodGroup: "B+",
      units: 2,
      hospitalName: "Memorial Hospital",
      city: "Springfield",
      district: "Central",
      urgency: "Urgent",
      status: "pending",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: non-admin cannot verify their own request directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("bloodRequests").doc("req_2").set({
      id: "req_2",
      requesterId: ALICE_UID,
      requesterName: "Alice Requester",
      bloodGroup: "B+",
      units: 2,
      hospitalName: "Memorial Hospital",
      city: "Springfield",
      district: "Central",
      urgency: "Urgent",
      status: "verified",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can read verified blood requests", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("bloodRequests").doc("req_verified").set({
      id: "req_verified",
      requesterId: BOB_UID,
      requesterName: "Bob",
      bloodGroup: "O-",
      units: 1,
      hospitalName: "General Hospital",
      city: "Springfield",
      urgency: "Critical",
      status: "verified",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("bloodRequests").doc("req_verified").get()
  );
});

test("Authenticated user: can create donation record for own self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("donations").doc("don_1").set({
      id: "don_1",
      userId: ALICE_UID,
      hospitalName: "Metro Blood Bank",
      bloodGroup: "O+",
      units: 1,
      donationDate: "2026-10-22",
      status: "completed",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can submit report against a request", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      id: "rep_1",
      reporterId: ALICE_UID,
      requestId: "req_fake",
      reason: "Invalid contact number, hospital reports no such patient",
      status: "open",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create own coin wallet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("coinWallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      balance: 20,
      totalEarned: 20,
      totalRedeemed: 0,
    })
  );
});

test("Authenticated user: cannot read another user's wallet", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("coinWallets").doc(BOB_UID).set({
      userId: BOB_UID,
      balance: 100,
      totalEarned: 100,
      totalRedeemed: 0,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("coinWallets").doc(BOB_UID).get());
});

test("Authenticated user: can create coin earn transaction for themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("coinTransactions").doc("tx_1").set({
      id: "tx_1",
      userId: ALICE_UID,
      transactionType: "EARN",
      amount: 20,
      reason: "Profile verification bonus",
      referenceId: "profile_init",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create redemption record", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("redemptions").doc("red_1").set({
      id: "red_1",
      userId: ALICE_UID,
      rewardId: "theme_gold",
      rewardTitle: "Golden Lifesaver Theme",
      coinsSpent: 50,
      redemptionStatus: "completed",
      createdAt: new Date(),
    })
  );
});

