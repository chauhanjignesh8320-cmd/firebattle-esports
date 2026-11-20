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
const ADMIN_UID = "admin_owner_001";
const ADMIN_EMAIL = "chauhanjignesh8320@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("attacker_999", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
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

test("Unauthenticated user is denied all reads and writes", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("matches").where("status", "==", "LIVE").get());
  await assertFails(unauth.collection("settings").doc("app").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "a@b.com",
      ign: "AliceFF",
      gameUid: "123456",
      phone: "9876543210",
      wallet: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Spoofed admin with unverified email is rejected", async () => {
  const spoofDb = spoofedAdminContext().firestore();
  await assertFails(
    spoofDb.collection("settings").doc("app").set({
      upiId: "hack@upi",
      upiName: "Hacker",
      adminPhone: "9999999999",
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    spoofDb.collection("matches").doc("match_1").set({
      title: "Fake Match",
      game: "Free Fire MAX",
      matchTime: "8:30 PM",
      mapName: "Bermuda",
      entry: 20,
      perKill: 10,
      prize: 500,
      slots: 48,
      joinedCount: 0,
      roomId: "",
      roomPass: "",
      status: "LIVE",
      createdBy: "attacker_999",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Player profile creation, isolation, and wallet protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true }).firestore();

  // Cannot create profile with non-zero wallet
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      ign: "AliceFF",
      gameUid: "99887766",
      phone: "9876543210",
      wallet: 500,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Valid create with wallet == 0
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      ign: "AliceFF",
      gameUid: "99887766",
      phone: "9876543210",
      wallet: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Valid profile update without changing wallet
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      ign: "AlicePro",
      updatedAt: pastDate(1000),
    })
  );

  // Cannot inflate own wallet
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wallet: 1000,
      updatedAt: pastDate(500),
    })
  );

  // Bob cannot read Alice's PII or list users
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Admin manages settings, matches, deposits, withdrawals, and user wallets", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();

  // Admin writes settings/app
  await assertSucceeds(
    adminDb.collection("settings").doc("app").set({
      upiId: "firebattle@upi",
      upiName: "Firebattle Esports",
      adminPhone: "9876543210",
      updatedAt: pastDate(),
    })
  );

  // Signed-in player can get settings/app, cannot list settings or write settings
  await assertSucceeds(aliceDb.collection("settings").doc("app").get());
  await assertFails(aliceDb.collection("settings").get());
  await assertFails(
    aliceDb.collection("settings").doc("app").update({
      upiId: "evil@upi",
      updatedAt: pastDate(1000),
    })
  );

  // Admin creates LIVE match
  await assertSucceeds(
    adminDb.collection("matches").doc("match_1").set({
      title: "Solo Bermuda #1",
      game: "Free Fire MAX",
      matchTime: "Today 8:30 PM",
      mapName: "Bermuda",
      entry: 50,
      perKill: 25,
      prize: 1000,
      slots: 48,
      joinedCount: 0,
      roomId: "",
      roomPass: "",
      status: "LIVE",
      createdBy: ADMIN_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Player query aligned with rules (.where status == LIVE) succeeds; unfiltered list fails
  await assertSucceeds(aliceDb.collection("matches").where("status", "==", "LIVE").get());
  await assertFails(aliceDb.collection("matches").get());

  // Admin updates Room ID & Password
  await assertSucceeds(
    adminDb.collection("matches").doc("match_1").update({
      roomId: "884422",
      roomPass: "FIRE99",
      updatedAt: pastDate(1000),
    })
  );
});

test("Atomic join match deducts wallet, increments joinedCount, and prevents free joins", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      ign: "AliceFF",
      gameUid: "99887766",
      phone: "9876543210",
      wallet: 100,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("matches").doc("match_1").set({
      title: "Solo Bermuda #1",
      game: "Free Fire MAX",
      matchTime: "Today 8:30 PM",
      mapName: "Bermuda",
      entry: 40,
      perKill: 20,
      prize: 800,
      slots: 48,
      joinedCount: 0,
      roomId: "",
      roomPass: "",
      status: "LIVE",
      createdBy: ADMIN_UID,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  // Fail if trying to create registration without deducting wallet & incrementing joinedCount
  await assertFails(
    aliceDb.collection("registrations").doc(`match_1_${ALICE_UID}`).set({
      matchId: "match_1",
      userId: ALICE_UID,
      ign: "AliceFF",
      gameUid: "99887766",
      phone: "9876543210",
      entryPaid: 40,
      createdAt: pastDate(1000),
    })
  );

  // Atomic batch join succeeds
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("registrations").doc(`match_1_${ALICE_UID}`), {
    matchId: "match_1",
    userId: ALICE_UID,
    ign: "AliceFF",
    gameUid: "99887766",
    phone: "9876543210",
    entryPaid: 40,
    createdAt: pastDate(1000),
  });
  batch.update(aliceDb.collection("matches").doc("match_1"), {
    joinedCount: 1,
    updatedAt: pastDate(1000),
  });
  batch.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 60,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Duplicate join fails
  const dupBatch = aliceDb.batch();
  dupBatch.set(aliceDb.collection("registrations").doc(`match_1_${ALICE_UID}`), {
    matchId: "match_1",
    userId: ALICE_UID,
    ign: "AliceFF",
    gameUid: "99887766",
    phone: "9876543210",
    entryPaid: 40,
    createdAt: pastDate(500),
  });
  dupBatch.update(aliceDb.collection("matches").doc("match_1"), {
    joinedCount: 2,
    updatedAt: pastDate(500),
  });
  dupBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 20,
    updatedAt: pastDate(500),
  });
  await assertFails(dupBatch.commit());

  // Scoped registration query succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("registrations").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("registrations").get());
  await assertSucceeds(adminDb.collection("registrations").get());
});

