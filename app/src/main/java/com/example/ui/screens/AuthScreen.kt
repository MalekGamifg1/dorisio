package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DorisioRepository
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var shirtNumber by remember { mutableStateOf(10) }
    var position by remember { mutableStateOf("وسط") }
    var capturedSelfieBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Google Sign-In Client Configuration with Web Client ID
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("221303155446-k4l3nsb53isdt2514f7jru19mb08r441.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Google Account Picker Activity Result Launcher
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    val res = viewModel.loginWithGoogleIdToken(idToken)
                    if (!res.first) errorMessage = res.second
                } else if (account != null && !account.email.isNullOrBlank()) {
                    val accountEmail = account.email!!
                    val accountName = account.displayName ?: accountEmail.substringBefore("@")
                    val photoUrl = account.photoUrl?.toString() ?: ""
                    val res = viewModel.repository.firebaseBridge.signInWithDirectSocial(
                        name = accountName,
                        email = accountEmail,
                        provider = "google",
                        photoUrl = photoUrl
                    )
                    if (res.isSuccess) {
                        viewModel.repository.saveUser(res.getOrThrow())
                    } else {
                        errorMessage = res.exceptionOrNull()?.message ?: "فشل تسجيل الدخول بحساب Google"
                    }
                } else {
                    val res = viewModel.loginWithGoogle(context)
                    if (!res.first) errorMessage = res.second
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "فشل تسجيل الدخول بحساب Google"
            } finally {
                isLoading = false
            }
        }
    }

    // Camera selfie launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            capturedSelfieBitmap = scaled
        }
    }

    // Gallery Photo Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                capturedSelfieBitmap = scaled
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CleanWhiteBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        // App Header & Logo
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Dorisio Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CleanWhiteSurface)
                        .border(1.5.dp, CleanWhiteBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dorisio_user_logo),
                        contentDescription = "شعار دوريسيو",
                        modifier = Modifier.size(68.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "دوريسيو",
                    color = TextDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "دوريكم... بشكل حقيقي • متصل بسحابة Firebase",
                    color = ModernEmeraldDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Toggle Login / Register
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            isRegisterMode = false
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isRegisterMode) CleanWhiteSurface else Color.Transparent,
                            contentColor = if (!isRegisterMode) TextDark else TextSecondarySlate
                        ),
                        elevation = ButtonDefaults.buttonElevation(if (!isRegisterMode) 2.dp else 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تسجيل الدخول", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isRegisterMode = true
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRegisterMode) CleanWhiteSurface else Color.Transparent,
                            contentColor = if (isRegisterMode) TextDark else TextSecondarySlate
                        ),
                        elevation = ButtonDefaults.buttonElevation(if (isRegisterMode) 2.dp else 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إنشاء حساب جديد 📸", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Social Logins (Google & GitHub)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                // Real Google Sign In Button
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        // Launch standard Google Sign In Intent Chooser
                        googleSignInClient.signOut()
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CleanWhiteSurface,
                        contentColor = TextDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_google_sign_in")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "المتابعة باستخدام حساب Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                    }
                }

                // Real GitHub Sign In Button
                OutlinedButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = viewModel.loginWithGitHub(activity)
                                isLoading = false
                                if (!res.first) {
                                    errorMessage = res.second
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CleanWhiteSurface,
                        contentColor = TextDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_github_sign_in")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub",
                            tint = Color(0xFF24292F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تسجيل الدخول عبر GitHub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CleanWhiteBorder)
                    Text(
                        text = "أو بالبريد وكلمة المرور",
                        color = TextSecondarySlate,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CleanWhiteBorder)
                }
            }
        }

        // Email / Password Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isRegisterMode) {
                        // Selfie / Photo capture
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CleanWhiteCardElevated, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, ModernEmerald, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (capturedSelfieBitmap != null) {
                                        Image(
                                            bitmap = capturedSelfieBitmap!!.asImageBitmap(),
                                            contentDescription = "صورتك الشخصية",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = ModernEmerald,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("صورتك الشخصية كلاعب", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("ستظهر في التشكيل ورجل المباراة", color = TextSecondarySlate, fontSize = 10.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { cameraLauncher.launch() },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(ModernEmeraldLight)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "سيلفي", tint = ModernEmeraldDark, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(ModernEmeraldLight)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "معرض الصور", tint = ModernEmeraldDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("الاسم الكامل") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_fullname")
                        )

                        // Shirt number and position
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = shirtNumber.toString(),
                                onValueChange = { shirtNumber = it.toIntOrNull() ?: 10 },
                                label = { Text("رقم القميص") },
                                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Position selector
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("المركز:", fontSize = 11.sp, color = TextSecondarySlate)
                                val posList = listOf("حارس مرمى", "مدافع", "وسط", "مهاجم")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    posList.take(2).forEach { p ->
                                        FilterChip(
                                            selected = position == p,
                                            onClick = { position = p },
                                            label = { Text(p, fontSize = 10.sp) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    posList.drop(2).forEach { p ->
                                        FilterChip(
                                            selected = position == p,
                                            onClick = { position = p },
                                            label = { Text(p, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_email")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_password")
                    )

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = CardRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Main Action Button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (email.isBlank()) {
                                errorMessage = "يرجى كتابة البريد الإلكتروني"
                                return@Button
                            }
                            if (password.isBlank() || password.length < 6) {
                                errorMessage = "كلمة المرور في Firebase يجب أن لا تقل عن 6 خانات"
                                return@Button
                            }

                            coroutineScope.launch {
                                isLoading = true
                                if (isRegisterMode) {
                                    if (fullName.isBlank()) {
                                        errorMessage = "يرجى كتابة اسمك الكامل"
                                        isLoading = false
                                        return@launch
                                    }
                                    val photoBase64 = if (capturedSelfieBitmap != null) {
                                        DorisioRepository.bitmapToBase64(capturedSelfieBitmap!!)
                                    } else ""

                                    val result = viewModel.registerUser(
                                        name = fullName.trim(),
                                        email = email.trim(),
                                        password = password,
                                        shirtNumber = shirtNumber,
                                        position = position,
                                        photoBase64 = photoBase64
                                    )
                                    isLoading = false
                                    if (!result.first) {
                                        errorMessage = result.second
                                    }
                                } else {
                                    val result = viewModel.loginUser(
                                        email = email.trim(),
                                        password = password
                                    )
                                    isLoading = false
                                    if (!result.first) {
                                        errorMessage = result.second
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_auth_submit")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "إنشاء الحساب والبدء" else "تسجيل الدخول",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
