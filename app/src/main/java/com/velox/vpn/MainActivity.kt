package com.velox.vpn

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.URLDecoder

private val Bg = Color(0xFF050914)
private val Panel = Color(0xFF101B2D)
private val Cyan = Color(0xFF27E5FF)
private val Blue = Color(0xFF397BFF)
private val Muted = Color(0xFF91A3BD)
private const val PREFS = "velox_configs"
private const val CONFIGS = "items"
private const val STATE_PREFS = "velox_state"

class MainActivity : ComponentActivity() {
    private var pendingConfig: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 9, 20)
        window.navigationBarColor = android.graphics.Color.rgb(5, 9, 20)
        setContent { VeloxHome(this, ::connect, ::disconnect) }
    }
    private fun connect(config: String) {
        pendingConfig = config
        val permission = VpnService.prepare(this)
        if (permission != null) startActivityForResult(permission, 900) else startVpn(config)
    }
    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 900 && resultCode == RESULT_OK) pendingConfig?.let(::startVpn)
    }
    private fun startVpn(config: String) {
        val i = Intent(this, VeloxVpnService::class.java).apply {
            action = VeloxVpnService.ACTION_CONNECT
            putExtra(VeloxVpnService.EXTRA_CONFIG, config)
        }
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }
    private fun disconnect() {
        startService(Intent(this, VeloxVpnService::class.java).setAction(VeloxVpnService.ACTION_DISCONNECT))
    }
}
private fun readConfigs(c: Context) = c.getSharedPreferences(PREFS, 0).getString(CONFIGS, "").orEmpty().split("\n").filter { it.isNotBlank() }
private fun saveConfigs(c: Context, list: List<String>) { c.getSharedPreferences(PREFS, 0).edit().putString(CONFIGS, list.joinToString("\n")).apply() }

