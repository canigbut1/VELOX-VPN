package com.velox.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF050B16)
private val Panel = Color(0xFF0D1A2B)
private val Cyan = Color(0xFF00D9FF)
private val Blue = Color(0xFF176BFF)
private val Muted = Color(0xFF8CA4BE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 11, 22)
        window.navigationBarColor = android.graphics.Color.rgb(5, 11, 22)
        setContent { VeloxHome() }
    }
}

@Composable
fun VeloxHome() {
    var connected by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("آلمان • فرانکفورت") }
    Surface(Modifier.fillMaxSize(), color = Night) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1B32), Night, Night))).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Cyan, Blue)), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Text("V", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("VELOX", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("SECURE CONNECTION", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("●", color = Cyan, fontSize = 13.sp)
                Text("  آماده", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(38.dp))
            Text(if (connected) "اتصال برقرار است" else "آماده‌ی اتصال", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(if (connected) "محافظت اتصال فعال است" else "برای شروع، دکمه را لمس کن", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(25.dp))
            Box(
                Modifier.size(218.dp).background(Brush.radialGradient(listOf(Color(0xFF123B66), Color(0xFF0A1D35), Night)), CircleShape)
                    .border(1.dp, Cyan.copy(alpha = .35f), CircleShape).padding(15.dp)
                    .border(1.dp, Blue.copy(alpha = .45f), CircleShape).padding(14.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF123D68), Color(0xFF071426))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { connected = !connected },
                    modifier = Modifier.size(132.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color(0xFF123D46) else Color(0xFF087DCE))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏻", fontSize = 42.sp, color = Color.White)
                        Text(if (connected) "قطع اتصال" else "اتصال", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("دانلود", "—", Modifier.weight(1f))
                MetricCard("آپلود", "—", Modifier.weight(1f))
                MetricCard("مدت اتصال", "00:00", Modifier.weight(1f))
            }
            Spacer(Modifier.height(25.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp)) {
                    Text("سرور انتخاب‌شده", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇩🇪", fontSize = 27.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selected, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("برای انتخاب سرورهای دیگر در نسخه بعدی", color = Muted, fontSize = 10.sp)
                        }
                        Text("›", color = Cyan, fontSize = 27.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("＋", color = Cyan, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("افزودن کانفیگ", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("لینک، فایل یا QR کد", color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("به‌زودی", color = Cyan, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("VELOX VPN  •  نسخه 1.0.0", color = Muted.copy(alpha = .7f), fontSize = 10.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(vertical = 13.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(7.dp))
            Text(value, color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
