package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgMidnight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        var promoCodeInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        var isPromoSuccess by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var promoMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        val context = androidx.compose.ui.platform.LocalContext.current

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎁 Redemptions & Promo Codes",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Have a special code? Enter it below to unlock premium plans or platform credits instantly.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it },
                        placeholder = { Text("e.g. OPDH IS BEST6785", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(50.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedBorderColor = AccentOrange,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Button(
                        onClick = {
                            if (promoCodeInput.trim() == "OPDH IS BEST6785") {
                                isProUserEnabled.value = true
                                isPromoSuccess = true
                                promoMessage = "👑 1-YEAR PRO PLAN ACTIVATED FREE!"
                                android.widget.Toast.makeText(context, "Promo Code Applied successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                isPromoSuccess = false
                                promoMessage = "❌ Invalid Promo Code. Please try again."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("REDEEM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (promoMessage.isNotEmpty()) {
                    Text(
                        text = promoMessage,
                        color = if (isPromoSuccess) BullGreen else BearRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isProUserEnabled.value) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BullGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👑 ACTIVE: 1-Year Pro Member Pass (OPDH Special VIP Tier)",
                            color = BullGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dark Theme",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Switch(
                    checked = isDarkThemeEnabled.value,
                    onCheckedChange = { isDarkThemeEnabled.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = AccentOrange,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = SurfaceCard
                    )
                )
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Push Notifications",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Switch(
                    checked = true, // Mock value
                    onCheckedChange = { /* TODO: Implement Notifications Toggle */ },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = AccentOrange,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = SurfaceCard
                    )
                )
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Groww Pro Subscription",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unlock 5s, 15s, 30s charts & AI Reports",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                
                Switch(
                    checked = isProUserEnabled.value,
                    onCheckedChange = { isProUserEnabled.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = AccentOrange,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = SurfaceCard
                    )
                )
            }
        }
        
        var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var selectedLanguage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("English") }
        val indianLanguages = listOf(
            "English", "Hindi", "Assamese", "Bengali", "Bodo", "Dogri", "Gujarati", 
            "Kannada", "Kashmiri", "Konkani", "Maithili", "Malayalam", "Manipuri", 
            "Marathi", "Nepali", "Odia", "Punjabi", "Sanskrit", "Santali", "Sindhi", 
            "Tamil", "Telugu", "Urdu"
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Language",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = selectedLanguage,
                        color = AccentOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SurfaceDark).heightIn(max = 300.dp)
                ) {
                    indianLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language, color = TextWhite) },
                            onClick = {
                                selectedLanguage = language
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Version",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "1.0.0",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Developer Credit",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "lakshophd",
                    color = AccentOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
