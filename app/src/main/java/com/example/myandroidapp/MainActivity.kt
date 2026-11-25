package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.myandroidapp.data.*
import com.example.myandroidapp.player.AuthorizedVideoPlayer
import com.example.myandroidapp.player.DemoVideoPlayer
import com.example.myandroidapp.player.DEMO_CC0_SAMPLE_URL

private enum class BatchTab { LIVE, RECORDED }
private val pageBg = Color(0xFF191820)
private val panel = Color(0xFF292735)
private val violet = Color(0xFF8062F2)
private val muted = Color(0xFFB8B4C7)
private val premiumGold = Color(0xFFFFD180)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BatchLearningApp() }
    }
}

@Composable
private fun BatchLearningApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var batches by remember { mutableStateOf(emptyList<BatchDto>()) }
    var courses by remember { mutableStateOf(emptyList<CourseDto>()) }
    var chapters by remember { mutableStateOf(emptyList<CourseChapterDto>()) }
    var lectures by remember { mutableStateOf(emptyList<LectureDto>()) }
    var liveClasses by remember { mutableStateOf(emptyList<LiveClassDto>()) }
    var selectedBatch by remember { mutableStateOf<BatchDto?>(null) }
    var tab by remember { mutableStateOf(BatchTab.LIVE) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var mediaUrl by remember { mutableStateOf<String?>(null) }
    var demoMediaUrl by remember { mutableStateOf<String?>(null) }
    var liveRoom by remember { mutableStateOf<DemoLiveJoinSessionDto?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    suspend fun refreshCatalogue() {
        loading = true
        error = null
        try {
            val api = ApiServiceFactory.learning(context)
            if (BuildConfig.DEMO_MODE) {
                val demo = api.demoCatalogue()
                batches = demo.batches.sortedBy { it.title.lowercase() }
                courses = demo.courses
                chapters = demo.chapters
                lectures = demo.lectures
                liveClasses = demo.liveClasses
            } else {
                val loadedBatches = api.batches()
                val loadedCourses = api.courses()
                val loadedChapters = mutableListOf<CourseChapterDto>()
                val loadedLectures = mutableListOf<LectureDto>()
                for (course in loadedCourses) {
                    val courseChapters = api.courseChapters(course.id)
                    loadedChapters += courseChapters
                    for (chapter in courseChapters) loadedLectures += api.lectures(chapter.id)
                }
                batches = loadedBatches.sortedBy { it.title.lowercase() }
                courses = loadedCourses
                chapters = loadedChapters
                lectures = loadedLectures
                liveClasses = api.liveLearningClasses()
            }
        } catch (e: Exception) {
            error = e.message?.substringBefore("\n")?.take(150) ?: "Catalogue unavailable. Please refresh."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { refreshCatalogue() }
    BackHandler(enabled = selectedBatch != null || mediaUrl != null || demoMediaUrl != null || liveRoom != null) {
        when {
            liveRoom != null -> liveRoom = null
            demoMediaUrl != null -> demoMediaUrl = null
            mediaUrl != null -> mediaUrl = null
            else -> { selectedBatch = null; notice = null }
        }
    }

    val visibleBatches = batches.filter { batch ->
        search.isBlank() || batch.title.contains(search, true) ||
            courses.any { it.batchId == batch.id && it.title.contains(search, true) }
    }
    val selected = selectedBatch
    val selectedCourses = courses.filter { it.batchId == selected?.id }
    val selectedCourseIds = selectedCourses.map { it.id }.toSet()
    val selectedChapters = chapters.filter { it.courseId in selectedCourseIds }.sortedBy { it.position }
    val selectedChapterIds = selectedChapters.map { it.id }.toSet()
    val selectedLectures = lectures.filter { it.chapterId in selectedChapterIds }
    val selectedLiveClasses = liveClasses.filter { it.courseId in selectedCourseIds }

    MaterialTheme(colorScheme = darkColorScheme(primary = violet, secondary = violet, background = pageBg,
        surface = panel, onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White)) {
        when {
            liveRoom != null -> {
                val room = liveRoom!!
                Column(Modifier.fillMaxSize().background(pageBg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("DEMO LIVE ROOM", color = premiumGold, fontWeight = FontWeight.Bold)
                    Text(room.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Joined • Local demo room", color = Color(0xFF7FE0A1))
                    Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Demo room", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("This is a simulated room for app testing. It is not an actual live broadcast or Original RWA meeting.", color = muted)
                            Text("Participants: You • Demo host • Demo learner", color = muted)
                        }
                    }
                    Button(onClick = { scope.launch {
                        runCatching { ApiServiceFactory.learning(context).leaveDemoLiveSession(room.liveClassId) }
                        liveRoom = null
                    } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = violet)) {
                        Text("Leave demo room")
                    }
                }
            }
            demoMediaUrl != null -> Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { demoMediaUrl = null }) { Text("Back to batch", color = Color.White) }
                DemoVideoPlayer(mediaUrl = demoMediaUrl!!, modifier = Modifier.fillMaxWidth().weight(1f))
            }
            mediaUrl != null -> Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { mediaUrl = null }) { Text("Back to batch", color = Color.White) }
                AuthorizedVideoPlayer(mediaUrl = mediaUrl!!, modifier = Modifier.fillMaxWidth().weight(1f))
            }
            else -> Scaffold(
                containerColor = pageBg,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF211F29), contentColor = Color.White) {
                        NavigationBarItem(selected = selectedBatch == null, onClick = { selectedBatch = null; notice = null },
                            icon = { Icon(Icons.Default.Menu, null) }, label = { Text("Batches") })
                        NavigationBarItem(selected = false, onClick = { notice = "Open a batch to view its Live and Recorded sections." },
                            icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Favourites") })
                    }
                }
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize().background(pageBg).padding(padding),
                    contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth().background(Color(0xFF201F27)).padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (selectedBatch != null) {
                                IconButton(onClick = { selectedBatch = null; notice = null }) {
                                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                                }
                            } else Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.Menu, "Menu", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Refresh catalogue") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = { menuExpanded = false; scope.launch { refreshCatalogue() } })
                                }
                            }
                            Text(selectedBatch?.title ?: "बलिदान RWA", Modifier.weight(1f), color = Color.White,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            IconButton(onClick = { scope.launch { refreshCatalogue() } }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                            }
                        }
                    }
                    if (BuildConfig.DEMO_MODE) item {
                        Text("DEMO MODE • Sample catalogue only • No payments or Original RWA authorization",
                            color = premiumGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    if (selectedBatch == null) {
                        item { Text("My Batches", color = violet, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)) }
                        item { OutlinedTextField(value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), singleLine = true,
                            placeholder = { Text("Search batches...") }, shape = RoundedCornerShape(16.dp)) }
                        if (error != null) item {
                            Text("Catalogue error: $error", color = Color(0xFFFF8A80), modifier = Modifier.padding(horizontal = 20.dp))
                        }
                        if (loading && batches.isEmpty()) item { Text("Loading batches…", color = muted, modifier = Modifier.padding(20.dp)) }
                        else if (visibleBatches.isEmpty()) item {
                            Text(if (error == null) "No batches available." else "Could not load batches. Tap refresh to retry.",
                                color = muted, modifier = Modifier.padding(20.dp))
                        } else items(visibleBatches, key = { it.id }) { batch ->
                            val batchCourses = courses.filter { it.batchId == batch.id }
                            val courseIds = batchCourses.map { it.id }.toSet()
                            val chapterIds = chapters.filter { it.courseId in courseIds }.map { it.id }.toSet()
                            val recordedCount = lectures.count { it.chapterId in chapterIds }
                            val liveCount = liveClasses.count { it.courseId in courseIds }
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable {
                                selectedBatch = batch; tab = BatchTab.LIVE; notice = null
                            }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
                                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(batch.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text("$liveCount Live • $recordedCount Recorded", color = muted, fontSize = 13.sp)
                                        Text("Open batch  →", color = violet, fontWeight = FontWeight.SemiBold)
                                    }
                                    Surface(color = Color(0xFF3B344D), shape = RoundedCornerShape(12.dp)) {
                                        Text("Study", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Batch classroom", color = violet, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(selected?.title ?: "Batch", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("${selectedCourses.size} courses • ${selectedLiveClasses.size} live classes • ${selectedLectures.size} recorded lectures",
                                    color = muted, fontSize = 13.sp)
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(BatchTab.LIVE, BatchTab.RECORDED).forEach { section ->
                                    val active = tab == section
                                    Surface(Modifier.weight(1f).clickable { tab = section; notice = null },
                                        color = if (active) violet else panel, shape = RoundedCornerShape(12.dp),
                                        border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, muted.copy(alpha = 0.5f))) {
                                        Text(if (section == BatchTab.LIVE) "Live Classes" else "Recorded Lectures",
                                            color = Color.White, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp))
                                    }
                                }
                            }
                        }
                        if (notice != null) item { Text(notice!!, color = premiumGold, modifier = Modifier.padding(horizontal = 20.dp)) }
                        if (tab == BatchTab.LIVE) {
                            if (selectedLiveClasses.isEmpty()) item {
                                Text("No live classes in this batch yet.", color = muted, modifier = Modifier.padding(20.dp))
                            } else items(selectedLiveClasses, key = { it.id }) { liveClass ->
                                val courseName = selectedCourses.firstOrNull { it.id == liveClass.courseId }?.title.orEmpty()
                                ContentCard(liveClass.title, courseName, liveClass.status, liveClass.visibility.equals("paid", true), true) {
                                    scope.launch {
                                        if (BuildConfig.DEMO_MODE) {
                                            notice = "Joining demo room…"
                                            try {
                                                val session = ApiServiceFactory.learning(context).createDemoLiveSession(DemoLiveJoinRequest(liveClass.id))
                                                if (session.status == "joined" && session.roomType == "local_demo" && !session.providerAuthorization) {
                                                    notice = null; liveRoom = session
                                                } else notice = "Demo room access was not granted."
                                            } catch (e: Exception) {
                                                notice = "Demo live room unavailable: ${e.message?.take(90) ?: "request failed"}"
                                            }
                                        } else notice = "Authorized live-class joining is not connected yet. No payment is collected in this app."
                                    }
                                }
                            }
                        } else {
                            if (selectedCourses.isEmpty()) item { Text("No courses in this batch yet.", color = muted, modifier = Modifier.padding(20.dp)) }
                            else items(selectedCourses, key = { it.id }) { course ->
                                val courseChapters = selectedChapters.filter { it.courseId == course.id }
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(course.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    if (courseChapters.isEmpty()) Text("No chapters available.", color = muted, fontSize = 13.sp)
                                    courseChapters.forEach { chapter ->
                                        Surface(color = Color(0xFF24222D), shape = RoundedCornerShape(12.dp)) {
                                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(chapter.title, color = violet, fontWeight = FontWeight.SemiBold)
                                                val chapterLectures = selectedLectures.filter { it.chapterId == chapter.id }
                                                if (chapterLectures.isEmpty()) Text("No lectures in this chapter.", color = muted, fontSize = 13.sp)
                                                chapterLectures.forEach { lecture ->
                                                    Row(Modifier.fillMaxWidth().clickable {
                                                        scope.launch {
                                                            notice = if (lecture.visibility.equals("paid", true)) "Checking Premium access…" else "Preparing recording…"
                                                            try {
                                                                if (BuildConfig.DEMO_MODE) {
                                                                    val session = ApiServiceFactory.learning(context).createDemoPlaybackSession(DemoPlaybackSessionRequest(lecture.id))
                                                                    val media = if (session.mediaRef == "demo-free" || session.mediaRef == "demo-premium") DEMO_CC0_SAMPLE_URL else null
                                                                    if (session.status == "demo_preview" && media != null && session.source == "demo_only" && !session.providerAuthorization) {
                                                                        notice = null; demoMediaUrl = media
                                                                    } else notice = "Demo playback access was not granted."
                                                                } else {
                                                                    val session = ApiServiceFactory.learning(context).createPlaybackSession(PlaybackSessionRequest(lecture.id))
                                                                    if (session.status == "created" && session.playbackUrl?.startsWith("https://") == true) {
                                                                        notice = null; mediaUrl = session.playbackUrl
                                                                    } else notice = "Authorized playback is not configured."
                                                                }
                                                            } catch (e: Exception) {
                                                                notice = if (lecture.visibility.equals("paid", true))
                                                                    "Premium access unavailable. No payment is collected in this app."
                                                                else "Playback unavailable: ${e.message?.take(90) ?: "request failed"}"
                                                            }
                                                        }
                                                    }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.PlayArrow, null, tint = violet)
                                                        Spacer(Modifier.width(8.dp))
                                                        Column(Modifier.weight(1f)) {
                                                            Text(lecture.title, color = Color.White, fontWeight = FontWeight.Medium)
                                                            Text(if (lecture.visibility.equals("paid", true)) "Premium lecture" else "Free lecture",
                                                                color = if (lecture.visibility.equals("paid", true)) premiumGold else Color(0xFF7FE0A1), fontSize = 12.sp)
                                                        }
                                                        Text("Play", color = violet, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
private fun ContentCard(title: String, subtitle: String, status: String, premium: Boolean, isLive: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isLive) Icons.Default.Sensors else Icons.Default.PlayArrow, null, tint = violet, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) Text(subtitle, color = muted, fontSize = 12.sp)
                Text(status, color = muted, fontSize = 12.sp)
            }
            Text(if (premium) "PREMIUM" else "FREE", color = if (premium) premiumGold else Color(0xFF7FE0A1),
                fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
