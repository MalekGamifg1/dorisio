package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DorisioRepository
import com.example.model.UserRole
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isFirebaseConfigured by viewModel.isFirebaseConfigured.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAdminClaimDialog by remember { mutableStateOf(false) }
    var adminSecretInput by remember { mutableStateOf("") }
    var adminMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    var editName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var editShirt by remember { mutableStateOf(currentUser?.shirtNumber ?: 10) }
    var editPos by remember { mutableStateOf(currentUser?.position ?: "وسط") }
    var editTeam by remember { mutableStateOf(currentUser?.favoriteTeamName ?: "صقور المجد") }
    var editPhotoBase64 by remember { mutableStateOf(currentUser?.photoUrl ?: "") }

    val userPhotoBitmap = remember(currentUser?.photoUrl) {
        if (!currentUser?.photoUrl.isNullOrBlank()) {
            DorisioRepository.base64ToBitmap(currentUser!!.photoUrl)
        } else null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
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
                // Scale down to avatar size (max 256x256)
                val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                val base64 = DorisioRepository.bitmapToBase64(scaled)
                editPhotoBase64 = base64
                viewModel.updateProfile(
                    name = currentUser?.displayName ?: "",
                    shirt = currentUser?.shirtNumber ?: 10,
                    pos = currentUser?.position ?: "وسط",
                    team = currentUser?.favoriteTeamName ?: "صقور المجد",
                    photoBase64 = base64
                )
                successToast = "تم تحديث الصورة الشخصية بنجاح 📸"
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CleanWhiteBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feedback toast
        if (successToast != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ModernEmeraldLight),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ModernEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(successToast!!, color = ModernEmeraldDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { successToast = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = ModernEmeraldDark)
                        }
                    }
                }
            }
        }

        // User Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image with Camera Badge
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(CleanWhiteCardElevated)
                                .border(2.dp, ModernEmerald, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPhotoBitmap != null) {
                                Image(
                                    bitmap = userPhotoBitmap.asImageBitmap(),
                                    contentDescription = "صورة الملف الشخصي",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ModernEmerald,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }

                        // Mini Camera Icon Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ModernEmerald)
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "تغيير الصورة",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentUser?.displayName ?: "مستخدم دوريسيو",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUser?.email ?: "captain@dorisio.com",
                        color = TextSecondarySlate,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Role Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentUser?.role == UserRole.ADMIN) LuxuryGoldLight else ModernEmeraldLight
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentUser?.role == UserRole.ADMIN) "⭐ منظم ومشرف الدوري (Admin)" else "لاعب / مستخدم",
                            color = if (currentUser?.role == UserRole.ADMIN) LuxuryGold else ModernEmeraldDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Player metadata grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("رقم القميص", color = TextSecondarySlate, fontSize = 11.sp)
                            Text("#${currentUser?.shirtNumber ?: 10}", color = ModernEmeraldDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("المركز", color = TextSecondarySlate, fontSize = 11.sp)
                            Text(currentUser?.position ?: "وسط", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الفريق المفضل", color = TextSecondarySlate, fontSize = 11.sp)
                            Text(currentUser?.favoriteTeamName ?: "صقور المجد", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editName = currentUser?.displayName ?: ""
                                editShirt = currentUser?.shirtNumber ?: 10
                                editPos = currentUser?.position ?: "وسط"
                                editTeam = currentUser?.favoriteTeamName ?: "صقور المجد"
                                editPhotoBase64 = currentUser?.photoUrl ?: ""
                                showEditProfileDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل البيانات والصورة")
                        }
                    }
                }
            }
        }

        // Admin Management Access (Only visible to authenticated Admins)
        if (currentUser?.role == UserRole.ADMIN) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LuxuryGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("لوحة تحكم المشرف", color = TextDark, fontWeight = FontWeight.Bold)
                                Text("إدارة الفرق، اللاعبين، الجداول والجوائز", color = TextSecondarySlate, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold)
                        ) {
                            Text("فتح اللوحة", color = TextDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Claim Admin Button for League Organizer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = ModernEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("هل أنت منظم الدوري؟", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("أدخل الرمز السري لتفعيل صلاحيات المشرف الكاملة", color = TextSecondarySlate, fontSize = 11.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { showAdminClaimDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ModernEmerald),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ModernEmerald)
                        ) {
                            Text("تفعيل الأدمن", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Cloud Synchronization Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = ModernEmerald
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("المزامنة السحابية (Firebase Firestore)", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(connectionStatus, color = ModernEmeraldDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    TextButton(onClick = { viewModel.navigateTo(Screen.FIREBASE_SETUP_GUIDE) }) {
                        Text("تعليمات الربط", color = ModernEmerald, fontSize = 11.sp)
                    }
                }
            }
        }

        // Logout Button
        item {
            OutlinedButton(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CardRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_logout")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = CardRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج من الحساب", color = CardRed, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val positions = listOf("حارس مرمى", "مدافع", "وسط", "مهاجم")
        val editDialogBitmap = remember(editPhotoBase64) {
            if (editPhotoBase64.isNotBlank()) DorisioRepository.base64ToBitmap(editPhotoBase64) else null
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("تعديل الملف الشخصي واللاعب", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar picker
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(CleanWhiteCardElevated)
                                .border(2.dp, ModernEmerald, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (editDialogBitmap != null) {
                                Image(
                                    bitmap = editDialogBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ModernEmerald, modifier = Modifier.size(36.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ModernEmerald)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }

                    Text("اضغط على الصورة لتغييرها من الاستوديو 🖼️", color = TextSecondarySlate, fontSize = 11.sp)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم الكامل") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editShirt.toString(),
                        onValueChange = { editShirt = it.toIntOrNull() ?: 10 },
                        label = { Text("رقم القميص (#)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("المركز الأساسي في الملعب:", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(positions) { pos ->
                            FilterChip(
                                selected = editPos == pos,
                                onClick = { editPos = pos },
                                label = { Text(pos, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ModernEmeraldLight,
                                    selectedLabelColor = ModernEmeraldDark
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editTeam,
                        onValueChange = { editTeam = it },
                        label = { Text("الفريق المفضل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editShirt, editPos, editTeam, editPhotoBase64)
                        showEditProfileDialog = false
                        successToast = "تم حفظ التعديلات بنجاح ⭐"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء", color = TextSecondarySlate)
                }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Admin Secret Dialog
    if (showAdminClaimDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminClaimDialog = false
                adminMessage = null
                adminSecretInput = ""
            },
            title = { Text("تفعيل صلاحيات المشرف", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "أدخل الرمز السري الخاص بمنظم الدوري لتفعيل صلاحيات الإدارة الكاملة:",
                        color = TextSecondarySlate,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adminSecretInput,
                        onValueChange = { adminSecretInput = it },
                        label = { Text("الرمز السري للإدارة") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (adminMessage != null) {
                        Text(adminMessage!!, color = CardRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.claimAdmin(adminSecretInput)
                        if (success) {
                            showAdminClaimDialog = false
                            adminMessage = null
                            adminSecretInput = ""
                            successToast = "تم تفعيل صلاحيات المشرف بنجاح 👑"
                        } else {
                            adminMessage = "الرمز السري غير صحيح. حاول مرة أخرى."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("تأكيد الصلاحية", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdminClaimDialog = false
                    adminMessage = null
                    adminSecretInput = ""
                }) {
                    Text("إلغاء", color = TextSecondarySlate)
                }
            },
            containerColor = CleanWhiteSurface
        )
    }
}
