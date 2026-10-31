package com.abcricket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF19B56B)
private val Dark = Color(0xFF08110D)
private val Panel = Color(0xFF14221B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ABTheme { CricketHome() } }
    }
}

@Composable
fun ABTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Dark, surface = Panel), content = content)
}

@Composable
fun CricketHome() {
    var selected by remember { mutableStateOf("Home") }
    Scaffold(
        containerColor = Dark,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Matches", "Players", "Rankings").forEach { item ->
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = { Icon(
                            when(item) {
                                "Matches" -> Icons.Default.SportsCricket
                                "Players" -> Icons.Default.People
                                "Rankings" -> Icons.Default.EmojiEvents
                                else -> Icons.Default.Home
                            }, contentDescription = item
                        ) },
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
            item { LiveMatchCard() }
            item {
                Text("QUICK ACCESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("New Match", Icons.Default.Add, Modifier.weight(1f))
                    QuickTile("Scorecards", Icons.Default.Assessment, Modifier.weight(1f))
                    QuickTile("Tournament", Icons.Default.EmojiEvents, Modifier.weight(1f))
                }
            }
            item {
                Text("PLAYER STATS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Matches", "0", Modifier.weight(1f))
                    StatCard("Runs", "0", Modifier.weight(1f))
                    StatCard("Wickets", "0", Modifier.weight(1f))
                }
            }
            item {
                Text("RECENT ACTIVITY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Text("No matches yet. Create your first match to get started.", color = Color.LightGray, modifier = Modifier.padding(18.dp), fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun LiveMatchCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Green.copy(alpha = .18f), shape = RoundedCornerShape(6.dp)) {
                    Text("  LIVE CENTER  ", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("NO LIVE MATCH", color = Color.LightGray, fontSize = 10.sp)
            }
            Text("Match scoreboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Your live scores will appear here when a match is started.", color = Color.LightGray, fontSize = 13.sp)
            HorizontalDivider(color = Color.DarkGray)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("TEAM A   0/0", color = Color.White, fontWeight = FontWeight.Bold)
                Text("TEAM B   0/0", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(92.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = Green)
            Spacer(Modifier.height(7.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, color = Green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}