private fun configLabel(raw: String): String {
    raw.substringAfter("#", "").takeIf { it.isNotBlank() }?.let { return runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    if (raw.startsWith("vmess://")) {
        val body = raw.removePrefix("vmess://").substringBefore("#").substringBefore("?")
        val decoded = runCatching { String(Base64.decode(body + "=".repeat((4-body.length%4)%4), Base64.DEFAULT), Charsets.UTF_8) }.getOrNull()
        val name = runCatching { JSONObject(decoded ?: "").optString("ps") }.getOrNull()
        if (!name.isNullOrBlank()) return name
    }
    val remark = runCatching { raw.substringAfter("?", "").split("&").firstOrNull { it.startsWith("remarks=") || it.startsWith("remark=") || it.startsWith("name=") }?.substringAfter("=")?.let { URLDecoder.decode(it,"UTF-8") } }.getOrNull()
    if (!remark.isNullOrBlank()) return remark
    val host = raw.substringAfter("@", "").substringBefore(":").substringBefore("?")
    return host.ifBlank { when { raw.startsWith("vless://") -> "VLESS Server"; raw.startsWith("vmess://") -> "VMess Server"; raw.startsWith("trojan://") -> "Trojan Server"; else -> "VELOX Server" } }
}
private fun protocol(raw: String) = when { raw.startsWith("vless://") -> "VLESS"; raw.startsWith("vmess://") -> "VMess"; raw.startsWith("trojan://") -> "TROJAN"; raw.startsWith("ss://") -> "SHADOWSOCKS"; else -> "SERVER" }

@Composable
private fun VeloxHome(context: Context, onConnect: (String) -> Unit, onDisconnect: () -> Unit) {
    var configs by remember { mutableStateOf(readConfigs(context)) }
    var selected by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }
    var showServers by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("disconnected") }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (true) { val p=context.getSharedPreferences(STATE_PREFS,0); status=p.getString("status","disconnected") ?: "disconnected"; message=p.getString("message","") ?: ""; delay(400) } }
    val connected = status == "connected"
    val busy = status == "connecting"
    val current = configs.getOrNull(selected)
    val headline = when(status) { "connected" -> "اتصال امن برقرار است"; "connecting" -> "در حال اتصال…"; "error" -> "اتصال برقرار نشد"; else -> "آماده برای اتصال" }

    Surface(Modifier.fillMaxSize(), color=Bg) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10294A), Bg, Color(0xFF030711))))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=22.dp)) {
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).shadow(12.dp,RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Cyan,Blue)),RoundedCornerShape(16.dp)), contentAlignment=Alignment.Center) { Text("V",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Black) }
                    Spacer(Modifier.width(12.dp))
                    Column { Text("VELOX",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp); Text("BEYOND THE LIMITS",color=Cyan,fontSize=9.sp,letterSpacing=2.sp) }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.clip(CircleShape).background(Color.White.copy(.08f)).padding(horizontal=12.dp,vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(if(connected) Color(0xFF40E69B) else if(busy) Color(0xFFFFC857) else Muted,CircleShape))
                        Spacer(Modifier.width(7.dp)); Text(if(connected) "ONLINE" else if(busy) "CONNECTING" else "OFFLINE",color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(27.dp))
                Text("اتصال بدون مرز",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.ExtraBold)
                Text("یک لمس تا تجربه‌ای سریع و امن",color=Muted,fontSize=12.sp)
                Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                    Box(Modifier.size(282.dp).background(Brush.radialGradient(listOf(Cyan.copy(.16f),Blue.copy(.08f),Color.Transparent)),CircleShape))
                    Box(Modifier.size(236.dp).border(1.dp,Cyan.copy(.18f),CircleShape).padding(14.dp).border(1.dp,Blue.copy(.24f),CircleShape).padding(14.dp).background(Brush.radialGradient(listOf(Color(0xFF1A4774),Color(0xFF0B1C33),Bg)),CircleShape),contentAlignment=Alignment.Center) {
                        Box(Modifier.size(154.dp).shadow(24.dp,CircleShape).background(Brush.linearGradient(if(connected) listOf(Color(0xFF11C98B),Color(0xFF087C79)) else listOf(Cyan,Blue)),CircleShape).border(2.dp,Color.White.copy(.35f),CircleShape).clickable(enabled=!busy) {
                            if(connected) onDisconnect() else if(current!=null) onConnect(current) else Toast.makeText(context,"ابتدا یک کانفیگ اضافه کن",Toast.LENGTH_SHORT).show()
                        },contentAlignment=Alignment.Center) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                Icon(if(connected) Icons.Default.PowerSettingsNew else Icons.Default.FlashOn,null,tint=Color.White,modifier=Modifier.size(46.dp))
                                Text(if(connected) "قطع اتصال" else if(busy) "صبر کن…" else "اتصال",color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
                Text(headline,Modifier.align(Alignment.CenterHorizontally),color=if(connected) Color(0xFF40E69B) else Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold)
                if(status=="error" && message.isNotBlank()) Text(message,Modifier.align(Alignment.CenterHorizontally),color=Color(0xFFFF8290),fontSize=10.sp,maxLines=2)
                Spacer(Modifier.height(17.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    StatTile("دانلود","—",Icons.Default.Download,Modifier.weight(1f))
                    StatTile("آپلود","—",Icons.Default.Upload,Modifier.weight(1f))
                    StatTile("مدت اتصال","--:--",Icons.Default.Timer,Modifier.weight(1f))
                }
                Spacer(Modifier.height(17.dp))
                Card(Modifier.fillMaxWidth().clickable { showServers=true },colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(Color(0xFF17385A),RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Default.Public,null,tint=Cyan,modifier=Modifier.size(27.dp)) }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) { Text("سرور انتخاب‌شده",color=Muted,fontSize=11.sp); Text(current?.let(::configLabel) ?: "هنوز سروری اضافه نشده",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis); Text(current?.let(::protocol) ?: "برای شروع سرور اضافه کن",color=Cyan,fontSize=9.sp,letterSpacing=1.sp) }
                        Icon(Icons.Default.ChevronRight,null,tint=Cyan)
                    }
                }
                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ActionTile("افزودن کانفیگ",Icons.Default.AddLink,Modifier.weight(1f)){showImport=true}
                    ActionTile("مدیریت سرورها",Icons.Default.Dns,Modifier.weight(1f)){showServers=true}
                }
                Spacer(Modifier.height(14.dp))
                Text("POWERED BY VELOX  •  PRIVATE NETWORK",Modifier.align(Alignment.CenterHorizontally),color=Muted.copy(.55f),fontSize=8.sp,letterSpacing=1.sp)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if(showImport) AlertDialog(onDismissRequest={showImport=false},containerColor=Panel,title={Text("افزودن سرور",color=Color.White,fontWeight=FontWeight.Bold)},text={
        Column { Text("لینک کانفیگ یا اشتراک را وارد کن",color=Muted,fontSize=12.sp); Spacer(Modifier.height(8.dp)); OutlinedTextField(value=link,onValueChange={link=it},placeholder={Text("vless:// یا vmess://")},minLines=3,colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=Cyan,unfocusedBorderColor=Muted)); TextButton(onClick={val clip=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;link=clip.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().trim()}){Text("جای‌گذاری از کلیپ‌بورد",color=Cyan)} }
    },confirmButton={TextButton(onClick={val v=link.trim();if(!listOf("vless://","vmess://","trojan://","ss://","https://","http://").any{v.startsWith(it,true)}) Toast.makeText(context,"فرمت لینک معتبر نیست",Toast.LENGTH_SHORT).show() else if(configs.contains(v)) Toast.makeText(context,"این کانفیگ قبلاً اضافه شده",Toast.LENGTH_SHORT).show() else {configs=configs+v;saveConfigs(context,configs);selected=configs.lastIndex;link="";showImport=false}}){Text("ذخیره سرور",color=Cyan)}},dismissButton={TextButton(onClick={showImport=false}){Text("انصراف",color=Muted)}})
    if(showServers) AlertDialog(onDismissRequest={showServers=false},containerColor=Panel,title={Text("سرورهای من",color=Color.White,fontWeight=FontWeight.Bold)},text={
        if(configs.isEmpty()) Text("هنوز کانفیگی اضافه نشده",color=Muted) else LazyColumn { itemsIndexed(configs){index,cfg-> Row(Modifier.fillMaxWidth().clickable{selected=index;showServers=false}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(configLabel(cfg),color=Color.White,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(protocol(cfg)+" • "+cfg.substringAfter("@","").substringBefore(":").take(28),color=Muted,fontSize=10.sp)};if(selected==index) Text("انتخاب",color=Cyan,fontSize=10.sp);TextButton(onClick={configs=configs.filterIndexed{i,_->i!=index};saveConfigs(context,configs);selected=if(configs.isEmpty())0 else selected.coerceAtMost(configs.lastIndex)}){Text("حذف",color=Color(0xFFFF8290))}}} }
    },confirmButton={TextButton(onClick={showServers=false}){Text("بستن",color=Cyan)}})
}
@Composable private fun StatTile(label:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(17.dp)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=Cyan,modifier=Modifier.size(19.dp));Spacer(Modifier.height(7.dp));Text(label,color=Muted,fontSize=9.sp);Text(value,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)}}}
@Composable private fun ActionTile(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){Card(modifier.clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=Color(0xFF14243A)),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){Icon(icon,null,tint=Cyan,modifier=Modifier.size(19.dp));Spacer(Modifier.width(7.dp));Text(label,color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}}}
