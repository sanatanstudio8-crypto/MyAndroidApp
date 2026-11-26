package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.myandroidapp.data.ApiServiceFactory
import com.example.myandroidapp.data.CatalogueRepository
import com.example.myandroidapp.data.PlaybackRepository
import com.example.myandroidapp.data.ClassDto
import com.example.myandroidapp.data.SubjectDto
import com.example.myandroidapp.data.ScheduleDto
import com.example.myandroidapp.data.PlaybackSessionRequest
import com.example.myandroidapp.data.DemoLiveJoinRequest
import com.example.myandroidapp.data.DemoLiveJoinSessionDto
import com.example.myandroidapp.data.DemoPlaybackSessionRequest
import com.example.myandroidapp.player.AuthorizedVideoPlayer
import com.example.myandroidapp.player.DemoVideoPlayer
import com.example.myandroidapp.player.DEMO_CC0_SAMPLE_URL
import com.example.myandroidapp.player.PlaybackMediaType

private enum class ClassTab { LIVE, RECORDED }
private enum class CategoryFilter { ALL, FREE, PREMIUM }
private enum class HomeSection { BATCHES, FAVOURITES }
private data class ClassItem(
    val id: String, val title: String, val subject: String, val teacher: String,
    val timeLabel: String, val status: String, val description: String, val chapterId: String?,
    val joinUrl: String?, val playbackUrl: String?, val visibility: String,
    val demoAccess: Boolean,
    val contentId: String = id,
    val providerContent: Boolean = false
) {
    val isPremium: Boolean get() = visibility.equals("paid", ignoreCase = true)
}
private fun ClassDto.toClassItem() = ClassItem(
    id, title, subject, teacher, startsAt.orEmpty(), status,
    description.orEmpty(), chapterId, joinUrl, playbackUrl, visibility, demoAccess
)

private val pageBg = Color(0xFF191820)
private val panel = Color(0xFF292735)
private val violet = Color(0xFF8062F2)
private val muted = Color(0xFFB8B4C7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuthenticationGate { onLogout -> PublicClassesApp(onLogout = onLogout) } }
    }
}

