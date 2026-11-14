package com.example.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.repository.AuthProfileRepository
import com.example.data.util.MediaHelper
import com.example.ui.components.DarlingLogoBadge
import com.example.ui.components.DarlingMediaImage
import com.example.ui.theme.DarlingCoral
import com.example.ui.theme.DarlingGradientBrush
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun LoginScreen(
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onPreviewFeatures: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarlingViolet.copy(alpha = 0.14f),
                        DarlingRose.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DarlingLogoBadge(size = 88.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Darling Date",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Share moments, discover reels, and spark real connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sign in securely with your Google account. Returning users go straight to their Feed; verified administrators are routed to the Admin Dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    authError?.let { errorMsg ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remember me on this device",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = rememberMe,
                            onCheckedChange = onRememberMeChange,
                            modifier = Modifier.testTag("remember_me_switch")
                        )
                    }

                    Button(
                        onClick = {
                            authError = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    authError = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  NEW TO DARLING DATE?  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedButton(
                        onClick = {
                            authError = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    authError = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_account_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Create New Account")
                    }

                    TextButton(
                        onClick = onPreviewFeatures,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_features_button")
                    ) {
                        Text(
                            text = "Preview App Features Without Sign-Up",
                            color = DarlingViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showSecurityInfoDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Security info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Protected by Firebase Auth & Role Security",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSecurityInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityInfoDialog = false },
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = DarlingRose) },
            title = { Text("Authentication & Role Security") },
            text = {
                Text(
                    "Darling Date authenticates exclusively via Google Sign-In (Jetpack Credential Manager) and Firebase Authentication. " +
                        "Account roles ('user' vs 'admin') are verified server-side via Firestore Security Rules and protected admin grants—never modifiable from client code.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showSecurityInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    email: String,
    initialDisplayName: String,
    initialPhotoUrl: String?,
    emailVerified: Boolean,
    isSubmitting: Boolean,
    serverError: String?,
    onCheckUsernameAvailable: suspend (String) -> Boolean,
    onSubmitRegistration: (
        fullName: String,
        username: String,
        dateOfBirth: String,
        gender: String?,
        avatarUrl: String?,
        bio: String?,
        lookingFor: String?,
        interests: List<String>,
        termsAccepted: Boolean
    ) -> Unit,
    onPreviewFeatures: () -> Unit,
    onSignOutToLogin: () -> Unit
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(initialDisplayName) }
    var username by remember {
        mutableStateOf(
            initialDisplayName.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9_]"), "_")
                .trim('_')
                .take(20)
                .ifBlank { "" }
        )
    }
    var dobText by remember { mutableStateOf("1998-05-15") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf<String?>(null) }
    var avatarData by remember { mutableStateOf(initialPhotoUrl) }
    var bio by remember { mutableStateOf("") }
    var lookingFor by remember { mutableStateOf("Meaningful Connection") }
    var selectedInterests by remember { mutableStateOf(setOf("Coffee", "Travel")) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var checkingUsername by remember { mutableStateOf(false) }

    val cleanUsername = username.trim().lowercase(Locale.US)
    val isUsernameFormatValid = cleanUsername.matches(Regex("^[a-z0-9_]{3,30}$"))
    val computedAge = remember(dobText) { AuthProfileRepository.computeAgeYears(dobText.trim()) }
    val isAgeValid = computedAge != null && computedAge >= 18

    LaunchedEffect(cleanUsername) {
        if (!isUsernameFormatValid) {
            usernameAvailable = null
            checkingUsername = false
            return@LaunchedEffect
        }
        checkingUsername = true
        delay(350)
        usernameAvailable = onCheckUsernameAvailable(cleanUsername)
        checkingUsername = false
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            MediaHelper.uriToCompactDataUri(context, uri)?.let { compact ->
                avatarData = compact
            }
        }
    }

    val genderOptions = listOf("Woman", "Man", "Non-binary", "Prefer not to say")
    val lookingForOptions = listOf("Meaningful Connection", "Casual Dates", "New Friends", "Long-Term")
    val interestCatalog = listOf("Coffee", "Travel", "Photography", "Music", "Fitness", "Foodie", "Art", "Hiking")

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Create Your Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Join Darling Date (18+ verified community)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DarlingLogoBadge(size = 44.dp)
            }

            // Profile Picture picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.dp, DarlingGradientBrush, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DarlingMediaImage(
                        mediaDataOrUrl = avatarData,
                        contentDescription = "Profile photo",
                        fallbackText = fullName.ifBlank { "DD" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Profile Picture")
                    }
                    Text(
                        text = "Optional • Uses Android Photo Picker",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Verified Google Email display
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email Address (Google Account)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = email.ifBlank { "Verified Google Account" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (emailVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = "Email verified",
                                tint = StatusSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified", style = MaterialTheme.typography.labelSmall, color = StatusSuccess)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it.take(80) },
                label = { Text("Full Name *") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_full_name")
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(30) },
                label = { Text("Unique Username *") },
                leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                trailingIcon = {
                    when {
                        checkingUsername -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        isUsernameFormatValid && usernameAvailable == true ->
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Available", tint = StatusSuccess)
                        isUsernameFormatValid && usernameAvailable == false ->
                            Icon(Icons.Filled.ErrorOutline, contentDescription = "Taken", tint = StatusDanger)
                    }
                },
                supportingText = {
                    val msg = when {
                        username.isBlank() -> "3-30 lowercase letters, numbers, or underscores"
                        !isUsernameFormatValid -> "Use only lowercase letters, numbers, and underscores (min 3)"
                        checkingUsername -> "Checking availability..."
                        usernameAvailable == true -> "@$cleanUsername is available!"
                        usernameAvailable == false -> "@$cleanUsername is already taken"
                        else -> ""
                    }
                    Text(
                        text = msg,
                        color = if (isUsernameFormatValid && usernameAvailable == false) StatusDanger
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_username")
            )

            OutlinedTextField(
                value = dobText,
                onValueChange = { dobText = it.take(10) },
                label = { Text("Date of Birth (YYYY-MM-DD) *") },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
                    }
                },
                isError = dobText.isNotBlank() && !isAgeValid,
                supportingText = {
                    val msg = when {
                        computedAge == null -> "Format: YYYY-MM-DD (e.g. 1998-05-15)"
                        computedAge < 18 -> "You must be at least 18 years old (currently $computedAge)."
                        else -> "Age verified: $computedAge years old"
                    }
                    Text(
                        text = msg,
                        color = if (!isAgeValid) StatusDanger else StatusSuccess
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_dob")
            )

            Text("Gender (Optional)", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                genderOptions.forEach { option ->
                    FilterChip(
                        selected = selectedGender == option,
                        onClick = { selectedGender = if (selectedGender == option) null else option },
                        label = { Text(option) }
                    )
                }
            }

            Text("Looking For", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                lookingForOptions.forEach { option ->
                    FilterChip(
                        selected = lookingFor == option,
                        onClick = { lookingFor = option },
                        label = { Text(option) }
                    )
                }
            }

            Text("Interests", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                interestCatalog.forEach { tag ->
                    val chosen = selectedInterests.contains(tag)
                    FilterChip(
                        selected = chosen,
                        onClick = {
                            selectedInterests = if (chosen) selectedInterests - tag else selectedInterests + tag
                        },
                        label = { Text(tag) }
                    )
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(300) },
                label = { Text("Bio (Optional)") },
                placeholder = { Text("Tell people what makes a great first date...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    modifier = Modifier.testTag("signup_terms_checkbox")
                )
                Column(modifier = Modifier.clickable { showTermsDialog = true }) {
                    Text(
                        text = "I accept the Terms of Service & Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tap to read community & safety guidelines",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarlingRose
                    )
                }
            }

            serverError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            val canSubmit = !isSubmitting &&
                fullName.trim().length >= 2 &&
                isUsernameFormatValid &&
                usernameAvailable != false &&
                isAgeValid &&
                termsAccepted

            Button(
                onClick = {
                    onSubmitRegistration(
                        fullName,
                        cleanUsername,
                        dobText.trim(),
                        selectedGender,
                        avatarData,
                        bio,
                        lookingFor,
                        selectedInterests.toList(),
                        termsAccepted
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign Up & Enter Darling Date", style = MaterialTheme.typography.labelLarge)
                }
            }

            TextButton(
                onClick = onPreviewFeatures,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("registration_preview_button")
            ) {
                Text(
                    text = "Preview App Features First",
                    color = DarlingViolet,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = onSignOutToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Already have an account? Switch Account / Log In")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            dobText = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Privacy Policy") },
            text = {
                Text(
                    "1. Minimum Age: You confirm you are at least 18 years old.\n" +
                        "2. Respect & Safety: Harassment, hate speech, impersonation, and spam result in account suspension or permanent ban.\n" +
                        "3. Privacy: Your email and date of birth are stored in isolated private records accessible only to you and authorized safety moderators.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        termsAccepted = true
                        showTermsDialog = false
                    }
                ) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }
}
