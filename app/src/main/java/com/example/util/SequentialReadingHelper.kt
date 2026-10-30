package com.example.util

import com.example.data.model.*

/**
 * Core business logic helper for Sequential Reading Mode, Future Part Hiding,
 * Access Verification, and Configurable Completion Validation.
 */
object SequentialReadingHelper {

    /**
     * Determines whether a given user is allowed to access and read a specific part.
     * Sequential Reading Mode & Gating:
     * - Admin/Owner has unrestricted access to all parts (including drafts and scheduled).
     * - The Writer/Author has unrestricted access to their own story parts.
     * - For ordinary Readers:
     *   - Draft and future scheduled parts are never accessible.
     *   - Story and part must be published.
     *   - If sequential reading is enabled:
     *     - Part 1 is always accessible.
     *     - Part N (> 1) requires prior part completion (progress.isCompleted == true)
     *       OR explicit manual unlock/override (userOverrides or isManuallyUnlocked).
     *   - If sequential reading is disabled, all published parts are accessible.
     */
    fun isPartAccessible(
        story: Story?,
        part: StoryPart?,
        currentUser: User?,
        userReadingProgress: List<ReadingProgress> = emptyList(),
        userOverrides: List<UserAccessOverride> = emptyList(),
        allPartsForStory: List<StoryPart> = emptyList()
    ): Boolean {
        if (story == null || part == null) return false

        // Owner/Admin has global access to all parts (including drafts and scheduled)
        if (currentUser?.role == UserRole.ADMIN) return true

        // Writer can access their own story parts (including drafts and scheduled)
        if (currentUser?.role == UserRole.WRITER && story.authorId == currentUser.id) return true

        // Drafts and future scheduled parts can NEVER be accessed by ordinary readers
        val isPartPublished = part.status == PartStatus.PUBLISHED &&
                (part.scheduledAt == null || part.scheduledAt <= System.currentTimeMillis()) &&
                !part.isDeleted
        if (!isPartPublished) return false

        // Story itself must be published
        val isStoryPublished = story.isPublished && !story.isDeleted
        if (!isStoryPublished) return false

        // If sequential reading is disabled for this story, all published parts are accessible
        if (!story.sequentialReadingEnabled) return true

        // Published parts ordered by partNumber
        val publishedParts = allPartsForStory
            .filter { it.status == PartStatus.PUBLISHED && (it.scheduledAt == null || it.scheduledAt <= System.currentTimeMillis()) && !it.isDeleted }
            .sortedBy { it.partNumber }

        // The first published part (Part 1) is always accessible
        val firstPart = publishedParts.firstOrNull() ?: part
        if (part.id == firstPart.id || part.partNumber <= firstPart.partNumber) {
            return true
        }

        // Check for author/admin manual unlock override
        val hasManualOverride = userOverrides.any {
            it.userId == currentUser?.id &&
            it.storyId == story.id &&
            (it.partId == part.id || it.partId == null) &&
            it.isUnlocked
        }
        if (hasManualOverride) return true

        // Check if explicitly unlocked in reading progress
        val currentPartProgress = userReadingProgress.find { it.userId == currentUser?.id && it.partId == part.id }
        if (currentPartProgress?.isManuallyUnlocked == true) return true

        // Check sequential reading progression: preceding published part must be completed
        val previousPart = publishedParts
            .filter { it.partNumber < part.partNumber }
            .maxByOrNull { it.partNumber }

        if (previousPart == null) return true

        val prevProgress = userReadingProgress.find { it.userId == currentUser?.id && it.partId == previousPart.id }
        return prevProgress?.isCompleted == true || prevProgress?.isManuallyUnlocked == true
    }

    /**
     * Returns the parts that should be visible to the user.
     * - Writer / Admin sees all non-deleted parts (including drafts and scheduled).
     * - If sequential reading is disabled, readers see all published parts.
     * - If sequential reading is enabled, future locked parts are hidden, returning only accessible parts.
     */
    fun getVisiblePartsForUser(
        story: Story?,
        allPartsForStory: List<StoryPart>,
        currentUser: User?,
        userReadingProgress: List<ReadingProgress> = emptyList(),
        userOverrides: List<UserAccessOverride> = emptyList()
    ): List<StoryPart> {
        if (story == null) return emptyList()

        // Writer / Admin sees all non-deleted parts
        val isPrivileged = currentUser?.role == UserRole.ADMIN ||
                (currentUser?.role == UserRole.WRITER && story.authorId == currentUser?.id)
        if (isPrivileged) {
            return allPartsForStory.filter { !it.isDeleted }.sortedBy { it.partNumber }
        }

        val publishedParts = allPartsForStory
            .filter { it.status == PartStatus.PUBLISHED && (it.scheduledAt == null || it.scheduledAt <= System.currentTimeMillis()) && !it.isDeleted }
            .sortedBy { it.partNumber }

        if (!story.sequentialReadingEnabled) {
            return publishedParts
        }

        // Return accessible parts for sequential reading (future locked parts hidden)
        return publishedParts.filter { part ->
            isPartAccessible(
                story = story,
                part = part,
                currentUser = currentUser,
                userReadingProgress = userReadingProgress,
                userOverrides = userOverrides,
                allPartsForStory = publishedParts
            )
        }
    }

    /**
     * Checks if a reader's reading activity on a part meets all completion criteria configured for the story.
     */
    fun isPartCompletionSatisfied(
        story: Story,
        progressPercent: Int,
        activeDurationSeconds: Long,
        reachedEndOfPart: Boolean
    ): Boolean {
        // If reader has completed 90%+ or reached the end of the text, consider it satisfied!
        if (progressPercent >= 90 || reachedEndOfPart) {
            return true
        }

        // 1. Minimum reading percentage check
        if (progressPercent < story.minReadingPercent) return false

        // 2. Active reading time verification check (if enabled and not at end)
        if (story.requireActiveReadingTime && activeDurationSeconds < story.minActiveReadingSeconds) {
            return false
        }

        // 3. Reached end of part check (if enabled)
        if (story.requireEndOfPart && !reachedEndOfPart) {
            return false
        }

        return true
    }
}
