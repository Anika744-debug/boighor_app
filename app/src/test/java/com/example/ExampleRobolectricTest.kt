package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.GolpoGhorDatabase
import com.example.data.model.UserRole
import com.example.data.repository.GolpoGhorRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: GolpoGhorDatabase
  private lateinit var repository: GolpoGhorRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, GolpoGhorDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = GolpoGhorRepository(database.dao(), context)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("গল্পঘর", appName)
  }

  @Test
  fun `admin account registration requires email identity verification and starts as reader`() = runBlocking {
    val regResult = repository.register(
      email = "Mdnajrulislam771@gmail.com",
      password = "securePassword123",
      displayName = "Md Nazrul Islam"
    )
    assertTrue(regResult.isSuccess)
    val user = regResult.getOrThrow()

    // Must start as READER and unverified; no admin grant on unverified string!
    assertEquals(UserRole.READER, user.role)
    assertFalse(user.isEmailVerified)
    assertNotNull(user.verificationCode)
    assertEquals(6, user.verificationCode?.length)

    // Verify wrong code fails
    val failVerify = repository.verifyEmailIdentity(user.id, "000000")
    assertTrue(failVerify.isFailure)

    // Verify with correct code succeeds and activates ADMIN role
    val correctCode = user.verificationCode!!
    val verifyResult = repository.verifyEmailIdentity(user.id, correctCode)
    assertTrue(verifyResult.isSuccess)
    val verifiedAdmin = verifyResult.getOrThrow()
    assertEquals(UserRole.ADMIN, verifiedAdmin.role)
    assertTrue(verifiedAdmin.isEmailVerified)
  }

  @Test
  fun `regular user registers as reader and cannot be elevated to admin`() = runBlocking {
    val regResult = repository.register(
      email = "reader@example.com",
      password = "readerPassword123",
      displayName = "Sabbir Ahmed"
    )
    assertTrue(regResult.isSuccess)
    val reader = regResult.getOrThrow()
    assertEquals(UserRole.READER, reader.role)

    // Even if verified, non-admin email stays READER
    val verifyResult = repository.verifyEmailIdentity(reader.id, reader.verificationCode!!)
    assertTrue(verifyResult.isSuccess)
    assertEquals(UserRole.READER, verifyResult.getOrThrow().role)
  }

  @Test
  fun `only verified admin can promote reader to writer and demote back to reader`() = runBlocking {
    // 1. Register and verify Admin
    val adminUser = repository.register(
      email = "Mdnajrulislam771@gmail.com",
      password = "adminPassword123",
      displayName = "Nazrul Admin"
    ).getOrThrow()
    val verifiedAdmin = repository.verifyEmailIdentity(adminUser.id, adminUser.verificationCode!!).getOrThrow()

    // 2. Register Reader
    val readerUser = repository.register(
      email = "reader2@example.com",
      password = "readerPassword123",
      displayName = "Afsana Mimi"
    ).getOrThrow()

    // 3. Admin promotes Reader to Writer
    val promoteResult = repository.updateUserRole(verifiedAdmin.id, readerUser.id, UserRole.WRITER)
    assertTrue(promoteResult.isSuccess)

    val updatedToWriter = database.dao().getUserById(readerUser.id)
    assertEquals(UserRole.WRITER, updatedToWriter?.role)

    // 4. Admin demotes Writer back to Reader
    val demoteResult = repository.updateUserRole(verifiedAdmin.id, readerUser.id, UserRole.READER)
    assertTrue(demoteResult.isSuccess)

    val updatedBackToReader = database.dao().getUserById(readerUser.id)
    assertEquals(UserRole.READER, updatedBackToReader?.role)

    // 5. Reader cannot promote themselves or anyone else
    val unauthorizedResult = repository.updateUserRole(readerUser.id, readerUser.id, UserRole.WRITER)
    assertTrue(unauthorizedResult.isFailure)

    // 6. Admin cannot promote anyone to ADMIN
    val cannotMakeAdmin = repository.updateUserRole(verifiedAdmin.id, readerUser.id, UserRole.ADMIN)
    assertTrue(cannotMakeAdmin.isFailure)

    // 7. Admin role itself cannot be modified
    val cannotDemoteAdmin = repository.updateUserRole(verifiedAdmin.id, verifiedAdmin.id, UserRole.READER)
    assertTrue(cannotDemoteAdmin.isFailure)
  }
}
