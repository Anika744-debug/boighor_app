package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserRole
import com.example.data.repository.GolpoGhorRepository
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Register

    // Google Sign-In State
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }

    // Login Form State
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Register Form State
    var regDisplayName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regError by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    // Email Identity Verification State
    var showVerificationModal by remember { mutableStateOf(false) }
    var verificationTargetEmail by remember { mutableStateOf("") }
    var verificationInputCode by remember { mutableStateOf("") }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var isVerifyingCode by remember { mutableStateOf(false) }
    var activeVerificationCode by remember { mutableStateOf<String?>(null) }

    // Forgot Password State
    var showForgotPasswordModal by remember { mutableStateOf(false) }
    var forgotPasswordTab by remember { mutableIntStateOf(0) } // 0 = Reset Email Link, 1 = OTP
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordStep by remember { mutableIntStateOf(1) }
    var forgotPasswordOtp by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotNewPasswordVisible by remember { mutableStateOf(false) }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }
    var forgotPasswordSuccessMsg by remember { mutableStateOf<String?>(null) }
    var isResettingPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Branding
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(appColors.surface)
                    .border(1.dp, appColors.border, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.golpoghor_logo),
                    contentDescription = "গল্পঘর লোগো",
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "গল্পঘর",
                fontFamily = LocalReadingFont.current,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.text
            )
            Text(
                text = "ব্যক্তিগত বাংলা সাহিত্য পাঠ ও ধারাবাহিক প্রকাশনা",
                fontSize = 13.sp,
                color = appColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Google Sign-In Card (Firebase Authentication)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "গুগল অ্যাকাউন্ট দিয়ে প্রবেশ করুন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )

                    // Single, Official Sign in with Google Button
                    Button(
                        onClick = {
                            val act = context.findActivity()
                            isGoogleSigningIn = true
                            googleSignInError = null

                            if (act != null) {
                                val credentialManager = CredentialManager.create(context)
                                val clientId = try {
                                    context.getString(R.string.default_web_client_id)
                                } catch (e: Exception) {
                                    null
                                }

                                if (clientId.isNullOrBlank()) {
                                    isGoogleSigningIn = false
                                    googleSignInError = "গুগল সাইন-ইন কনফিগারেশন ত্রুটি: default_web_client_id পাওয়া যায়নি।"
                                    return@Button
                                }

                                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                                coroutineScope.launch {
                                    try {
                                        val result = credentialManager.getCredential(act, request)
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                            val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                                            val fbUser = authResult.user
                                            if (fbUser != null) {
                                                val syncRes = viewModel.loginWithGoogleUser(fbUser)
                                                isGoogleSigningIn = false
                                                syncRes.onSuccess { u ->
                                                    val roleName = when (u.role) {
                                                        UserRole.ADMIN -> "অ্যাডমিন (Owner)"
                                                        UserRole.WRITER -> "লেখক (Writer)"
                                                        UserRole.READER -> "পাঠক (Reader)"
                                                    }
                                                    Toast.makeText(context, "স্বাগতম, ${u.displayName} ($roleName)!", Toast.LENGTH_SHORT).show()
                                                    viewModel.navigateTo(ScreenDestination.Home)
                                                }.onFailure { err ->
                                                    googleSignInError = err.message ?: "গুগল অ্যাকাউন্ট সিঙ্ক করা যায়নি।"
                                                }
                                            } else {
                                                isGoogleSigningIn = false
                                                googleSignInError = "গুগল অথেন্টিকেশন ব্যর্থ হয়েছে।"
                                            }
                                        } else {
                                            isGoogleSigningIn = false
                                            googleSignInError = "অনুপযুক্ত গুগল ক্রেডেনশিয়াল।"
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
                                        isGoogleSigningIn = false
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Google Sign-In exception: ${e.message}", e)
                                        isGoogleSigningIn = false
                                        googleSignInError = e.localizedMessage ?: "গুগল সাইন-ইন সম্পন্ন করা যায়নি।"
                                    }
                                }
                            } else {
                                isGoogleSigningIn = false
                                googleSignInError = "অ্যাক্টিভিটি প্রস্তুত নয়।"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.surface,
                            contentColor = appColors.text
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, appColors.primary),
                        enabled = !isGoogleSigningIn
                    ) {
                        if (isGoogleSigningIn) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = appColors.primary, strokeWidth = 2.dp)
                        } else {
                            // Google G Logo / Icon
                            Surface(
                                shape = CircleShape,
                                color = appColors.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = appColors.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.text
                            )
                        }
                    }

                    if (googleSignInError != null) {
                        Text(
                            text = googleSignInError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.border)
                Text(
                    text = " অথবা ইমেইল দিয়ে ",
                    fontSize = 12.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.border)
            }
        }

        // Auth Tabs (Login vs Register)
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = appColors.surface,
                contentColor = appColors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, appColors.border, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        loginError = null
                        regError = null
                    },
                    modifier = Modifier.testTag("tab_login"),
                    text = {
                        Text(
                            text = "লগইন",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        loginError = null
                        regError = null
                    },
                    modifier = Modifier.testTag("tab_register"),
                    text = {
                        Text(
                            text = "নতুন নিবন্ধন",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Form Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        // --- LOGIN FORM ---
                        Text(
                            text = "আপনার অ্যাকাউন্টে প্রবেশ করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )

                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = {
                                loginEmail = it
                                loginError = null
                            },
                            label = { Text("ইমেইল ঠিকানা") },
                            placeholder = { Text("উদাহরণ: reader@email.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = appColors.primary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                loginError = null
                            },
                            label = { Text("পাসওয়ার্ড") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = appColors.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (loginPasswordVisible) "গোপন করুন" else "দেখান",
                                        tint = appColors.textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    forgotPasswordEmail = loginEmail.ifBlank { "" }
                                    forgotPasswordTab = 0 // 0 = Reset Email Link
                                    forgotPasswordStep = 1
                                    forgotPasswordOtp = ""
                                    forgotNewPassword = ""
                                    forgotPasswordError = null
                                    forgotPasswordSuccessMsg = null
                                    showForgotPasswordModal = true
                                },
                                modifier = Modifier.testTag("forgot_password_button"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = appColors.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "পাসওয়ার্ড ভুলে গেছেন?",
                                    fontSize = 13.sp,
                                    color = appColors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = {
                                    forgotPasswordEmail = loginEmail.ifBlank { "" }
                                    forgotPasswordTab = 1 // 1 = OTP
                                    forgotPasswordStep = 1
                                    forgotPasswordOtp = ""
                                    forgotNewPassword = ""
                                    forgotPasswordError = null
                                    forgotPasswordSuccessMsg = null
                                    showForgotPasswordModal = true
                                },
                                modifier = Modifier.testTag("forgot_password_otp_button"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pin,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = appColors.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ওটিপি (OTP) রিসেট",
                                    fontSize = 12.sp,
                                    color = appColors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (loginError != null) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = loginError!!,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                    loginError = "অনুগ্রহ করে ইমেইল ও পাসওয়ার্ড প্রদান করুন।"
                                    return@Button
                                }
                                isLoggingIn = true
                                coroutineScope.launch {
                                    val result = viewModel.loginWithCredentials(loginEmail, loginPassword)
                                    isLoggingIn = false
                                    result.onSuccess { user ->
                                        val isAdminEmail = user.username.equals(GolpoGhorRepository.ADMIN_EMAIL, ignoreCase = true)
                                        if (isAdminEmail && !user.isEmailVerified) {
                                            // Admin account exists but identity is not verified yet
                                            verificationTargetEmail = user.username
                                            activeVerificationCode = user.verificationCode
                                            verificationInputCode = ""
                                            verificationError = null
                                            showVerificationModal = true
                                        } else {
                                            val roleText = when (user.role) {
                                                UserRole.ADMIN -> "অ্যাডমিন"
                                                UserRole.WRITER -> "লেখক"
                                                UserRole.READER -> "পাঠক"
                                            }
                                            Toast.makeText(context, "স্বাগতম, ${user.displayName} ($roleText)!", Toast.LENGTH_SHORT).show()
                                            viewModel.navigateTo(ScreenDestination.Home)
                                        }
                                    }.onFailure { err ->
                                        loginError = err.message ?: "লগইন ব্যর্থ হয়েছে।"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appColors.primary,
                                contentColor = appColors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoggingIn
                        ) {
                            if (isLoggingIn) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = appColors.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("লগইন করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // --- REGISTRATION FORM ---
                        Text(
                            text = "নতুন অ্যাকাউন্ট তৈরি করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )

                        OutlinedTextField(
                            value = regDisplayName,
                            onValueChange = {
                                regDisplayName = it
                                regError = null
                            },
                            label = { Text("আপনার পূর্ণ নাম") },
                            placeholder = { Text("যেমন: নজমুল ইসলাম") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = appColors.primary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = {
                                regEmail = it
                                regError = null
                            },
                            label = { Text("ইমেইল ঠিকানা") },
                            placeholder = { Text("যেমন: user@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = appColors.primary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        // Admin designation hint if matches admin email
                        if (regEmail.trim().equals(GolpoGhorRepository.ADMIN_EMAIL, ignoreCase = true)) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "এই ইমেইলটি সিস্টেমের একমাত্র 'অ্যাডমিন' হিসেবে সংরক্ষিত। নিবন্ধনের পর পরিচয় যাচাই সম্পন্ন হলেই কেবল অ্যাডমিন ক্ষমতা অর্পিত হবে। ক্লায়েন্ট স্ট্রিংয়ের ভিত্তিতে সরাসরি কাউকে অ্যাডমিন ভূমিকা দেওয়া হয় না।",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "সকল নতুন নিবন্ধিত ব্যবহারকারী স্বয়ংক্রিয়ভাবে 'পাঠক' ভূমিকা লাভ করবেন।",
                                fontSize = 11.sp,
                                color = appColors.textSecondary
                            )
                        }

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = {
                                regPassword = it
                                regError = null
                            },
                            label = { Text("পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = appColors.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                    Icon(
                                        imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = appColors.textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = {
                                regConfirmPassword = it
                                regError = null
                            },
                            label = { Text("পাসওয়ার্ড নিশ্চিতকরণ") },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = appColors.primary)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        if (regError != null) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = regError!!,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (regDisplayName.isBlank()) {
                                    regError = "অনুগ্রহ করে আপনার নাম দিন।"
                                    return@Button
                                }
                                if (regEmail.isBlank() || !regEmail.contains("@") || !regEmail.contains(".")) {
                                    regError = "সঠিক ইমেইল ঠিকানা দিন।"
                                    return@Button
                                }
                                if (regPassword.length < 6) {
                                    regError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
                                    return@Button
                                }
                                if (regPassword != regConfirmPassword) {
                                    regError = "পাসওয়ার্ড দুটির মিল নেই!"
                                    return@Button
                                }

                                isRegistering = true
                                coroutineScope.launch {
                                    val result = viewModel.registerUser(regEmail, regPassword, regDisplayName)
                                    isRegistering = false
                                    result.onSuccess { newUser ->
                                        val isAdminEmail = newUser.username.equals(GolpoGhorRepository.ADMIN_EMAIL, ignoreCase = true)
                                        if (isAdminEmail) {
                                            // Identity verification required before granting Admin role
                                            verificationTargetEmail = newUser.username
                                            activeVerificationCode = newUser.verificationCode
                                            verificationInputCode = ""
                                            verificationError = null
                                            showVerificationModal = true
                                        } else {
                                            Toast.makeText(context, "নিবন্ধন সফল হয়েছে! স্বাগতম পাঠক হিসেবে।", Toast.LENGTH_LONG).show()
                                            viewModel.navigateTo(ScreenDestination.Home)
                                        }
                                    }.onFailure { err ->
                                        regError = err.message ?: "নিবন্ধন ব্যর্থ হয়েছে।"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("register_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appColors.primary,
                                contentColor = appColors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isRegistering
                        ) {
                            if (isRegistering) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = appColors.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("নিবন্ধন সম্পন্ন করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Info footer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "নিরাপত্তা ও ভূমিকা নীতি:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = appColors.text
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Mdnajrulislam771@gmail.com একমাত্র অ্যাডমিন একাউন্ট।\n• পরিচয় যাচাইয়ের পরেই কেবল অ্যাডমিন ভূমিকা কার্যকর হয়।\n• অন্যান্য ব্যবহারকারী স্বয়ংক্রিয়ভাবে পাঠক হিসেবে নিবন্ধিত হবেন।\n• শুধুমাত্র অনুমোদিত ও যাচাইকৃত অ্যাডমিন পাঠকদের লেখক ভূমিকায় উন্নীত বা প্রত্যাহার করতে পারেন।",
                        fontSize = 11.sp,
                        color = appColors.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Email Identity Verification Modal
    if (showVerificationModal) {
        AlertDialog(
            onDismissRequest = {
                showVerificationModal = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ইমেইল পরিচয় যাচাইকরণ",
                        fontFamily = LocalReadingFont.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "“$verificationTargetEmail” হলো গল্পঘরের একমাত্র সংরক্ষিত অ্যাডমিন অ্যাকাউন্ট।\n\nনিরাপত্তা নীতি অনুসারে ক্লায়েন্ট স্ট্রিংয়ের ভিত্তিতে সরাসরি কাউকে অ্যাডমিন ভূমিকা দেওয়া হয় না। আপনার ইমেইল অ্যাকাউন্টের মালিকানা নিশ্চিত করতে নিচে ৬-সংখ্যার সিকিউরিটি কোডটি প্রদান করুন।",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = appColors.text
                    )

                    // Verification Code Display Box (Simulated Secure Dispatch)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "প্রেরিত সিকিউরিটি ভেরিফিকেশন কোড:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeVerificationCode ?: "কোড তৈরি হচ্ছে...",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 4.sp,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.testTag("verification_code_display")
                                )
                                TextButton(
                                    onClick = {
                                        activeVerificationCode?.let { verificationInputCode = it }
                                    },
                                    modifier = Modifier.testTag("autofill_verification_code_button")
                                ) {
                                    Text("কোড বসান", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = verificationInputCode,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                verificationInputCode = input.filter { it.isDigit() }
                                verificationError = null
                            }
                        },
                        label = { Text("৬-সংখ্যার কোড প্রবেশ করান") },
                        placeholder = { Text("যেমন: 123456") },
                        leadingIcon = {
                            Icon(Icons.Default.Security, contentDescription = null, tint = appColors.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verification_code_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF047857),
                            unfocusedBorderColor = appColors.border
                        )
                    )

                    if (verificationError != null) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = verificationError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.resendVerificationCode(verificationTargetEmail)
                                    res.onSuccess { newCode ->
                                        activeVerificationCode = newCode
                                        Toast.makeText(context, "নতুন ভেরিফিকেশন কোড পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        verificationError = it.message ?: "কোড পাঠানো যায়নি।"
                                    }
                                }
                            },
                            modifier = Modifier.testTag("resend_code_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পুনরায় কোড পাঠান", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (verificationInputCode.length < 6) {
                            verificationError = "অনুগ্রহ করে পূর্ণ ৬-সংখ্যার কোড দিন।"
                            return@Button
                        }
                        isVerifyingCode = true
                        coroutineScope.launch {
                            val res = viewModel.verifyEmailIdentity(verificationTargetEmail, verificationInputCode)
                            isVerifyingCode = false
                            res.onSuccess { verifiedAdmin ->
                                showVerificationModal = false
                                Toast.makeText(
                                    context,
                                    "অভিনন্দন! আপনার পরিচয় সফলভাবে যাচাই করা হয়েছে। অ্যাডমিন হিসেবে স্বাগতম!",
                                    Toast.LENGTH_LONG
                                ).show()
                                viewModel.navigateTo(ScreenDestination.Home)
                            }.onFailure { err ->
                                verificationError = err.message ?: "যাচাইকরণ ব্যর্থ হয়েছে।"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF047857),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isVerifyingCode,
                    modifier = Modifier.testTag("verify_code_button")
                ) {
                    if (isVerifyingCode) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পরিচয় যাচাই ও সক্রিয়করণ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVerificationModal = false }
                ) {
                    Text("পরে করব", color = appColors.textSecondary)
                }
            }
        )
    }

    // Forgot Password Modal Dialog
    if (showForgotPasswordModal) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = appColors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পাসওয়ার্ড ভুলে গেছেন?",
                        fontFamily = LocalReadingFont.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mode Switcher Tabs
                    PrimaryTabRow(
                        selectedTabIndex = forgotPasswordTab,
                        containerColor = appColors.background,
                        contentColor = appColors.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = forgotPasswordTab == 0,
                            onClick = {
                                forgotPasswordTab = 0
                                forgotPasswordError = null
                                forgotPasswordSuccessMsg = null
                            },
                            text = { Text("রিসেট লিংক (Email)", fontSize = 12.sp, fontWeight = if (forgotPasswordTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = forgotPasswordTab == 1,
                            onClick = {
                                forgotPasswordTab = 1
                                forgotPasswordError = null
                                forgotPasswordSuccessMsg = null
                            },
                            text = { Text("ওটিপি (OTP) কোড", fontSize = 12.sp, fontWeight = if (forgotPasswordTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }

                    if (forgotPasswordTab == 0) {
                        // --- EMAIL RESET LINK FLOW ---
                        Text(
                            text = "আপনার নিবন্ধিত ইমেইল ঠিকানা প্রদান করুন। আমরা পাসওয়ার্ড পরিবর্তন করার জন্য একটি নিরাপদ রিসেট লিংক পাঠাব:",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = {
                                forgotPasswordEmail = it
                                forgotPasswordError = null
                                forgotPasswordSuccessMsg = null
                            },
                            label = { Text("নিবন্ধিত ইমেইল ঠিকানা") },
                            placeholder = { Text("যেমন: reader@email.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = appColors.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_link_email_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            )
                        )

                        if (forgotPasswordSuccessMsg != null) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = forgotPasswordSuccessMsg!!,
                                        color = Color(0xFF047857),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // --- OTP RESET FLOW ---
                        if (forgotPasswordStep == 1) {
                            Text(
                                text = "নিবন্ধিত ইমেইল দিলে একটি ৬-সংখ্যার ওটিপি কোড প্রস্তুত করা হবে:",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                            OutlinedTextField(
                                value = forgotPasswordEmail,
                                onValueChange = {
                                    forgotPasswordEmail = it
                                    forgotPasswordError = null
                                },
                                label = { Text("ইমেইল ঠিকানা") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = appColors.primary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_email_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appColors.primary,
                                    unfocusedBorderColor = appColors.border
                                )
                            )
                        } else {
                            Text(
                                text = "আপনার ইমেইলে ($forgotPasswordEmail) প্রেরিত ৬-সংখ্যার ওটিপি কোড এবং নতুন পাসওয়ার্ড দিন:",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                            OutlinedTextField(
                                value = forgotPasswordOtp,
                                onValueChange = {
                                    forgotPasswordOtp = it
                                    forgotPasswordError = null
                                },
                                label = { Text("৬-সংখ্যার ওটিপি কোড") },
                                placeholder = { Text("যেমন: 123456") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = appColors.primary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_otp_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appColors.primary,
                                    unfocusedBorderColor = appColors.border
                                )
                            )
                            OutlinedTextField(
                                value = forgotNewPassword,
                                onValueChange = {
                                    forgotNewPassword = it
                                    forgotPasswordError = null
                                },
                                label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = appColors.primary) },
                                trailingIcon = {
                                    IconButton(onClick = { forgotNewPasswordVisible = !forgotNewPasswordVisible }) {
                                        Icon(
                                            imageVector = if (forgotNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (forgotNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_new_password_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appColors.primary,
                                    unfocusedBorderColor = appColors.border
                                )
                            )
                        }
                    }

                    if (forgotPasswordError != null) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = forgotPasswordError!!, color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordTab == 0) {
                            // Send Reset Email Link
                            if (forgotPasswordEmail.isBlank() || !forgotPasswordEmail.contains("@") || !forgotPasswordEmail.contains(".")) {
                                forgotPasswordError = "অনুগ্রহ করে সঠিক ইমেইল ঠিকানা দিন।"
                                return@Button
                            }
                            isResettingPassword = true
                            coroutineScope.launch {
                                val res = viewModel.sendPasswordResetEmail(forgotPasswordEmail)
                                isResettingPassword = false
                                res.onSuccess {
                                    forgotPasswordError = null
                                    forgotPasswordSuccessMsg = "পাসওয়ার্ড রিসেট লিংক সফলভাবে পাঠানো হয়েছে! আপনার ইনবক্স এবং স্প্যাম ফোল্ডার পরীক্ষা করুন।"
                                    Toast.makeText(context, "রিসেট লিংক পাঠানো হয়েছে!", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    forgotPasswordError = err.message ?: "রিসেট লিংক পাঠানো যায়নি।"
                                }
                            }
                        } else {
                            // OTP Flow
                            if (forgotPasswordStep == 1) {
                                if (forgotPasswordEmail.isBlank()) {
                                    forgotPasswordError = "অনুগ্রহ করে ইমেইল ঠিকানা দিন।"
                                    return@Button
                                }
                                isResettingPassword = true
                                coroutineScope.launch {
                                    val res = viewModel.requestPasswordResetOtp(forgotPasswordEmail)
                                    isResettingPassword = false
                                    res.onSuccess { code ->
                                        forgotPasswordStep = 2
                                        forgotPasswordError = null
                                        Toast.makeText(context, "ওটিপি কোড পাঠানো হয়েছে! (কোড: $code)", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        forgotPasswordError = err.message ?: "রিকোয়েস্ট ব্যর্থ হয়েছে।"
                                    }
                                }
                            } else {
                                if (forgotPasswordOtp.length < 6 || forgotNewPassword.length < 6) {
                                    forgotPasswordError = "সঠিক ওটিপি এবং কমপক্ষে ৬ অক্ষরের নতুন পাসওয়ার্ড দিন।"
                                    return@Button
                                }
                                isResettingPassword = true
                                coroutineScope.launch {
                                    val res = viewModel.resetPasswordWithOtp(forgotPasswordEmail, forgotPasswordOtp, forgotNewPassword)
                                    isResettingPassword = false
                                    res.onSuccess { user ->
                                        showForgotPasswordModal = false
                                        loginEmail = user.username
                                        loginPassword = ""
                                        Toast.makeText(context, "পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে! এখন নতুন পাসওয়ার্ড দিয়ে লগইন করুন।", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        forgotPasswordError = err.message ?: "পাসওয়ার্ড পরিবর্তন ব্যর্থ হয়েছে।"
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isResettingPassword,
                    modifier = Modifier.testTag("forgot_submit_button")
                ) {
                    if (isResettingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = appColors.onPrimary, strokeWidth = 2.dp)
                    } else {
                        val btnText = if (forgotPasswordTab == 0) {
                            "রিসেট লিংক পাঠান"
                        } else {
                            if (forgotPasswordStep == 1) "ওটিপি কোড পাঠান" else "পাসওয়ার্ড পরিবর্তন করুন"
                        }
                        Text(btnText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordModal = false }) {
                    Text("ফিরে যান", color = appColors.textSecondary)
                }
            },
            containerColor = appColors.surface
        )
    }
}

/**
 * Traverses context wrappers to find the underlying Activity safely.
 */
private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
