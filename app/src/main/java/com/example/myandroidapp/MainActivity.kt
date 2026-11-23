package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.myandroidapp.ui.AuthScreen
import com.example.myandroidapp.security.SecureTokenStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ClassTab { LIVE, RECORDED }

private data class ClassItem(
    val title: String,
    val subject: String,
    val teacher: String,
    val timeLabel: String,
    val status: String,
    val description: String
)

private val liveClasses = listOf(
    ClassItem("Current Affairs – Daily Analysis", "Current Affairs", "Ankit Sir", "Today • 7:00 PM", "LIVE NOW", "Daily news, important events and exam-focused discussion."),
    ClassItem("General Science Practice", "Science", "Faculty Team", "Today • 8:30 PM", "UPCOMING", "Concept revision followed by practice questions."),
    ClassItem("Reasoning: Number Series", "Reasoning", "Ankit Sir", "Tomorrow • 6:00 PM", "UPCOMING", "Learn common patterns and solve examples.")
)

private val recordedClasses = listOf(
    ClassItem("Indian Polity – Fundamental Rights", "Polity", "Ankit Sir", "52 min • 12 lessons", "RECORDED", "A structured explanation of fundamental rights."),
    ClassItem("Maths – Percentage Basics", "Maths", "Faculty Team", "41 min • 8 lessons", "RECORDED", "Core percentage concepts with solved examples."),
    ClassItem("Reasoning – Analogy", "Reasoning", "Ankit Sir", "36 min • 6 lessons", "RECORDED", "Practice analogy questions for competitive exams.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var authenticated by remember {
                mutableStateOf(SecureTokenStore(applicationContext).readToken() != null)
            }
            if (authenticated) {
                ClassesApp()
            } else {
                AuthScreen(onAuthenticated = { authenticated = true })
            }
        }
    }
}

@Composable
private fun ClassesApp() {
    var selectedTab by remember { mutableStateOf(ClassTab.LIVE) }
    var search by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var notice by remember { mutableStateOf<String?>(null) }

    val source = if (selectedTab == ClassTab.LIVE) liveClasses else recordedClasses
    val subjects = listOf("All") + source.map { it.subject }.distinct()
    val visible = source.filter {
        (selectedSubject == "All" || it.subject == selectedSubject) &&
            (search.isBlank() || it.title.contains(search, true) || it.teacher.contains(search, true))
    }

    MaterialTheme {
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
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF182B55)).padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Text("MY LEARNING", color = Color(0xFFB9C8F5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (selectedTab == ClassTab.LIVE) "Live Classes" else "Recorded Classes",
                        color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (selectedTab == ClassTab.LIVE) "Join a class or check the upcoming schedule" else "Learn at your own pace",
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
                    if (notice != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(notice!!, color = Color(0xFF8A4B00), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (visible.isEmpty()) {
                        Text(if (search.isBlank()) "No authorized classes are available yet. Live and recorded lessons will appear here after the backend is configured." else "No classes match your search.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(visible) { item ->
                                ClassCard(item = item, live = selectedTab == ClassTab.LIVE) {
                                    notice = if (selectedTab == ClassTab.LIVE && item.status == "LIVE NOW") {
                                        "Live streaming needs a configured streaming provider and backend."
                                    } else if (selectedTab == ClassTab.LIVE) {
                                        "This class is scheduled. A join link will appear when the backend is connected."
                                    } else {
                                        "Video playback needs a configured video URL or hosting provider."
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
            Spacer(Modifier.height(8.dp))
            Text(item.description, color = Color(0xFF626B7B), fontSize = 13.sp)
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