@Composable
private fun PublicClassesApp(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var tab by remember { mutableStateOf(ClassTab.LIVE) }
    var section by remember { mutableStateOf(HomeSection.BATCHES) }
    var category by remember { mutableStateOf(CategoryFilter.ALL) }
    var search by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedCourse by remember { mutableStateOf("All") }
    var selectedChapter by remember { mutableStateOf("All") }
    var chapters by remember { mutableStateOf(emptyList<com.example.myandroidapp.data.ChapterDto>()) }
    var live by remember { mutableStateOf(emptyList<ClassItem>()) }
    var recorded by remember { mutableStateOf(emptyList<ClassItem>()) }
    var subjects by remember { mutableStateOf(emptyList<SubjectDto>()) }
    var schedules by remember { mutableStateOf(emptyList<ScheduleDto>()) }
    var favourites by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var mediaUrl by remember { mutableStateOf<String?>(null) }
    var mediaTitle by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf(PlaybackMediaType.AUTO) }
    var mediaExpiresAt by remember { mutableStateOf<String?>(null) }
    var activePlaybackSessionId by remember { mutableStateOf<String?>(null) }
    var activePlaybackContentId by remember { mutableStateOf<String?>(null) }
    var demoMediaUrl by remember { mutableStateOf<String?>(null) }
    var liveRoom by remember { mutableStateOf<DemoLiveJoinSessionDto?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    suspend fun refresh(forceRefresh: Boolean = false) {
        loading = true
        error = null
        try {
            val api = ApiServiceFactory.learning(context)
            if (BuildConfig.DEMO_MODE) {
                val demo = api.demoCatalogue()
                subjects = demo.batches.map { SubjectDto(it.id, it.title) }
                recorded = demo.lectures.map { lecture ->
                    val chapter = demo.chapters.firstOrNull { it.id == lecture.chapterId }
                    val course = demo.courses.firstOrNull { it.id == chapter?.courseId }
                    val batch = demo.batches.firstOrNull { it.id == course?.batchId }
                    ClassItem(
                        id = lecture.id, title = lecture.title,
                        subject = batch?.title ?: "Demo batch",
                        teacher = course?.title ?: "Demo course",
                        timeLabel = "", status = "Recorded • DEMO",
                        description = lecture.description.orEmpty(), chapterId = lecture.chapterId,
                        joinUrl = null, playbackUrl = null, visibility = lecture.visibility,
                        demoAccess = true, contentId = lecture.id
                    )
                }
                live = demo.liveClasses.map { liveClass ->
                    val course = demo.courses.firstOrNull { it.id == liveClass.courseId }
                    val batch = demo.batches.firstOrNull { it.id == course?.batchId }
                    ClassItem(
                        id = liveClass.id, title = liveClass.title,
                        subject = batch?.title ?: "Demo batch",
                        teacher = course?.title ?: "Demo course",
                        timeLabel = liveClass.startsAt.orEmpty(), status = "Available • DEMO",
                        description = "Locally controlled demo room. No external meeting link.",
                        chapterId = null, joinUrl = null, playbackUrl = null,
                        visibility = liveClass.visibility, demoAccess = true,
                        contentId = liveClass.id
                    )
                }
                schedules = emptyList()
                return
            }
            val snapshot = CatalogueRepository.load(api, forceRefresh)
            val loadedBatches = snapshot.batches
            val legacyLive = snapshot.legacyLive.map { it.toClassItem() }
            val legacyRecorded = snapshot.legacyRecorded.map { it.toClassItem() }
            val batchNames = loadedBatches.associate { it.id to it.title }
            val modernRecorded = loadedBatches.flatMap { batch ->
                batch.courses.flatMap { course ->
                    course.chapters.flatMap { chapter ->
                        chapter.lectures.filter { it.type == "recorded" }.map { lecture ->
                            ClassItem(
                                id = lecture.id, title = lecture.title, subject = batch.title,
                                teacher = course.title, timeLabel = "", status = "Recorded",
                                description = lecture.description.orEmpty(), chapterId = chapter.id,
                                joinUrl = null, playbackUrl = null, visibility = lecture.visibility,
                                demoAccess = false, contentId = lecture.id, providerContent = true
                            )
                        }
                    }
                }
            }
            val modernLive = snapshot.liveClasses.map { liveClass ->
                val course = loadedBatches.asSequence().flatMap { it.courses.asSequence() }
                    .firstOrNull { it.id == liveClass.courseId }
                val batchName = liveClass.courseId?.let { id ->
                    loadedBatches.firstOrNull { batch -> batch.courses.any { it.id == id } }?.title
                } ?: course?.title ?: "Live classes"
                ClassItem(
                    id = liveClass.id, title = liveClass.title, subject = batchName,
                    teacher = course?.title.orEmpty(), timeLabel = liveClass.startsAt.orEmpty(),
                    status = liveClass.status, description = "", chapterId = null,
                    joinUrl = null, playbackUrl = null, visibility = liveClass.visibility,
                    demoAccess = false, contentId = liveClass.id, providerContent = true
                )
            }
            live = (legacyLive + modernLive).distinctBy { it.id }
            recorded = (legacyRecorded + modernRecorded).distinctBy { it.id }
            subjects = (snapshot.subjects + loadedBatches.map { SubjectDto(it.id, it.title) }).distinctBy { it.id }
            schedules = snapshot.schedules
        } catch (e: Exception) {
            error = e.message?.take(140) ?: "Catalogue unavailable."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            kotlinx.coroutines.delay(5 * 60_000L)
        }
    }

    LaunchedEffect(selectedSubject, subjects) {
        selectedCourse = "All"
        selectedChapter = "All"
        chapters = if (selectedSubject == "All") emptyList() else {
            val batch = subjects.firstOrNull { it.name == selectedSubject }
            if (BuildConfig.DEMO_MODE) {
                runCatching {
                    val demo = ApiServiceFactory.learning(context).demoCatalogue()
                    val courseIds = demo.courses.filter { it.batchId == batch?.id }.map { it.id }.toSet()
                    demo.chapters.filter { it.courseId in courseIds }.map {
                        com.example.myandroidapp.data.ChapterDto(it.id, batch?.id.orEmpty(), it.title)
                    }
                }.getOrDefault(emptyList())
            } else if (batch == null) emptyList() else runCatching {
                val snapshot = CatalogueRepository.load(ApiServiceFactory.learning(context))
                val batchData = snapshot.batches.firstOrNull { it.id == batch.id }
                batchData?.courses.orEmpty().flatMap { course ->
                    course.chapters.map { chapter ->
                        com.example.myandroidapp.data.ChapterDto(chapter.id, batch.id, chapter.title)
                    }
                }.distinctBy { it.id }
            }.getOrDefault(emptyList())
        }
    }

    val allClasses = live + recorded
    val current = if (tab == ClassTab.LIVE) live else recorded
    val filtered = current.filter { item ->
        (category == CategoryFilter.ALL ||
            (category == CategoryFilter.FREE && !item.isPremium) ||
            (category == CategoryFilter.PREMIUM && item.isPremium)) &&
        (selectedSubject == "All" || item.subject == selectedSubject) &&
        (selectedCourse == "All" || item.teacher == selectedCourse) &&
        (selectedChapter == "All" || item.chapterId == selectedChapter) &&
        (section != HomeSection.FAVOURITES || item.id in favourites) &&
        (search.isBlank() || item.title.contains(search, true) ||
            item.subject.contains(search, true) || item.teacher.contains(search, true) ||
            item.description.contains(search, true))
    }
    val subjectNames = (subjects.map { it.name } + allClasses.map { it.subject })
        .filter { it.isNotBlank() }.distinct().sorted()
    val courseNames = recorded.filter { selectedSubject == "All" || it.subject == selectedSubject }
        .map { it.teacher }.filter { it.isNotBlank() }.distinct().sorted()
    val categoryClasses = allClasses.filter { item ->
        category == CategoryFilter.ALL ||
            (category == CategoryFilter.FREE && !item.isPremium) ||
            (category == CategoryFilter.PREMIUM && item.isPremium)
    }
    val batchCounts = categoryClasses.groupingBy { it.subject }.eachCount()

    MaterialTheme(colorScheme = darkColorScheme(
        primary = violet, secondary = violet, background = pageBg, surface = panel,
        onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White
    )) {
        if (liveRoom != null) {
            val room = liveRoom!!
            Column(Modifier.fillMaxSize().background(pageBg).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("DEMO LIVE ROOM", color = Color(0xFFFFD180), fontWeight = FontWeight.Bold)
                Text(room.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Joined • Locally controlled room", color = Color(0xFF7FE0A1))
                Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Demo room", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("You are connected to this app's local demo room session. This is not a live broadcast or Original RWA meeting.",
                            color = muted)
                        Text("Room participants", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("• You (student)\n• Demo host\n• Demo learner", color = muted)
                    }
                }
                Button(onClick = {
                    scope.launch {
                        runCatching { ApiServiceFactory.learning(context).leaveDemoLiveSession(room.liveClassId) }
                        liveRoom = null
                    }
                }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = violet)) {
                    Text("Leave demo room")
                }
            }
        } else if (demoMediaUrl != null) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { demoMediaUrl = null }) {
                    Text("Back to lectures", color = Color.White)
                }
                DemoVideoPlayer(mediaUrl = demoMediaUrl!!,
                    modifier = Modifier.fillMaxWidth().weight(1f))
            }
        } else if (mediaUrl != null) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { mediaUrl = null }) {
                    Text("Back to lectures", color = Color.White)
                }
                AuthorizedVideoPlayer(mediaUrl = mediaUrl!!,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    title = mediaTitle, mediaType = mediaType, expiresAt = mediaExpiresAt,
                    onRetrySession = {
                        val sessionId = requireNotNull(activePlaybackSessionId)
                        val contentId = requireNotNull(activePlaybackContentId)
                        val refreshed = PlaybackRepository(ApiServiceFactory.learning(context))
                            .refreshSession(sessionId, contentId)
                        mediaExpiresAt = refreshed.expiresAt
                        requireNotNull(refreshed.playbackUrl)
                    },
                    onCloseSession = {
                        activePlaybackSessionId?.let { sessionId ->
                            PlaybackRepository(ApiServiceFactory.learning(context)).closeSession(sessionId)
                        }
                        activePlaybackSessionId = null
                        activePlaybackContentId = null
                    })
            }
        } else {
            Scaffold(
                containerColor = pageBg,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF211F29), contentColor = Color.White) {
                        NavigationBarItem(
                            selected = section == HomeSection.BATCHES,
                            onClick = { section = HomeSection.BATCHES },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("My Batches") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Red, selectedTextColor = Color.Red,
                                indicatorColor = Color(0xFF494253),
                                unselectedIconColor = Color.White, unselectedTextColor = Color.White
                            )
                        )
                        NavigationBarItem(
                            selected = section == HomeSection.FAVOURITES,
                            onClick = { section = HomeSection.FAVOURITES },
                            icon = { Icon(Icons.Default.Favorite, null) },
                            label = { Text("Favourite") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = violet, selectedTextColor = violet,
                                indicatorColor = Color(0xFF494253),
                                unselectedIconColor = Color.White, unselectedTextColor = Color.White
                            )
                        )
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(pageBg).padding(padding),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFF201F27))
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.Menu, "Menu", tint = Color.White,
                                        modifier = Modifier.size(30.dp))
                                }
                                DropdownMenu(expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh catalogue") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = { menuExpanded = false; scope.launch { refresh(forceRefresh = true) } }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sign out") },
                                        onClick = {
                                            menuExpanded = false
                                            onLogout()
                                            CatalogueRepository.invalidate()
                                        }
                                    )
                                }
                            }
                            Text("बलिदान RWA", modifier = Modifier.weight(1f),
                                color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { scope.launch { refresh() } }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                            }
                        }
                    }
                    if (BuildConfig.DEMO_MODE) {
                        item {
                            Text("DEMO MODE • Sample data only • No Original RWA authorization or playback",
                                color = Color(0xFFFFD180), fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = search, onValueChange = { search = it },
                                modifier = Modifier.weight(1f), singleLine = true,
                                placeholder = { Text("⌕  Search batches, lectures...") },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = violet, unfocusedBorderColor = Color(0xFF55515F),
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedContainerColor = panel, unfocusedContainerColor = panel,
                                    focusedPlaceholderColor = muted, unfocusedPlaceholderColor = muted
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { tab = if (tab == ClassTab.LIVE) ClassTab.RECORDED else ClassTab.LIVE },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = violet),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
                            ) { Text("Study", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = tab == ClassTab.LIVE,
                                onClick = { tab = ClassTab.LIVE }, label = { Text("Live") })
                            FilterChip(selected = tab == ClassTab.RECORDED,
                                onClick = { tab = ClassTab.RECORDED }, label = { Text("Recorded") })
                            FilterChip(selected = category == CategoryFilter.ALL,
                                onClick = { category = CategoryFilter.ALL }, label = { Text("All") })
                            FilterChip(selected = category == CategoryFilter.FREE,
                                onClick = { category = CategoryFilter.FREE }, label = { Text("Free") })
                            FilterChip(selected = category == CategoryFilter.PREMIUM,
                                onClick = { category = CategoryFilter.PREMIUM }, label = { Text("Premium") })
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (section == HomeSection.BATCHES) "My Batches" else "Favourite",
                                color = violet, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${allClasses.size} lectures • ${subjectNames.size} batches • ${schedules.size} scheduled", color = muted, fontSize = 12.sp)
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val cards = listOf("All" to categoryClasses.size) +
                                subjectNames.map { it to (batchCounts[it] ?: 0) }
                            cards.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { (name, count) ->
                                        Card(
                                            onClick = { selectedSubject = name },
                                            modifier = Modifier.weight(1f).height(104.dp),
                                            shape = RoundedCornerShape(22.dp),
                                            colors = CardDefaults.cardColors(containerColor = panel),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp, if (selectedSubject == name) violet else Color(0xFF514B61)
                                            )
                                        ) {
                                            Column(Modifier.fillMaxSize().padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center) {
                                                Surface(color = violet,
                                                    shape = RoundedCornerShape(30.dp),
                                                    modifier = Modifier.fillMaxWidth()) {
                                                    Text(name, modifier = Modifier.padding(vertical = 9.dp),
                                                        color = Color.White, textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold, maxLines = 1)
                                                }
                                                Spacer(Modifier.height(7.dp))
                                                Text("$count", color = violet, fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (selectedSubject == "All") "All Batch Lectures" else "$selectedSubject Lectures", color = Color.White, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { selectedSubject = "All"; selectedCourse = "All"; selectedChapter = "All" }) {
                                Text("All batches", color = violet)
                            }
                        }
                    }
                    if (selectedSubject != "All" && courseNames.isNotEmpty()) {
                        item {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = selectedCourse == "All",
                                    onClick = { selectedCourse = "All"; selectedChapter = "All" },
                                    label = { Text("All courses") })
                                courseNames.forEach { course ->
                                    FilterChip(selected = selectedCourse == course,
                                        onClick = { selectedCourse = course; selectedChapter = "All" },
                                        label = { Text(course) })
                                }
                            }
                        }
                    }
                    if (selectedSubject != "All" && chapters.isNotEmpty()) {
                        item {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = selectedChapter == "All", onClick = { selectedChapter = "All" }, label = { Text("All chapters") })
                                chapters.forEach { chapter ->
                                    FilterChip(selected = selectedChapter == chapter.id, onClick = { selectedChapter = chapter.id }, label = { Text(chapter.name) })
                                }
                            }
                        }
                    }
                    if (error != null) {
                        item { Text("Catalogue error: $error", color = Color(0xFFFF8A80),
                            modifier = Modifier.padding(horizontal = 20.dp)) }
                    }
                    if (notice != null) {
                        item { Text(notice!!, color = Color(0xFFFFD180),
                            modifier = Modifier.padding(horizontal = 20.dp)) }
                    }
                    if (loading && allClasses.isEmpty()) {
                        item { Text("Loading batches and lectures…", color = muted,
                            modifier = Modifier.padding(20.dp)) }
                    } else if (filtered.isEmpty()) {
                        item {
                            Text(
                                if (section == HomeSection.FAVOURITES) "No favourite lectures yet."
                                else if (allClasses.isEmpty()) "No demo batches or lectures are listed yet."
                                else "No lectures match this selection.",
                                color = muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { item ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = panel)) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.status, color = muted, fontSize = 12.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(if (item.isPremium) {
                                            if (item.demoAccess) "PREMIUM DEMO" else "PREMIUM"
                                        } else "FREE",
                                            color = if (item.isPremium) Color(0xFFFFD180) else Color(0xFF7FE0A1),
                                            fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            favourites = if (item.id in favourites) favourites - item.id
                                            else favourites + item.id
                                        }) {
                                            Icon(Icons.Default.Favorite, "Favourite",
                                                tint = if (item.id in favourites) Color.Red else muted)
                                        }
                                    }
                                    Text(item.title, color = Color.White, fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text("${item.subject} • ${item.teacher}", color = muted, fontSize = 13.sp)
                                    if (item.description.isNotBlank()) {
                                        Text(item.description, color = Color.White,
                                            modifier = Modifier.padding(top = 6.dp), fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                when {
                                                    BuildConfig.DEMO_MODE && tab == ClassTab.LIVE -> {
                                                        notice = "Checking demo-room entitlement…"
                                                        try {
                                                            val session = ApiServiceFactory.learning(context)
                                                                .createDemoLiveSession(DemoLiveJoinRequest(item.contentId))
                                                            if (session.status == "joined" &&
                                                                session.roomType == "local_demo" &&
                                                                !session.providerAuthorization
                                                            ) {
                                                                notice = null
                                                                liveRoom = session
                                                            } else {
                                                                notice = "Demo room authorization was not granted."
                                                            }
                                                        } catch (_: Exception) {
                                                            notice = "Demo live-room join denied or unavailable."
                                                        }
                                                    }
                                                    BuildConfig.DEMO_MODE -> {
                                                        notice = "Checking server-side demo entitlement…"
                                                        try {
                                                            val session = ApiServiceFactory.learning(context)
                                                                .createDemoPlaybackSession(DemoPlaybackSessionRequest(item.contentId))
                                                            val approvedMedia = when (session.mediaRef) {
                                                                "demo-free", "demo-premium" -> DEMO_CC0_SAMPLE_URL
                                                                else -> null
                                                            }
                                                            if (session.status == "demo_preview" && approvedMedia != null &&
                                                                session.source == "demo_only" && !session.providerAuthorization
                                                            ) {
                                                                notice = null
                                                                demoMediaUrl = approvedMedia
                                                            } else {
                                                                notice = "Demo playback authorization was not granted."
                                                            }
                                                        } catch (_: Exception) {
                                                            notice = "Demo playback denied, expired or unavailable."
                                                        }
                                                    }
                                                    tab == ClassTab.LIVE && (item.providerContent || item.isPremium) ->
                                                        notice = "Authorized live-class join details are not configured."
                                                    item.providerContent || item.isPremium -> {
                                                        notice = "Checking playback authorization…"
                                                        try {
                                                            val session = PlaybackRepository(ApiServiceFactory.learning(context))
                                                                .createSession(item.contentId)
                                                            if (session.status == "created" &&
                                                                session.playbackUrl?.startsWith("https://") == true
                                                            ) {
                                                                notice = null
                                                                mediaTitle = item.title
                                                                mediaType = when (session.protocol?.lowercase()) {
                                                                    "hls" -> PlaybackMediaType.HLS
                                                                    "dash" -> PlaybackMediaType.DASH
                                                                    "progressive", "mp4" -> PlaybackMediaType.PROGRESSIVE
                                                                    else -> PlaybackMediaType.AUTO
                                                                }
                                                                mediaExpiresAt = session.expiresAt
                                                                activePlaybackSessionId = session.id
                                                                activePlaybackContentId = item.contentId
                                                                mediaUrl = session.playbackUrl
                                                            } else {
                                                                notice = "Authorized playback session is unavailable."
                                                            }
                                                        } catch (_: Exception) {
                                                            notice = "Playback authorization failed. Sign in and verify access."
                                                        }
                                                    }
                                                    item.playbackUrl?.startsWith("https://") == true -> {
                                                        mediaTitle = item.title
                                                        mediaType = PlaybackMediaType.AUTO
                                                        mediaExpiresAt = null
                                                        mediaUrl = item.playbackUrl
                                                    }
                                                    tab == ClassTab.LIVE && item.joinUrl?.startsWith("https://") == true ->
                                                        runCatching { uriHandler.openUri(item.joinUrl) }
                                                            .onFailure { notice = "Could not open the published class link." }
                                                    tab == ClassTab.LIVE ->
                                                        notice = "No authorized live demo link is available."
                                                    else -> notice = "No authorized demo recording is available."
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = violet)
                                    ) {
                                        Icon(if (tab == ClassTab.LIVE) Icons.Default.Sensors
                                            else Icons.Default.PlayArrow, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(when {
                                            item.providerContent || item.isPremium -> "Verify access and play"
                                            tab == ClassTab.LIVE -> "Open live class"
                                            else -> "Play recording"
                                        })
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
