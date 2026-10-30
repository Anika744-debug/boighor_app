const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085
    }
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

test("Unauthenticated user cannot read users", async () => {
  const unauthContext = testEnv.unauthenticatedContext();
  const db = unauthContext.firestore();
  await assertFails(db.collection("users").get());
});

test("Normal user can register with READER role only", async () => {
  const userContext = testEnv.authenticatedContext("reader_1", {
    email: "reader1@example.com"
  });
  const db = userContext.firestore();
  await assertSucceeds(
    db.collection("users").doc("reader_1").set({
      uid: "reader_1",
      email: "reader1@example.com",
      displayName: "Reader One",
      role: "READER",
      isSuspended: false
    })
  );

  // Attempting to self-promote to WRITER must fail
  await assertFails(
    db.collection("users").doc("reader_2").set({
      uid: "reader_2",
      email: "reader2@example.com",
      displayName: "Reader Two",
      role: "WRITER",
      isSuspended: false
    })
  );
});

test("Designated Owner can register with OWNER role and promote users", async () => {
  const ownerContext = testEnv.authenticatedContext("owner_1", {
    email: "mdnajrulislam771@gmail.com"
  });
  const db = ownerContext.firestore();
  await assertSucceeds(
    db.collection("users").doc("owner_1").set({
      uid: "owner_1",
      email: "mdnajrulislam771@gmail.com",
      displayName: "Najrul Islam",
      role: "OWNER",
      isSuspended: false
    })
  );

  // Setup a reader document
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("reader_user").set({
      uid: "reader_user",
      email: "reader@example.com",
      displayName: "Some Reader",
      role: "READER",
      isSuspended: false
    });
  });

  // Owner can promote reader to WRITER
  await assertSucceeds(
    db.collection("users").doc("reader_user").update({
      role: "WRITER"
    })
  );
});

test("Reader cannot read DRAFT part of another author", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("parts").doc("part_draft").set({
      id: "part_draft",
      storyId: "story_1",
      authorId: "writer_author",
      partNumber: 1,
      title: "Draft Episode",
      status: "DRAFT"
    });
  });

  const readerContext = testEnv.authenticatedContext("reader_bob", {
    email: "bob@example.com"
  });
  await assertFails(readerContext.firestore().collection("parts").doc("part_draft").get());

  const authorContext = testEnv.authenticatedContext("writer_author", {
    email: "author@example.com"
  });
  await assertSucceeds(authorContext.firestore().collection("parts").doc("part_draft").get());
});

test("User cannot read or write another user's reading progress", async () => {
  const aliceContext = testEnv.authenticatedContext("alice", { email: "alice@example.com" });
  await assertSucceeds(
    aliceContext.firestore().collection("readingProgress").doc("prog_alice").set({
      id: "prog_alice",
      userId: "alice",
      storyId: "s1",
      partId: "p1",
      progressPercent: 50
    })
  );

  const bobContext = testEnv.authenticatedContext("bob", { email: "bob@example.com" });
  await assertFails(bobContext.firestore().collection("readingProgress").doc("prog_alice").get());
  await assertFails(
    bobContext.firestore().collection("readingProgress").doc("prog_alice").update({
      progressPercent: 100
    })
  );
});

test("Public story access: All authenticated readers can read published stories and published parts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stories").doc("pub_story_1").set({
      id: "pub_story_1",
      title: "জনপ্রিয় গল্প",
      authorId: "writer_author",
      authorName: "লেখক",
      isPublished: true
    });
    await context.firestore().collection("stories").doc("draft_story_1").set({
      id: "draft_story_1",
      title: "অপ্রকাশিত গল্প",
      authorId: "writer_author",
      authorName: "লেখক",
      isPublished: false
    });
    await context.firestore().collection("parts").doc("pub_part_1").set({
      id: "pub_part_1",
      storyId: "pub_story_1",
      authorId: "writer_author",
      partNumber: 1,
      title: "পর্ব ১",
      status: "PUBLISHED"
    });
  });

  const readerContext = testEnv.authenticatedContext("reader_public", {
    email: "reader_public@example.com"
  });
  const readerDb = readerContext.firestore();

  // Published story and part must be readable by ordinary reader
  await assertSucceeds(readerDb.collection("stories").doc("pub_story_1").get());
  await assertSucceeds(readerDb.collection("parts").doc("pub_part_1").get());

  // Draft story must NOT be readable by ordinary reader
  await assertFails(readerDb.collection("stories").doc("draft_story_1").get());

  // Unauthenticated user cannot read published stories
  const unauthContext = testEnv.unauthenticatedContext();
  await assertFails(unauthContext.firestore().collection("stories").doc("pub_story_1").get());
});

