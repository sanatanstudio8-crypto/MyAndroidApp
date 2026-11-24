package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.data.ApiServiceFactory
import com.example.myandroidapp.data.ClassDto
import com.example.myandroidapp.data.SubjectDto
import com.example.myandroidapp.player.AuthorizedVideoPlayer
import kotlinx.coroutines.launch

private enum class ClassTab { LIVE, RECORDED }
private enum class CategoryFilter { ALL, FREE, PREMIUM }
private data class ClassItem(
    val id: String,
    val title: String,
    val subject: String,
    val teacher: String,
    val timeLabel: String,
    val status: String,
    val description: String,
    val joinUrl: String?,
    val playbackUrl: String?,
    val visibility: String,
    val demoAccess: Boolean
) {
    val isPremium: Boolean get() = visibility.equals("paid", ignoreCase = true)
}
private fun ClassDto.toClassItem() = ClassItem(
    id, title, subject, teacher, startsAt.orEmpty(), status,
    description.orEmpty(), joinUrl, playbackUrl, visibility, demoAccess
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // College demo mode: the public catalogue opens directly without login.
        setContent { PublicClassesApp() }
    }
}

@Composable
private fun PublicClassesApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var tab by remember { mutableStateOf(ClassTab.LIVE) }
    var category by remember { mutableStateOf(CategoryFilter.ALL) }
    var search by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("All") }
    var live by remember { mutableStateOf(emptyList<ClassItem>()) }
    var recorded by remember { mutableStateOf(emptyList<ClassItem>()) }
    var subjects by remember { mutableStateOf(emptyList<SubjectDto>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var mediaUrl by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        error = null
        try {
            val api = ApiServiceFactory.learning(context)
            live = api.liveClasses().map { it.toClassItem() }
            recorded = api.recordedClasses().map { it.toClassItem() }
            subjects = api.subjects()
        } catch (e: Exception) {
            error = e.message?.take(160) ?: "Catalogue unavailable."
        } finally {
            loading = false
        }
    }
    // Refresh the live and recorded catalogues periodically so newly published live classes appear without reopening the app.
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val source = if (tab == ClassTab.LIVE) live else recorded
    val subjectNames = if (subjects.isNotEmpty()) subjects.map { it.name }
        else (live + recorded).map { it.subject }.distinct()
    val filtered = source.filter { item ->
        (category == CategoryFilter.ALL ||
            (category == CategoryFilter.FREE && !item.isPremium) ||
            (category == CategoryFilter.PREMIUM && item.isPremium)) &&
        (subject == "All" || item.subject == subject) &&
        (search.isBlank() || item.title.contains(search, true) ||
            item.subject.contains(search, true) || item.teacher.contains(search, true) ||
            item.description.contains(search, true))
    }

    MaterialTheme {
        if (mediaUrl != null) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { mediaUrl = null }) {
                    Text("Back to lectures", color = Color.White)
                }
                AuthorizedVideoPlayer(
                    mediaUrl = mediaUrl!!,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        } else {
            Scaffold(bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == ClassTab.LIVE,
                        onClick = { tab = ClassTab.LIVE; subject = "All" },
                        icon = { Icon(Icons.Default.Sensors, null) },
                        label = { Text("Live") }
                    )
                    NavigationBarItem(
                        selected = tab == ClassTab.RECORDED,
                        onClick = { tab = ClassTab.RECORDED; subject = "All" },
                        icon = { Icon(Icons.Default.PlayCircle, null) },
                        label = { Text("Recorded") }
                    )
                }
            }) { padding ->
                Column(
                    Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(padding)
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF182B55)).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painterResource(R.drawable.rwa_logo),
                            contentDescription = "बलिदान RWA",
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("बलिदान RWA", color = Color.White, fontSize = 22.sp,
                                fontWeight = FontWeight.Bold)
                            Text("Search lectures and batches", color = Color(0xFFD9E1F7))
                        }
                        TextButton(onClick = { scope.launch { refresh() } }) {
                            Text("Refresh", color = Color.White)
                        }
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("Search lecture, batch, subject or teacher") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                CategoryFilter.ALL to "All",
                                CategoryFilter.FREE to "Free",
                                CategoryFilter.PREMIUM to "Premium"
                            ).forEach { (value, label) ->
                                FilterChip(
                                    selected = category == value,
                                    onClick = { category = value },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (listOf("All") + subjectNames).distinct().forEach { name ->
                                FilterChip(
                                    selected = subject == name,
                                    onClick = { subject = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                        notice?.let {
                            Text(it, color = Color(0xFF8A4B00), modifier = Modifier.padding(top = 8.dp))
                        }
                        error?.let {
                            Text("Catalogue error: $it", color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        when {
                            loading -> Text("Loading lectures…", modifier = Modifier.padding(16.dp))
                            filtered.isEmpty() -> Text(
                                if (error == null) "No demo lectures are listed yet." else "No lectures loaded.",
                                modifier = Modifier.padding(16.dp)
                            )
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(filtered, key = { it.id }) { item ->
                                    Card(
                                        Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                AssistChip(onClick = {}, label = { Text(item.status) })
                                                Spacer(Modifier.width(6.dp))
                                                if (item.isPremium) {
                                                    Surface(
                                                        color = Color(0xFFFFE8B2),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            if (item.demoAccess) "PREMIUM DEMO" else "PREMIUM",
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                            color = Color(0xFF704500),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        color = Color(0xFFE2F3E8),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            "FREE",
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                            color = Color(0xFF17643A),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Text(item.timeLabel, fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Text("${item.subject} • ${item.teacher}", fontSize = 13.sp, color = Color.Gray)
                                            if (item.description.isNotBlank()) {
                                                Text(item.description, modifier = Modifier.padding(top = 6.dp),
                                                    fontSize = 13.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    when {
                                                        item.isPremium && !item.demoAccess ->
                                                            notice = "This Premium lecture is listed for demo display only; no authorized demo video is enabled."
                                                        item.playbackUrl?.startsWith("https://") == true ->
                                                            mediaUrl = item.playbackUrl
                                                        tab == ClassTab.LIVE && item.joinUrl?.startsWith("https://") == true ->
                                                            runCatching { uriHandler.openUri(item.joinUrl) }
                                                                .onFailure { notice = "Could not open the published class link." }
                                                        tab == ClassTab.LIVE ->
                                                            notice = "No authorized live demo link is available."
                                                        else -> notice = "No authorized demo recording is available."
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                                            ) {
                                                Icon(
                                                    if (tab == ClassTab.LIVE) Icons.Default.Sensors
                                                    else Icons.Default.PlayCircle, null
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    when {
                                                        item.isPremium && !item.demoAccess -> "Premium Demo unavailable"
                                                        tab == ClassTab.LIVE -> "Open live class"
                                                        else -> "Play recording"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
