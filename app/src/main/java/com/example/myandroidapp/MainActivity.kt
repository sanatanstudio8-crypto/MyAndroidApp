package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.data.ApiServiceFactory
import com.example.myandroidapp.data.ClassDto
import com.example.myandroidapp.data.SubjectDto
import com.example.myandroidapp.data.ScheduleDto
import com.example.myandroidapp.player.AuthorizedVideoPlayer
import com.example.myandroidapp.security.SecureTokenStore

private enum class ClassTab { LIVE, RECORDED }

private data class ClassItem(
    val id: String,
    val title: String,
    val subject: String,
    val teacher: String,
    val timeLabel: String,
    val status: String,
    val description: String,
    val joinUrl: String?,
    val playbackUrl: String?
)

private fun ClassDto.toClassItem() = ClassItem(
    id = id,
    title = title,
    subject = subject,
    teacher = teacher,
    timeLabel = startsAt ?: "",
    status = status,
    description = description.orEmpty(),
    joinUrl = joinUrl,
    playbackUrl = playbackUrl
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var authenticated by remember {
                mutableStateOf(SecureTokenStore(applicationContext).readToken() != null)
            }
            if (authenticated) {
                ClassesApp(onLogout = {
                    SecureTokenStore(applicationContext).clear()
                    authenticated = false
                })
            } else {
                com.example.myandroidapp.ui.AuthScreen(onAuthenticated = { authenticated = true })
            }
        }
    }
}

@Composable
private fun ClassesApp(onLogout: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var selectedTab by remember { mutableStateOf(ClassTab.LIVE) }
    var search by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var notice by remember { mutableStateOf<String?>(null) }
    var liveClasses by remember { mutableStateOf(emptyList<ClassItem>()) }
    var recordedClasses by remember { mutableStateOf(emptyList<ClassItem>()) }
    var subjectRecords by remember { mutableStateOf(emptyList<SubjectDto>()) }
    var chapterNames by remember { mutableStateOf(emptyList<String>()) }
    var scheduleEntries by remember { mutableStateOf(emptyList<ScheduleDto>()) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var activeMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val api = ApiServiceFactory.learning(context)
            subjectRecords = api.subjects()
            scheduleEntries = api.schedule()
        } catch (error: Exception) {
            loadError = error.message?.take(180) ?: "Unable to load subjects and schedule."
        }
    }

    LaunchedEffect(selectedSubject, subjectRecords) {
        val selected = subjectRecords.firstOrNull { it.name == selectedSubject }
        if (selected == null) {
            chapterNames = emptyList()
        } else {
            try {
                chapterNames = ApiServiceFactory.learning(context)
                    .chapters(selected.id)
                    .map { it.name }
            } catch (_: Exception) {
                chapterNames = emptyList()
            }
        }
    }

    LaunchedEffect(selectedTab) {
        loading = true
        loadError = null
        try {
            val api = ApiServiceFactory.learning(context)
            if (selectedTab == ClassTab.LIVE) {
                liveClasses = api.liveClasses().map { it.toClassItem() }
            } else {
                recordedClasses = api.recordedClasses().map { it.toClassItem() }
            }
        } catch (error: Exception) {
            loadError = error.message?.take(180) ?: "Unable to load classes from the backend."
        } finally {
            loading = false
        }
    }

    val source = if (selectedTab == ClassTab.LIVE) liveClasses else recordedClasses
    val subjectNames = if (subjectRecords.isNotEmpty()) subjectRecords.map { it.name } else source.map { it.subject }.filter { it.isNotBlank() }.distinct()\n    val subjects = listOf("All") + subjectNames.distinct()
    val visible = source.filter {
        (selectedSubject == "All" || it.subject == selectedSubject) &&
            (search.isBlank() || it.title.contains(search, true) ||
                it.teacher.contains(search, true) || it.subject.contains(search, true))
    }

    MaterialTheme {
        if (activeMediaUrl != null) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { activeMediaUrl = null }) {
                    Text("Back to classes", color = Color.White)
                }
                AuthorizedVideoPlayer(
                    mediaUrl = activeMediaUrl!!,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == ClassTab.LIVE,
                            onClick = { selectedTab = ClassTab.LIVE; selectedSubject = "All"; notice = null },
                            icon = { androidx.compose.material3.Icon(Icons.Default.Sensors, contentDescription = null) },
                            label = { Text("Live Classes") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == ClassTab.RECORDED,
                            onClick = { selectedTab = ClassTab.RECORDED; selectedSubject = "All"; notice = null },
                            icon = { androidx.compose.material3.Icon(Icons.Default.PlayCircle, contentDescription = null) },
                            label = { Text("Recorded") }
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(padding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF182B55)).padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("MY LEARNING", color = Color(0xFFB9C8F5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (selectedTab == ClassTab.LIVE) "Live Classes" else "Recorded Classes",
                                    color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = onLogout) { Text("Log out", color = Color.White) }
                        }
                        Text(
                            if (selectedTab == ClassTab.LIVE) "Live schedule from your learning backend" else "Authorized recordings from your learning backend",
                            color = Color(0xFFD9E1F7), fontSize = 13.sp
                        )
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search classes or teachers") }
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            subjects.forEach { subject ->
                                FilterChip(
                                    selected = selectedSubject == subject,
                                    onClick = { selectedSubject = subject },
                                    label = { Text(subject) }
                                )
                            }
                        }
                        if (scheduleEntries.isNotEmpty()) {
                            Text("Schedule loaded: ${scheduleEntries.size} entries", color = Color(0xFF626B7B), fontSize = 12.sp)
                        }
                        if (chapterNames.isNotEmpty()) {
                            Text("Chapters: ${chapterNames.joinToString(" • ")}", color = Color(0xFF626B7B), fontSize = 12.sp)
                        }
                        notice?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = Color(0xFF8A4B00), fontSize = 12.sp)
                        }
                        loadError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("Backend error: $it", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        when {
                            loading -> Text("Loading classes…", modifier = Modifier.padding(16.dp))
                            visible.isEmpty() -> Text(
                                if (loadError == null) "No authorized classes are available yet. This list will show real backend data when available."
                                else "No classes loaded. Check your backend configuration.",
                                modifier = Modifier.padding(16.dp)
                            )
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(visible, key = { it.id }) { item ->
                                    ClassCard(item = item, live = selectedTab == ClassTab.LIVE) {
                                        val mediaUrl = item.playbackUrl
                                        when {
                                            mediaUrl != null && mediaUrl.startsWith("https://") -> activeMediaUrl = mediaUrl
                                            selectedTab == ClassTab.LIVE && item.joinUrl?.startsWith("https://") == true -> {
                                                runCatching { uriHandler.openUri(item.joinUrl) }
                                                    .onFailure { notice = "Could not open the authorized class link." }
                                            }
                                            selectedTab == ClassTab.LIVE -> notice = "No authorized live stream or join link was returned."
                                            else -> notice = "No authorized recording URL was returned."
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

@Composable
private fun ClassCard(item: ClassItem, live: Boolean, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(item.status) })
                Spacer(Modifier.weight(1f))
                Text(item.timeLabel, color = Color(0xFF626B7B), fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17233D))
            Spacer(Modifier.height(4.dp))
            Text("${item.subject}  •  ${item.teacher}", color = Color(0xFF5F6878), fontSize = 13.sp)
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.description, color = Color(0xFF626B7B), fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Icon(
                    if (live) Icons.Default.Sensors else Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(if (live) "Open class" else "Play recording")
            }
        }
    }
}
