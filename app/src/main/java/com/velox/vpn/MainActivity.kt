package com.velox.vpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
private const val PREFS = "velox_configs"
private const val CONFIGS = "items"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 11, 22)
        window.navigationBarColor = android.graphics.Color.rgb(5, 11, 22)
        setContent { VeloxHome(this) }
    }
}

private fun readConfigs(context: Context): List<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(CONFIGS, "")!!.split("\\n").filter { it.isNotBlank() }

private fun saveConfigs(context: Context, configs: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString(CONFIGS, configs.joinToString("\\n")).apply()
}

private fun configLabel(link: String): String {
    val fragment = link.substringAfter("#", "")
    if (fragment.isNotBlank()) return runCatching {
        java.net.URLDecoder.decode(fragment, "UTF-8")
    }.getOrDefault(fragment)
    return when {
        link.startsWith("vless://") -> "سرور VLESS"
        link.startsWith("vmess://") -> "سرور VMess"
        link.startsWith("trojan://") -> "سرور Trojan"
        link.startsWith("ss://") -> "سرور Shadowsocks"
        else -> "کانفیگ"
    }
}

@Composable
fun VeloxHome(context: Context) {
    var configs by remember { mutableStateOf(readConfigs(context)) }
    var selected by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }
    var showServers by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = Night) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1B32), Night, Night)))
                .padding(horizontal = 22.dp),
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
            Spacer(Modifier.height(34.dp))
            Text("آماده‌ی اتصال", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("برای اتصال، ابتدا کانفیگ معتبر اضافه کن", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(210.dp).background(Brush.radialGradient(listOf(Color(0xFF123B66), Color(0xFF0A1D35), Night)), CircleShape)
                    .border(1.dp, Cyan.copy(alpha = .35f), CircleShape).padding(15.dp)
                    .border(1.dp, Blue.copy(alpha = .45f), CircleShape).padding(14.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF123D68), Color(0xFF071426))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { Toast.makeText(context, "موتور اتصال VPN هنوز در حال پیاده‌سازی است؛ فعلاً اتصال فعال نمی‌شود.", Toast.LENGTH_LONG).show() },
                    modifier = Modifier.size(132.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087DCE))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏻", fontSize = 42.sp, color = Color.White)
                        Text("اتصال", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(25.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("دانلود", "—", Modifier.weight(1f))
                MetricCard("آپلود", "—", Modifier.weight(1f))
                MetricCard("مدت اتصال", "00:00", Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().clickable { showServers = true }
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌐", fontSize = 25.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("سرور انتخاب‌شده", color = Muted, fontSize = 12.sp)
                        Text(configs.getOrNull(selected)?.let { configLabel(it) } ?: "هنوز کانفیگی اضافه نشده", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${configs.size} کانفیگ ذخیره‌شده", color = Muted, fontSize = 10.sp)
                    }
                    Text("›", color = Cyan, fontSize = 27.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable { showImport = true }
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("＋", color = Cyan, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("افزودن کانفیگ", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("لینک تکی یا آدرس اشتراک سرورها", color = Muted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("وارد کردن", color = Cyan, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("VELOX VPN  •  نسخه 1.0.1", color = Muted.copy(alpha = .7f), fontSize = 10.sp)
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            containerColor = Panel,
            title = { Text("افزودن کانفیگ", color = Color.White) },
            text = {
                Column {
                    Text("لینک کانفیگ یا آدرس اشتراک را وارد کن:", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it.trim() },
                        placeholder = { Text("vless://… یا https://…") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Cyan, unfocusedBorderColor = Muted,
                            focusedPlaceholderColor = Muted, unfocusedPlaceholderColor = Muted
                        )
                    )
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        linkText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                    }) { Text("جای‌گذاری از حافظه موقت", color = Cyan) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val candidate = linkText.trim()
                    val valid = listOf("vless://", "vmess://", "trojan://", "ss://", "socks://", "wireguard://", "hy2://", "hysteria2://", "tuic://", "https://", "http://").any { candidate.startsWith(it, true) }
                    if (!valid) {
                        Toast.makeText(context, "لینک معتبر نیست یا فرمت آن پشتیبانی نمی‌شود.", Toast.LENGTH_LONG).show()
                    } else if (configs.contains(candidate)) {
                        Toast.makeText(context, "این کانفیگ قبلاً ذخیره شده است.", Toast.LENGTH_SHORT).show()
                    } else {
                        configs = configs + candidate
                        saveConfigs(context, configs)
                        selected = configs.lastIndex
                        linkText = ""
                        showImport = false
                        Toast.makeText(context, "کانفیگ ذخیره شد؛ اتصال هنوز فعال نیست.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("ذخیره", color = Cyan) }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("انصراف", color = Muted) } }
        )
    }

    if (showServers) {
        AlertDialog(
            onDismissRequest = { showServers = false },
            containerColor = Panel,
            title = { Text("کانفیگ‌های من", color = Color.White) },
            text = {
                if (configs.isEmpty()) Text("هنوز کانفیگی ذخیره نکرده‌ای.", color = Muted)
                else LazyColumn {
                    items(configs.indices.toList()) { index ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = index; showServers = false }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(configLabel(configs[index]), color = Color.White, fontWeight = FontWeight.Bold)
                                Text(configs[index].substringBefore("#").take(38), color = Muted, fontSize = 9.sp)
                            }
                            if (selected == index) Text("انتخاب‌شده", color = Cyan, fontSize = 10.sp)
                            TextButton(onClick = {
                                configs = configs.filterIndexed { i, _ -> i != index }
                                saveConfigs(context, configs)
                                selected = if (configs.isEmpty()) 0 else selected.coerceAtMost(configs.lastIndex)
                            }) { Text("حذف", color = Color(0xFFFF6B75)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showServers = false }) { Text("بستن", color = Cyan) } }
        )
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
