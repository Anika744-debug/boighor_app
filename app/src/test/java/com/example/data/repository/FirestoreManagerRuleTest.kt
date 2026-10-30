package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserRole
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

class FirestoreManagerRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun testUserSync_AdminEmail_AssignsAdminRole() = runBlocking {
        signInTestUser("mdnajrulislam771@gmail.com")
        val currentUser = auth.currentUser
        assertNotNull(currentUser)

        val manager = FirestoreManager(firestore)
        val user = manager.syncGoogleUser(currentUser!!)

        assertEquals("mdnajrulislam771@gmail.com", user.username)
        assertEquals(UserRole.ADMIN, user.role)
        assertTrue(user.isEmailVerified)
    }

    @Test
    fun testUserSync_RegularReaderEmail_AssignsReaderRole() = runBlocking {
        signInTestUser("reader_guest_1@example.com")
        val currentUser = auth.currentUser
        assertNotNull(currentUser)

        val manager = FirestoreManager(firestore)
        val user = manager.syncGoogleUser(currentUser!!)

        assertEquals("reader_guest_1@example.com", user.username)
        assertEquals(UserRole.READER, user.role)
    }

    @Test
    fun testOwner_CanPromoteReaderToWriter() = runBlocking {
        // First register reader
        signInTestUser("reader_to_promote@example.com")
        val readerUser = auth.currentUser!!
        val manager = FirestoreManager(firestore)
        manager.syncGoogleUser(readerUser)

        // Now sign in as Owner
        signInTestUser("mdnajrulislam771@gmail.com")
        val ownerUser = auth.currentUser!!
        manager.syncGoogleUser(ownerUser)

        // Owner promotes reader to Writer
        val result = manager.updateUserRole(readerUser.uid, UserRole.WRITER)
        assertTrue(result.isSuccess)

        // Read reader doc to verify role is WRITER
        val doc = firestore.collection("users").document(readerUser.uid).get().await()
        assertEquals("WRITER", doc.getString("role"))
    }

    @Test
    fun testOwner_CanCreateStoryAndPartAndSync() = runBlocking {
        signInTestUser("mdnajrulislam771@gmail.com")
        val ownerUser = auth.currentUser!!
        val manager = FirestoreManager(firestore)
        manager.syncGoogleUser(ownerUser)

        val storyId = "story_test_1"
        val story = com.example.data.model.Story(
            id = storyId,
            title = "নীলকণ্ঠ পাখি",
            description = "একটি বিখ্যাত উপন্যাস",
            authorId = ownerUser.uid,
            authorName = "শ্রাবণ মজুমদার"
        )
        val storyResult = manager.createOrUpdateStory(story)
        assertTrue("Story create should succeed", storyResult.isSuccess)

        val partId = "part_test_1"
        val part = com.example.data.model.StoryPart(
            id = partId,
            storyId = storyId,
            partNumber = 1,
            title = "প্রথম অধ্যায়",
            content = "একদিন সকাল বেলা...",
            status = com.example.data.model.PartStatus.PUBLISHED,
            publishedAt = System.currentTimeMillis()
        )
        val partResult = manager.createOrUpdatePart(part)
        assertTrue("Part create should succeed", partResult.isSuccess)

        // Verify Firestore documents
        val storyDoc = firestore.collection("stories").document(storyId).get().await()
        assertTrue(storyDoc.exists())
        assertEquals("নীলকণ্ঠ পাখি", storyDoc.getString("title"))

        val partDoc = firestore.collection("parts").document(partId).get().await()
        assertTrue(partDoc.exists())
        assertEquals("PUBLISHED", partDoc.getString("status"))
    }

    @Test
    fun testSyncAllFromCloud_ReaderGetsPublishedPartsOnly_NoPermissionDenied() = runBlocking {
        // 1. Owner creates a published part and a draft part
        signInTestUser("mdnajrulislam771@gmail.com")
        val ownerUser = auth.currentUser!!
        val manager = FirestoreManager(firestore)
        manager.syncGoogleUser(ownerUser)

        val storyId = "story_sync_test"
        val story = com.example.data.model.Story(
            id = storyId,
            title = "গল্পের আসর",
            description = "বিবরণ",
            authorId = ownerUser.uid,
            authorName = "লেখক"
        )
        manager.createOrUpdateStory(story)

        val pubPart = com.example.data.model.StoryPart(
            id = "part_pub_1",
            storyId = storyId,
            partNumber = 1,
            title = "প্রকাশিত পর্ব",
            content = "পড়ার জন্য উন্মুক্ত",
            status = com.example.data.model.PartStatus.PUBLISHED
        )
        manager.createOrUpdatePart(pubPart)

        val draftPart = com.example.data.model.StoryPart(
            id = "part_draft_1",
            storyId = storyId,
            partNumber = 2,
            title = "খসড়া পর্ব",
            content = "এখনো প্রকাশিত নয়",
            status = com.example.data.model.PartStatus.DRAFT
        )
        manager.createOrUpdatePart(draftPart)

        // 2. Now a Reader signs in
        signInTestUser("reader_sync_user@example.com")
        val readerUser = auth.currentUser!!
        val readerManager = FirestoreManager(firestore)
        readerManager.syncGoogleUser(readerUser)

        // 3. Reader syncs from cloud: must succeed and only retrieve PUBLISHED parts
        val syncedStories = mutableListOf<com.example.data.model.Story>()
        val syncedParts = mutableListOf<com.example.data.model.StoryPart>()
        val syncedReviews = mutableListOf<com.example.data.model.PassageReview>()

        readerManager.syncAllFromCloud(
            onStories = { syncedStories.addAll(it) },
            onParts = { syncedParts.addAll(it) },
            onReviews = { syncedReviews.addAll(it) }
        )

        assertTrue(syncedStories.any { it.id == storyId })
        assertTrue(syncedParts.any { it.id == "part_pub_1" })
        assertFalse("Draft parts must NOT be visible to reader", syncedParts.any { it.id == "part_draft_1" })
    }

    @Test
    fun testDeleteStoryAndPart_RemovesFromCloud() = runBlocking {
        signInTestUser("mdnajrulislam771@gmail.com")
        val ownerUser = auth.currentUser!!
        val manager = FirestoreManager(firestore)
        manager.syncGoogleUser(ownerUser)

        val storyId = "story_to_delete"
        val partId = "part_to_delete"
        manager.createOrUpdateStory(com.example.data.model.Story(id = storyId, title = "To Delete", description = "Desc", authorId = ownerUser.uid, authorName = "Author"))
        manager.createOrUpdatePart(com.example.data.model.StoryPart(id = partId, storyId = storyId, partNumber = 1, title = "Part To Delete", content = "Content to delete", status = com.example.data.model.PartStatus.PUBLISHED))

        // Delete from cloud
        val deletePartRes = manager.deletePart(partId)
        assertTrue(deletePartRes.isSuccess)
        val deleteStoryRes = manager.deleteStory(storyId)
        assertTrue(deleteStoryRes.isSuccess)

        val storyCheck = firestore.collection("stories").document(storyId).get().await()
        assertFalse(storyCheck.exists())
        val partCheck = firestore.collection("parts").document(partId).get().await()
        assertFalse(partCheck.exists())
    }

    @Test
    fun testSequentialReadingAccess_ReadersMustCompletePriorPartsOrHaveUnlock() {
        val reader = com.example.data.model.User(
            id = "reader_test_user",
            username = "public_reader@example.com",
            passwordHash = "",
            displayName = "সাধারণ পাঠক",
            role = UserRole.READER
        )

        val story = com.example.data.model.Story(
            id = "pub_story_test",
            title = "জনপ্রিয় গল্প",
            description = "বিবরণ",
            authorId = "writer_uid",
            authorName = "লেখক",
            sequentialReadingEnabled = true,
            isPublished = true
        )

        val publishedPart1 = com.example.data.model.StoryPart(
            id = "part_pub_1",
            storyId = story.id,
            partNumber = 1,
            title = "প্রথম পর্ব",
            content = "প্রথম পর্বের গল্প...",
            status = com.example.data.model.PartStatus.PUBLISHED
        )

        val publishedPart2 = com.example.data.model.StoryPart(
            id = "part_pub_2",
            storyId = story.id,
            partNumber = 2,
            title = "দ্বিতীয় পর্ব",
            content = "দ্বিতীয় পর্বের গল্প...",
            status = com.example.data.model.PartStatus.PUBLISHED
        )

        val draftPart = com.example.data.model.StoryPart(
            id = "part_draft_3",
            storyId = story.id,
            partNumber = 3,
            title = "তৃতীয় পর্ব (খসড়া)",
            content = "খসড়া বিষয়বস্তু...",
            status = com.example.data.model.PartStatus.DRAFT
        )

        val allParts = listOf(publishedPart1, publishedPart2, draftPart)

        // 1. Reader without progress can access published part 1
        val isPart1Accessible = com.example.util.SequentialReadingHelper.isPartAccessible(
            story = story,
            part = publishedPart1,
            currentUser = reader,
            allPartsForStory = allParts
        )
        assertTrue("Reader must have access to published part 1", isPart1Accessible)

        // 2. Reader without progress CANNOT access published part 2 (locked)
        val isPart2InitiallyAccessible = com.example.util.SequentialReadingHelper.isPartAccessible(
            story = story,
            part = publishedPart2,
            currentUser = reader,
            allPartsForStory = allParts
        )
        assertFalse("Reader must NOT have access to part 2 before part 1 is completed", isPart2InitiallyAccessible)

        // 3. When reader completes Part 1, Part 2 becomes accessible
        val completedProgressPart1 = listOf(
            com.example.data.model.ReadingProgress(
                id = "${reader.id}_${story.id}_${publishedPart1.id}",
                userId = reader.id,
                storyId = story.id,
                partId = publishedPart1.id,
                progressPercent = 100,
                isCompleted = true
            )
        )
        val isPart2AccessibleAfterPart1 = com.example.util.SequentialReadingHelper.isPartAccessible(
            story = story,
            part = publishedPart2,
            currentUser = reader,
            userReadingProgress = completedProgressPart1,
            allPartsForStory = allParts
        )
        assertTrue("Reader must have access to part 2 once part 1 is completed", isPart2AccessibleAfterPart1)

        // 4. Alternatively, manual unlock gives access even if part 1 is incomplete
        val overridePart2 = listOf(
            com.example.data.model.UserAccessOverride(
                id = "${reader.id}_${publishedPart2.id}",
                userId = reader.id,
                storyId = story.id,
                partId = publishedPart2.id,
                isUnlocked = true
            )
        )
        val isPart2AccessibleWithOverride = com.example.util.SequentialReadingHelper.isPartAccessible(
            story = story,
            part = publishedPart2,
            currentUser = reader,
            userOverrides = overridePart2,
            allPartsForStory = allParts
        )
        assertTrue("Reader must have access to part 2 when manual override is present", isPart2AccessibleWithOverride)

        // 5. Draft part must NEVER be accessible to reader
        val isDraftAccessible = com.example.util.SequentialReadingHelper.isPartAccessible(
            story = story,
            part = draftPart,
            currentUser = reader,
            allPartsForStory = allParts
        )
        assertFalse("Reader must NEVER have access to unpublished draft part", isDraftAccessible)

        // 6. getVisiblePartsForUser hides future locked parts for a new reader
        val visibleParts = com.example.util.SequentialReadingHelper.getVisiblePartsForUser(
            story = story,
            allPartsForStory = allParts,
            currentUser = reader
        )
        assertEquals(1, visibleParts.size)
        assertEquals("part_pub_1", visibleParts.first().id)
    }
}

