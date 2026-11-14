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
const ADMIN_EMAIL = "fucksgonze@gmail.com";

const ts = () => new Date(Date.now() - 1000);

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function adminContext(verified = true) {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: verified,
  });
}

test("Unauthenticated user: cannot read users, public_profiles, posts, or reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("public_profiles").where("accountStatus", "==", "active").get());
  await assertFails(unauthDb.collection("posts").where("status", "==", "active").get());
  await assertFails(unauthDb.collection("reports").get());
});

test("Ordinary user: can register own profile with role=user, cannot self-assign role=admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      username: "alice_w",
      email: "alice@example.com",
      dateOfBirth: "1998-05-12",
      role: "admin",
      status: "active",
      termsAccepted: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      username: "alice_w",
      email: "alice@example.com",
      dateOfBirth: "1998-05-12",
      role: "user",
      status: "active",
      termsAccepted: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Ordinary user: cannot elevate role/status or inject ghost fields on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    fullName: "Alice Wonder",
    username: "alice_w",
    email: "alice@example.com",
    dateOfBirth: "1998-05-12",
    role: "user",
    status: "active",
    termsAccepted: true,
    createdAt: ts(),
    updatedAt: ts(),
  });

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: ts(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      ghostField: true,
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Coffee & sunsets",
      updatedAt: ts(),
    })
  );
});

test("PII isolation: Bob cannot read Alice private /users document or list /users", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      username: "alice_w",
      email: "alice@example.com",
      dateOfBirth: "1998-05-12",
      role: "user",
      status: "active",
      termsAccepted: true,
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Email spoofing attack: unverified admin email is rejected from admin routes and admin bootstrap", async () => {
  const spoofDb = adminContext(false).firestore();

  await assertFails(
    spoofDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      displayName: "Spoof",
      grantedBy: "self",
      createdAt: ts(),
    })
  );
  await assertFails(spoofDb.collection("users").get());
  await assertFails(spoofDb.collection("admin_logs").get());
});

test("Verified designated admin: can bootstrap admin record, manage users, resolve reports, and log actions", async () => {
  const admDb = adminContext(true).firestore();

  await assertSucceeds(
    admDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      displayName: "Darling Admin",
      grantedBy: "primary_designated_admin",
      createdAt: ts(),
    })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Wonder",
      username: "alice_w",
      email: "alice@example.com",
      dateOfBirth: "1998-05-12",
      role: "user",
      status: "active",
      termsAccepted: true,
      createdAt: ts(),
      updatedAt: ts(),
    });
    await ctx.firestore().collection("reports").doc("rep_1").set({
      reporterId: BOB_UID,
      targetType: "user",
      targetId: ALICE_UID,
      reason: "Spam",
      status: "pending",
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  await assertSucceeds(admDb.collection("users").get());
  await assertSucceeds(
    admDb.collection("users").doc(ALICE_UID).update({
      status: "suspended",
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    admDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      resolutionNote: "Account suspended",
      resolvedBy: ADMIN_UID,
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    admDb.collection("admin_logs").doc("log_1").set({
      adminId: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "SUSPEND_USER",
      targetType: "user",
      targetId: ALICE_UID,
      details: "Suspended for spam",
      createdAt: ts(),
    })
  );
});

test("Posts & query alignment: active query succeeds, unfiltered query fails for ordinary user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_w",
      authorName: "Alice Wonder",
      mediaType: "text",
      caption: "Hello Darling Date!",
      likesCount: 0,
      commentsCount: 0,
      isReel: false,
      status: "active",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("posts").where("status", "==", "active").get()
  );
  await assertFails(aliceDb.collection("posts").get());
});