test("Deposit & Withdrawal workflow with atomic wallet balance guards", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      ign: "AliceFF",
      gameUid: "99887766",
      phone: "9876543210",
      wallet: 100,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  // Player submits deposit request (cannot self-approve)
  await assertFails(
    aliceDb.collection("deposits").doc("dep_bad").set({
      userId: ALICE_UID,
      phone: "9876543210",
      ign: "AliceFF",
      amount: 200,
      utr: "123456789012",
      status: "Approved",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("deposits").doc("dep_1").set({
      userId: ALICE_UID,
      phone: "9876543210",
      ign: "AliceFF",
      amount: 200,
      utr: "123456789012",
      status: "Pending",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Scoped query works, unfiltered fails for player
  await assertSucceeds(aliceDb.collection("deposits").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("deposits").get());

  // Admin approves deposit atomically
  const approveBatch = adminDb.batch();
  approveBatch.update(adminDb.collection("deposits").doc("dep_1"), {
    status: "Approved",
    updatedAt: pastDate(500),
  });
  approveBatch.update(adminDb.collection("users").doc(ALICE_UID), {
    wallet: 300,
    updatedAt: pastDate(500),
  });
  await assertSucceeds(approveBatch.commit());

  // Withdrawal without deducting wallet fails
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_bad").set({
      userId: ALICE_UID,
      phone: "9876543210",
      ign: "AliceFF",
      amount: 150,
      upiId: "alice@ybl",
      status: "Pending",
      createdAt: pastDate(400),
      updatedAt: pastDate(400),
    })
  );

  // Atomic withdrawal request deducts wallet from 300 -> 150
  const wdBatch = aliceDb.batch();
  wdBatch.set(aliceDb.collection("withdrawals").doc("wd_1"), {
    userId: ALICE_UID,
    phone: "9876543210",
    ign: "AliceFF",
    amount: 150,
    upiId: "alice@ybl",
    status: "Pending",
    createdAt: pastDate(300),
    updatedAt: pastDate(300),
  });
  wdBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 150,
    updatedAt: pastDate(300),
  });
  await assertSucceeds(wdBatch.commit());

  // Admin rejects and refunds withdrawal (150 -> 300)
  const rejectBatch = adminDb.batch();
  rejectBatch.update(adminDb.collection("withdrawals").doc("wd_1"), {
    status: "Rejected",
    updatedAt: pastDate(100),
  });
  rejectBatch.update(adminDb.collection("users").doc(ALICE_UID), {
    wallet: 300,
    updatedAt: pastDate(100),
  });
  await assertSucceeds(rejectBatch.commit());
});
