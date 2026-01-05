package com.example.inoconnect.ui.auth

import android.app.Activity
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.R
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.UserRole
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // State for the form
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // State for Verification Waiting Screen
    var isVerificationSent by remember { mutableStateOf(false) }

    val defaultRole = UserRole.PARTICIPANT

    // --- AUTO-POLLING LOGIC ---
    // If we have sent the email, we start a loop to check if they verified it.
    LaunchedEffect(isVerificationSent) {
        if (isVerificationSent) {
            while (true) {
                delay(3000) // Check every 3 seconds
                val isVerified = repository.checkEmailVerification()
                if (isVerified) {
                    Toast.makeText(context, "Email Verified! Logging in...", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess(defaultRole)
                    break // Stop the loop
                }
            }
        }
    }

    // --- 1. Google Sign-In ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    scope.launch {
                        isLoading = true
                        val role = repository.signInWithGoogle(idToken)
                        isLoading = false
                        if (role != null) onRegisterSuccess(role)
                        else Toast.makeText(context, "Google Sign-Up Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Error: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val launchGoogle = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    // --- 2. Facebook Sign-In ---
    val loginManager = LoginManager.getInstance()
    val callbackManager = remember { CallbackManager.Factory.create() }

    DisposableEffect(Unit) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onCancel() {}
            override fun onError(error: FacebookException) {
                Toast.makeText(context, "Facebook Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
            override fun onSuccess(result: LoginResult) {
                scope.launch {
                    isLoading = true
                    val role = repository.signInWithFacebook(result.accessToken)
                    isLoading = false
                    if (role != null) onRegisterSuccess(role)
                    else Toast.makeText(context, "Facebook Auth Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        loginManager.registerCallback(callbackManager, callback)
        onDispose { loginManager.unregisterCallback(callbackManager) }
    }
    val launchFacebook = {
        loginManager.logInWithReadPermissions(context as Activity, listOf("email", "public_profile"))
    }

    // --- 3. GitHub Sign-In ---
    val launchGithub = {
        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "true")
        val activity = context as? Activity
        if (activity != null) {
            auth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener {
                    scope.launch {
                        isLoading = true
                        val role = repository.onSignInWithGithubSuccess()
                        isLoading = false
                        if (role != null) onRegisterSuccess(role)
                        else Toast.makeText(context, "GitHub Setup Failed", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "GitHub Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Regex
    val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+\$).{8,}\$".toRegex()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Blue Header
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 80)
                quadraticBezierTo(size.width / 2, size.height + 40, 0f, size.height - 80)
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // --- CONTENT SWAPPING LOGIC ---
        if (isVerificationSent) {
            // ================= VERIFICATION WAITING SCREEN =================
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Sent",
                    modifier = Modifier.size(80.dp),
                    tint = BrandBlue
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Verify Your Email", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "We've sent a verification link to:\n$email",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Loading Spinner
                CircularProgressIndicator(color = BrandBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Waiting for verification...", color = Color.Gray)

                Spacer(modifier = Modifier.height(32.dp))

                // Manual Check Button (In case polling is slow)
                Button(
                    onClick = {
                        scope.launch {
                            val isVerified = repository.checkEmailVerification()
                            if (isVerified) {
                                onRegisterSuccess(defaultRole)
                            } else {
                                Toast.makeText(context, "Not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("I have verified my email")
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    repository.logout()
                    onBackClick() // Go back to Login
                }) {
                    Text("Cancel / Wrong Email", color = Color.Gray)
                }
            }

        } else {
            // ================= REGISTRATION FORM =================
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(100.dp))
                Surface(
                    shape = CircleShape, color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(90.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person, contentDescription = null,
                        tint = Color.Black, modifier = Modifier.padding(16.dp).fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Create Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(20.dp))

                        StyledTextField(value = username, onValueChange = { username = it }, label = "Username", icon = Icons.Default.Person)
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email)
                        Spacer(modifier = Modifier.height(12.dp))
                        StyledTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

                        Text(
                            text = "Min 8 chars, 1 Uppercase, 1 Number, 1 Special Char",
                            fontSize = 10.sp, color = Color.Gray, lineHeight = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        StyledTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", icon = Icons.Default.CheckCircle, isPassword = true)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isLoading) {
                            CircularProgressIndicator(color = BrandBlue)
                        } else {
                            Button(
                                onClick = {
                                    if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                    } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        Toast.makeText(context, "Invalid email address", Toast.LENGTH_SHORT).show()
                                    } else if (password != confirmPassword) {
                                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    } else if (!password.matches(passwordRegex)) {
                                        Toast.makeText(context, "Password is too weak.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            isLoading = true
                                            val success = repository.registerUser(email, password, defaultRole, username)
                                            isLoading = false
                                            if (success) {
                                                // SWITCH TO VERIFICATION SCREEN instead of navigating away
                                                isVerificationSent = true
                                            } else {
                                                Toast.makeText(context, "Registration Failed.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Or sign up with", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Surface(modifier = Modifier.size(50.dp).clickable { launchGoogle() }, shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Image(painter = painterResource(id = R.drawable.google_color_icon), contentDescription = "Google", modifier = Modifier.fillMaxSize())
                                }
                            }
                            Surface(modifier = Modifier.size(50.dp).clickable { launchFacebook() }, shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Image(painter = painterResource(id = R.drawable.facebook), contentDescription = "Facebook", modifier = Modifier.fillMaxSize())
                                }
                            }
                            Surface(modifier = Modifier.size(50.dp).clickable { launchGithub() }, shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Image(painter = painterResource(id = R.drawable.github_icon), contentDescription = "GitHub", modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Have an account?", color = Color.Gray, fontSize = 12.sp)
                            Text(" Login", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { onBackClick() })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 40.dp, start = 16.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}