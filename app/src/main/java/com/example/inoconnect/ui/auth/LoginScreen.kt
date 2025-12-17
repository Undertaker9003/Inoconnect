package com.example.inoconnect.ui.auth

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.R // <--- MAKE SURE THIS IMPORT IS HERE
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.launch

// Define the custom Blue Color from your image
val BrandBlue = Color(0xFF0083B0) // A nice ocean blue
val LightGrayInput = Color(0xFFF5F5F5)

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Root Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. The Blue Curved Header
        Canvas(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 100)
                // Creates the curve at the bottom
                quadraticBezierTo(
                    size.width / 2, size.height + 50, // Control point (pulls curve down)
                    0f, size.height - 100 // End point
                )
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // 2. The Main Content (Card + Icon)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Spacer to push the icon down to the curve line
            Spacer(modifier = Modifier.height(140.dp))

            // The Profile Icon (White Circle with Icon inside)
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Logo",
                    tint = Color.Black,
                    modifier = Modifier.padding(16.dp).fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f) // 85% screen width
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Welcome", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Sign in to your account", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Input
                    StyledTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        icon = Icons.Default.AccountCircle
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    StyledTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )

                    // Forgot Password (Visual only)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { /* TODO */ }) {
                            Text("Forgot Password?", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = BrandBlue)
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        isLoading = true
                                        val role = repository.loginUser(email, password)
                                        isLoading = false
                                        if (role != null) {
                                            onLoginSuccess(role)
                                        } else {
                                            Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Or sign in with", color = Color.Gray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social Icons (Updated to use your drawables)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Assuming your files are named exactly like this in the drawable folder:
                        SocialIcon(iconResId = R.drawable.google_color_icon)
                        SocialIcon(iconResId = R.drawable.facebook)
                        SocialIcon(iconResId = R.drawable.github_icon)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Register Link
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Don't have an account?", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = " Register",
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onRegisterClick() }
                        )
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENTS TO KEEP CODE CLEAN ---

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Enter your $label", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LightGrayInput,
                unfocusedContainerColor = LightGrayInput,
                disabledContainerColor = LightGrayInput,
                focusedIndicatorColor = Color.Transparent, // Hides the underline
                unfocusedIndicatorColor = Color.Transparent,
            ),
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true
        )
    }
}

// Updated SocialIcon helper to take a Drawable Resource ID
@Composable
fun SocialIcon(@DrawableRes iconResId: Int) {
    Surface(
        modifier = Modifier.size(50.dp).clickable { /* No Op */ },
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        // Using Box with padding so the logo doesn't touch the edge of the circle
        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            // Using Image composable to preserve original colors (especially for Google)
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null, // Decorative only
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}