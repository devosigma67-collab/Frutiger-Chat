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

const nowTimestamp = () => new Date();

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

test("Unauthenticated user cannot read or write users, friends, chats, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("friends").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(
    unauthDb.collection("chats").doc("chat_1").collection("messages").get()
  );
});

test("Authenticated user can create their own profile and view other users for live sync", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates her profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AeroAlice",
      signature: "feeling bubbly and glossy ~",
      presenceStatus: "online",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  // Bob can discover and live-sync Alice from the directory
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(bobDb.collection("users").get());

  // Alice cannot overwrite Bob's profile
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "SpoofBob",
    })
  );
});

test("Authenticated user can add friends, create chats, and exchange messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice adds Bob as a friend
  await assertSucceeds(
    aliceDb.collection("friends").doc("friend_bob").set({
      userId: ALICE_UID,
      friendUid: BOB_UID,
      friendUsername: "BrotherBob",
      createdAt: nowTimestamp(),
    })
  );

  // Alice creates a chat room with participantUids
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_alice_bob").set({
      ownerId: ALICE_UID,
      participantUids: [ALICE_UID, BOB_UID],
      title: "BrotherBob",
      isGroup: false,
      lastMessagePreview: "Hello!",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  // Bob can see the chat room
  await assertSucceeds(bobDb.collection("chats").doc("chat_alice_bob").get());

  // Alice sends an encrypted message
  await assertSucceeds(
    aliceDb
      .collection("chats")
      .doc("chat_alice_bob")
      .collection("messages")
      .doc("msg_1")
      .set({
        chatId: "chat_alice_bob",
        ownerId: ALICE_UID,
        participantUids: [ALICE_UID, BOB_UID],
        senderId: ALICE_UID,
        senderName: "AeroAlice",
        ciphertext: "U2FsdGVkX1+vupppZksvRf5pq5g5XjFRlipRkwB0K1Y=",
        iv: "MTIzNDU2Nzg5MDEy",
        messageType: "text",
        deliveryStatus: "sent",
        createdAt: nowTimestamp(),
      })
  );

  // Bob can read the message
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc("chat_alice_bob")
      .collection("messages")
      .doc("msg_1")
      .get()
  );
});
