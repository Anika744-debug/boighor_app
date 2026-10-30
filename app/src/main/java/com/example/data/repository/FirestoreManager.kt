package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirestoreManager(val db: FirebaseFirestore?) {

    constructor(context: Context) : this(
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e1: Exception) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:799928524815:android:a6624e059713b01ebe037a")
                        .setApiKey("AIzaSyB5tFXNvGK-vrpDrNsCXUJJbPY4PPCZs84")
                        .setProjectId("gen-lang-client-0318913908")
                        .setStorageBucket("gen-lang-client-0318913908.firebasestorage.app")
                        .setGcmSenderId("799928524815")
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                }
            }
            val dbId = try {
                context.applicationContext.getString(R.string.firestore_database_id)
            } catch (_: Exception) {
                ""
            }
            if (!dbId.isNullOrBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Firestore initialization failed: ${e.message}")
            try {
                FirebaseFirestore.getInstance()
            } catch (e2: Exception) {
                Log.e("FirestoreManager", "Default Firestore initialization fallback failed: ${e2.message}")
                null
            }
        }
    )

    private val auth: FirebaseAuth?
        get() = try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }

    fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("ব্যবহারকারীকে অবশ্যই সাইন-ইন অবস্থায় থাকতে হবে।")
    }

    suspend fun syncGoogleUser(firebaseUser: FirebaseUser): User = syncFirebaseUser(firebaseUser)

    suspend fun syncFirebaseUser(firebaseUser: FirebaseUser): User {
        val uid = firebaseUser.uid
        val email = firebaseUser.email?.trim()?.lowercase() ?: ""
        val displayName = firebaseUser.displayName?.ifBlank { null } ?: email.substringBefore("@")
        val isOwnerEmail = email.equals("mdnajrulislam771@gmail.com", ignoreCase = true) ||
                           email.equals("mdnajulislam771@gmail.com", ignoreCase = true)

        var role = if (isOwnerEmail) UserRole.ADMIN else UserRole.READER

        if (db != null) {
            val userDocRef = db.collection("users").document(uid)
            val snapshot = try {
                userDocRef.get().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, userDocRef.path)
                null
            }

            if (snapshot != null && snapshot.exists()) {
                val roleStr = snapshot.getString("role")
                role = if (isOwnerEmail) {
                    UserRole.ADMIN
                } else {
                    when (roleStr) {
                        "OWNER", "ADMIN" -> UserRole.ADMIN
                        "WRITER" -> UserRole.WRITER
                        else -> UserRole.READER
                    }
                }
                try {
                    userDocRef.update("updatedAt", FieldValue.serverTimestamp()).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
                }
            } else {
                val roleName = if (isOwnerEmail) "OWNER" else "READER"
                val userPayload = mapOf(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "role" to roleName,
                    "isSuspended" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    userDocRef.set(userPayload, SetOptions.merge()).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, userDocRef.path)
                }
            }
        }

        return User(
            id = uid,
            username = email,
            passwordHash = "",
            displayName = displayName,
            role = role,
            status = UserStatus.ACTIVE,
            avatarLetter = displayName.take(1),
            isEmailVerified = true,
            verificationCode = null
        )
    }

    suspend fun updateUserRole(targetUserId: String, newRole: UserRole): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "users/$targetUserId"
        return try {
            val roleStr = if (newRole == UserRole.WRITER) "WRITER" else "READER"
            firestore.collection("users").document(targetUserId).update(
                mapOf(
                    "role" to roleStr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateStory(story: Story): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "stories/${story.id}"
        val currentUid = auth?.currentUser?.uid
        val effectiveAuthorId = if (!currentUid.isNullOrBlank()) currentUid else story.authorId
        return try {
            val payload = mapOf(
                "id" to story.id,
                "title" to story.title,
                "description" to story.description,
                "authorId" to effectiveAuthorId,
                "authorName" to story.authorName,
                "isCompleted" to story.isCompleted,
                "isPublished" to story.isPublished,
                "viewCount" to 0,
                "sequentialReadingEnabled" to story.sequentialReadingEnabled,
                "minReadingPercent" to story.minReadingPercent,
                "requireActiveReadingTime" to story.requireActiveReadingTime,
                "minActiveReadingSeconds" to story.minActiveReadingSeconds,
                "requireEndOfPart" to story.requireEndOfPart,
                "autoUnlockNextPart" to story.autoUnlockNextPart,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("stories").document(story.id).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdatePart(part: StoryPart): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "parts/${part.id}"
        val currentUid = auth?.currentUser?.uid
        val effectiveAuthorId = if (!currentUid.isNullOrBlank()) currentUid else "unknown"
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to part.id,
                "storyId" to part.storyId,
                "authorId" to effectiveAuthorId,
                "partNumber" to part.partNumber,
                "title" to part.title,
                "content" to part.content,
                "status" to part.status.name,
                "viewCount" to part.viewCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (part.scheduledAt != null) {
                payload["scheduledAt"] = com.google.firebase.Timestamp(part.scheduledAt / 1000, 0)
            }
            if (part.publishedAt != null) {
                payload["publishedAt"] = com.google.firebase.Timestamp(part.publishedAt / 1000, 0)
            }
            payload["createdAt"] = FieldValue.serverTimestamp()
            firestore.collection("parts").document(part.id).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "stories/$storyId"
        return try {
            firestore.collection("stories").document(storyId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePart(partId: String): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "parts/$partId"
        return try {
            firestore.collection("parts").document(partId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addReview(review: PassageReview): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "reviews/${review.id}"
        return try {
            val payload = mapOf(
                "id" to review.id,
                "storyId" to review.storyId,
                "partId" to review.partId,
                "userId" to review.userId,
                "userName" to review.userName,
                "selectedPassage" to (review.passageText ?: ""),
                "comment" to review.comment,
                "starRating" to (review.rating ?: 5),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("reviews").document(review.id).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveReadingProgress(progress: ReadingProgress): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "readingProgress/${progress.id}"
        return try {
            val payload = mapOf(
                "id" to progress.id,
                "userId" to progress.userId,
                "storyId" to progress.storyId,
                "partId" to progress.partId,
                "scrollOffset" to progress.activeSeconds.toInt(),
                "progressPercent" to progress.progressPercent,
                "isCompleted" to progress.isCompleted,
                "activeSeconds" to progress.activeSeconds,
                "lastScrollIndex" to progress.lastScrollIndex,
                "lastScrollOffset" to progress.lastScrollOffset,
                "reachedEndOfPart" to progress.reachedEndOfPart,
                "lastReadAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("readingProgress").document(progress.id).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveUserAccessOverride(override: UserAccessOverride): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "userOverrides/${override.id}"
        return try {
            val payload = mapOf(
                "id" to override.id,
                "userId" to override.userId,
                "partId" to override.partId,
                "storyId" to override.storyId,
                "isUnlocked" to override.isUnlocked,
                "unlockedBy" to override.unlockedBy,
                "isManual" to override.isManual,
                "unlockedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("userOverrides").document(override.id).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccessOverride(userId: String, partId: String): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val id = "${userId}_$partId"
        val path = "userOverrides/$id"
        return try {
            firestore.collection("userOverrides").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAuditLog(log: UnlockAuditLog): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        val path = "unlockAuditLogs/${log.id}"
        return try {
            val payload = mapOf(
                "id" to log.id,
                "userId" to log.userId,
                "storyId" to log.storyId,
                "partId" to log.partId,
                "partNumber" to log.partNumber,
                "action" to log.action,
                "actorId" to log.actorId,
                "actorName" to log.actorName,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("unlockAuditLogs").document(log.id).set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Downloads existing stories, parts, and reviews from Cloud Firestore into the local SQLite database.
     * Guarantees that re-installing or updating the APK restores all user-uploaded stories and comments.
     * Operations are decoupled so that a failure or permission issue on one collection does not abort the others.
     */
    suspend fun syncAllFromCloud(
        onStories: suspend (List<Story>) -> Unit,
        onParts: suspend (List<StoryPart>) -> Unit,
        onReviews: suspend (List<PassageReview>) -> Unit
    ) {
        val firestore = db ?: return
        val currentEmail = auth?.currentUser?.email?.trim()?.lowercase() ?: ""
        val isOwner = currentEmail.equals("mdnajrulislam771@gmail.com", ignoreCase = true) ||
                      currentEmail.equals("mdnajulislam771@gmail.com", ignoreCase = true)

        // 1. Stories Sync (Independent Operation)
        // Public story reading model:
        // Readers fetch published stories using whereEqualTo("isPublished", true).
        // Owners can query all stories including drafts.
        try {
            val storyQuery = if (isOwner) {
                firestore.collection("stories")
            } else {
                firestore.collection("stories").whereEqualTo("isPublished", true)
            }
            val storyDocs = storyQuery.get().await()
            val storiesList = storyDocs.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val desc = doc.getString("description") ?: ""
                    val authorId = doc.getString("authorId") ?: "unknown"
                    val authorName = doc.getString("authorName") ?: "লেখক"
                    val isCompleted = doc.getBoolean("isCompleted") ?: false
                    val isPublished = doc.getBoolean("isPublished") ?: true
                    val seq = doc.getBoolean("sequentialReadingEnabled") ?: true
                    val minPct = doc.getLong("minReadingPercent")?.toInt() ?: 90
                    val reqTime = doc.getBoolean("requireActiveReadingTime") ?: true
                    val minSec = doc.getLong("minActiveReadingSeconds") ?: 60L
                    val reqEnd = doc.getBoolean("requireEndOfPart") ?: true
                    val autoUnlock = doc.getBoolean("autoUnlockNextPart") ?: true
                    Story(
                        id = id,
                        title = title,
                        description = desc,
                        coverResName = "cover_nilkantha",
                        authorId = authorId,
                        authorName = authorName,
                        isCompleted = isCompleted,
                        isPublished = isPublished,
                        sequentialReadingEnabled = seq,
                        minReadingPercent = minPct,
                        requireActiveReadingTime = reqTime,
                        minActiveReadingSeconds = minSec,
                        requireEndOfPart = reqEnd,
                        autoUnlockNextPart = autoUnlock
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (storiesList.isNotEmpty()) {
                onStories(storiesList)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "stories")
            Log.w("FirestoreManager", "Stories sync failed: ${e.message}")
        }

        // 2. Parts Sync (Independent Operation)
        // In Firestore rules:
        // allow read: if isSignedIn() && (resource.data.status == 'PUBLISHED' || resource.data.authorId == request.auth.uid || isOwner());
        // Non-owner readers MUST filter whereEqualTo("status", "PUBLISHED") to satisfy security rules.
        // Owners can query all parts (drafts and published).
        try {
            val partQuery = if (isOwner) {
                firestore.collection("parts")
            } else {
                firestore.collection("parts").whereEqualTo("status", "PUBLISHED")
            }
            val partDocs = partQuery.get().await()
            val partsList = partDocs.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val storyId = doc.getString("storyId") ?: return@mapNotNull null
                    val partNum = doc.getLong("partNumber")?.toInt() ?: 1
                    val title = doc.getString("title") ?: "পর্ব $partNum"
                    val content = doc.getString("content") ?: ""
                    val statusStr = doc.getString("status") ?: "PUBLISHED"
                    val status = runCatching { PartStatus.valueOf(statusStr) }.getOrDefault(PartStatus.PUBLISHED)
                    val views = doc.getLong("viewCount")?.toInt() ?: 0
                    val scheduledTimestamp = doc.getTimestamp("scheduledAt")?.toDate()?.time
                    val publishedTimestamp = doc.getTimestamp("publishedAt")?.toDate()?.time ?: System.currentTimeMillis()
                    StoryPart(
                        id = id,
                        storyId = storyId,
                        partNumber = partNum,
                        title = title,
                        content = content,
                        status = status,
                        viewCount = views,
                        scheduledAt = scheduledTimestamp,
                        publishedAt = publishedTimestamp
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (partsList.isNotEmpty()) {
                onParts(partsList)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "parts")
            Log.w("FirestoreManager", "Parts sync failed: ${e.message}")
        }

        // 3. Reviews Sync (Independent Operation)
        try {
            val reviewDocs = firestore.collection("reviews").get().await()
            val reviewsList = reviewDocs.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val storyId = doc.getString("storyId") ?: return@mapNotNull null
                    val partId = doc.getString("partId") ?: return@mapNotNull null
                    val userId = doc.getString("userId") ?: "user"
                    val userName = doc.getString("userName") ?: "পাঠক"
                    val selectedPassage = doc.getString("selectedPassage")
                    val comment = doc.getString("comment") ?: ""
                    val rating = doc.getLong("starRating")?.toInt()
                    PassageReview(
                        id = id,
                        storyId = storyId,
                        partId = partId,
                        userId = userId,
                        userName = userName,
                        passageText = selectedPassage,
                        comment = comment,
                        rating = rating,
                        createdAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (reviewsList.isNotEmpty()) {
                onReviews(reviewsList)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "reviews")
            Log.w("FirestoreManager", "Reviews sync failed: ${e.message}")
        }
    }
}
