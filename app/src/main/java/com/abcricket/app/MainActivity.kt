package com.abcricket.app

import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF19B56B)
private val Dark = Color(0xFF08110D)
private val Panel = Color(0xFF14221B)

private data class MatchRecord(val date: String, val team: String, val opponent: String, val score: String, val overs: String, val toss: String)

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
    var extras by remember { mutableIntStateOf(0) }
    var innings by remember { mutableIntStateOf(1) }
    var firstInningsRuns by remember { mutableIntStateOf(0) }
    var firstInningsWickets by remember { mutableIntStateOf(0) }
    var firstInningsOvers by remember { mutableStateOf("0.0") }
    var firstInningsTeam by remember { mutableStateOf("") }
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
    var strikerRuns by remember { mutableIntStateOf(0) }
    var strikerBalls by remember { mutableIntStateOf(0) }
    var nonStrikerRuns by remember { mutableIntStateOf(0) }
    var nonStrikerBalls by remember { mutableIntStateOf(0) }
    var bowlerRuns by remember { mutableIntStateOf(0) }
    var bowlerWickets by remember { mutableIntStateOf(0) }
    var bowlerBalls by remember { mutableIntStateOf(0) }
    var playerName by remember { mutableStateOf("") }
    var playerRole by remember { mutableStateOf("Batsman") }
    var matchSaved by remember { mutableStateOf(false) }
    val matchHistory = remember { mutableStateListOf<MatchRecord>() }
    val players = remember { mutableStateListOf<Pair<String, String>>() }
    val context = LocalContext.current
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
            matchHistory.add(MatchRecord(item.optString("date"), item.optString("team"), item.optString("opponent"), item.optString("score"), item.optString("overs"), item.optString("toss")))
        }
        val savedPlayers = JSONArray(preferences.getString("players", "[]"))
        for (i in 0 until savedPlayers.length()) {
            val item = savedPlayers.getJSONObject(i)
            players.add(item.optString("name") to item.optString("role"))
        }
    }
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
    LaunchedEffect(matchSaved) { preferences.edit().putBoolean("matchSaved", matchSaved).apply() }
    LaunchedEffect(strikerRuns, strikerBalls, nonStrikerRuns, nonStrikerBalls, bowlerRuns, bowlerWickets, bowlerBalls) {
        preferences.edit().putInt("strikerRuns", strikerRuns).putInt("strikerBalls", strikerBalls)
            .putInt("nonStrikerRuns", nonStrikerRuns).putInt("nonStrikerBalls", nonStrikerBalls)
            .putInt("bowlerRuns", bowlerRuns).putInt("bowlerWickets", bowlerWickets).putInt("bowlerBalls", bowlerBalls).apply()
    }

    Scaffold(
        containerColor = Dark,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Matches", "Players", "Rankings", "History").forEach { item ->
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = { Icon(when(item) {
                            "Matches" -> Icons.Default.SportsCricket
                            "Players" -> Icons.Default.People
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
                        Text("MATCH SETUP", color = Green, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = team, onValueChange = { team = it }, label = { Text("Team 1 / batting team") }, singleLine = true, enabled = !started, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = opponentTeam, onValueChange = { opponentTeam = it }, label = { Text("Team 2 / opponent") }, singleLine = true, enabled = !started, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
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
                        PlayerPicker("Striker", strikerName, players, !started) { strikerName = it }
                        PlayerPicker("Non-striker", nonStrikerName, players, !started) { nonStrikerName = it }
                        PlayerPicker("Opening bowler", bowlerName, players, !started) { bowlerName = it }
                        Text("Toss: " + tossWinner + " elected to " + tossDecision + " | " + (oversLimit.ifBlank { "20" }) + " overs", color = Color.LightGray, fontSize = 12.sp)
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
                    item { ScoreCard(team, runs, wickets, overs, started) }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("LIVE PLAYER FIGURES", color = Green, fontWeight = FontWeight.Bold)
                                Text("Extras: $extras", color = Color.LightGray, fontSize = 12.sp)
                                Text((strikerName.ifBlank { "Striker" }) + " (on strike): $strikerRuns runs off $strikerBalls balls", color = Color.White, fontSize = 13.sp)
                                Text((nonStrikerName.ifBlank { "Non-striker" }) + ": $nonStrikerRuns runs off $nonStrikerBalls balls", color = Color.White, fontSize = 13.sp)
                                Text((bowlerName.ifBlank { "Bowler" }) + ": ${bowlerBalls / 6}.${bowlerBalls % 6} overs, $bowlerRuns runs, $bowlerWickets wickets", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Button(onClick = { started = !started }, enabled = started || (team.isNotBlank() && opponentTeam.isNotBlank() && (oversLimit.toIntOrNull() ?: 0) > 0), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text(if (started) "PAUSE INNINGS" else "START INNINGS", color = Dark, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (started) {
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
                                Button(onClick = { if (wickets < 10 && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && (innings == 1 || runs < target)) { wickets++; balls++; bowlerBalls++; bowlerWickets++; strikerBalls++; awaitingNextBatter = wickets < 10; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val r = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = r; val b = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = b } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E3030))) { Text("WICKET") }
                                Button(onClick = { if (!awaitingNextBatter && balls < (oversLimit.toIntOrNull() ?: 20) * 6 && wickets < 10 && (innings == 1 || runs < target)) { balls++; bowlerBalls++; strikerBalls++; if (balls % 6 == 0) { val old = strikerName; strikerName = nonStrikerName; nonStrikerName = old; val r = strikerRuns; strikerRuns = nonStrikerRuns; nonStrikerRuns = r; val b = strikerBalls; strikerBalls = nonStrikerBalls; nonStrikerBalls = b } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Panel)) { Text("DOT BALL", color = Color.White) }
                            }
                        }
                        if (awaitingNextBatter && wickets < 10) {
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("WICKET FALLEN — SELECT NEXT BATTER", color = Green, fontWeight = FontWeight.Bold)
                                        PlayerPicker("Next batter", "", players.filter { it.first != strikerName && it.first != nonStrikerName }, true) { next -> strikerName = next; strikerRuns = 0; strikerBalls = 0; awaitingNextBatter = false }
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(onClick = { runs = 0; wickets = 0; balls = 0; extras = 0; innings = 1; firstInningsRuns = 0; firstInningsWickets = 0; firstInningsOvers = "0.0"; firstInningsTeam = ""; target = 0; strikerRuns = 0; strikerBalls = 0; nonStrikerRuns = 0; nonStrikerBalls = 0; bowlerRuns = 0; bowlerWickets = 0; bowlerBalls = 0; started = false }, modifier = Modifier.fillMaxWidth()) { Text("RESET INNINGS") }
                        }
                    }
                    item {
                        if (innings == 1 && (balls >= (oversLimit.toIntOrNull() ?: 20) * 6 || wickets >= 10)) {
                            Button(onClick = {
                                firstInningsRuns = runs
                                firstInningsWickets = wickets
                                firstInningsOvers = overs
                                firstInningsTeam = team
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
                                    val record = MatchRecord(date, firstInningsTeam.ifBlank { team }, opponentTeam, scoreText, overs, "$tossWinner chose to $tossDecision")
                                    matchHistory.add(0, record)
                                    val json = JSONArray()
                                    matchHistory.forEach { m -> json.put(JSONObject().put("date", m.date).put("team", m.team).put("opponent", m.opponent).put("score", m.score).put("overs", m.overs).put("toss", m.toss)) }
                                    preferences.edit().putString("matchHistory", json.toString()).apply()
                                    matchSaved = true
                                    started = false
                                }
                            }, enabled = !matchSaved, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                Text(if (matchSaved) "MATCH SAVED" else "SAVE MATCH TO HISTORY", color = Dark, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(onClick = { runs = 0; wickets = 0; balls = 0; extras = 0; innings = 1; firstInningsRuns = 0; firstInningsWickets = 0; firstInningsOvers = "0.0"; firstInningsTeam = ""; target = 0; strikerRuns = 0; strikerBalls = 0; nonStrikerRuns = 0; nonStrikerBalls = 0; bowlerRuns = 0; bowlerWickets = 0; bowlerBalls = 0; started = false; matchSaved = false }, modifier = Modifier.fillMaxWidth()) { Text("NEW MATCH / RESET") }
                    }
                    item { Text("Saved scorecards are stored on this device. Detailed player-by-player statistics and cloud sync are future upgrades.", color = Color.LightGray, fontSize = 12.sp) }
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
                "History" -> {
                    item { InfoCard("MATCH HISTORY", "Your saved match scorecards appear here.") }
                    if (matchHistory.isEmpty()) {
                        item { InfoCard("NO SAVED MATCHES", "Complete some scoring and tap Save Match to keep its scorecard here.") }
                    } else {
                        items(matchHistory.size) { index ->
                            val match = matchHistory[index]
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(match.date, color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            matchHistory.removeAt(index)
                                            val json = JSONArray()
                                            matchHistory.forEach { m -> json.put(JSONObject().put("date", m.date).put("team", m.team).put("opponent", m.opponent).put("score", m.score).put("overs", m.overs).put("toss", m.toss)) }
                                            preferences.edit().putString("matchHistory", json.toString()).apply()
                                        }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete scorecard", tint = Color(0xFFE57373)) }
                                    }
                                    Text(match.team + " vs " + match.opponent, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(match.team + "  " + match.score, color = Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    Text("Overs: " + match.overs + "  |  " + match.toss, color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
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
                        InfoCard("Match history", "Open History to view saved scorecards")
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
