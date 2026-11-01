package com.abcricket.app

import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFFFF8A00)
private val Dark = Color(0xFF050505)
private val Panel = Color(0xFF171717)

private data class MatchRecord(val date: String, val team: String, val opponent: String, val score: String, val overs: String, val toss: String, val details: String = "")
data class PlayerCareer(val name: String, val role: String, val matches: Int = 0, val runs: Int = 0, val balls: Int = 0, val wickets: Int = 0, val bowlingRuns: Int = 0, val bowlingBalls: Int = 0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ABTheme { var showSplash by remember { mutableStateOf(true) }; if (showSplash) ABLaunchScreen { showSplash = false } else CricketEntry() } }
    }
}


@Composable
fun CricketEntry() {
    CricketHome()
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
    var extras by remember { mutableIntStateOf(0) }
    var innings by remember { mutableIntStateOf(1) }
    var opponentBattingFirst by remember { mutableStateOf(false) }
    var firstInningsRuns by remember { mutableIntStateOf(0) }
    var firstInningsWickets by remember { mutableIntStateOf(0) }
    var firstInningsOvers by remember { mutableStateOf("0.0") }
    var firstInningsTeam by remember { mutableStateOf("") }
    var firstStrikerName by remember { mutableStateOf("") }
    var firstStrikerRuns by remember { mutableIntStateOf(0) }
    var firstStrikerBalls by remember { mutableIntStateOf(0) }
    var firstNonStrikerName by remember { mutableStateOf("") }
    var firstNonStrikerRuns by remember { mutableIntStateOf(0) }
    var firstNonStrikerBalls by remember { mutableIntStateOf(0) }
    var firstBowlerName by remember { mutableStateOf("") }
    var firstBowlerRuns by remember { mutableIntStateOf(0) }
    var firstBowlerWickets by remember { mutableIntStateOf(0) }
    var firstBowlerBalls by remember { mutableIntStateOf(0) }
    val inningsBowlerFigures = remember { mutableStateListOf<String>() }
    var firstInningsBowlerFigures by remember { mutableStateOf("") }
    var overNeedsBowler by remember { mutableStateOf(false) }
    var target by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var team by remember { mutableStateOf("AB Cricket XI") }
    var opponentTeam by remember { mutableStateOf("Opposition XI") }
    var oversLimit by remember { mutableStateOf("20") }
    var tossWinner by remember { mutableStateOf("AB Cricket XI") }
    var tossDecision by remember { mutableStateOf("Bat") }
    var strikerName by remember { mutableStateOf("") }
    var nonStrikerName by remember { mutableStateOf("") }
    var bowlerName by remember { mutableStateOf("") }
    var awaitingNextBatter by remember { mutableStateOf(false) }
    var showDismissalDialog by remember { mutableStateOf(false) }
    var selectedDismissal by remember { mutableStateOf("Bowled") }
    var lastDismissal by remember { mutableStateOf("") }
    val ballByBallLog = remember { mutableStateListOf<String>() }
    var strikerRuns by remember { mutableIntStateOf(0) }
    var strikerBalls by remember { mutableIntStateOf(0) }
    var nonStrikerRuns by remember { mutableIntStateOf(0) }
    var nonStrikerBalls by remember { mutableIntStateOf(0) }
    var bowlerRuns by remember { mutableIntStateOf(0) }
    var bowlerWickets by remember { mutableIntStateOf(0) }
    var bowlerBalls by remember { mutableIntStateOf(0) }
    var playerName by remember { mutableStateOf("") }
    var playerRole by remember { mutableStateOf("Batsman") }
    val opponentPlayerNames = remember { mutableStateListOf<String>().apply { repeat(11) { add("") } } }
    var opponentPlayersText by remember { mutableStateOf("") }
    var matchSaved by remember { mutableStateOf(false) }
    val matchHistory = remember { mutableStateListOf<MatchRecord>() }
    var selectedHistoryMatch by remember { mutableStateOf<MatchRecord?>(null) }
    val players = remember { mutableStateListOf<Pair<String, String>>() }
    val careerStats = remember { mutableStateListOf<PlayerCareer>() }
    val context = LocalContext.current
    val cloudScope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("ab_cricket_data", android.content.Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) {
        team = preferences.getString("team", team) ?: team
        opponentTeam = preferences.getString("opponentTeam", opponentTeam) ?: opponentTeam
        oversLimit = preferences.getString("oversLimit", oversLimit) ?: oversLimit
        tossWinner = preferences.getString("tossWinner", tossWinner) ?: tossWinner
        tossDecision = preferences.getString("tossDecision", tossDecision) ?: tossDecision
        strikerName = preferences.getString("strikerName", "") ?: ""
        nonStrikerName = preferences.getString("nonStrikerName", "") ?: ""
        bowlerName = preferences.getString("bowlerName", "") ?: ""
        runs = preferences.getInt("runs", 0)
        wickets = preferences.getInt("wickets", 0)
        balls = preferences.getInt("balls", 0)
        extras = preferences.getInt("extras", 0)
        innings = preferences.getInt("innings", 1)
        firstInningsRuns = preferences.getInt("firstInningsRuns", 0)
        firstInningsWickets = preferences.getInt("firstInningsWickets", 0)
        firstInningsOvers = preferences.getString("firstInningsOvers", "0.0") ?: "0.0"
        firstInningsTeam = preferences.getString("firstInningsTeam", "") ?: ""
        target = preferences.getInt("target", 0)
        strikerRuns = preferences.getInt("strikerRuns", 0)
        strikerBalls = preferences.getInt("strikerBalls", 0)
        nonStrikerRuns = preferences.getInt("nonStrikerRuns", 0)
        nonStrikerBalls = preferences.getInt("nonStrikerBalls", 0)
        bowlerRuns = preferences.getInt("bowlerRuns", 0)
        bowlerWickets = preferences.getInt("bowlerWickets", 0)
        bowlerBalls = preferences.getInt("bowlerBalls", 0)
        matchSaved = preferences.getBoolean("matchSaved", false)
        val savedHistory = JSONArray(preferences.getString("matchHistory", "[]"))
        for (i in 0 until savedHistory.length()) {
            val item = savedHistory.getJSONObject(i)
            matchHistory.add(MatchRecord(item.optString("date"), item.optString("team"), item.optString("opponent"), item.optString("score"), item.optString("overs"), item.optString("toss"), item.optString("details")))
        }
        val savedCareer = JSONArray(preferences.getString("careerStats", "[]"))
        for (i in 0 until savedCareer.length()) { val item = savedCareer.getJSONObject(i); careerStats.add(PlayerCareer(item.optString("name"), item.optString("role"), item.optInt("matches"), item.optInt("runs"), item.optInt("balls"), item.optInt("wickets"), item.optInt("bowlingRuns"), item.optInt("bowlingBalls"))) }
        val savedPlayers = JSONArray(preferences.getString("players", "[]"))
        for (i in 0 until savedPlayers.length()) {
            val item = savedPlayers.getJSONObject(i)
            players.add(item.optString("name") to item.optString("role"))
        }
    }
    LaunchedEffect(careerStats.size) { val saved = JSONArray(); careerStats.forEach { p -> saved.put(JSONObject().put("name", p.name).put("role", p.role).put("matches", p.matches).put("runs", p.runs).put("balls", p.balls).put("wickets", p.wickets).put("bowlingRuns", p.bowlingRuns).put("bowlingBalls", p.bowlingBalls)) }; preferences.edit().putString("careerStats", saved.toString()).apply() }
    LaunchedEffect(players.size) {
        val savedPlayers = JSONArray()
        players.forEach { (name, role) -> savedPlayers.put(JSONObject().put("name", name).put("role", role)) }
        preferences.edit().putString("players", savedPlayers.toString()).apply()
    }
    LaunchedEffect(team, opponentTeam, oversLimit, tossWinner, tossDecision, strikerName, nonStrikerName, bowlerName, runs, wickets, balls, extras, innings, firstInningsRuns, firstInningsWickets, firstInningsOvers, firstInningsTeam, target) {
        preferences.edit().putString("team", team).putString("opponentTeam", opponentTeam)
            .putString("oversLimit", oversLimit).putString("tossWinner", tossWinner)
            .putString("tossDecision", tossDecision).putString("strikerName", strikerName)
            .putString("nonStrikerName", nonStrikerName).putString("bowlerName", bowlerName)
            .putInt("runs", runs).putInt("wickets", wickets).putInt("balls", balls).putInt("extras", extras).putInt("innings", innings).putInt("firstInningsRuns", firstInningsRuns).putInt("firstInningsWickets", firstInningsWickets).putString("firstInningsOvers", firstInningsOvers).putString("firstInningsTeam", firstInningsTeam).putInt("target", target).apply()
    }
    val overs = "${balls / 6}.${balls % 6}"
    val opponentRoster = opponentPlayerNames.filter { it.isNotBlank() }.map { it to "Opponent" }
    val setupOpponentBatting = (tossWinner == team && tossDecision == "Bowl") || (tossWinner == opponentTeam && tossDecision == "Bat")
    val liveBattingPlayers = if ((innings == 1 && opponentBattingFirst) || (innings == 2 && !opponentBattingFirst)) opponentRoster else players
    val liveBowlingPlayers = if ((innings == 1 && opponentBattingFirst) || (innings == 2 && !opponentBattingFirst)) players else opponentRoster
    LaunchedEffect(matchSaved) { preferences.edit().putBoolean("matchSaved", matchSaved).apply() }
    LaunchedEffect(strikerRuns, strikerBalls, nonStrikerRuns, nonStrikerBalls, bowlerRuns, bowlerWickets, bowlerBalls) {
        preferences.edit().putInt("strikerRuns", strikerRuns).putInt("strikerBalls", strikerBalls)
            .putInt("nonStrikerRuns", nonStrikerRuns).putInt("nonStrikerBalls", nonStrikerBalls)
            .putInt("bowlerRuns", bowlerRuns).putInt("bowlerWickets", bowlerWickets).putInt("bowlerBalls", bowlerBalls).apply()
    }

        if (showDismissalDialog) {
            AlertDialog(
                onDismissRequest = { showDismissalDialog = false },
                containerColor = Panel,
                title = { Text("SELECT DISMISSAL", color = Color.White, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("Bowled", "Caught", "LBW", "Run out", "Stumped", "Hit wicket", "Retired hurt", "Obstructing the field", "Other").forEach { method ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedDismissal == method, onClick = { selectedDismissal = method }, colors = RadioButtonDefaults.colors(selectedColor = Green))
                                Text(method, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        ballByBallLog.add("${balls / 6}.${balls % 6 + 1}  $bowlerName to $strikerName  WICKET — $selectedDismissal")
                        balls++; bowlerBalls++; strikerBalls++
                        val retiredHurt = selectedDismissal == "Retired hurt"
                        if (!retiredHurt) {
                            wickets++
                            if (selectedDismissal in listOf("Bowled", "Caught", "LBW", "Stumped", "Hit wicket")) bowlerWickets++
                        }
                        lastDismissal = "$strikerName — " + if (retiredHurt) "Retired hurt (not out)" else selectedDismissal
                        awaitingNextBatter = liveBattingPlayers.any { it.first != strikerName && it.first != nonStrikerName } && (retiredHurt || wickets < 10)
                        if (balls % 6 == 0) {
                            overNeedsBowler = true
                            val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old
                            val rr = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = rr
                            val bb = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = bb
                        }
                        showDismissalDialog = false
                    }) { Text("CONFIRM WICKET", color = Green) }
                },
                dismissButton = { TextButton(onClick = { showDismissalDialog = false }) { Text("CANCEL", color = Color.LightGray) } }
            )
        }
    Scaffold(
        containerColor = Dark,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Matches", "Players", "Stats", "Rankings", "History").forEach { item ->
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = { Icon(when(item) {
                            "Matches" -> Icons.Default.SportsCricket
                            "Players" -> Icons.Default.People
                            "Stats" -> Icons.Default.BarChart
                            "Rankings" -> Icons.Default.EmojiEvents
                            "History" -> Icons.Default.History
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
            if (selected != "Home") {
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
            }
            when (selected) {
                "Matches" -> {
                    item {
                        if (!started) {
                        Text("MATCH SETUP", color = Green, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = team, onValueChange = { team = it }, label = { Text("Team 1 / batting team") }, singleLine = true, enabled = !started, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = opponentTeam, onValueChange = { opponentTeam = it }, label = { Text("Team 2 / opponent") }, singleLine = true, enabled = !started, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(8.dp))
                        Text("OPPONENT PLAYERS", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            opponentPlayerNames.forEachIndexed { index, playerName ->
                                OutlinedTextField(value = playerName, onValueChange = { value ->
                                    opponentPlayerNames[index] = value
                                    opponentPlayersText = opponentPlayerNames.joinToString("\n")
                                }, label = { Text("Player ${index + 1}") }, singleLine = true, enabled = !started, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = oversLimit, onValueChange = { value -> if (value.length <= 3 && value.all { it.isDigit() }) oversLimit = value }, label = { Text("Overs per innings") }, singleLine = true, enabled = !started, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(12.dp))
                        Text("TOSS WINNER", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(team.ifBlank { "Team 1" }, opponentTeam.ifBlank { "Team 2" }).forEach { name ->
                                FilterChip(selected = tossWinner == name, onClick = { tossWinner = name }, enabled = !started, label = { Text(name, maxLines = 1) })
                            }
                        }
                        Text("TOSS DECISION", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf("Bat", "Bowl").forEach { decision ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = tossDecision == decision, onClick = { tossDecision = decision }, enabled = !started, colors = RadioButtonDefaults.colors(selectedColor = Green))
                                    Text(decision, color = Color.White)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.DarkGray)
                        Text("PLAYING XI DETAILS", color = Green, fontWeight = FontWeight.Bold)
                        PlayerPicker("Striker", strikerName, if (setupOpponentBatting) opponentRoster else players, !started) { strikerName = it }
                        PlayerPicker("Non-striker", nonStrikerName, if (setupOpponentBatting) opponentRoster else players, !started) { nonStrikerName = it }
                        PlayerPicker("Opening bowler", bowlerName, if (setupOpponentBatting) players else opponentRoster, !started) { bowlerName = it }
                        Text("Toss: " + tossWinner + " elected to " + tossDecision + " | " + (oversLimit.ifBlank { "20" }) + " overs", color = Color.LightGray, fontSize = 12.sp)
                        Button(onClick = {
                            val configuredOvers = oversLimit.toIntOrNull() ?: 0
                            val validTeams = team.isNotBlank() && opponentTeam.isNotBlank() && !team.equals(opponentTeam, true)
                            val validBatters = strikerName.isBlank() || nonStrikerName.isBlank() || !strikerName.equals(nonStrikerName, true)
                            if (validTeams && configuredOvers in 1..100 && validBatters) {
                                if (innings == 1) {
                                    val firstBattingTeam = if ((tossWinner == team && tossDecision == "Bat") || (tossWinner == opponentTeam && tossDecision == "Bowl")) team else opponentTeam
                                    opponentBattingFirst = firstBattingTeam != team
                                    if (firstBattingTeam != team) { val oldTeam = team; team = opponentTeam; opponentTeam = oldTeam }
                                }
                                started = true
                            }
                        }, enabled = !started && team.isNotBlank() && opponentTeam.isNotBlank() && !team.equals(opponentTeam, true) && (oversLimit.toIntOrNull() ?: 0) in 1..100 && (strikerName.isBlank() || nonStrikerName.isBlank() || !strikerName.equals(nonStrikerName, true)), modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text("START MATCH", color = Dark, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                    }
                    if (started) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("LIVE MATCH • INNINGS $innings", color = Green, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        Spacer(Modifier.weight(1f))
                                        Text("$runs/$wickets  ($overs)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                    }
                                    Text("BATTING NOW", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            PlayerPicker("STRIKER", strikerName, liveBattingPlayers, true) { strikerName = it }
                                            Text("$strikerRuns runs • $strikerBalls balls", color = Green, fontSize = 11.sp)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            PlayerPicker("NON-STRIKER", nonStrikerName, liveBattingPlayers, true) { nonStrikerName = it }
                                            Text("$nonStrikerRuns runs • $nonStrikerBalls balls", color = Color.LightGray, fontSize = 11.sp)
                                        }
                                    }
                                    Text("CURRENT BOWLER", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (overNeedsBowler) {
                                        InfoCard("OVER COMPLETE", "Naya over shuru karne ke liye naya bowler select karein.")
                                        PlayerPicker("NEXT OVER BOWLER", "", liveBowlingPlayers.filter { it.first != bowlerName }, true) { nextBowler ->
                                            if (bowlerName.isNotBlank()) inningsBowlerFigures.add("$bowlerName: ${bowlerBalls / 6}.${bowlerBalls % 6} overs, $bowlerRuns runs, $bowlerWickets wickets")
                                            bowlerName = nextBowler
                                            bowlerRuns = 0; bowlerWickets = 0; bowlerBalls = 0
                                            overNeedsBowler = false
                                        }
                                    } else PlayerPicker("BOWLER", bowlerName, liveBowlingPlayers, true) { bowlerName = it }
                                    Text("${bowlerBalls / 6}.${bowlerBalls % 6} overs • $bowlerRuns runs • $bowlerWickets wickets", color = Green, fontSize = 11.sp)
                                    HorizontalDivider(color = Color.DarkGray)
                                    Text("QUICK SCORING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        listOf(0, 1, 2, 3, 4, 6).forEach { value ->
                                            Button(onClick = { if (!awaitingNextBatter && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && wickets < 10 && (innings == 1 || runs < target)) { ballByBallLog.add("${balls / 6}.${balls % 6 + 1}  $bowlerName to $strikerName  ${value} run${if (value == 1) "" else "s"}"); runs += value; balls++; bowlerBalls++; if (balls % 6 == 0) overNeedsBowler = true; bowlerRuns += value; strikerRuns += value; strikerBalls++; if (value % 2 == 1) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val rr = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = rr; val bb = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = bb }; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val rr = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = rr; val bb = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = bb } } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("$value", color = Green, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { ballByBallLog.add("${balls / 6}.${balls % 6 + 1}  $bowlerName to $strikerName  WD"); runs++; extras++; bowlerRuns++ } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("WIDE", color = Green, fontSize = 10.sp) }
                                        Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { ballByBallLog.add("${balls / 6}.${balls % 6 + 1}  $bowlerName to $strikerName  NB"); runs++; extras++; bowlerRuns++; strikerBalls++ } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("NO BALL", color = Green, fontSize = 10.sp) }
                                        Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { ballByBallLog.add("${balls / 6}.${balls % 6 + 1}  $bowlerName to $strikerName  B"); runs++; extras++; balls++; bowlerBalls++; strikerBalls++; if (balls % 6 == 0) overNeedsBowler = true; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val rr = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = rr; val bb = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = bb } } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("BYE", color = Green, fontSize = 10.sp) }
                                        Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { runs++; extras++; balls++; bowlerBalls++; strikerBalls++; if (balls % 6 == 0) overNeedsBowler = true; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val rr = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = rr; val bb = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = bb } } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("LEG-BYE", color = Green, fontSize = 10.sp) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { selectedDismissal = "Bowled"; showDismissalDialog = true } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))) { Text("WICKET") }
                                        Button(onClick = { if (!awaitingNextBatter && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && wickets < 10 && (innings == 1 || runs < target)) { ballByBallLog.add("${balls / 6}.${balls % 6 + 1}  $bowlerName to $strikerName  0"); balls++; bowlerBalls++; strikerBalls++; if (balls % 6 == 0) { overNeedsBowler = true; val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val rr = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = rr; val bb = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = bb } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("DOT BALL", color = Color.White) }
                                    }
                                    if (awaitingNextBatter && wickets < 10) {
                                        Text("WICKET FALLEN — SELECT NEXT BATTER", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        PlayerPicker("NEXT BATTER", "", liveBattingPlayers.filter { it.first != strikerName && it.first != nonStrikerName }, true) { next -> strikerName = next; strikerRuns = 0; strikerBalls = 0; awaitingNextBatter = false }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("INNINGS $innings OF 2", color = Green, fontWeight = FontWeight.Bold)
                                if (innings == 2) {
                                    Text("${firstInningsTeam}: $firstInningsRuns/$firstInningsWickets ($firstInningsOvers)", color = Color.White)
                                    Text("TARGET: $target RUNS", color = Green, fontWeight = FontWeight.Bold)
                                    Text(if (runs >= target) "$team won by ${10 - wickets} wickets" else "Need ${(target - runs).coerceAtLeast(0)} runs to win", color = Color.LightGray)
                                } else Text("First innings. Complete the innings to set a target.", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("LIVE PLAYER FIGURES", color = Green, fontWeight = FontWeight.Bold)
                                Text("EXTRAS  $extras", color = Color.LightGray, fontSize = 12.sp)
                                if (ballByBallLog.isNotEmpty()) {
                                    Text("BALL-BY-BALL", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    ballByBallLog.takeLast(12).asReversed().forEach { delivery -> Text(delivery, color = Color.White, fontSize = 11.sp) }
                                }
                                if (lastDismissal.isNotBlank()) Text("LAST WICKET  $lastDismissal", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text((strikerName.ifBlank { "Striker" }) + " (on strike): $strikerRuns runs off $strikerBalls balls", color = Color.White, fontSize = 13.sp)
                                Text((nonStrikerName.ifBlank { "Non-striker" }) + ": $nonStrikerRuns runs off $nonStrikerBalls balls", color = Color.White, fontSize = 13.sp)
                                Text((bowlerName.ifBlank { "Bowler" }) + ": ${bowlerBalls / 6}.${bowlerBalls % 6} overs, $bowlerRuns runs, $bowlerWickets wickets", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Button(onClick = {
                            if (started) {
                                started = false
                            } else {
                                val configuredOvers = oversLimit.toIntOrNull() ?: 0
                                val validTeams = team.isNotBlank() && opponentTeam.isNotBlank() && !team.equals(opponentTeam, true)
                                val validBatters = strikerName.isBlank() || nonStrikerName.isBlank() || !strikerName.equals(nonStrikerName, true)
                                if (validTeams && configuredOvers in 1..100 && validBatters) {
                                    if (innings == 1) {
                                        val firstBattingTeam = if ((tossWinner == team && tossDecision == "Bat") || (tossWinner == opponentTeam && tossDecision == "Bowl")) team else opponentTeam
                                        if (firstBattingTeam != team) {
                                            val oldTeam = team
                                            team = opponentTeam
                                            opponentTeam = oldTeam
                                        }
                                    }
                                    started = true
                                }
                            }
                        }, enabled = started || (team.isNotBlank() && opponentTeam.isNotBlank() && !team.equals(opponentTeam, true) && (oversLimit.toIntOrNull() ?: 0) in 1..100 && (strikerName.isBlank() || nonStrikerName.isBlank() || !strikerName.equals(nonStrikerName, true))), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text(if (started) "PAUSE INNINGS" else "START INNINGS", color = Dark, fontWeight = FontWeight.Bold)
                        }
                        if (!started && (team.isBlank() || opponentTeam.isBlank() || team.equals(opponentTeam, true) || (oversLimit.toIntOrNull() ?: 0) !in 1..100 || (strikerName.isNotBlank() && strikerName.equals(nonStrikerName, true))) ) {
                            Text("Check team names, overs (1–100), and choose two different opening batters.", color = Color(0xFFFFB36B), fontSize = 11.sp)
                        }
                    }
                    if (started && false) {
                        item {
                            Text("EXTRAS", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { runs++; extras++; bowlerRuns++ } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("WIDE +1", color = Green) }
                                Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { runs++; extras++; bowlerRuns++; strikerBalls++ } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("NO-BALL +1", color = Green) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { runs++; extras++; balls++; bowlerBalls++ } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("BYE +1", color = Green) }
                                Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { runs++; extras++; balls++; bowlerBalls++ } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("LEG-BYE +1", color = Green) }
                            }
                        }
                        item {
                            Text("ADD RUNS", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 1, 2, 3, 4, 6).forEach { value ->
                                    Button(onClick = { if (!awaitingNextBatter && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && wickets < 10 && (innings == 1 || runs < target)) { runs += value; balls++; bowlerBalls++; bowlerRuns += value; if (strikerName.isNotBlank()) { strikerRuns += value; strikerBalls++ } else { strikerRuns += value; strikerBalls++ }; if (value % 2 == 1) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val r = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = r; val b = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = b }; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val r = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = r; val b = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = b } } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("$value", color = Green, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { if (!awaitingNextBatter && wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { wickets++; balls++; bowlerBalls++; bowlerWickets++; strikerBalls++; awaitingNextBatter = wickets < 10 && players.any { it.first != strikerName && it.first != nonStrikerName }; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val r = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = r; val b = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = b } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E3030))) { Text("WICKET") }
                                Button(onClick = { if (!awaitingNextBatter && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && wickets < 10 && (innings == 1 || runs < target)) { balls++; bowlerBalls++; strikerBalls++; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val r = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = r; val b = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = b } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("DOT BALL", color = Color.White) }
                            }
                        }
                        if (awaitingNextBatter && wickets < 10) {
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("WICKET FALLEN — SELECT NEXT BATTER", color = Green, fontWeight = FontWeight.Bold)
                                        PlayerPicker("Next batter", "", liveBattingPlayers.filter { it.first != strikerName && it.first != nonStrikerName }, true) { next -> strikerName = next; strikerRuns = 0; strikerBalls = 0; awaitingNextBatter = false }
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(onClick = { runs = 0; wickets = 0; balls = 0; extras = 0; innings = 1; firstInningsRuns = 0; firstInningsWickets = 0; firstInningsOvers = "0.0"; firstInningsTeam = ""; firstStrikerName = ""; firstStrikerRuns = 0; firstStrikerBalls = 0; firstNonStrikerName = ""; firstNonStrikerRuns = 0; firstNonStrikerBalls = 0; firstBowlerName = ""; firstBowlerRuns = 0; firstBowlerWickets = 0; firstBowlerBalls = 0; target = 0; strikerRuns = 0; strikerBalls = 0; nonStrikerRuns = 0; nonStrikerBalls = 0; bowlerRuns = 0; bowlerWickets = 0; bowlerBalls = 0; started = false }, modifier = Modifier.fillMaxWidth()) { Text("RESET INNINGS") }
                        }
                    }
                    item {
                        if (innings == 1 && (balls >= (oversLimit.toIntOrNull() ?: 20) * 6 || wickets >= 10)) {
                            Button(onClick = {
                                firstInningsRuns = runs
                                firstInningsWickets = wickets
                                firstInningsOvers = overs
                                firstInningsTeam = team
                                firstStrikerName = strikerName
                                firstStrikerRuns = strikerRuns
                                firstStrikerBalls = strikerBalls
                                firstNonStrikerName = nonStrikerName
                                firstNonStrikerRuns = nonStrikerRuns
                                firstNonStrikerBalls = nonStrikerBalls
                                firstBowlerName = bowlerName
                                firstBowlerRuns = bowlerRuns
                                firstBowlerWickets = bowlerWickets
                                firstBowlerBalls = bowlerBalls
                                firstInningsBowlerFigures = (inningsBowlerFigures + if (bowlerName.isNotBlank()) listOf("$bowlerName: ${bowlerBalls / 6}.${bowlerBalls % 6} overs, $bowlerRuns runs, $bowlerWickets wickets") else emptyList()).joinToString("\\n")
                                inningsBowlerFigures.clear()
                                overNeedsBowler = false
                                target = runs + 1
                                val oldTeam = team
                                team = opponentTeam
                                opponentTeam = oldTeam
                                innings = 2
                                runs = 0; wickets = 0; balls = 0; extras = 0
                                strikerRuns = 0; strikerBalls = 0; nonStrikerRuns = 0; nonStrikerBalls = 0
                                bowlerRuns = 0; bowlerWickets = 0; bowlerBalls = 0
                                started = false; matchSaved = false
                            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                Text("END 1ST INNINGS • START CHASE", color = Dark, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (innings == 2 && runs >= target) {
                            InfoCard("MATCH RESULT", "$team won by ${10 - wickets} wickets")
                        }
                        if (innings == 2 && (runs >= target || balls >= (oversLimit.toIntOrNull() ?: 20) * 6 || wickets >= 10)) {
                            InfoCard("INNINGS COMPLETE", if (runs >= target) "$team won the match" else if (runs == firstInningsRuns) "Match tied" else "$firstInningsTeam won by ${firstInningsRuns - runs} runs")
                        }
                        if (balls > 0) {
                            Button(onClick = {
                                if (!matchSaved) {
                                    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                    val result = if (innings == 2 && runs >= target) "$team won by ${10 - wickets} wickets" else if (innings == 2 && runs < target && (wickets >= 10 || balls >= (oversLimit.toIntOrNull() ?: 20) * 6)) "$firstInningsTeam won by ${firstInningsRuns - runs} runs" else "Innings in progress"
                                    val scoreText = if (innings == 2) "$firstInningsRuns/$firstInningsWickets ($firstInningsOvers) & $runs/$wickets ($overs) • $result" else "$runs/$wickets"
                                    val currentBowlerLines = inningsBowlerFigures.toList() + if (bowlerName.isNotBlank()) listOf("$bowlerName: ${bowlerBalls / 6}.${bowlerBalls % 6} overs, $bowlerRuns runs, $bowlerWickets wickets") else emptyList()
                                    val detailsText = "INNINGS 1: $firstInningsTeam $firstInningsRuns/$firstInningsWickets ($firstInningsOvers)\\nBATTERS\\n$firstStrikerName: $firstStrikerRuns runs off $firstStrikerBalls balls\\n$firstNonStrikerName: $firstNonStrikerRuns runs off $firstNonStrikerBalls balls\\nBOWLERS\\n$firstInningsBowlerFigures\\nINNINGS 2: $team $runs/$wickets ($overs)\\nBATTERS\\n$strikerName: $strikerRuns runs off $strikerBalls balls\\n$nonStrikerName: $nonStrikerRuns runs off $nonStrikerBalls balls\\nBOWLERS\\n${currentBowlerLines.joinToString("\\n")}\\nLAST WICKET: ${lastDismissal.ifBlank { "Not recorded" }}\\nBALL-BY-BALL\n${ballByBallLog.joinToString("\\n")}\nEXTRAS: $extras"
                                    val record = MatchRecord(date, firstInningsTeam.ifBlank { team }, opponentTeam, scoreText, overs, "$tossWinner chose to $tossDecision", detailsText)
                                    matchHistory.add(0, record)
                                    val battingFigures = mutableListOf<Triple<String, Int, Int>>()
                                    if (innings == 2) {
                                        battingFigures.add(Triple(firstStrikerName, firstStrikerRuns, firstStrikerBalls))
                                        battingFigures.add(Triple(firstNonStrikerName, firstNonStrikerRuns, firstNonStrikerBalls))
                                    }
                                    battingFigures.add(Triple(strikerName, strikerRuns, strikerBalls))
                                    battingFigures.add(Triple(nonStrikerName, nonStrikerRuns, nonStrikerBalls))
                                    val matchPlayerNames = mutableSetOf<String>()
                                    battingFigures.filter { it.first.isNotBlank() }.groupBy { it.first.lowercase(Locale.ROOT) }.forEach { (_, entries) ->
                                        val name = entries.first().first
                                        val index = careerStats.indexOfFirst { it.name.equals(name, true) }
                                        val role = players.firstOrNull { it.first.equals(name, true) }?.second ?: "Player"
                                        val previous = if (index >= 0) careerStats.removeAt(index) else PlayerCareer(name, role)
                                        careerStats.add(PlayerCareer(name, role, previous.matches + 1, previous.runs + entries.sumOf { it.second }, previous.balls + entries.sumOf { it.third }, previous.wickets, previous.bowlingRuns, previous.bowlingBalls))
                                        matchPlayerNames.add(name.lowercase(Locale.ROOT))
                                    }
                                    val bowlingFigures = mutableListOf<Triple<String, Int, Pair<Int, Int>>>()
                                    if (innings == 2) bowlingFigures.add(Triple(firstBowlerName, firstBowlerRuns, firstBowlerWickets to firstBowlerBalls))
                                    bowlingFigures.add(Triple(bowlerName, bowlerRuns, bowlerWickets to bowlerBalls))
                                    bowlingFigures.filter { it.first.isNotBlank() }.groupBy { it.first.lowercase(Locale.ROOT) }.forEach { (_, entries) ->
                                        val name = entries.first().first
                                        val index = careerStats.indexOfFirst { it.name.equals(name, true) }
                                        val role = players.firstOrNull { it.first.equals(name, true) }?.second ?: "Bowler"
                                        val previous = if (index >= 0) careerStats.removeAt(index) else PlayerCareer(name, role)
                                        val bowlingWicketsAdded = entries.sumOf { it.third.first }
                                        val bowlingBallsAdded = entries.sumOf { it.third.second }
                                        careerStats.add(PlayerCareer(name, role, previous.matches + if (matchPlayerNames.add(name.lowercase(Locale.ROOT))) 1 else 0, previous.runs, previous.balls, previous.wickets + bowlingWicketsAdded, previous.bowlingRuns + entries.sumOf { it.second }, previous.bowlingBalls + bowlingBallsAdded))
                                    }
                                    val json = JSONArray()
                                    matchHistory.forEach { m -> json.put(JSONObject().put("date", m.date).put("team", m.team).put("opponent", m.opponent).put("score", m.score).put("overs", m.overs).put("toss", m.toss).put("details", m.details)) }
                                    preferences.edit().putString("matchHistory", json.toString()).apply()
                                    matchSaved = true
                                    started = false
                                }
                            }, enabled = !matchSaved, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                Text(if (matchSaved) "MATCH SAVED" else "SAVE MATCH TO HISTORY", color = Dark, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(onClick = { runs = 0; wickets = 0; balls = 0; extras = 0; innings = 1; firstInningsRuns = 0; firstInningsWickets = 0; firstInningsOvers = "0.0"; firstInningsTeam = ""; firstStrikerName = ""; firstStrikerRuns = 0; firstStrikerBalls = 0; firstNonStrikerName = ""; firstNonStrikerRuns = 0; firstNonStrikerBalls = 0; firstBowlerName = ""; firstBowlerRuns = 0; firstBowlerWickets = 0; firstBowlerBalls = 0; target = 0; strikerRuns = 0; strikerBalls = 0; nonStrikerRuns = 0; nonStrikerBalls = 0; bowlerRuns = 0; bowlerWickets = 0; bowlerBalls = 0; inningsBowlerFigures.clear(); firstInningsBowlerFigures = ""; overNeedsBowler = false; started = false; matchSaved = false }, modifier = Modifier.fillMaxWidth()) { Text("NEW MATCH / RESET") }
                    }
                    item { Text("Match history aur player career records isi device par save hote hain. App data clear ya app uninstall karne se pehle backup ka dhyan rakhein.", color = Color.LightGray, fontSize = 12.sp) }
                }
                "Players" -> {
                    item { InfoCard("YOUR PERMANENT XI", "Yahan sirf apni team ke 11 players add karein. Inhi ke profiles, career stats aur rankings permanently save honge. Opponent XI match setup mein alag enter hoti hai.") }
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
                        item { InfoCard("EMPTY SQUAD", "Add your first player above. Your permanent squad can contain up to 11 players.") }
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
                    item { Text("Squad members are saved on this device and remain available when you reopen the app.", color = Color.LightGray, fontSize = 11.sp) }
                }
                "History" -> {
                    val openedMatch = selectedHistoryMatch
                    if (openedMatch != null) {
                        item {
                            TextButton(onClick = { selectedHistoryMatch = null }) { Text("← BACK TO MATCH HISTORY", color = Green) }
                            Text(openedMatch.team + " vs " + openedMatch.opponent, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text(openedMatch.date + "  •  " + openedMatch.toss, color = Color.LightGray, fontSize = 11.sp)
                            Text(openedMatch.score, color = Green, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Text("Overs: " + openedMatch.overs, color = Color.LightGray, fontSize = 12.sp)
                            HorizontalDivider(color = Green)
                            MatchScorecardDetails(openedMatch.details)
                        }
                    } else {
                        item { InfoCard("MATCH HISTORY", "Kisi match par tap karein aur uska complete available scorecard dekhein.") }
                        if (matchHistory.isEmpty()) item { InfoCard("NO SAVED MATCHES", "Scoring ke baad Save Match karein. Scorecard yahan save hoga.") }
                        items(matchHistory.size) { index ->
                            val match = matchHistory[index]
                            Card(onClick = { selectedHistoryMatch = match }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(match.date, color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            matchHistory.removeAt(index)
                                            val json = JSONArray()
                                            matchHistory.forEach { m -> json.put(JSONObject().put("date", m.date).put("team", m.team).put("opponent", m.opponent).put("score", m.score).put("overs", m.overs).put("toss", m.toss).put("details", m.details)) }
                                            preferences.edit().putString("matchHistory", json.toString()).apply()
                                        }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete scorecard", tint = Color(0xFFE57373)) }
                                    }
                                    Text(match.team + " vs " + match.opponent, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(match.team + "  " + match.score, color = Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    Text("Overs: " + match.overs + "  |  Tap to view scorecard", color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                "Stats" -> {
                    item { InfoCard("PLAYER PERFORMANCE CENTRE", "Har player ka career record yahan dekhein. Stats saved match records se calculate hote hain.") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile("PLAYERS", players.size.toString(), Modifier.weight(1f))
                            StatTile("MATCHES", matchHistory.size.toString(), Modifier.weight(1f))
                            StatTile("TRACKED", careerStats.size.toString(), Modifier.weight(1f))
                        }
                    }
                    val profiles = (players.map { it.first } + careerStats.map { it.name }).distinctBy { it.lowercase(Locale.ROOT) }
                    if (profiles.isEmpty()) item { InfoCard("NO PLAYER DATA", "Players section mein squad add karein aur match score karein.") }
                    profiles.forEach { name ->
                        val p = careerStats.firstOrNull { it.name.equals(name, true) } ?: PlayerCareer(name, players.firstOrNull { it.first.equals(name, true) }?.second ?: "Player")
                        val average = if (p.matches > 0 && p.runs > 0) p.runs.toDouble() / p.matches else 0.0
                        val strikeRate = if (p.balls > 0) p.runs * 100.0 / p.balls else 0.0
                        val economy = if (p.bowlingBalls > 0) p.bowlingRuns * 6.0 / p.bowlingBalls else 0.0
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null, tint = Green)
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                            Text(p.role, color = Color.LightGray, fontSize = 11.sp)
                                        }
                                        Text("#${careerStats.sortedByDescending { rankingPoints(it) }.indexOfFirst { it.name.equals(name, true) }.let { if (it < 0) "—" else it + 1 }}", color = Green, fontWeight = FontWeight.Black)
                                    }
                                    HorizontalDivider(color = Color.DarkGray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        StatTile("MATCHES", p.matches.toString(), Modifier.weight(1f))
                                        StatTile("RUNS", p.runs.toString(), Modifier.weight(1f))
                                        StatTile("WICKETS", p.wickets.toString(), Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        StatTile("AVERAGE*", String.format(Locale.getDefault(), "%.2f", average), Modifier.weight(1f))
                                        StatTile("STRIKE RATE", String.format(Locale.getDefault(), "%.2f", strikeRate), Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        StatTile("BOWLING", "${p.bowlingBalls / 6}.${p.bowlingBalls % 6} ov", Modifier.weight(1f))
                                        StatTile("ECONOMY", if (p.bowlingBalls > 0) String.format(Locale.getDefault(), "%.2f", economy) else "—", Modifier.weight(1f))
                                    }
                                    Text("Bowling runs: ${p.bowlingRuns} • Ranking points: ${rankingPoints(p)}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("Wins, fours, sixes, catches aur dismissals abhi scorer mein record nahi hote; inhein numbers ki shakal mein show nahi kiya ja raha.", color = Color(0xFFFFD180), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    item { Text("*Average filhaal runs per recorded appearance hai; official batting average ke liye dismissals ka record zaroori hai.", color = Color.Gray, fontSize = 10.sp) }
                }
                "Rankings" -> {
                    val topBatters = careerStats.filter { it.balls > 0 || it.runs > 0 }.sortedWith(compareByDescending<PlayerCareer> { it.runs * 100.0 / (if (it.balls == 0) 1 else it.balls) }.thenByDescending { it.runs })
                    val topBowlers = careerStats.filter { it.bowlingBalls > 0 || it.wickets > 0 }.sortedWith(compareByDescending<PlayerCareer> { it.wickets }.thenBy { if (it.bowlingBalls == 0) Double.MAX_VALUE else it.bowlingRuns * 6.0 / it.bowlingBalls }.thenByDescending { it.bowlingBalls })
                    item { InfoCard("SEPARATE LEADERBOARDS", "Batting aur bowling ki rankings alag criteria par calculate hoti hain.") }
                    item {
                        Text("🏏  BATTER RANKINGS", color = Green, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Rank  •  Runs  •  Balls  •  Strike Rate", color = Color.LightGray, fontSize = 11.sp)
                    }
                    if (topBatters.isEmpty()) item { InfoCard("NO BATTING RECORDS", "Batting figures save hone par batters yahan nazar aayenge.") }
                    topBatters.forEachIndexed { index, p ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${index + 1}", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${p.runs} runs  •  ${p.balls} balls", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Text("SR ${if (p.balls == 0) "0.00" else String.format(Locale.getDefault(), "%.2f", p.runs * 100.0 / p.balls)}", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("🎯  BOWLER RANKINGS", color = Green, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Rank  •  Wickets  •  Runs conceded  •  Economy", color = Color.LightGray, fontSize = 11.sp)
                    }
                    if (topBowlers.isEmpty()) item { InfoCard("NO BOWLING RECORDS", "Bowling figures save hone par bowlers yahan nazar aayenge.") }
                    topBowlers.forEachIndexed { index, p ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${index + 1}", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${p.wickets} wickets  •  ${p.bowlingRuns} runs  •  ${p.bowlingBalls / 6}.${p.bowlingBalls % 6} overs", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Text("ECO ${if (p.bowlingBalls == 0) "—" else String.format(Locale.getDefault(), "%.2f", p.bowlingRuns * 6.0 / p.bowlingBalls)}", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                else -> {
                    item { ABHomeDashboard(team, opponentTeam, runs, wickets, overs, started, careerStats, { selected = "Matches" }, { selected = "Players" }, { selected = "Rankings" }, { selected = "History" }, { selected = "Players" }) }
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
fun MatchScorecardDetails(details: String) {
    if (details.isBlank()) {
        InfoCard("DETAILED SCORECARD UNAVAILABLE", "Is saved match mein detailed player figures record nahi hue thay.")
        return
    }
    val lines = details.split("\\n")
    val inningsHeaders = lines.filter { it.startsWith("INNINGS ") }
    inningsHeaders.forEach { header ->
        val title = header.substringBefore(":")
        val summary = header.substringAfter(":").trim()
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101C16)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title + "  •  INNINGS SCORECARD", color = Green, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(summary, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                val start = lines.indexOf(header)
                val end = lines.indexOfFirstFrom(start + 1) { it.startsWith("INNINGS ") }.let { if (it < 0) lines.size else it }
                val section = lines.subList(start + 1, end)
                val batterLines = section.dropWhile { it != "BATTERS" }.drop(1).takeWhile { it != "BOWLERS" }
                val bowlerLines = section.dropWhile { it != "BOWLERS" }.drop(1)
                if (batterLines.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF34483B))
                    Text("BATTING", color = Green, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Batter", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text("R", color = Color.LightGray, fontSize = 10.sp)
                        Text("B", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(35.dp))
                        Text("SR", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(48.dp))
                    }
                    batterLines.forEach { line ->
                        val name = line.substringBefore(":")
                        val runs = Regex("(\\d+) runs off (\\d+) balls").find(line)
                        val r = runs?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val b = runs?.groupValues?.get(2)?.toIntOrNull() ?: 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("$r", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("$b", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(35.dp))
                            Text(if (b == 0) "—" else String.format(Locale.getDefault(), "%.1f", r * 100.0 / b), color = Green, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                        }
                    }
                }
                if (bowlerLines.isNotEmpty()) {
                    HorizontalDivider(color = Color.DarkGray)
                    Text("BOWLING", color = Green, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bowler", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text("O", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(35.dp))
                        Text("R", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(30.dp))
                        Text("W", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(25.dp))
                        Text("ECO", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(45.dp))
                    }
                    bowlerLines.filter { it.contains(":") }.forEach { line ->
                        val name = line.substringBefore(":")
                        val figures = Regex("(\\d+)\\.(\\d+) overs, (\\d+) runs, (\\d+) wickets").find(line)
                        val balls = (figures?.groupValues?.get(1)?.toIntOrNull() ?: 0) * 6 + (figures?.groupValues?.get(2)?.toIntOrNull() ?: 0)
                        val conceded = figures?.groupValues?.get(3)?.toIntOrNull() ?: 0
                        val wkts = figures?.groupValues?.get(4)?.toIntOrNull() ?: 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("${balls / 6}.${balls % 6}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(35.dp))
                            Text("$conceded", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                            Text("$wkts", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(25.dp))
                            Text(if (balls == 0) "—" else String.format(Locale.getDefault(), "%.2f", conceded * 6.0 / balls), color = Green, fontSize = 11.sp, modifier = Modifier.width(45.dp))
                        }
                    }
                }
            }
        }
    }
}

private inline fun <T> List<T>.indexOfFirstFrom(startIndex: Int, predicate: (T) -> Boolean): Int {
    for (i in startIndex until size) if (predicate(this[i])) return i
    return -1
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1711)), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.LightGray,
    focusedContainerColor = Color(0xFF050805),
    unfocusedContainerColor = Color(0xFF050805),
    disabledContainerColor = Color(0xFF101510),
    focusedBorderColor = Green,
    unfocusedBorderColor = Color(0xFF607067),
    disabledBorderColor = Color.DarkGray,
    focusedLabelColor = Green,
    unfocusedLabelColor = Color(0xFFB8C8BE),
    disabledLabelColor = Color.Gray,
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

@Composable
fun PlayerPicker(label: String, selectedPlayer: String, players: List<Pair<String, String>>, enabled: Boolean, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && players.isNotEmpty(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
            Text(label + ": " + selectedPlayer.ifBlank { if (players.isEmpty()) "Add players first" else "Select player" })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            players.forEach { (name, role) ->
                DropdownMenuItem(text = { Text(name + " • " + role) }, onClick = { onSelected(name); expanded = false })
            }
        }
    }
}

fun rankingPoints(p: PlayerCareer): Int {
    val strikeRate = if (p.balls > 0) p.runs * 100.0 / p.balls else 0.0
    val overs = p.bowlingBalls / 6.0
    val economy = if (overs > 0) p.bowlingRuns / overs else 0.0
    val battingBonus = (strikeRate / 10.0).toInt().coerceAtMost(20)
    val bowlingBonus = if (overs > 0) (10.0 - economy).coerceAtLeast(0.0).toInt().coerceAtMost(10) else 0
    return p.runs + p.wickets * 20 + battingBonus + bowlingBonus
}

@Composable
fun ABHomeDashboard(team:String, opponent:String, runs:Int, wickets:Int, overs:String, started:Boolean, careerStats:List<PlayerCareer>, onMatches:()->Unit, onPlayers:()->Unit, onRankings:()->Unit, onHistory:()->Unit, onAccount:()->Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(17.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column { Text("AB CRICKET", color=Color.White, fontSize=28.sp, fontWeight=FontWeight.Black); Text("PLAY • COMPETE • WIN", color=Green, fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp) }
            Spacer(Modifier.weight(1f))

        }
        Card(shape=RoundedCornerShape(24.dp), modifier=Modifier.fillMaxWidth()) {
            Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF075B36),Color(0xFF102B43),Color(0xFF07130E)))).padding(20.dp)) {
                Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text("YOUR CRICKET UNIVERSE", color=Color(0xFF8DFFB5), fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
                    Text("Every run tells\na story.", color=Color.White, fontSize=31.sp, fontWeight=FontWeight.Black, lineHeight=35.sp)
                    Text("Start a match. Track every performance. Climb the rankings.", color=Color.White.copy(alpha=.85f), fontSize=12.sp)
                    Button(onClick=onMatches, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF55E879)), shape=RoundedCornerShape(50)) { Text("LET'S PLAY  →", color=Dark, fontWeight=FontWeight.Black) }
                    Text("🏏", fontSize=48.sp, modifier=Modifier.align(Alignment.End))
                }
            }
        }
        Text("QUICK ACCESS", color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            QuickTile("Start Match",Icons.Default.Add,Modifier.weight(1f)){onMatches()}
            QuickTile("Players",Icons.Default.People,Modifier.weight(1f)){onPlayers()}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            QuickTile("Rankings",Icons.Default.EmojiEvents,Modifier.weight(1f)){onRankings()}
            QuickTile("History",Icons.Default.History,Modifier.weight(1f)){onHistory()}
        }
        Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(22.dp), modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(17.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("🔥  MATCH CENTRE", color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp)
                    Spacer(Modifier.weight(1f)); Text(if(started) "● LIVE" else "READY", color=Green, fontWeight=FontWeight.Bold, fontSize=10.sp)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment=Alignment.CenterHorizontally) { Text("🏏",fontSize=25.sp); Text(team,color=Color.White,fontWeight=FontWeight.Bold,maxLines=1,fontSize=12.sp) }
                    Text("VS",color=Green,fontWeight=FontWeight.Black,fontSize=20.sp)
                    Column(Modifier.weight(1f), horizontalAlignment=Alignment.CenterHorizontally) { Text("🏆",fontSize=25.sp); Text(opponent,color=Color.White,fontWeight=FontWeight.Bold,maxLines=1,fontSize=12.sp) }
                }
                HorizontalDivider(color=Color.DarkGray)
                Text("$runs/$wickets",color=Color.White,fontSize=35.sp,fontWeight=FontWeight.Black)
                Text("Overs: $overs",color=Color.LightGray,fontSize=12.sp)
                OutlinedButton(onClick=onMatches,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(50)){Text(if(started)"OPEN LIVE MATCH" else "CREATE A MATCH",color=Green,fontWeight=FontWeight.Bold)}
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("⭐  TOP RANKINGS",color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)
            Spacer(Modifier.weight(1f)); TextButton(onClick=onRankings){Text("VIEW ALL",color=Green)}
        }
        val top=careerStats.sortedByDescending{rankingPoints(it)}.take(3)
        if(top.isEmpty()) InfoCard("YOUR LEADERBOARD STARTS HERE","Save your first match to see top run scorers and bowling leaders.")
        else top.forEachIndexed{ i,p ->
            Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("#${i+1}",color=Green,fontSize=22.sp,fontWeight=FontWeight.Black); Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)){Text(p.name,color=Color.White,fontWeight=FontWeight.Bold);Text("${p.role} • ${p.runs} runs • ${p.wickets} wickets • ${rankingPoints(p)} pts",color=Color.LightGray,fontSize=11.sp)}
                    Icon(Icons.Default.EmojiEvents,null,tint=Color(0xFFFFC857))
                }
            }
        }
        OutlinedButton(onClick=onHistory,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.History,null,tint=Green);Spacer(Modifier.width(8.dp));Text("VIEW MATCH HISTORY",color=Green)}
        Text("ESTABLISHED BY • SYED ABDUL BASIT",color=Color.Gray,fontSize=10.sp,letterSpacing=1.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
    }
}
