package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.*
import com.example.viewmodel.Screen

@Composable
fun DorisioTopBar(
    currentScreen: Screen,
    isAdmin: Boolean,
    isFirebaseConnected: Boolean,
    onAdminClick: () -> Unit,
    onFirebaseGuideClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        color = CharcoalSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button if in subscreen, or Dorisio brand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentScreen in listOf(Screen.MATCH_DETAILS, Screen.LIVE_ADMIN_CENTER, Screen.ADMIN_PANEL, Screen.FIREBASE_SETUP_GUIDE)) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .testTag("btn_top_back")
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CharolaCardElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "رجوع",
                                tint = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        // Dorisio Emblem
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, CleanWhiteBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_dorisio_user_logo),
                                contentDescription = "شعار دوريسيو",
                                modifier = Modifier.size(34.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "دوريسيو",
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PitchGreenContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "دوري مدرسي",
                                    color = PitchGreenVibrant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "دوريكم... بشكل حقيقي",
                            color = TextDim,
                            fontSize = 11.sp
                        )
                    }
                }

                // Actions: Firebase Status & Admin Panel
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Firebase connection badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFirebaseConnected) PitchGreenContainer else CharcoalCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isFirebaseConnected) PitchGreenVibrant else CharcoalBorder
                        ),
                        modifier = Modifier
                            .testTag("btn_firebase_status")
                            .clickable { onFirebaseGuideClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirebaseConnected) PitchGreenVibrant else GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFirebaseConnected) "متصل" else "السحابة",
                                fontSize = 11.sp,
                                color = if (isFirebaseConnected) PitchGreenVibrant else GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onAdminClick,
                            modifier = Modifier
                                .testTag("btn_admin_hub")
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldContainer)
                                .border(1.dp, GoldAccent, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "لوحة تحكم المشرف",
                                tint = GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)
        }
    }
}

val CharolaCardElevated = Color(0xFF282E37)

@Composable
fun DorisioBottomNav(
    currentScreen: Screen,
    isAdmin: Boolean,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = CharcoalSurface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.HOME,
            onClick = { onNavigate(Screen.HOME) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "الرئيسية"
                )
            },
            label = { Text("الرئيسية", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextWhite,
                selectedTextColor = PitchGreenVibrant,
                indicatorColor = PitchGreenPrimary,
                unselectedIconColor = TextDim,
                unselectedTextColor = TextDim
            ),
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.MATCHES,
            onClick = { onNavigate(Screen.MATCHES) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.MATCHES) Icons.Filled.SportsSoccer else Icons.Outlined.SportsSoccer,
                    contentDescription = "المباريات"
                )
            },
            label = { Text("المباريات", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextWhite,
                selectedTextColor = PitchGreenVibrant,
                indicatorColor = PitchGreenPrimary,
                unselectedIconColor = TextDim,
                unselectedTextColor = TextDim
            ),
            modifier = Modifier.testTag("nav_matches")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.STANDINGS,
            onClick = { onNavigate(Screen.STANDINGS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.STANDINGS) Icons.Filled.Leaderboard else Icons.Outlined.Leaderboard,
                    contentDescription = "الترتيب"
                )
            },
            label = { Text("الترتيب", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextWhite,
                selectedTextColor = PitchGreenVibrant,
                indicatorColor = PitchGreenPrimary,
                unselectedIconColor = TextDim,
                unselectedTextColor = TextDim
            ),
            modifier = Modifier.testTag("nav_standings")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.PLAYERS,
            onClick = { onNavigate(Screen.PLAYERS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.PLAYERS) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                    contentDescription = "اللاعبون"
                )
            },
            label = { Text("اللاعبون", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextWhite,
                selectedTextColor = PitchGreenVibrant,
                indicatorColor = PitchGreenPrimary,
                unselectedIconColor = TextDim,
                unselectedTextColor = TextDim
            ),
            modifier = Modifier.testTag("nav_players")
        )

        NavigationBarItem(
            selected = currentScreen == Screen.PROFILE,
            onClick = { onNavigate(Screen.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "حسابي"
                )
            },
            label = { Text("حسابي", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextWhite,
                selectedTextColor = PitchGreenVibrant,
                indicatorColor = PitchGreenPrimary,
                unselectedIconColor = TextDim,
                unselectedTextColor = TextDim
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}
