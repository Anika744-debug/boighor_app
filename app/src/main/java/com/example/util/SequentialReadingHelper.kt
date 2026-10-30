package com.example.util

import com.example.data.model.*

/**
 * Core business logic helper for Sequential Reading Mode, Future Part Hiding,
 * Access Verification, and Configurable Completion Validation.
 */
object SequentialReadingHelper {

    /**
     * Determines whether a given user is allowed to access and read a specific part.
     */
    fun isPartAccessible(
        story: Story?,
        part: StoryPart?,
        currentUser: User?,
        userReadingProgress: List<ReadingProgress>,
        userOverrides: List<UserAccessOverride>,
        allPartsForStory: List<StoryPart>
    ): Boolean {
        if (story == null || part == null) return false

        // Owner/Admin has global access to all parts
        if (currentUser?.role == UserRole.ADMIN) return true

        // Writer can access their own story parts
        if (currentUser?.role == UserRole.WRITER && story.authorId == currentUser.id) return true

        // A reader can NEVER access an unpublished part or future scheduled part,
        // even if a manual override was attempted!
        val isPublished = part.status == PartStatus.PUBLISHED &&
                (part.scheduledAt == null || part.scheduledAt <= System.currentTimeMillis())
        if (!isPublished) return false

        // If sequential reading is disabled for this story, all published parts are accessible
        if (!story.sequentialReadingEnabled) return true

        val userId = currentUser?.id ?: "guest"

        // 1. Check if reader has an active manual unlock for this part
        if (userOverrides.any { (it.userId == userId || it.userId == "guest") && it.partId == part.id && it.isUnlocked }) {
            return true
        }

        // 2. Check if this part has already been completed by the reader (allowed to reread)
        val partProgress = userReadingProgress.find { (it.userId == userId || it.userId == "guest" || currentUser == null) && it.partId == part.id }
        if (partProgress != null && (partProgress.isCompleted || partProgress.progressPercent >= 90 || partProgress.reachedEndOfPart)) {
            return true
        }

        // 3. Sequential ordering: find published parts sorted by partNumber
        val publishedParts = allPartsForStory
            .filter { it.status == PartStatus.PUBLISHED && (it.scheduledAt == null || it.scheduledAt <= System.currentTimeMillis()) && !it.isDeleted }
            .sortedBy { it.partNumber }

        if (publishedParts.isEmpty()) return false

        // The very first published part is ALWAYS accessible to any reader
        if (publishedParts.first().id == part.id) return true

        // For any subsequent part, check if the immediately preceding published part is completed or reached end
        val partIndex = publishedParts.indexOfFirst { it.id == part.id }
        if (partIndex > 0) {
            val previousPart = publishedParts[partIndex - 1]
            val prevProgress = userReadingProgress.find { (it.userId == userId || it.userId == "guest" || currentUser == null) && it.partId == previousPart.id }
            if (prevProgress != null && (prevProgress.isCompleted || prevProgress.progressPercent >= 90 || prevProgress.reachedEndOfPart)) {
                return true
            }
        }

        return false
    }

    /**
     * Returns ONLY the parts that should be visible to the user.
     * For readers with sequential reading enabled, ALL future locked parts are COMPLETELY HIDDEN.
     */
    fun getVisiblePartsForUser(
        story: Story?,
        allPartsForStory: List<StoryPart>,
        currentUser: User?,
        userReadingProgress: List<ReadingProgress>,
        userOverrides: List<UserAccessOverride>
    ): List<StoryPart> {
        if (story == null) return emptyList()

        // Writer / Admin sees all non-deleted parts
        val isPrivileged = currentUser?.role == UserRole.ADMIN ||
                (currentUser?.role == UserRole.WRITER && story.authorId == currentUser?.id)
        if (isPrivileged) {
            return allPartsForStory.filter { !it.isDeleted }.sortedBy { it.partNumber }
        }

        // Reader sees only published parts
        val publishedParts = allPartsForStory
            .filter { it.status == PartStatus.PUBLISHED && (it.scheduledAt == null || it.scheduledAt <= System.currentTimeMillis()) && !it.isDeleted }
            .sortedBy { it.partNumber }

        // If sequential mode is turned off, all published parts are visible
        if (!story.sequentialReadingEnabled) {
            return publishedParts
        }

        // In sequential reading mode, readers only see:
        // 1. Previously read/completed parts
        // 2. The current accessible part
        // 3. Parts explicitly manually unlocked
        // ALL future locked parts are omitted completely!
        val visibleList = mutableListOf<StoryPart>()
        for (part in publishedParts) {
            val accessible = isPartAccessible(
                story = story,
                part = part,
                currentUser = currentUser,
                userReadingProgress = userReadingProgress,
                userOverrides = userOverrides,
                allPartsForStory = allPartsForStory
            )
            if (accessible) {
                visibleList.add(part)
            } else {
                // Future parts must remain genuinely undiscoverable to readers!
                // Stop adding further parts so they are completely hidden.
                break
            }
        }
        return visibleList
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
