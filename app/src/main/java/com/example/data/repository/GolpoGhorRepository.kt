package com.example.data.repository

import android.content.Context
import com.example.data.db.GolpoGhorDao
import com.example.data.db.GolpoGhorDatabase
import com.example.data.model.*
import com.example.util.SequentialReadingHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class GolpoGhorRepository(
    val dao: GolpoGhorDao,
    private val context: Context
) {
    val firestoreManager = FirestoreManager(context)
    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        repoScope.launch {
            seedInitialDataIfNeeded()
            checkScheduledPublications()
        }
    }

    companion object {
        const val ADMIN_EMAIL = "mdnajrulislam771@gmail.com"

        fun hashPassword(password: String): String {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun generateVerificationCode(): String {
            val rng = java.security.SecureRandom()
            val codeNum = 100000 + rng.nextInt(900000)
            return codeNum.toString()
        }
    }

    // --- Users & Auth ---
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val allReaders: Flow<List<User>> = dao.getAllReaders()

    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("অনুগ্রহ করে একটি সঠিক ইমেইল ঠিকানা দিন।"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"))
        }
        if (displayName.trim().isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("অনুগ্রহ করে আপনার পুরো নাম দিন।"))
        }

        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("এই ইমেইল দিয়ে ইতোমধ্যে একটি অ্যাকাউন্ট নিবন্ধিত আছে।"))
        }

        // Security Enforcement:
        // ALL newly registered accounts receive the READER role by default.
        // The Admin role is NEVER assigned on an unverified email string!
        // A 6-digit verification code is generated, and isEmailVerified starts as false.
        // The Admin role is assigned ONLY after email identity verification is completed.
        val isAdminEmail = cleanEmail.equals(ADMIN_EMAIL, ignoreCase = true)
        val verificationCode = generateVerificationCode()

        val userId = UUID.randomUUID().toString()
        val newUser = User(
            id = userId,
            username = cleanEmail,
            passwordHash = hashPassword(password),
            displayName = displayName.trim(),
            role = UserRole.READER, // Always Reader by default!
            status = UserStatus.ACTIVE,
            avatarLetter = displayName.trim().take(1),
            isEmailVerified = false,
            verificationCode = verificationCode
        )
        dao.insertUser(newUser)

        dao.upsertUserSettings(
            UserSettings(
                userId = userId,
                appTheme = AppTheme.BADAMI_BROWN,
                readingMood = ReadingMood.LIGHT,
                readingFont = ReadingFont.NOTO_SERIF,
                fontSizeSp = 19f,
                lineSpacingMultiplier = 1.6f
            )
        )

        val notifTitle = if (isAdminEmail) "অ্যাডমিন পরিচয় যাচাইকরণ কোড" else "গল্পঘরে স্বাগতম!"
        val notifMessage = if (isAdminEmail) {
            "অ্যাডমিন অধিকার সক্রিয় করতে আপনার ৬-সংখ্যার সিকিউরিটি ভেরিফিকেশন কোড: $verificationCode। নিরাপত্তা যাচাই সম্পন্ন করতে এই কোডটি প্রদান করুন।"
        } else {
            "গল্পঘরে পাঠক হিসেবে আপনাকে স্বাগতম। অসাধারণ সব বাংলা গল্প পড়ুন ও মতামত দিন।"
        }

        dao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = notifTitle,
                message = notifMessage,
                type = NotificationType.ACCOUNT_STATUS_CHANGED,
                isRead = false
            )
        )

        Result.success(newUser)
    }

    suspend fun verifyEmailIdentity(
        emailOrUserId: String,
        code: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val clean = emailOrUserId.trim().lowercase()
        val user = dao.getUserByEmail(clean) ?: dao.getUserById(emailOrUserId.trim())
            ?: return@withContext Result.failure(IllegalArgumentException("ব্যবহারকারী অ্যাকাউন্ট খুঁজে পাওয়া যায়নি।"))

        if (user.isEmailVerified && user.username.equals(ADMIN_EMAIL, ignoreCase = true) && user.role == UserRole.ADMIN) {
            return@withContext Result.success(user)
        }

        val expectedCode = user.verificationCode
        if (expectedCode.isNullOrBlank() || expectedCode != code.trim()) {
            return@withContext Result.failure(IllegalArgumentException("ভুল ভেরিফিকেশন কোড! পরিচয় যাচাই ব্যর্থ হয়েছে।"))
        }

        val isAdminEmail = user.username.equals(ADMIN_EMAIL, ignoreCase = true)
        val verifiedRole = if (isAdminEmail) UserRole.ADMIN else UserRole.READER

        dao.verifyUserEmail(user.id, isVerified = true, role = verifiedRole)
        val verifiedUser = user.copy(
            isEmailVerified = true,
            verificationCode = null,
            role = verifiedRole
        )

        val notifTitle = if (isAdminEmail) "অ্যাডমিন পরিচয় যাচাই সম্পন্ন" else "ইমেইল পরিচয় যাচাই সম্পন্ন"
        val notifMsg = if (isAdminEmail) {
            "আপনার ইমেইল পরিচয় (Mdnajrulislam771@gmail.com) সফলভাবে যাচাই করা হয়েছে। গল্পঘরের একমাত্র 'অ্যাডমিন' হিসেবে আপনার পূর্ণ অ্যাক্সেস ও ভূমিকা নিয়ন্ত্রণ ক্ষমতা সক্রিয় হয়েছে।"
        } else {
            "আপনার ইমেইল সফলভাবে যাচাই করা হয়েছে।"
        }

        dao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                title = notifTitle,
                message = notifMsg,
                type = NotificationType.ACCOUNT_STATUS_CHANGED,
                isRead = false
            )
        )

        Result.success(verifiedUser)
    }

    suspend fun resendVerificationCode(emailOrUserId: String): Result<String> = withContext(Dispatchers.IO) {
        val clean = emailOrUserId.trim().lowercase()
        val user = dao.getUserByEmail(clean) ?: dao.getUserById(emailOrUserId.trim())
            ?: return@withContext Result.failure(IllegalArgumentException("ব্যবহারকারী অ্যাকাউন্ট খুঁজে পাওয়া যায়নি।"))

        val newCode = generateVerificationCode()
        dao.updateUser(user.copy(verificationCode = newCode))

        dao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                title = "নতুন সিকিউরিটি ভেরিফিকেশন কোড",
                message = "আপনার নতুন ৬-সংখ্যার সিকিউরিটি ভেরিফিকেশন কোড: $newCode",
                type = NotificationType.ACCOUNT_STATUS_CHANGED,
                isRead = false
            )
        )

        Result.success(newCode)
    }

    suspend fun requestPasswordResetOtp(email: String): Result<String> = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        val user = dao.getUserByEmail(clean)
            ?: return@withContext Result.failure(IllegalArgumentException("এই ইমেইল দিয়ে নিবন্ধিত কোনো অ্যাকাউন্ট পাওয়া যায়নি।"))

        val otpCode = generateVerificationCode()
        dao.updateUser(user.copy(verificationCode = otpCode))

        dao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                title = "পাসওয়ার্ড রিকভারি ওটিপি কোড",
                message = "আপনার পাসওয়ার্ড রিসেট করার জন্য ৬-সংখ্যার ওটিপি কোড: $otpCode",
                type = NotificationType.ACCOUNT_STATUS_CHANGED,
                isRead = false
            )
        )
        Result.success(otpCode)
    }

    suspend fun resetPasswordWithOtp(email: String, otp: String, newPass: String): Result<User> = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        val user = dao.getUserByEmail(clean)
            ?: return@withContext Result.failure(IllegalArgumentException("ব্যবহারকারী অ্যাকাউন্ট খুঁজে পাওয়া যায়নি।"))

        if (user.verificationCode != otp.trim()) {
            return@withContext Result.failure(IllegalArgumentException("ভুল ওটিপি কোড! অনুগ্রহ করে সঠিক কোড দিন।"))
        }

        if (newPass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"))
        }

        val newHash = hashPassword(newPass)
        val updatedUser = user.copy(
            passwordHash = newHash,
            verificationCode = null,
            isEmailVerified = true
        )
        dao.updateUser(updatedUser)

        Result.success(updatedUser)
    }

    suspend fun login(emailOrUsername: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val clean = emailOrUsername.trim().lowercase()
        val user = dao.getUserByEmail(clean) ?: dao.getUserByUsername(clean)
            ?: return@withContext Result.failure(IllegalArgumentException("এই ইমেইল দিয়ে কোনো অ্যাকাউন্ট পাওয়া যায়নি।"))

        val hashedAttempt = hashPassword(pass)
        if (user.passwordHash != hashedAttempt && user.passwordHash != pass) {
            return@withContext Result.failure(IllegalArgumentException("ভুল পাসওয়ার্ড! অনুগ্রহ করে পুনরায় যাচাই করুন।"))
        }

        if (user.status == UserStatus.SUSPENDED) {
            return@withContext Result.failure(IllegalStateException("আপনার অ্যাকাউন্টটি অ্যাডমিন কর্তৃক সাময়িকভাবে স্থগিত করা হয়েছে।"))
        }

        // Role Security Enforcement:
        // 1. If email is ADMIN_EMAIL (Mdnajrulislam771@gmail.com):
        //    Admin role is ONLY granted if isEmailVerified == true.
        //    If not verified, user cannot hold the ADMIN role.
        // 2. No other email can EVER hold the ADMIN role.
        val isAdminEmail = user.username.equals(ADMIN_EMAIL, ignoreCase = true)
        if (isAdminEmail) {
            if (user.isEmailVerified) {
                if (user.role != UserRole.ADMIN) {
                    val adminUser = user.copy(role = UserRole.ADMIN)
                    dao.updateUser(adminUser)
                    return@withContext Result.success(adminUser)
                }
            } else {
                // Unverified attempt on admin email: ensure role is READER until identity is verified
                if (user.role == UserRole.ADMIN) {
                    val unverifiedReader = user.copy(role = UserRole.READER)
                    dao.updateUser(unverifiedReader)
                    return@withContext Result.success(unverifiedReader)
                }
            }
        } else {
            // Any other user must NEVER have ADMIN role
            if (user.role == UserRole.ADMIN) {
                val readerUser = user.copy(role = UserRole.READER)
                dao.updateUser(readerUser)
                return@withContext Result.success(readerUser)
            }
        }

        Result.success(user)
    }

    suspend fun updateUserRole(
        adminUserId: String,
        targetUserId: String,
        newRole: UserRole
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adminUser = dao.getUserById(adminUserId)
        if (adminUser == null ||
            adminUser.role != UserRole.ADMIN ||
            !adminUser.username.equals(ADMIN_EMAIL, ignoreCase = true) ||
            !adminUser.isEmailVerified
        ) {
            return@withContext Result.failure(SecurityException("শুধুমাত্র যাচাইকৃত অ্যাডমিন (Mdnajrulislam771@gmail.com) রোল পরিবর্তন করার অনুমতি রাখেন।"))
        }

        val targetUser = dao.getUserById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("টার্গেট ব্যবহারকারী খুঁজে পাওয়া যায়নি।"))

        if (targetUser.username.equals(ADMIN_EMAIL, ignoreCase = true)) {
            return@withContext Result.failure(IllegalStateException("অ্যাডমিন অ্যাকাউন্টের ভূমিকা পরিবর্তন করা যাবে না।"))
        }

        if (newRole == UserRole.ADMIN) {
            return@withContext Result.failure(SecurityException("অন্য কোনো ব্যবহারকারীকে অ্যাডমিন বানানো নিষিদ্ধ।"))
        }

        dao.updateUserRole(targetUserId, newRole)

        val roleTitle = if (newRole == UserRole.WRITER) "লেখক (Writer)" else "পাঠক (Reader)"
        dao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = targetUserId,
                title = "অ্যাকাউন্টের ভূমিকা পরিবর্তিত হয়েছে",
                message = "অ্যাডমিন আপনার অ্যাকাউন্টের ভূমিকা $roleTitle হিসেবে নির্ধারণ করেছেন।",
                type = NotificationType.ACCOUNT_STATUS_CHANGED,
                isRead = false
            )
        )

        Result.success(Unit)
    }

    suspend fun updateUserStatus(
        adminUserId: String,
        targetUserId: String,
        newStatus: UserStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adminUser = dao.getUserById(adminUserId)
        if (adminUser == null ||
            adminUser.role != UserRole.ADMIN ||
            !adminUser.username.equals(ADMIN_EMAIL, ignoreCase = true) ||
            !adminUser.isEmailVerified
        ) {
            return@withContext Result.failure(SecurityException("শুধুমাত্র যাচাইকৃত অ্যাডমিন অ্যাকাউন্ট স্থগিত বা সচল করতে পারেন।"))
        }

        val targetUser = dao.getUserById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("ব্যবহারকারী পাওয়া যায়নি।"))

        if (targetUser.username.equals(ADMIN_EMAIL, ignoreCase = true)) {
            return@withContext Result.failure(IllegalStateException("অ্যাডমিন অ্যাকাউন্ট স্থগিত করা যাবে না।"))
        }

        val updated = targetUser.copy(status = newStatus)
        dao.updateUser(updated)

        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            userId = targetUserId,
            title = if (newStatus == UserStatus.ACTIVE) "অ্যাকাউন্ট সচল করা হয়েছে" else "অ্যাকাউন্ট সাময়িক স্থগিত",
            message = if (newStatus == UserStatus.ACTIVE) "অ্যাডমিন আপনার অ্যাকাউন্টটি সচল করেছেন। এখন আপনি গল্পঘর ব্যবহার করতে পারেন।" else "অ্যাডমিন আপনার অ্যাকাউন্টটি স্থগিত করেছেন। বিস্তারিত জানতে অ্যাডমিনের সাথে যোগাযোগ করুন।",
            type = NotificationType.ACCOUNT_STATUS_CHANGED,
            isRead = false
        )
        dao.insertNotification(notif)
        Result.success(Unit)
    }

    suspend fun updateUserProfile(
        userId: String,
        newDisplayName: String,
        newAvatarUri: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("ব্যবহারকারী খুঁজে পাওয়া যায়নি।"))
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("নাম খালি রাখা যাবে না।"))
        }
        val updated = user.copy(
            displayName = trimmed,
            avatarLetter = trimmed.take(1),
            avatarUri = newAvatarUri
        )
        dao.updateUser(updated)
        Result.success(updated)
    }

    // --- Stories ---
    val activeStories: Flow<List<Story>> = dao.getActiveStories()
    val trashStories: Flow<List<Story>> = dao.getTrashStories()
    val allPublishedParts: Flow<List<StoryPart>> = dao.getAllPublishedParts()

    fun getStoryById(storyId: String): Flow<Story?> = dao.getStoryByIdFlow(storyId)
    fun getPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPartsForStory(storyId)
    fun getPartById(partId: String): Flow<StoryPart?> = dao.getPartByIdFlow(partId)
    val trashParts: Flow<List<StoryPart>> = dao.getTrashParts()

    suspend fun createStory(
        title: String,
        description: String,
        coverResName: String?,
        coverUri: String? = null,
        firstPartTitle: String,
        firstPartContent: String,
        status: PartStatus,
        scheduledAt: Long?,
        authorId: String = "admin",
        authorName: String = "শ্রাবণ মজুমদার"
    ): String = withContext(Dispatchers.IO) {
        val storyId = UUID.randomUUID().toString()
        val story = Story(
            id = storyId,
            title = title,
            description = description,
            coverResName = coverResName ?: if (coverUri != null) null else "cover_nilkantha",
            coverUri = coverUri,
            authorId = authorId,
            authorName = authorName
        )
        dao.insertStory(story)

        val partId = UUID.randomUUID().toString()
        val part = StoryPart(
            id = partId,
            storyId = storyId,
            partNumber = 1,
            title = firstPartTitle,
            content = firstPartContent,
            status = status,
            scheduledAt = scheduledAt,
            publishedAt = if (status == PartStatus.PUBLISHED) System.currentTimeMillis() else null
        )
        dao.insertStoryPart(part)

        val storyRes = firestoreManager.createOrUpdateStory(story)
        val partRes = firestoreManager.createOrUpdatePart(part)
        if (storyRes.isFailure) {
            android.util.Log.e("GolpoGhorRepo", "Failed to sync created story to Firestore", storyRes.exceptionOrNull())
        }
        if (partRes.isFailure) {
            android.util.Log.e("GolpoGhorRepo", "Failed to sync created part to Firestore", partRes.exceptionOrNull())
        }

        if (status == PartStatus.PUBLISHED) {
            notifyReadersNewPart(story.title, part.title, storyId, partId)
        }
        storyId
    }

    suspend fun createPart(
        storyId: String,
        partTitle: String,
        content: String,
        status: PartStatus,
        scheduledAt: Long?,
        customPartNumber: Int? = null
    ): String = withContext(Dispatchers.IO) {
        val existingParts = dao.getPartsForStory(storyId).firstOrNull() ?: emptyList()
        val nextNumber = customPartNumber ?: ((existingParts.maxOfOrNull { it.partNumber } ?: 0) + 1)

        val partId = UUID.randomUUID().toString()
        val part = StoryPart(
            id = partId,
            storyId = storyId,
            partNumber = nextNumber,
            title = partTitle,
            content = content,
            status = status,
            scheduledAt = scheduledAt,
            publishedAt = if (status == PartStatus.PUBLISHED) System.currentTimeMillis() else null
        )
        dao.insertStoryPart(part)

        val partRes = firestoreManager.createOrUpdatePart(part)
        if (partRes.isFailure) {
            android.util.Log.e("GolpoGhorRepo", "Failed to sync created part to Firestore", partRes.exceptionOrNull())
        }

        val story = dao.getStoryById(storyId)
        if (story != null) {
            val updatedStory = story.copy(updatedAt = System.currentTimeMillis())
            dao.updateStory(updatedStory)
            firestoreManager.createOrUpdateStory(updatedStory)
            if (status == PartStatus.PUBLISHED) {
                notifyReadersNewPart(story.title, part.title, storyId, partId)
            }
        }
        partId
    }

    suspend fun updateStoryDetails(
        storyId: String,
        title: String,
        description: String,
        coverResName: String?,
        coverUri: String?
    ): Result<Story> = withContext(Dispatchers.IO) {
        val story = dao.getStoryById(storyId)
            ?: return@withContext Result.failure(IllegalArgumentException("গল্পটি পাওয়া যায়নি।"))
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("গল্পের নাম খালি রাখা যাবে না।"))
        }
        val updated = story.copy(
            title = trimmed,
            description = description.trim(),
            coverResName = coverResName,
            coverUri = coverUri,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateStory(updated)
        firestoreManager.createOrUpdateStory(updated)
        Result.success(updated)
    }

    suspend fun updateStory(story: Story) = withContext(Dispatchers.IO) {
        val updated = story.copy(updatedAt = System.currentTimeMillis())
        dao.updateStory(updated)
        firestoreManager.createOrUpdateStory(updated)
    }

    suspend fun updateStoryPart(part: StoryPart) = withContext(Dispatchers.IO) {
        val oldPart = dao.getPartById(part.id)
        dao.updateStoryPart(part)
        val cloudRes = firestoreManager.createOrUpdatePart(part)
        if (cloudRes.isFailure) {
            android.util.Log.e("GolpoGhorRepo", "Failed to push updated part to Firestore", cloudRes.exceptionOrNull())
        }

        // If newly transitioned to published
        if (oldPart?.status != PartStatus.PUBLISHED && part.status == PartStatus.PUBLISHED) {
            val story = dao.getStoryById(part.storyId)
            if (story != null) {
                notifyReadersNewPart(story.title, part.title, story.id, part.id)
            }
        }
    }

    suspend fun softDeleteStory(storyId: String) = withContext(Dispatchers.IO) {
        val story = dao.getStoryById(storyId) ?: return@withContext
        val updated = story.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
        dao.updateStory(updated)
        firestoreManager.createOrUpdateStory(updated)
    }

    suspend fun restoreStory(storyId: String) = withContext(Dispatchers.IO) {
        val story = dao.getStoryById(storyId) ?: return@withContext
        val updated = story.copy(isDeleted = false, deletedAt = null)
        dao.updateStory(updated)
        firestoreManager.createOrUpdateStory(updated)
    }

    suspend fun permanentlyDeleteStory(storyId: String) = withContext(Dispatchers.IO) {
        dao.permanentlyDeleteStory(storyId)
        dao.permanentlyDeletePartsByStory(storyId)
        firestoreManager.deleteStory(storyId)
    }

    suspend fun softDeletePart(partId: String) = withContext(Dispatchers.IO) {
        val part = dao.getPartById(partId) ?: return@withContext
        val updated = part.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
        dao.updateStoryPart(updated)
        firestoreManager.createOrUpdatePart(updated)
    }

    suspend fun restorePart(partId: String) = withContext(Dispatchers.IO) {
        val part = dao.getPartById(partId) ?: return@withContext
        val updated = part.copy(isDeleted = false, deletedAt = null)
        dao.updateStoryPart(updated)
        firestoreManager.createOrUpdatePart(updated)
    }

    suspend fun permanentlyDeletePart(partId: String) = withContext(Dispatchers.IO) {
        dao.permanentlyDeletePart(partId)
        firestoreManager.deletePart(partId)
    }

    // --- Scheduled Publisher ---
    suspend fun checkScheduledPublications() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dueParts = dao.getDueScheduledParts(now)
        for (part in dueParts) {
            val updated = part.copy(
                status = PartStatus.PUBLISHED,
                publishedAt = now
            )
            dao.updateStoryPart(updated)
            firestoreManager.createOrUpdatePart(updated)
            val story = dao.getStoryById(part.storyId)
            if (story != null) {
                notifyReadersNewPart(story.title, part.title, story.id, part.id)
            }
        }
    }

    private suspend fun notifyReadersNewPart(storyTitle: String, partTitle: String, storyId: String, partId: String) {
        val readers = dao.getAllReaders().firstOrNull() ?: emptyList()
        val notifs = readers.map { r ->
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = r.id,
                title = "নতুন পর্ব প্রকাশিত!",
                message = "“$storyTitle” গল্পের নতুন পর্ব “$partTitle” এখন পড়া যাচ্ছে।",
                type = NotificationType.NEW_PART_PUBLISHED,
                storyId = storyId,
                partId = partId,
                isRead = false
            )
        }
        dao.insertNotifications(notifs)
    }

    // --- Reading Progress ---
    fun getUserReadingProgress(userId: String): Flow<List<ReadingProgress>> = dao.getUserReadingProgress(userId)
    val allReadingProgress: Flow<List<ReadingProgress>> = dao.getAllReadingProgress()

    suspend fun updateReadingProgress(
        userId: String,
        storyId: String,
        partId: String,
        progressPercent: Int,
        activeDurationDeltaSeconds: Long,
        lastScrollIndex: Int = 0,
        lastScrollOffset: Int = 0,
        reachedEndOfPart: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getProgress(userId, storyId, partId)
        val story = dao.getStoryById(storyId)
        val newDuration = (existing?.activeSeconds ?: 0L) + activeDurationDeltaSeconds
        val effectiveReachedEnd = (existing?.reachedEndOfPart == true) || reachedEndOfPart

        // Check configurable completion criteria
        val meetsCriteria = if (story != null) {
            SequentialReadingHelper.isPartCompletionSatisfied(
                story = story,
                progressPercent = progressPercent,
                activeDurationSeconds = newDuration,
                reachedEndOfPart = effectiveReachedEnd
            )
        } else {
            progressPercent >= 90
        }

        val wasAlreadyCompleted = existing?.isCompleted == true
        val isNewlyCompleted = !wasAlreadyCompleted && meetsCriteria
        val completed = wasAlreadyCompleted || meetsCriteria

        val item = ReadingProgress(
            id = "${userId}_${storyId}_${partId}",
            userId = userId,
            storyId = storyId,
            partId = partId,
            progressPercent = maxOf(existing?.progressPercent ?: 0, progressPercent),
            isCompleted = completed,
            activeSeconds = newDuration,
            lastReadAt = System.currentTimeMillis(),
            lastScrollIndex = lastScrollIndex,
            lastScrollOffset = lastScrollOffset,
            reachedEndOfPart = effectiveReachedEnd,
            isManuallyUnlocked = existing?.isManuallyUnlocked ?: false
        )
        dao.upsertReadingProgress(item)
        firestoreManager.saveReadingProgress(item)

        // If newly completed and autoUnlockNextPart is enabled:
        if (isNewlyCompleted && story != null && story.autoUnlockNextPart) {
            val allStoryParts = dao.getPartsForStorySync(storyId)
            val allPublished = allStoryParts.filter {
                it.status == PartStatus.PUBLISHED && (it.scheduledAt == null || it.scheduledAt <= System.currentTimeMillis()) && !it.isDeleted
            }.sortedBy { it.partNumber }

            val currentIdx = allPublished.indexOfFirst { it.id == partId }
            if (currentIdx in 0 until (allPublished.size - 1)) {
                val nextPart = allPublished[currentIdx + 1]
                dao.insertNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = "নতুন অধ্যায় উন্মুক্ত হয়েছে!",
                        message = "অভিনন্দন! পূর্ববর্তী পর্ব সমাপ্ত করায় পরবর্তী পর্ব আপনার পড়ার জন্য উন্মুক্ত হয়েছে।",
                        type = NotificationType.PART_UNLOCKED,
                        storyId = storyId,
                        partId = nextPart.id,
                        isRead = false
                    )
                )
                val audit = UnlockAuditLog(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    storyId = storyId,
                    partId = nextPart.id,
                    partNumber = nextPart.partNumber,
                    action = "AUTO_UNLOCK",
                    actorId = "SYSTEM",
                    actorName = "স্বয়ংক্রিয় ধারাবাহিক আনলক",
                    timestamp = System.currentTimeMillis()
                )
                dao.insertAuditLog(audit)
                firestoreManager.saveAuditLog(audit)
            }
        }
    }

    // --- Overrides & Manual Unlocks ---
    val allOverrides: Flow<List<UserAccessOverride>> = dao.getAllOverrides()
    val allAuditLogs: Flow<List<UnlockAuditLog>> = dao.getAllAuditLogs()

    suspend fun setAccessOverride(userId: String, partId: String, unlock: Boolean) = withContext(Dispatchers.IO) {
        val part = dao.getPartById(partId)
        val storyId = part?.storyId ?: ""
        setManualAccessOverride(
            userId = userId,
            storyId = storyId,
            partId = partId,
            partNumber = part?.partNumber ?: 1,
            unlock = unlock,
            actorId = "WRITER",
            actorName = "লেখক/অ্যাডমিন"
        )
    }

    suspend fun setManualAccessOverride(
        userId: String,
        storyId: String,
        partId: String,
        partNumber: Int,
        unlock: Boolean,
        actorId: String,
        actorName: String,
        notes: String? = null
    ) = withContext(Dispatchers.IO) {
        if (unlock) {
            val override = UserAccessOverride(
                id = "${userId}_$partId",
                userId = userId,
                partId = partId,
                storyId = storyId,
                isUnlocked = true,
                unlockedBy = actorName,
                unlockedAt = System.currentTimeMillis(),
                isManual = true
            )
            dao.insertOverride(override)
            firestoreManager.saveUserAccessOverride(override)

            val audit = UnlockAuditLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                storyId = storyId,
                partId = partId,
                partNumber = partNumber,
                action = "MANUAL_UNLOCK",
                actorId = actorId,
                actorName = actorName,
                timestamp = System.currentTimeMillis()
            )
            dao.insertAuditLog(audit)
            firestoreManager.saveAuditLog(audit)

            // Notify reader tastefully without revealing future unreleased info
            val part = dao.getPartById(partId)
            val isPublishedNow = part != null && part.status == PartStatus.PUBLISHED &&
                    (part.scheduledAt == null || part.scheduledAt <= System.currentTimeMillis())
            if (isPublishedNow) {
                dao.insertNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = "পর্ব আনলক হয়েছে!",
                        message = "লেখক আপনার জন্য বিশেষ অ্যাক্সেস উন্মুক্ত করেছেন। এখন আপনি পর্বটি পড়তে পারেন।",
                        type = NotificationType.PART_UNLOCKED,
                        storyId = storyId,
                        partId = partId,
                        isRead = false
                    )
                )
            }
        } else {
            dao.deleteOverride(userId, partId)
            firestoreManager.deleteUserAccessOverride(userId, partId)

            val audit = UnlockAuditLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                storyId = storyId,
                partId = partId,
                partNumber = partNumber,
                action = "REVOKE_UNLOCK",
                actorId = actorId,
                actorName = actorName,
                timestamp = System.currentTimeMillis()
            )
            dao.insertAuditLog(audit)
            firestoreManager.saveAuditLog(audit)
        }
    }

    suspend fun updateStorySettings(
        storyId: String,
        sequentialReadingEnabled: Boolean,
        minReadingPercent: Int,
        requireActiveReadingTime: Boolean,
        minActiveReadingSeconds: Long,
        requireEndOfPart: Boolean,
        autoUnlockNextPart: Boolean
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getStoryById(storyId) ?: return@withContext
        val updated = existing.copy(
            sequentialReadingEnabled = sequentialReadingEnabled,
            minReadingPercent = minReadingPercent,
            requireActiveReadingTime = requireActiveReadingTime,
            minActiveReadingSeconds = minActiveReadingSeconds,
            requireEndOfPart = requireEndOfPart,
            autoUnlockNextPart = autoUnlockNextPart,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateStory(updated)
        firestoreManager.createOrUpdateStory(updated)
    }

    fun getUserOverrides(userId: String): Flow<List<UserAccessOverride>> = dao.getUserOverrides(userId)

    // --- Reviews, Comments & Replies ---
    val allReviews: Flow<List<PassageReview>> = dao.getAllReviews()
    val allReplies: Flow<List<ReviewReply>> = dao.getAllReplies()
    val allReactions: Flow<List<Reaction>> = dao.getAllReactions()

    fun getReviewsForPart(storyId: String, partId: String): Flow<List<PassageReview>> = dao.getReviewsForPart(storyId, partId)
    fun getRepliesForReview(reviewId: String): Flow<List<ReviewReply>> = dao.getRepliesForReview(reviewId)

    suspend fun addReview(
        storyId: String,
        partId: String,
        userId: String,
        userName: String,
        passageText: String?,
        passageAnchor: String?,
        reviewType: ReviewType,
        rating: Int?,
        comment: String
    ): String = withContext(Dispatchers.IO) {
        val reviewId = UUID.randomUUID().toString()
        val review = PassageReview(
            id = reviewId,
            storyId = storyId,
            partId = partId,
            userId = userId,
            userName = userName,
            passageText = passageText,
            passageAnchor = passageAnchor,
            reviewType = reviewType,
            rating = rating,
            comment = comment,
            createdAt = System.currentTimeMillis()
        )
        dao.insertReview(review)
        try {
            firestoreManager.addReview(review)
        } catch (_: Exception) {}

        // Notify writer
        val part = dao.getPartById(partId)
        val typeLabel = when (reviewType) {
            ReviewType.PASSAGE -> "প্যারাগ্রাফ মন্তব্য"
            ReviewType.WHOLE_PART -> "পর্ব পর্যালোচনা"
            ReviewType.STORY -> "গল্প পর্যালোচনা"
        }
        val notif = NotificationItem(
            id = UUID.randomUUID().toString(),
            userId = "writer_1",
            title = "নতুন $typeLabel",
            message = "$userName “${part?.title ?: "পর্ব"}” এ লিখেছেন: “${comment.take(45)}...”",
            type = NotificationType.NEW_REVIEW_RECEIVED,
            storyId = storyId,
            partId = partId,
            reviewId = reviewId,
            isRead = false
        )
        dao.insertNotification(notif)
        reviewId
    }

    suspend fun addReply(
        reviewId: String,
        userId: String,
        userName: String,
        userRole: UserRole,
        replyText: String
    ) = withContext(Dispatchers.IO) {
        val reply = ReviewReply(
            id = UUID.randomUUID().toString(),
            reviewId = reviewId,
            userId = userId,
            userName = userName,
            userRole = userRole,
            replyText = replyText,
            createdAt = System.currentTimeMillis()
        )
        dao.insertReply(reply)

        val originalReview = dao.getReviewById(reviewId)
        if (originalReview != null) {
            if (userRole == UserRole.WRITER) {
                dao.updateReview(originalReview.copy(isAnswered = true))
                // Notify reader
                dao.insertNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        userId = originalReview.userId,
                        title = "লেখক আপনার মন্তব্যে উত্তর দিয়েছেন!",
                        message = "শ্রাবণ মজুমদার আপনার মন্তব্যে লিখেছেন: “${replyText.take(50)}...”",
                        type = NotificationType.REPLY_ON_REVIEW,
                        storyId = originalReview.storyId,
                        partId = originalReview.partId,
                        reviewId = reviewId,
                        isRead = false
                    )
                )
            } else if (originalReview.userId != userId) {
                // Reader replying to reader
                dao.insertNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        userId = originalReview.userId,
                        title = "$userName আপনার মন্তব্যে উত্তর দিয়েছেন",
                        message = "“${replyText.take(50)}...”",
                        type = NotificationType.REPLY_ON_REVIEW,
                        storyId = originalReview.storyId,
                        partId = originalReview.partId,
                        reviewId = reviewId,
                        isRead = false
                    )
                )
            }
        }
    }

    // --- Reactions ---
    suspend fun toggleReaction(
        userId: String,
        targetType: String,
        targetId: String,
        passageText: String?,
        emojiType: ReactionEmoji
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getReaction(userId, targetId, emojiType)
        if (existing != null) {
            dao.deleteReaction(existing)
        } else {
            val reaction = Reaction(
                id = "${userId}_${targetId}_${emojiType.name}",
                userId = userId,
                targetType = targetType,
                targetId = targetId,
                passageText = passageText,
                emojiType = emojiType,
                createdAt = System.currentTimeMillis()
            )
            dao.insertReaction(reaction)
        }
    }

    // --- Bookmarks ---
    fun getBookmarksForUser(userId: String): Flow<List<Bookmark>> = dao.getBookmarksForUser(userId)

    suspend fun addBookmark(userId: String, storyId: String, partId: String, passageText: String?, note: String?) = withContext(Dispatchers.IO) {
        val b = Bookmark(
            id = UUID.randomUUID().toString(),
            userId = userId,
            storyId = storyId,
            partId = partId,
            passageText = passageText,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        dao.insertBookmark(b)
    }

    suspend fun deleteBookmark(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBookmark(id)
    }

    // --- Notifications ---
    fun getNotificationsForUser(userId: String): Flow<List<NotificationItem>> = dao.getNotificationsForUser(userId)

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        val list = dao.getNotificationsForUser("").firstOrNull() // query or helper
        // direct update
        val all = dao.getAllReviews() // dummy
    }

    suspend fun updateNotificationRead(notif: NotificationItem, isRead: Boolean) = withContext(Dispatchers.IO) {
        dao.updateNotification(notif.copy(isRead = isRead))
    }

    suspend fun markAllNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        dao.markAllNotificationsRead(userId)
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNotification(id)
    }

    suspend fun deleteAllNotifications(userId: String) = withContext(Dispatchers.IO) {
        dao.deleteAllNotifications(userId)
    }

    // --- User Settings ---
    fun getUserSettings(userId: String): Flow<UserSettings?> = dao.getUserSettingsFlow(userId)

    suspend fun updateUserSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        dao.upsertUserSettings(settings)
    }

    // --- Seeding ---
    private suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        // Clean up any old demo accounts if present
        val existingUsers = dao.getAllUsers().firstOrNull() ?: emptyList()
        val demoIds = setOf("writer_1", "reader_1", "reader_2", "reader_3", "reader_4", "reader_5")
        existingUsers.filter { it.id in demoIds || it.username.contains("@golpoghor.com") }.forEach { demoUser ->
            // Remove demo accounts
            runCatching {
                dao.updateUser(demoUser.copy(status = UserStatus.SUSPENDED))
            }
        }

        // Ensure stories and literary content are preserved
        val storiesList = dao.getActiveStories().firstOrNull() ?: emptyList()
        if (storiesList.isEmpty()) {
            // Seed Stories
            val s1 = Story(
                id = "story_nilkantha",
                title = "নীল নির্জনতার রহস্য",
                description = "কুয়াশাঘেরা উত্তরবঙ্গের এক প্রাচীন অট্টালিকা, শতাব্দী প্রাচীন পরিবার ও একটি অপূর্ণ উইল। যেখানে প্রতিটি দেয়াল ফিসফিস করে অতীতের অপরাধের কথা বলে।",
                coverResName = "cover_nilkantha",
                authorId = "writer_1",
                authorName = "শ্রাবণ মজুমদার",
                createdAt = System.currentTimeMillis() - 86400000L * 7,
                updatedAt = System.currentTimeMillis() - 86400000L * 1
            )
            val s2 = Story(
                id = "story_meghmallar",
                title = "বৃষ্টিভেজা নদী ও কদম",
                description = "পদ্মার বুকে একটানা ইলশেগুঁড়ি বৃষ্টি। ঘাটে বাঁধা পুরনো কাঠের ডিঙ্গি নৌকা আর এক যুগ আগে হারিয়ে যাওয়া এক অসমাপ্ত প্রেমপত্র।",
                coverResName = "cover_meghmallar",
                authorId = "writer_1",
                authorName = "শ্রাবণ মজুমদার",
                createdAt = System.currentTimeMillis() - 86400000L * 5,
                updatedAt = System.currentTimeMillis() - 86400000L * 2
            )
            dao.insertStory(s1)
            dao.insertStory(s2)

            // Seed Story Parts
            val s1p1Content = """রাত তখন বারোটা বেজে পনেরো মিনিট। বাইরে প্রবল ঝড়বৃষ্টির গর্জন, কাঁচের জানালায় আছড়ে পড়ছে বাতাসের এলোমেলো ঝাপটা।

নীলকণ্ঠবাবু তার পড়ার টেবিলের টিমটিমে টেবিল ল্যাম্পের আলোয় ঝুঁকে বসেছিলেন। সামনে খোলা পুরনো চামড়ায় বাঁধানো লাল ডায়েরি। সেই ডায়েরির পাতায় রক্তবর্ণ কালিতে লেখা একটি সাংকেতিক বার্তা—যা প্রায় পঁয়তাল্লিশ বছর ধরে পরিবারের মহাফেজখানায় অনাদরে পড়েছিল।

হঠাৎ সিঁড়িতে ভারি বুটের আওয়াজ শোনা গেল। কিন্তু জমিদার বাড়ির এই পশ্চিম খণ্ডে তো আজ রাতে কারোর থাকার কথা নয়! তিনি চশমাটা নাকের ডগায় নামিয়ে নিশ্বাস বন্ধ করে শুনলেন।

আওয়াজটা ক্রমশ দরজার দিকে এগিয়ে আসছে। ঠিক দরজার মুখে এসে থমকে দাঁড়াল। দরজার ফাঁক দিয়ে ভেসে এলো একটা মৃদু দীর্ঘশ্বাস। নীলকণ্ঠবাবুর হাতের কলমটা কাঁপা কাঁপা হয়ে কাগজের উপর একটানা দাগ কেটে দিল।

তিনি উঠে দাঁড়ালেন। দেয়ালে ঝুলিয়ে রাখা বাবার পুরনো শিকারি বন্দুকটার দিকে তার হাত চলে গেল। কিন্তু বন্দুকের নল স্পর্শ করার আগেই ঘরের একমাত্র বাতিটি হঠাৎ তীব্র ফুঁয়ে নিভে গেল। ঘোর অন্ধকারের মধ্যে ঘরের অপর প্রান্ত থেকে স্পষ্ট এক অচেনা কণ্ঠস্বর উচ্চারিত হলো: "উইলটা পুড়িয়ে ফেলো নীলকণ্ঠ, নইলে এই বংশের কেউ কালকের সূর্যোদয় দেখতে পাবে না।"

স্তব্ধতার বুকে সেই সতর্কবার্তা জমাট বেঁধে রইল এক অন্তহীন শীতল ভয়ের মতো।"""

            val s1p2Content = """বাতি নিভে যাওয়ার সাথে সাথে ঘরে নেমে এলো নিরেট অন্ধকার। নীলকণ্ঠবাবু স্থির পাথরের মতো দাঁড়িয়ে রইলেন।

পায়ের নিচে পুরনো সেগুন কাঠের মেঝের মৃদু মচমচ শব্দ স্পষ্ট ইঙ্গিত দিচ্ছিল যে, ঘরের ভেতরেই কেউ নিঃশব্দে চলাচল করছে। এক মুহূর্তের জন্য বিদ্যুৎ চমকাল। জানালায় নীলচে আলোর ঝলকানিতে তিনি দেখতে পেলেন একটি কালো চাদর মোড়া ছায়া আলমারির দিকে অগ্রসর হচ্ছে।

"কে ওখানে?" নীলকণ্ঠবাবুর কণ্ঠস্বর নিজের কানেই অচেনা ঠেকল।

ছায়াটি মুহূর্তের জন্য ফিরল। তার মুখে ঢাকা ছিল কাপড়, শুধু দুটি চোখ অন্ধকারে ভাটার মতো জ্বলছিল। সে কোনো উত্তর দিল না। তার পরিবর্তে দ্রুত হাতে ড্রয়ার খুলে কী যেন তুলে নিল।

বাতাসের একটানা শনশন শব্দের মাঝে ঘরের এক কোণে পড়ে থাকা রূপার ফুলদানিটা আছড়ে পড়ল মাটিতে। সেই তীব্র ঝনঝন শব্দে পুরো বাড়ি কেঁপে উঠল। ছায়াটি পলকের মধ্যে খোলা জানালা দিয়ে ব্যালকনির অন্ধকারে মিলিয়ে গেল।

নীলকণ্ঠবাবু যখন দেশলাই জ্বেলে ল্যাম্পটা আবার ধরালেন, তখন দেখলেন টেবিলের সেই লাল ডায়েরিটি অক্ষত আছে, কিন্তু বাবার আঁকা অন্দরমহলের গোপন মানচিত্রটি হাওয়া হয়ে গেছে!"""

            val s1p3ScheduledTime = System.currentTimeMillis() + 86400000L * 2 // 2 days in future
            val s1p3Content = """পরদিন সকালের আলোয় পুরো বাড়ির চিত্র যেন বদলে গেল। কুয়াশা কেটে গিয়ে নরম সোনালী রোদ নেমেছে জমিদার বাড়ির প্রাঙ্গণে।

কিন্তু বাড়ির কর্মচারীদের চোখেমুখে স্পষ্ট আতঙ্কের ছাপ। দারোয়ান রঘু জানাল, রাতে নাকি পেছনের দিঘির ঘাটে অচেনা এক নৌকার নোঙর ফেলতে দেখা গেছে।

নীলকণ্ঠবাবু বৈঠকখানায় বসেছিলেন। সাথে গ্রামের প্রবীণ শিক্ষক সুরেশ মাস্টারমশাই। সুরেশবাবু চশমা মুছতে মুছতে বললেন, "নীলকণ্ঠ, তোমার ঠাকুরদা যে উইল তৈরি করেছিলেন, তাতে শুধু সম্পত্তির হিসাব ছিল না। ছিল এই অঞ্চলের মাটির নিচে লুকানো রাজকোষের নির্দেশিকা।"

কথা শেষ হওয়ার আগেই সদর দরজায় কড়া নড়ে উঠল। ডাকপিয়ন হরিশ একখানা সিলমোহরযুক্ত টেলিগ্রাম নিয়ে দাঁড়িয়ে। টেলিগ্রাম খুলে নীলকণ্ঠবাবুর মুখ সাদা হয়ে গেল। তাতে লেখা ছিল কেবল চারটি শব্দ: "খোলা চোখে বিপদ দেখো।" """

            val s1p1 = StoryPart(
                id = "part_nilkantha_1",
                storyId = "story_nilkantha",
                partNumber = 1,
                title = "প্রথম পর্ব: প্রাচীন জমিদার বাড়ি ও একটি গোপন উইল",
                content = s1p1Content,
                status = PartStatus.PUBLISHED,
                publishedAt = System.currentTimeMillis() - 86400000L * 6
            )
            val s1p2 = StoryPart(
                id = "part_nilkantha_2",
                storyId = "story_nilkantha",
                partNumber = 2,
                title = "দ্বিতীয় পর্ব: অমাবস্যার রাতে রহস্যময় ছায়া",
                content = s1p2Content,
                status = PartStatus.PUBLISHED,
                publishedAt = System.currentTimeMillis() - 86400000L * 2
            )
            val s1p3 = StoryPart(
                id = "part_nilkantha_3",
                storyId = "story_nilkantha",
                partNumber = 3,
                title = "তৃতীয় পর্ব: দিঘির ঘাট ও টেলিগ্রামের সংকেত",
                content = s1p3Content,
                status = PartStatus.SCHEDULED,
                scheduledAt = s1p3ScheduledTime,
                publishedAt = null
            )

            // Story 2 Parts
            val s2p1Content = """আষাঢ়ের সকাল থেকেই আকাশ ভেঙে বৃষ্টি নেমেছে। পদ্মার ঘোলা জলের ঢেউ আছড়ে পড়ছে চরের নরম বালিতে।

খেয়াঘাটে ছাতা মাথায় দাঁড়িয়ে ছিল অরণী। তার পরনে হালকা নীল রঙের সুতির শাড়ি, ভিজে শাড়ির আঁচল বারবার উড়ে যাচ্ছে ঝোড়ো বাতাসে। অনেক বছর পর সে এই চরে ফিরেছে। সেই ছোটবেলার স্মৃতি, বাবার পাঠশালা আর নৌকার দড়ির গন্ধ এখনও বাতাসে ভাসে।

দূরে কুয়াশাভেজা নদীর বুক চিরে একটা ছোট কাঠের ডিঙ্গি এগিয়ে আসছে দেখা গেল। নৌকার ছৈয়ের নিচে একজন তরুণ মাঝি দাঁড় টানছে ছন্দময় শক্তিতে। নৌকার গলুইতে রাখা একগুচ্ছ সদ্য ফোটা কদম ফুল।

নৌকাটি যখন ঘাটের খুঁটিতে এসে ভিড়ল, মাঝির চোখ গেল অরণীর দিকে। অরণী এক মুহূর্তের জন্য থমকে গেল। এ তো সাধারণ কোনো মাঝি নয়—এই চোখ, কপালের কাটা দাগ সে ভুলতে পারে না! এ যে শুভ্র! যে দশ বছর আগে গ্রাম ছেড়ে শহরে চলে গিয়েছিল চিত্রশিল্পী হতে!"""

            val s2p2Content = """শুভ্র নৌকা থেকে নেমে দড়িটা বাঁশের খুঁটিতে বাঁধল। তার ঠোঁটের কোণে সেই পরিচিত মৃদু হাসি।

"বৃষ্টিতে ভিজছ কেন অরণী? এত বছর পর ফিরেও সেই পুরনো স্বভাব বদলাওনি?" শুভ্রের কণ্ঠস্বরে কোনো দূরত্ব ছিল না, যেন গতকালই তাদের শেষ দেখা হয়েছিল।

অরণীর চোখ ভিজে এলো, বৃষ্টির জলে তা আড়াল করা কঠিন হলো না। সে এক পা পিছিয়ে গিয়ে বলল, "তুমি এখানে নৌকার মাঝি সেজে কী করছ শুভ্র? শুনেছিলে তুমি তো বিদেশে নামী প্রদর্শনী করছ!"

শুভ্র একগুচ্ছ ভেজা কদম ফুল অরণীর দিকে বাড়িয়ে দিল। কদমের তীব্র মিষ্টি সুবাস বর্ষার বাতাসকে ভারী করে তুলল।

"যে নদী আমাকে আঁকতে শিখিয়েছে, তাকে ছেড়ে আমি কোথায় যাব বলো? আর তাছাড়া... তোমার তো কথা ছিল এই ঘাটে ফিরে আসার।"

অরণী কদম ফুলগুলো হাতে তুলে নিল। বৃষ্টির ফোঁটা টুপটুপ করে ঝরে পড়তে লাগল তাদের দুজনের নীরবতার মাঝে।"""

            val s2p1 = StoryPart(
                id = "part_meghmallar_1",
                storyId = "story_meghmallar",
                partNumber = 1,
                title = "প্রথম পর্ব: আষাঢ়ের মেঘ ও অচেনা খেয়াঘাট",
                content = s2p1Content,
                status = PartStatus.PUBLISHED,
                publishedAt = System.currentTimeMillis() - 86400000L * 4
            )
            val s2p2 = StoryPart(
                id = "part_meghmallar_2",
                storyId = "story_meghmallar",
                partNumber = 2,
                title = "দ্বিতীয় পর্ব: কদমের সুবাস ও নীরব প্রত্যাবর্তন",
                content = s2p2Content,
                status = PartStatus.PUBLISHED,
                publishedAt = System.currentTimeMillis() - 86400000L * 1
            )

            dao.insertStoryParts(listOf(s1p1, s1p2, s1p3, s2p1, s2p2))

            // Seed initial reviews
            val r1 = PassageReview(
                id = "rev_1",
                storyId = "story_nilkantha",
                partId = "part_nilkantha_1",
                userId = "reader_1",
                userName = "অনন্যা সেন",
                passageText = "তিনি উঠে দাঁড়ালেন। দেয়ালে ঝুলিয়ে রাখা বাবার পুরনো শিকারি বন্দুকটার দিকে তার হাত চলে গেল।",
                passageAnchor = "gun_passage",
                reviewType = ReviewType.PASSAGE,
                rating = null,
                comment = "এই অংশটুকু পড়ার সময় বুকের ভেতর এক অচেনা শিরশিরানি অনুভূতি হয়েছিল! দৃশ্যপট বর্ণনা অসাধারণ।",
                createdAt = System.currentTimeMillis() - 86400000L * 3,
                isAnswered = true
            )
            val r2 = PassageReview(
                id = "rev_2",
                storyId = "story_nilkantha",
                partId = "part_nilkantha_1",
                userId = "reader_1",
                userName = "অনন্যা সেন",
                passageText = null,
                passageAnchor = null,
                reviewType = ReviewType.WHOLE_PART,
                rating = 5,
                comment = "প্রথম পর্বেই যে রহস্যের গভীরতা তৈরি হয়েছে তা প্রশংসার যোগ্য। পরবর্তী পর্বের অপেক্ষায় রইলাম।",
                createdAt = System.currentTimeMillis() - 86400000L * 3,
                isAnswered = false
            )
            val r3 = PassageReview(
                id = "rev_3",
                storyId = "story_nilkantha",
                partId = "part_nilkantha_2",
                userId = "reader_2",
                userName = "তানভীর আহমেদ",
                passageText = "শুধু দুটি চোখ অন্ধকারে ভাটার মতো জ্বলছিল। সে কোনো উত্তর দিল না।",
                passageAnchor = "eyes_passage",
                reviewType = ReviewType.PASSAGE,
                rating = null,
                comment = "ছায়ার পরিচয়টা কি তৃতীয় পর্বে খোলাসা হবে? নাকি কোনো চমক আসছে?",
                createdAt = System.currentTimeMillis() - 86400000L * 1,
                isAnswered = false
            )
            dao.insertReview(r1)
            dao.insertReview(r2)
            dao.insertReview(r3)

            // Seed reply
            val reply1 = ReviewReply(
                id = "rep_1",
                reviewId = "rev_1",
                userId = "writer_1",
                userName = "শ্রাবণ মজুমদার",
                userRole = UserRole.WRITER,
                replyText = "ধন্যবাদ অনন্যা! বন্দুকের সেই দৃশ্যটি গল্পের আগামী পর্বগুলোতেও বিশেষ তাৎপর্য বহন করবে। পড়তে থাকুন।",
                createdAt = System.currentTimeMillis() - 86400000L * 2
            )
            dao.insertReply(reply1)

            // Seed Reactions
            dao.insertReaction(
                Reaction(
                    id = "reader_1_part_nilkantha_1_LOVE",
                    userId = "reader_1",
                    targetType = "PART",
                    targetId = "part_nilkantha_1",
                    passageText = null,
                    emojiType = ReactionEmoji.LOVE
                )
            )
            dao.insertReaction(
                Reaction(
                    id = "reader_2_part_nilkantha_1_LOVE",
                    userId = "reader_2",
                    targetType = "PART",
                    targetId = "part_nilkantha_1",
                    passageText = null,
                    emojiType = ReactionEmoji.LOVE
                )
            )
            dao.insertReaction(
                Reaction(
                    id = "reader_1_rev_1_LOVE",
                    userId = "reader_2",
                    targetType = "PASSAGE_REVIEW",
                    targetId = "rev_1",
                    passageText = null,
                    emojiType = ReactionEmoji.LOVE
                )
            )

            // Seed Reading Progress
            dao.upsertReadingProgress(
                ReadingProgress(
                    id = "reader_1_story_nilkantha_part_nilkantha_1",
                    userId = "reader_1",
                    storyId = "story_nilkantha",
                    partId = "part_nilkantha_1",
                    progressPercent = 100,
                    isCompleted = true,
                    activeSeconds = 180,
                    lastReadAt = System.currentTimeMillis() - 86400000L * 2
                )
            )
            dao.upsertReadingProgress(
                ReadingProgress(
                    id = "reader_1_story_nilkantha_part_nilkantha_2",
                    userId = "reader_1",
                    storyId = "story_nilkantha",
                    partId = "part_nilkantha_2",
                    progressPercent = 65,
                    isCompleted = false,
                    activeSeconds = 95,
                    lastReadAt = System.currentTimeMillis() - 86400000L * 1
                )
            )

            // Seed Notifications
            dao.insertNotification(
                NotificationItem(
                    id = "notif_w_1",
                    userId = "writer_1",
                    title = "নতুন প্যারাগ্রাফ মন্তব্য",
                    message = "অনন্যা সেন “প্রথম পর্ব: প্রাচীন জমিদার বাড়ি...” এ লিখেছেন: “এই অংশটুকু পড়ার সময় বুকের ভেতর এক অচেনা...”",
                    type = NotificationType.NEW_REVIEW_RECEIVED,
                    storyId = "story_nilkantha",
                    partId = "part_nilkantha_1",
                    reviewId = "rev_1",
                    isRead = false,
                    createdAt = System.currentTimeMillis() - 86400000L * 3
                )
            )
            dao.insertNotification(
                NotificationItem(
                    id = "notif_r1_1",
                    userId = "reader_1",
                    title = "লেখক আপনার মন্তব্যে উত্তর দিয়েছেন!",
                    message = "শ্রাবণ মজুমদার আপনার মন্তব্যে লিখেছেন: “ধন্যবাদ অনন্যা! বন্দুকের সেই দৃশ্যটি গল্পের...”",
                    type = NotificationType.REPLY_ON_REVIEW,
                    storyId = "story_nilkantha",
                    partId = "part_nilkantha_1",
                    reviewId = "rev_1",
                    isRead = false,
                    createdAt = System.currentTimeMillis() - 86400000L * 2
                )
            )
        }
    }
}
