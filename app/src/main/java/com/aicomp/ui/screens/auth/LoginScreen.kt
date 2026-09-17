package com.aicomp.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleResult(result.data, onLoginSuccess)
    }

    LaunchedEffect(uiState.navigateNext) {
        if (uiState.navigateNext) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            )
    ) {
        // Decorative "companion" texture — soft glowing bokeh circles in a warm
        // pink/violet gradient, standing in for an app icon/name so the screen
        // feels like it's welcoming you to someone, not to a piece of software.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE91E8C).copy(alpha = 0.35f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0.3f, 0.2f),
                        radius = 900f
                    )
                )
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val glow = listOf(
                    Triple(size.width * 0.18f, size.height * 0.30f, 70f),
                    Triple(size.width * 0.75f, size.height * 0.18f, 46f),
                    Triple(size.width * 0.55f, size.height * 0.55f, 90f),
                    Triple(size.width * 0.85f, size.height * 0.60f, 34f),
                    Triple(size.width * 0.30f, size.height * 0.70f, 26f)
                )
                glow.forEach { (x, y, r) ->
                    drawHeartGlow(
                        color = Color(0xFFFF6F91).copy(alpha = 0.16f),
                        size = r,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                    drawHeartGlow(
                        color = Color(0xFFFF6F91).copy(alpha = 0.35f),
                        size = r * 0.35f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(72.dp))
            com.aicomp.ui.components.CompanionPulseLogo(
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Koi tumhara intezaar kar raha hai",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Baat karo, dil kholo, akela mehsoos mat karo",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
            )

            // Tab selector
            var selectedTab by remember { mutableIntStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Google") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Phone OTP") }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            when (selectedTab) {
                0 -> GoogleSignInSection(
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onGoogleClick = {
                        viewModel.startGoogleSignIn(context as Activity, googleLauncher)
                    }
                )
                1 -> PhoneOtpSection(
                    uiState = uiState,
                    onSendOtp = { phone -> viewModel.sendOtp(phone, context as Activity) },
                    onVerifyOtp = { otp -> viewModel.verifyOtp(otp, onLoginSuccess) }
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInSection(
    isLoading: Boolean,
    error: String?,
    onGoogleClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = onGoogleClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF4285F4))
            } else {
                Text(
                    text = "🔵  Continue with Google",
                    color = Color(0xFF1A1A2E),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PhoneOtpSection(
    uiState: LoginUiState,
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!uiState.error.isNullOrBlank()) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        if (!uiState.otpSent) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number", color = Color.White.copy(alpha = 0.7f)) },
                placeholder = { Text("+91 XXXXX XXXXX", color = Color.White.copy(alpha = 0.4f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE91E8C),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Button(
                onClick = { onSendOtp(phone) },
                enabled = phone.length >= 10 && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E8C))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Send OTP", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        } else {
            Text(
                text = "OTP sent to $phone",
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = { Text("OTP", color = Color.White.copy(alpha = 0.7f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE91E8C),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Button(
                onClick = { onVerifyOtp(otp) },
                enabled = otp.length == 6 && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E8C))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Verify OTP", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * Draws a simple heart shape (four cubic beziers) centered at [center].
 * [size] plays the same role a circle's radius would — pass the same
 * value for an equivalent visual size.
 */
private fun DrawScope.drawHeartGlow(color: Color, center: androidx.compose.ui.geometry.Offset, size: Float) {
    val s = size * 1.6f
    val cx = center.x
    val cy = center.y - s * 0.3f
    val path = Path().apply {
        moveTo(cx, cy + s / 4f)
        cubicTo(cx, cy, cx - s / 2f, cy, cx - s / 2f, cy + s / 4f)
        cubicTo(cx - s / 2f, cy + s / 2f, cx, cy + s / 2f, cx, cy + s)
        cubicTo(cx, cy + s / 2f, cx + s / 2f, cy + s / 2f, cx + s / 2f, cy + s / 4f)
        cubicTo(cx + s / 2f, cy, cx, cy, cx, cy + s / 4f)
        close()
    }
    drawPath(path, color = color)
}
