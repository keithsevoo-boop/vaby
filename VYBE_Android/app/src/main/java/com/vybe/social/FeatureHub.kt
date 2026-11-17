package com.vybe.social

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** Offline-only creator features. No user account, remote publishing or cloud sync is implied. */
@Composable
fun FeatureHub(localPostCount: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("vybe_features", Context.MODE_PRIVATE) }
    var section by remember { mutableStateOf("hub") }
    var statusText by remember { mutableStateOf("") }
    var communityName by remember { mutableStateOf("") }
    var draftCaption by remember { mutableStateOf("") }
    var mediaFile by remember { mutableStateOf(prefs.getString("media_file", "") ?: "") }
    var dataSaver by remember { mutableStateOf(prefs.getBoolean("data_saver", true)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    var communities by remember { mutableStateOf(readStrings(prefs.getString("communities", "[]"))) }
    var statuses by remember { mutableStateOf(readStatuses(prefs.getString("statuses", "[]"))) }
    var planned by remember { mutableStateOf(readStrings(prefs.getString("planned", "[]"))) }
    fun persist() {
        prefs.edit().putString("communities", JSONArray(communities).toString())
            .putString("statuses", JSONArray().apply { statuses.forEach { put(JSONObject().put("text", it.first).put("expires", it.second)) } }.toString())
            .putString("planned", JSONArray(planned).toString()).apply()
    }
    fun saveBitmap(b: Bitmap) {
        val file = File(ctx.filesDir, "vybe_studio_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        mediaFile = file.absolutePath
        prefs.edit().putString("media_file", mediaFile).apply()
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) saveBitmap(bmp)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) try {
            val file = File(ctx.filesDir, "vybe_gallery_${System.currentTimeMillis()}.jpg")
            ctx.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            mediaFile = file.absolutePath
            prefs.edit().putString("media_file", mediaFile).apply()
        } catch (_: Exception) { mediaFile = "" }
    }
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = { if (section == "hub") onClose() else section = "hub" }) { Text("← Back") }
        when (section) {
            "hub" -> {
                Text("VYBE Feature Hub", style = MaterialTheme.typography.headlineMedium)
                Text("Offline tools and upcoming online experiences")
                Spacer(Modifier.height(12.dp))
                val sections = listOf("studio" to "Camera Studio", "status" to "24-hour Status", "communities" to "Communities", "schedule" to "Scheduled drafts", "dashboard" to "Creator Dashboard", "data" to "Data Saver & Offline", "roadmap" to "Online features roadmap")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sections) { (id, title) -> OutlinedButton(onClick = { section = id }, modifier = Modifier.fillMaxWidth()) { Text(title) } }
                }
            }
            "studio" -> {
                Text("Camera Studio", style = MaterialTheme.typography.headlineMedium)
                Text("Capture or select an image. Saved privately on this device; not published.")
                Row {
                    TextButton(onClick = { camera.launch(null) }) { Icon(Icons.Default.CameraAlt, null); Text("Camera") }
                    TextButton(onClick = { gallery.launch("image/*") }) { Icon(Icons.Default.PhotoLibrary, null); Text("Gallery") }
                }
                val bitmap = remember(mediaFile) { try { android.graphics.BitmapFactory.decodeFile(mediaFile) } catch (_: Exception) { null } }
                if (bitmap != null) Image(bitmap.asImageBitmap(), null, modifier = Modifier.fillMaxWidth().height(220.dp))
                OutlinedTextField(draftCaption, { draftCaption = it }, label = { Text("Caption idea") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { prefs.edit().putString("studio_caption", draftCaption).apply() }) { Text("Save caption locally") }
            }
            "status" -> {
                Text("24-hour Status", style = MaterialTheme.typography.headlineMedium)
                Text("Private demo statuses expire after 24 hours; they are not shared online.")
                OutlinedTextField(statusText, { statusText = it }, label = { Text("Your status") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = statusText.isNotBlank(), onClick = {
                    statuses = statuses.filter { it.second > System.currentTimeMillis() } + (statusText.trim() to (System.currentTimeMillis() + 86400000L))
                    statusText = ""; persist()
                }) { Text("Save 24-hour status") }
                statuses.filter { it.second > System.currentTimeMillis() }.forEach { (s, _) -> Text("• $s", modifier = Modifier.padding(8.dp)) }
            }
            "communities" -> {
                Text("Communities", style = MaterialTheme.typography.headlineMedium)
                Text("Create local community ideas. Joining or inviting others requires online accounts.")
                OutlinedTextField(communityName, { communityName = it }, label = { Text("Community name") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = communityName.isNotBlank(), onClick = { communities = communities + communityName.trim(); communityName = ""; persist() }) { Text("Create local community") }
                communities.forEach { Text("• $it", modifier = Modifier.padding(8.dp)) }
            }
            "schedule" -> {
                Text("Scheduled Posts", style = MaterialTheme.typography.headlineMedium)
                Text("Prepare posts to publish later. Automatic online publishing is not available yet.")
                OutlinedTextField(draftCaption, { draftCaption = it }, label = { Text("Future post draft") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = draftCaption.isNotBlank(), onClick = { planned = planned + draftCaption.trim(); draftCaption = ""; persist() }) { Text("Save planned draft") }
                planned.forEach { Text("• $it", modifier = Modifier.padding(8.dp)) }
            }
            "dashboard" -> {
                Text("Creator Dashboard", style = MaterialTheme.typography.headlineMedium)
                Text("Local posts: $localPostCount")
                Text("Local communities: ${communities.size}")
                Text("Planned drafts: ${planned.size}")
                Text("Active local statuses: ${statuses.count { it.second > System.currentTimeMillis() }}")
                Text("Views, followers, reach and revenue require an online analytics backend.")
            }
            "data" -> {
                Text("Data Saver & Offline", style = MaterialTheme.typography.headlineMedium)
                Row { Text("Prefer lower data usage", Modifier.weight(1f)); Switch(dataSaver, { dataSaver = it; prefs.edit().putBoolean("data_saver", it).apply() }) }
                Row { Text("Wi-Fi only for future auto-downloads", Modifier.weight(1f)); Switch(wifiOnly, { wifiOnly = it; prefs.edit().putBoolean("wifi_only", it).apply() }) }
                Text("These are saved preferences. The future streaming engine must enforce them; manual video downloads are unchanged.")
            }
            else -> {
                Text("Online roadmap", style = MaterialTheme.typography.headlineMedium)
                val items = listOf("Smart For You recommendations", "Voice/video calls", "Livestreaming", "Instant translation", "AI Studio", "Licensed music library", "VYBE Shop", "Challenges and duets", "Smart push notifications", "Security and moderation backend", "Cloud backup and sync", "Mini Player", "Custom public profiles", "Automatic offline playlists")
                LazyColumn { items(items) { Text("• $it — requires further development", modifier = Modifier.padding(vertical = 7.dp)) } }
            }
        }
    }
}
private fun readStrings(raw: String?): List<String> = try {
    val arr = JSONArray(raw ?: "[]")
    (0 until arr.length()).map { arr.getString(it) }
} catch (_: Exception) { emptyList() }
private fun readStatuses(raw: String?): List<Pair<String, Long>> = try {
    val arr = JSONArray(raw ?: "[]")
    (0 until arr.length()).map { arr.getJSONObject(it).let { obj -> obj.getString("text") to obj.getLong("expires") } }
} catch (_: Exception) { emptyList() }
