package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel

@Composable
fun FirebaseSetupScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val isConfigured by viewModel.isFirebaseConfigured.collectAsState()
    val statusText by viewModel.connectionStatus.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isConfigured) PitchGreenDark else Color(0xFF2E2214)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isConfigured) PitchGreenVibrant else GoldAccent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isConfigured) PitchGreenVibrant else GoldBright)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConfigured) "حالة السحابة: متصل بـ Firebase" else "حالة السحابة: قاعدة بيانات محلية جاهزة للربط",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        color = GoldBright,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isConfigured)
                            "تطبيق دوريسيو متصل حالياً بخدمات Firebase Cloud Firestore و Authentication بنجاح مع تزامن حي للنتائج."
                        else
                            "يعمل التطبيق حالياً بكامل وظائفه الميدانية والحية محلياً مع حفظ دائم، وهو مهيأ ومبني بالكامل للارتباط بمشروع Firebase الخاص بك بمجرد إضافة ملف الإعدادات google-services.json.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Setup Steps
        item {
            Text("خطوات ربط Firebase بمشروع دوريسيو:", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        item {
            SetupStepItem(
                stepNum = "1",
                title = "إنشاء مشروع Firebase",
                desc = "افتح console.firebase.google.com وأنشئ مشروعاً جديداً باسم دوريسيو (Dorisio)."
            )
        }

        item {
            SetupStepItem(
                stepNum = "2",
                title = "إضافة تطبيق Android وتحميل google-services.json",
                desc = "سجل تطبيق Android بالحزمة: com.aistudio.dorisio.flgbxz وحمل ملف google-services.json وضعه داخل مجلد /app."
            )
        }

        item {
            SetupStepItem(
                stepNum = "3",
                title = "تفعيل المصادقة (Firebase Authentication)",
                desc = "قم بتفعيل Email/Password و Google Sign-In و Facebook و GitHub من تبويب Authentication > Sign-in method."
            )
        }

        item {
            SetupStepItem(
                stepNum = "4",
                title = "إنشاء Cloud Firestore",
                desc = "أنشئ قاعدة بيانات Firestore في وضع Production، واستخدم ملف قواعد الأمان firestore.rules المرفق في المشروع."
            )
        }

        item {
            SetupStepItem(
                stepNum = "5",
                title = "تفعيل حساب المشرف الأول (First Admin)",
                desc = "بعد تسجيل حسابك، ادخل على صفحة 'حسابي' واضغط 'تفعيل صلاحيات المشرف' وأدخل الرمز السري: DORISIO2026."
            )
        }
    }
}

@Composable
fun SetupStepItem(
    stepNum: String,
    title: String,
    desc: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PitchGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(stepNum, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
