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
import com.example.myandroidapp.data.ApiServiceFactory
import com.example.myandroidapp.data.ClassDto
import com.example.myandroidapp.data.SubjectDto
import com.example.myandroidapp.player.AuthorizedVideoPlayer

private enum class ClassTab { LIVE, RECORDED }
private enum class CategoryFilter { ALL, FREE, PREMIUM }
private enum class HomeSection { BATCHES, FAVOURITES }
private data class ClassItem(
    val id: String, val title: String, val subject: String, val teacher: String,
    val timeLabel: String, val status: String, val description: String,
    val joinUrl: String?, val playbackUrl: String?, val visibility: String,
    val demoAccess: Boolean
) {
    val isPremium: Boolean get() = visibility.equals("paid", ignoreCase = true)
}
private fun ClassDto.toClassItem() = ClassItem(
    id, title, subject, teacher, startsAt.orEmpty(), status,
    description.orEmpty(), joinUrl, playbackUrl, visibility, demoAccess
)

private val pageBg = Color(0xFF191820)
private val panel = Color(0xFF292735)
private val violet = Color(0xFF8062F2)
private val muted = Color(0xFFB8B4C7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PublicClassesApp() }
    }
}

@Composable
private fun PublicClassesApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var tab by remember { mutableStateOf(ClassTab.LIVE) }
    var section by remember { mutableStateOf(HomeSection.BATCHES) }
    var category by remember { mutableStateOf(CategoryFilter.ALL) }
    var search by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var live by remember { mutableStateOf(emptyList<ClassItem>()) }
    var recorded by remember { mutableStateOf(emptyList<ClassItem>()) }
    var subjects by remember { mutableStateOf(emptyList<SubjectDto>()) }
    var favourites by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var mediaUrl by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    suspend fun refresh() {
        loading = true
        error = null
        try {
            val api = ApiServiceFactory.learning(context)
            live = api.liveClasses().map { it.toClassItem() }
            recorded = api.recordedClasses().map { it.toClassItem() }
            subjects = api.subjects()
        } catch (e: Exception) {
            error = e.message?.take(140) ?: "Catalogue unavailable."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val allClasses = live + recorded
    val current = if (tab == ClassTab.LIVE) live else recorded
    val filtered = current.filter { item ->
        (category == CategoryFilter.ALL ||
            (category == CategoryFilter.FREE && !item.isPremium) ||
            (category == CategoryFilter.PREMIUM && item.isPremium)) &&
        (selectedSubject == "All" || item.subject == selectedSubject) &&
        (section != HomeSection.FAVOURITES || item.id in favourites) &&
        (search.isBlank() || item.title.contains(search, true) ||
            item.subject.contains(search, true) || item.teacher.contains(search, true) ||
            item.description.contains(search, true))
    }
    val subjectNames = (subjects.map { it.name } + allClasses.map { it.subject })
        .filter { it.isNotBlank() }.distinct().sorted()
    val batchCounts = allClasses.groupingBy { it.subject }.eachCount()

    MaterialTheme(colorScheme = darkColorScheme(
        primary = violet, secondary = violet, background = pageBg, surface = panel,
        onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White
    )) {
        if (mediaUrl != null) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(onClick = { mediaUrl = null }) {
                    Text("Back to lectures", color = Color.White)
                }
                AuthorizedVideoPlayer(mediaUrl = mediaUrl!!,
                    modifier = Modifier.fillMaxWidth().weight(1f))
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
                                        onClick = { menuExpanded = false; scope.launch { refresh() } }
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
                            Text("${allClasses.size} lectures", color = muted, fontSize = 12.sp)
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val cards = listOf("All" to allClasses.size) +
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
                            Text("Lectures", color = Color.White, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { selectedSubject = "All" }) {
                                Text("Clear batch", color = violet)
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
                                            when {
                                                item.isPremium && !item.demoAccess ->
                                                    notice = "Premium Demo is listed, but no authorized demo video is enabled."
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
                                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = violet)
                                    ) {
                                        Icon(if (tab == ClassTab.LIVE) Icons.Default.Sensors
                                            else Icons.Default.PlayArrow, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(when {
                                            item.isPremium && !item.demoAccess -> "Premium Demo unavailable"
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
