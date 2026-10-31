package com.abcricket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF19B56B)
private val Dark = Color(0xFF08110D)
private val Panel = Color(0xFF14221B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ABTheme { var showSplash by remember { mutableStateOf(true) }; if (showSplash) ABLaunchScreen { showSplash = false } else CricketHome() } }
    }
}


@Composable
fun ABLaunchScreen(onFinished: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ab_splash")
    val ballRotation by transition.animateFloat(
        initialValue = -25f,
        targetValue = 335f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "ball_rotation"
    )
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "logo_glow"
    )
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()
        while (progress < 1f) {
            progress = ((System.currentTimeMillis() - startedAt) / 2800f).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(35)
        }
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Dark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🏏", fontSize = 58.sp, modifier = Modifier.rotate(ballRotation))
                Text(
                    "AB",
                    color = Color.White.copy(alpha = glow),
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-5).sp
                )
                Text("CRICKET", color = Green, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                Text("PLAY  •  SCORE  •  RANK", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(18.dp))
                Text("EVERY RUN TELLS A STORY", color = Color.White, fontSize = 13.sp, letterSpacing = 1.5.sp)
                Text("BY SYED ABDUL BASIT", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = Green,
                    trackColor = Panel
                )
                Text("GETTING THE GAME READY", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
fun ABTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Dark, surface = Panel), content = content)
}

@Composable
fun CricketHome() {
    var selected by remember { mutableStateOf("Home") }
    var runs by remember { mutableIntStateOf(0) }
    var wickets by remember { mutableIntStateOf(0) }
    var balls by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var team by remember { mutableStateOf("AB Cricket XI") }
    var playerName by remember { mutableStateOf("") }
    var playerRole by remember { mutableStateOf("Batsman") }
    val players = remember { mutableStateListOf<Pair<String, String>>() }
    val overs = "${balls / 6}.${balls % 6}"

    Scaffold(
        containerColor = Dark,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Matches", "Players", "Rankings").forEach { item ->
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = { Icon(when(item) {
                            "Matches" -> Icons.Default.SportsCricket
                            "Players" -> Icons.Default.People
                            "Rankings" -> Icons.Default.EmojiEvents
                            else -> Icons.Default.Home
                        }, contentDescription = item) },
                        label = { Text(item) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Green.copy(alpha = .15f), unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                Text("AB CRICKET", color = Green, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Every run tells a story", color = Color.LightGray, fontSize = 13.sp)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selected, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("SEASON 2026", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            when (selected) {
                "Matches" -> {
                    item {
                        Text("LIVE SCORING", color = Green, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = team, onValueChange = { team = it }, label = { Text("Batting team") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                    }
                    item { ScoreCard(team, runs, wickets, overs, started) }
                    item {
                        Button(onClick = { started = !started }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text(if (started) "PAUSE INNINGS" else "START INNINGS", color = Dark, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (started) {
                        item {
                            Text("ADD RUNS", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 1, 2, 3, 4, 6).forEach { value ->
                                    Button(onClick = { runs += value; balls++ }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("$value", color = Green, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { if (wickets < 10) wickets++; balls++ }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E3030))) { Text("WICKET") }
                                Button(onClick = { balls++ }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("DOT BALL", color = Color.White) }
                            }
                        }
                        item {
                            OutlinedButton(onClick = { runs = 0; wickets = 0; balls = 0; started = false }, modifier = Modifier.fillMaxWidth()) { Text("RESET INNINGS") }
                        }
                    }
                    item { Text("Note: This first scoring demo keeps score only while the app is open. Match saving and cloud sync are coming next.", color = Color.LightGray, fontSize = 12.sp) }
                }
                "Players" -> {
                    item { InfoCard("PLAYER MANAGEMENT", "Add squad members and assign each player a role.") }
                    item {
                        OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Player name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(10.dp))
                        Text("PLAYER ROLE", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Batsman", "Bowler", "All-rounder", "Wicketkeeper").forEach { role ->
                                FilterChip(selected = playerRole == role, onClick = { playerRole = role }, label = { Text(role, fontSize = 10.sp) })
                            }
                        }
                        Button(
                            onClick = {
                                val name = playerName.trim()
                                if (name.isNotEmpty() && players.none { it.first.equals(name, ignoreCase = true) }) {
                                    players.add(name to playerRole)
                                    playerName = ""
                                }
                            },
                            enabled = playerName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) { Text("ADD PLAYER", color = Dark, fontWeight = FontWeight.Bold) }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SQUAD MEMBERS", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${players.size} PLAYERS", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (players.isEmpty()) {
                        item { InfoCard("EMPTY SQUAD", "Add your first player above.") }
                    } else {
                        items(players.size) { index ->
                            val player = players[index]
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Green)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(player.first, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(player.second, color = Color.LightGray, fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { players.removeAt(index) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove player", tint = Color(0xFFE57373))
                                    }
                                }
                            }
                        }
                    }
                    item { Text("Squad entries are temporary for now. Permanent saving and cloud sync will be added in the database stage.", color = Color.LightGray, fontSize = 11.sp) }
                }
                "Rankings" -> {
                    item { InfoCard("TOP BATSMEN", "Rankings will appear after match statistics are saved.") }
                    item { InfoCard("TOP BOWLERS", "Wickets, economy and bowling averages will be calculated here.") }
                }
                else -> {
                    item { ScoreCard(team, runs, wickets, overs, started) }
                    item {
                        Text("QUICK ACCESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickTile("New Match", Icons.Default.Add, Modifier.weight(1f)) { selected = "Matches" }
                            QuickTile("Players", Icons.Default.People, Modifier.weight(1f)) { selected = "Players" }
                            QuickTile("Rankings", Icons.Default.EmojiEvents, Modifier.weight(1f)) { selected = "Rankings" }
                        }
                    }
                    item {
                        Text("APP MODULES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        InfoCard("Live scorecards", "Ball-by-ball scoring and match summaries")
                        InfoCard("Player statistics", "Batting, bowling and career records")
                        InfoCard("Tournaments", "Fixtures, points tables and leaderboards")
                    }
                }
            }
            item {
                InfoCard("ESTABLISHED BY", "Syed Abdul Basit")
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = Green, unfocusedBorderColor = Color.Gray,
    focusedLabelColor = Green, unfocusedLabelColor = Color.LightGray,
    cursorColor = Green
)

@Composable
fun ScoreCard(team: String, runs: Int, wickets: Int, overs: String, started: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (started) "LIVE INNINGS" else "MATCH CENTRE", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(if (started) "● LIVE" else "NOT STARTED", color = Color.LightGray, fontSize = 10.sp)
            }
            Text(team.ifBlank { "Your Team" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$runs/$wickets", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                Text("($overs overs)", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 7.dp))
            }
            HorizontalDivider(color = Color.DarkGray)
            Text("Run rate: ${if (overs == "0.0") "0.00" else String.format("%.2f", runs / ( (overs.substringBefore('.').toInt() * 6 + overs.substringAfter('.').toInt()) / 6.0))}", color = Green, fontSize = 13.sp)
        }
    }
}

@Composable
fun InfoCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Green, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(description, color = Color.LightGray, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuickTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(92.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = Green)
            Spacer(Modifier.height(7.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
