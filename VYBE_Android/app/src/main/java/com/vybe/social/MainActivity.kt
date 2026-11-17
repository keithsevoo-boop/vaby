package com.vybe.social

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF0B0A11)
private val Panel = Color(0xFF201D2C)
private val Violet = Color(0xFFB99AFF)
private data class Post(val id: Long, val text: String, val author: String, val liked: Boolean = false, val saved: Boolean = false)
private fun loadPosts(ctx: Context): List<Post> = try {
    val arr = JSONArray(ctx.getSharedPreferences("vybe", 0).getString("posts", "[]"))
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Post(o.getLong("id"), o.getString("text"), o.getString("author"), o.optBoolean("liked"), o.optBoolean("saved"))
    }
} catch (_: Exception) { emptyList() }
private fun persistPosts(ctx: Context, posts: List<Post>) {
    val arr = JSONArray()
    posts.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("author", it.author).put("liked", it.liked).put("saved", it.saved)) }
    ctx.getSharedPreferences("vybe", 0).edit().putString("posts", arr.toString()).apply()
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VybeApp() }
    }
}
@Composable
fun VybeApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("vybe", 0) }
    var tab by remember { mutableIntStateOf(0) }
    var page by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(prefs.getString("name", "newcreator") ?: "newcreator") }
    var nameEdit by remember { mutableStateOf(name) }
    var draft by remember { mutableStateOf(prefs.getString("draft", "") ?: "") }
    val posts = remember { mutableStateListOf<Post>().apply { addAll(loadPosts(ctx)) } }
    val downloads = remember { mutableStateListOf<Long>().apply { addAll(prefs.getString("downloads", "")!!.split(",").mapNotNull { it.toLongOrNull() }) } }
    var videoUrl by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>().apply { addAll(prefs.getString("messages", "")!!.split("\u001f").filter { it.isNotEmpty() }) } }
    var light by remember { mutableStateOf(prefs.getBoolean("light", false)) }
    var privateAccount by remember { mutableStateOf(prefs.getBoolean("private", false)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    fun savePosts() = persistPosts(ctx, posts)
    fun saveDownloads() = prefs.edit().putString("downloads", downloads.joinToString(",")).apply()
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()
    fun share(s: String) {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, s)
        }, "Share with"))
    }
    BackHandler(page.isNotEmpty()) { page = "" }
    val colors = if (light) lightColorScheme(primary = Color(0xFF6734AA)) else darkColorScheme(primary = Violet, background = Ink, surface = Panel)
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("VYBE", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { page = "search" }) { Icon(Icons.Default.Search, "Search") }
                    IconButton(onClick = { page = "settings" }) { Icon(Icons.Default.Settings, "Settings") }
                }
            },
            bottomBar = {
                NavigationBar {
                    val titles = listOf("Home", "Clips", "Create", "Chats", "Profile")
                    val icons = listOf(Icons.Default.Home, Icons.Default.PlayArrow, Icons.Default.AddCircle, Icons.Default.Chat, Icons.Default.Person)
                    titles.forEachIndexed { i, title ->
                        NavigationBarItem(selected = tab == i && page.isEmpty(), onClick = { tab = i; page = "" },
                            icon = { Icon(icons[i], title) }, label = { Text(title) })
                    }
                }
            }
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)) {
                if (page.isNotEmpty()) {
                    TextButton(onClick = { page = "" }) { Text("← Back") }
                }
                when (page) {
                    "search" -> {
                        Heading("Explore")
                        OutlinedTextField(search, { search = it }, label = { Text("Search local posts") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        PostList(posts.filter { it.text.contains(search, true) || it.author.contains(search, true) }, onLike = { id ->
                            val i = posts.indexOfFirst { it.id == id }; if (i >= 0) { posts[i] = posts[i].copy(liked = !posts[i].liked); savePosts() }
                        }, onSave = { id ->
                            val i = posts.indexOfFirst { it.id == id }; if (i >= 0) { posts[i] = posts[i].copy(saved = !posts[i].saved); savePosts() }
                        }, onShare = ::share)
                    }
                    "features" -> {
                        FeatureHub(posts.size) { page = "" }
                    }
                    "saved" -> {
                        Heading("Saved posts")
                        PostList(posts.filter { it.saved }, onLike = { id ->
                            val i = posts.indexOfFirst { it.id == id }; if (i >= 0) { posts[i] = posts[i].copy(liked = !posts[i].liked); savePosts() }
                        }, onSave = { id ->
                            val i = posts.indexOfFirst { it.id == id }; if (i >= 0) { posts[i] = posts[i].copy(saved = false); savePosts() }
                        }, onShare = ::share)
                    }
                    "settings" -> {
                        Heading("Settings & privacy")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Light theme", modifier = Modifier.weight(1f))
                            Switch(light, onCheckedChange = { light = it; prefs.edit().putBoolean("light", it).apply() })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Private profile (local preference)", modifier = Modifier.weight(1f))
                            Switch(privateAccount, onCheckedChange = { privateAccount = it; prefs.edit().putBoolean("private", it).apply() })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notifications preference", modifier = Modifier.weight(1f))
                            Switch(notifications, onCheckedChange = { notifications = it; prefs.edit().putBoolean("notifications", it).apply() })
                        }
                        Text("Privacy and notification preferences are saved locally. Server enforcement is not implemented.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(18.dp))
                        OutlinedButton(onClick = { page = "features" }) { Text("Open Feature Hub") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { share("Try VYBE — a new social app!") }) { Text("Invite friends") }
                    }
                    "downloads" -> {
                        Heading("Offline downloads")
                        Text("Saved in Downloads/VYBE. Download only videos you have permission to save.")
                        Spacer(Modifier.height(12.dp))
                        val dm = remember { ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(downloads.reversed(), key = { it }) { id ->
                                var status = "Unavailable"
                                var uri: Uri? = null
                                try {
                                    dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                                        if (c.moveToFirst()) {
                                            status = when(c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                                                DownloadManager.STATUS_SUCCESSFUL -> "Ready offline"
                                                DownloadManager.STATUS_RUNNING -> "Downloading"
                                                DownloadManager.STATUS_PENDING -> "Queued"
                                                DownloadManager.STATUS_PAUSED -> "Paused"
                                                DownloadManager.STATUS_FAILED -> "Failed"
                                                else -> "Unknown"
                                            }
                                            uri = dm.getUriForDownloadedFile(id)
                                        }
                                    }
                                } catch (_: Exception) { }
                                Card {
                                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                        Text("Video #$id", fontWeight = FontWeight.Bold)
                                        Text(status)
                                        Row {
                                            if (uri != null) TextButton(onClick = {
                                                try { ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }) } catch (_: Exception) { toast("Install a video player to open this file") }
                                            }) { Text("Play") }
                                            TextButton(onClick = { dm.remove(id); downloads.remove(id); saveDownloads() }) { Text("Delete") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> when (tab) {
                        0 -> {
                            Heading("Your space")
                            OutlinedButton(onClick = { page = "features" }) { Text("Explore VYBE features") }
                            Text("Create • connect • share", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            if (posts.isEmpty()) Text("Your feed is ready. Create your first post in the Create tab.")
                            PostList(posts, onLike = { id ->
                                val i = posts.indexOfFirst { it.id == id }; if (i >= 0) { posts[i] = posts[i].copy(liked = !posts[i].liked); savePosts() }
                            }, onSave = { id ->
                                val i = posts.indexOfFirst { it.id == id }; if (i >= 0) { posts[i] = posts[i].copy(saved = !posts[i].saved); savePosts() }
                            }, onShare = ::share)
                        }
                        1 -> {
                            Heading("Clips")
                            Text("Offline-first video library", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("Paste a direct HTTPS video file URL (for example, an MP4) you own or are authorized to download. TikTok and Instagram page links aren't supported.")
                            OutlinedTextField(videoUrl, { videoUrl = it }, label = { Text("Direct HTTPS video URL") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = {
                                try {
                                    val u = Uri.parse(videoUrl.trim())
                                    require(u.scheme == "https" && !u.host.isNullOrBlank()) { "Enter a valid HTTPS video URL" }
                                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    val filename = "vybe_${System.currentTimeMillis()}.mp4"
                                    val request = DownloadManager.Request(u)
                                        .setTitle(filename).setMimeType("video/mp4")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VYBE/$filename")
                                    downloads.add(dm.enqueue(request)); saveDownloads()
                                    videoUrl = ""; toast("Download queued")
                                } catch (e: Exception) { toast(e.message ?: "Download failed") }
                            }) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Download video") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { page = "downloads" }) { Text("My offline videos (${downloads.size})") }
                            Spacer(Modifier.height(20.dp))
                            Text("A real swipeable creator feed needs licensed video content and a server. This prototype does not claim to import TikTok's feed.")
                        }
                        2 -> {
                            Heading("Create a post")
                            Text("Your draft is automatically saved on this phone.")
                            OutlinedTextField(draft, { draft = it; prefs.edit().putString("draft", it).apply() },
                                label = { Text("What's happening?") }, minLines = 5, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            Button(enabled = draft.isNotBlank(), onClick = {
                                posts.add(0, Post(System.currentTimeMillis(), draft.trim(), name))
                                savePosts(); draft = ""; prefs.edit().remove("draft").apply()
                                tab = 0; toast("Posted to your local feed")
                            }) { Text("Publish") }
                            Spacer(Modifier.height(18.dp))
                            Text("Photo/video uploads and public publishing will require cloud storage and accounts.")
                        }
                        3 -> {
                            Heading("Chats")
                            Text("Private local demo — messages are stored on this device only.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(messages) { m ->
                                    Card { Text(m, Modifier.fillMaxWidth().padding(14.dp)) }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(message, { message = it }, label = { Text("Write a note") }, modifier = Modifier.weight(1f))
                                IconButton(enabled = message.isNotBlank(), onClick = {
                                    messages.add(message.trim()); message = ""
                                    prefs.edit().putString("messages", messages.joinToString("\u001f")).apply()
                                }) { Icon(Icons.Default.Send, "Save note") }
                            }
                        }
                        else -> {
                            Heading("Profile")
                            OutlinedButton(onClick = { page = "features" }) { Text("Creator tools & features") }
                            Text("@$name", fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(nameEdit, { nameEdit = it }, label = { Text("Display username") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                val candidate = nameEdit.trim().replace(" ", "_")
                                if (candidate.length in 3..24) {
                                    name = candidate; prefs.edit().putString("name", name).apply(); toast("Profile saved on this phone")
                                } else toast("Use 3 to 24 characters")
                            }) { Text("Save profile") }
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(onClick = { page = "saved" }) { Text("Saved posts") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { page = "downloads" }) { Text("Downloaded videos") }
                            Spacer(Modifier.height(8.dp))
                            Text("${posts.count { it.author == name }} local posts")
                            Spacer(Modifier.height(12.dp))
                            Text("Online registration, followers and verified profiles are not yet connected.")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun Heading(title: String) {
    Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
}
@Composable
private fun PostList(posts: List<Post>, onLike: (Long) -> Unit, onSave: (Long) -> Unit, onShare: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(posts, key = { it.id }) { post ->
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("@${post.author}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(post.text)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onLike(post.id) }) {
                            Icon(if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                            Spacer(Modifier.width(4.dp)); Text(if (post.liked) "Liked" else "Like")
                        }
                        TextButton(onClick = { onSave(post.id) }) {
                            Icon(if (post.saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null)
                            Spacer(Modifier.width(4.dp)); Text("Save")
                        }
                        TextButton(onClick = { onShare(post.text) }) { Icon(Icons.Default.Share, null); Text("Share") }
                    }
                }
            }
        }
    }
}
