package com.abcricket.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val RoomGreen = Color(0xFF19B56B)
private val RoomPanel = Color(0xFF14221B)

private const val SUPABASE_URL = "https://wcpzmaqwxvvowkdgapsj.supabase.co"
private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_epu8W3eriwDPTVm7TLvoOg_87fuLBQx"

data class RoomPlayer(val id: String, val name: String, val role: String)

data class CricketRoom(
    val id: String,
    val name: String,
    val description: String,
    val inviteCode: String,
    val role: String
)

object SupabaseRoomsApi {
    private fun request(path: String, method: String, token: String? = null, body: JSONObject? = null, prefer: String? = null): String {
        val connection = (URL("$SUPABASE_URL$path").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
            }
            if (prefer != null) connection.setRequestProperty("Prefer", prefer)
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (status !in 200..299) {
                val message = try {
                    val json = JSONObject(response)
                    json.optString("msg", json.optString("message", json.optString("error_description", response)))
                } catch (_: Exception) { response.ifBlank { "Request failed ($status)" } }
                throw IllegalStateException(message)
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    suspend fun signIn(email: String, password: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(request("/auth/v1/token?grant_type=password", "POST",
            body = JSONObject().put("email", email).put("password", password)))
    }

    suspend fun signUp(email: String, password: String, displayName: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(request("/auth/v1/signup", "POST",
            body = JSONObject().put("email", email).put("password", password)
                .put("data", JSONObject().put("full_name", displayName))))
    }

    suspend fun rooms(token: String, userId: String): List<CricketRoom> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(userId, "UTF-8")
        val json = JSONArray(request(
            "/rest/v1/circle_members?select=role,cricket_circles(id,name,description,invite_code)&user_id=eq.$encoded&order=joined_at.desc",
            "GET", token
        ))
        buildList {
            for (i in 0 until json.length()) {
                val row = json.getJSONObject(i)
                val room = row.optJSONObject("cricket_circles") ?: continue
                add(CricketRoom(room.optString("id"), room.optString("name"),
                    room.optString("description"), room.optString("invite_code"), row.optString("role")))
            }
        }
    }

    suspend fun roomPlayers(token: String, circleId: String): List<RoomPlayer> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(circleId, "UTF-8")
        val json = JSONArray(request("/rest/v1/room_players?select=id,name,role&circle_id=eq.$encoded&is_active=eq.true&order=name.asc", "GET", token))
        buildList {
            for (i in 0 until json.length()) {
                val row = json.getJSONObject(i)
                add(RoomPlayer(row.optString("id"), row.optString("name"), row.optString("role")))
            }
        }
    }

    suspend fun addRoomPlayer(token: String, circleId: String, name: String, role: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("circle_id", circleId).put("name", name).put("role", role)
        request("/rest/v1/room_players", "POST", token, body, "return=minimal")
    }

    suspend fun createRoom(token: String, userId: String, name: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("p_name", name)
        request("/rest/v1/rpc/create_cricket_circle", "POST", token, body).trim().trim('"')
    }

    suspend fun joinRoom(token: String, code: String) = withContext(Dispatchers.IO) {
        request("/rest/v1/rpc/join_circle_by_code", "POST", token, JSONObject().put("p_code", code))
    }

    suspend fun saveMatch(token: String, circleId: String, userId: String, teamOne: String, teamTwo: String, oversLimit: Int, status: String, scorecard: JSONObject) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("circle_id", circleId).put("title", "$teamOne vs $teamTwo").put("team_one", teamOne).put("team_two", teamTwo).put("overs_limit", oversLimit.coerceIn(1, 100)).put("status", if (status == "live") "live" else "completed").put("scorecard", scorecard).put("created_by", userId)
        val response = request("/rest/v1/room_matches", "POST", token, body, "return=representation")
        val rows = JSONArray(response)
        if (rows.length() == 0) throw IllegalStateException("Supabase ne match ID return nahi ki.")
        rows.getJSONObject(0).optString("id")
    }}

@Composable
fun RoomsScreen(onRoomEntered: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("ab_cricket_rooms", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(prefs.getString("email", "") ?: "") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf(prefs.getString("access_token", "") ?: "") }
    var userId by remember { mutableStateOf(prefs.getString("user_id", "") ?: "") }
    var rooms by remember { mutableStateOf(emptyList<CricketRoom>()) }
    var selectedRoom by remember { mutableStateOf<CricketRoom?>(null) }
    var roomPlayers by remember { mutableStateOf(emptyList<RoomPlayer>()) }
    var newPlayerName by remember { mutableStateOf("") }
    var newPlayerRole by remember { mutableStateOf("Batsman") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    fun saveSession(auth: JSONObject) {
        accessToken = auth.optString("access_token")
        val user = auth.optJSONObject("user")
        userId = user?.optString("id").orEmpty()
        if (accessToken.isNotBlank() && userId.isNotBlank()) {
            prefs.edit().putString("access_token", accessToken).putString("user_id", userId)
                .putString("email", email).apply()
        }
    }

    fun loadRoomPlayers(room: CricketRoom) {
        scope.launch {
            busy = true
            try {
                roomPlayers = SupabaseRoomsApi.roomPlayers(accessToken, room.id)
                message = ""
            } catch (e: Exception) { message = e.message ?: "Players load nahi ho sake." }
            busy = false
        }
    }

    fun loadRooms() {
        if (accessToken.isBlank() || userId.isBlank()) return
        scope.launch {
            busy = true
            try {
                rooms = SupabaseRoomsApi.rooms(accessToken, userId)
                message = if (rooms.isEmpty()) "Abhi kisi room ke member nahi ho. Naya room banao ya invite code se join karo." else ""
            } catch (e: Exception) { message = e.message ?: "Rooms load nahi ho sake." }
            busy = false
        }
    }

    LaunchedEffect(accessToken, userId) { if (accessToken.isNotBlank() && userId.isNotBlank()) loadRooms() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("PRIVATE CRICKET ROOMS", color = RoomGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Har room ka apna squad, matches aur scorecards honge.", color = Color.LightGray, fontSize = 12.sp)
        if (accessToken.isBlank()) {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            if (isSignUp) OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = ""
                        try {
                            if (isSignUp) {
                                val result = SupabaseRoomsApi.signUp(email.trim(), password, displayName.trim())
                                if (result.has("access_token")) {
                                    saveSession(result)
                                    message = "Account ban gaya aur login ho gaya."
                                } else message = "Signup request ho gayi. Email confirmation required ho sakti hai; email verify karke login karo."
                            } else {
                                saveSession(SupabaseRoomsApi.signIn(email.trim(), password))
                                message = "Login successful."
                            }
                        } catch (e: Exception) { message = e.message ?: "Authentication failed." }
                        busy = false
                    }
                },
                enabled = !busy && email.contains("@") && password.length >= 6 && (!isSignUp || displayName.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "PLEASE WAIT..." else if (isSignUp) "CREATE ACCOUNT" else "LOGIN") }
            TextButton(onClick = { isSignUp = !isSignUp; message = "" }) {
                Text(if (isSignUp) "Already have an account? Login" else "New here? Create account", color = RoomGreen)
            }
        } else {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(email, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = {
                    prefs.edit().remove("access_token").remove("user_id").apply()
                    accessToken = ""; userId = ""; rooms = emptyList(); password = ""
                    message = "Logout ho gaya."
                }) { Text("LOG OUT", color = RoomGreen) }
            }
            HorizontalDivider(color = Color.DarkGray)
            Text("CREATE A ROOM", color = RoomGreen, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = roomName, onValueChange = { roomName = it }, label = { Text("Room name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            Button(onClick = {
                scope.launch {
                    busy = true
                    message = ""
                    try {
                        val createdRoomId = SupabaseRoomsApi.createRoom(accessToken, userId, roomName.trim())
                        val updatedRooms = SupabaseRoomsApi.rooms(accessToken, userId)
                        rooms = updatedRooms
                        val createdRoom = updatedRooms.firstOrNull { it.id == createdRoomId }
                        if (createdRoom != null) {
                            selectedRoom = createdRoom
                            prefs.edit()
                                .putString("selected_room_id", createdRoom.id)
                                .putString("selected_room_name", createdRoom.name)
                                .putString("selected_room_role", createdRoom.role)
                                .apply()
                            roomName = ""
                            onRoomEntered()
                        } else {
                            message = "Room ban gaya, lekin list mein load nahi hua. REFRESH dabayein."
                        }
                    } catch (e: Exception) { message = e.message ?: "Room create nahi hua." }
                    busy = false
                }
            }, enabled = !busy && roomName.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "PLEASE WAIT..." else "CREATE ROOM") }
            HorizontalDivider(color = Color.DarkGray)
            Text("JOIN WITH INVITE CODE", color = RoomGreen, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it.uppercase().take(20) }, label = { Text("Invite code") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            OutlinedButton(onClick = {
                scope.launch {
                    busy = true
                    try {
                        SupabaseRoomsApi.joinRoom(accessToken, inviteCode.trim())
                        inviteCode = ""; loadRooms(); message = "Room join ho gaya."
                    } catch (e: Exception) { message = e.message ?: "Room join nahi hua." }
                    busy = false
                }
            }, enabled = !busy && inviteCode.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("JOIN ROOM") }
            HorizontalDivider(color = Color.DarkGray)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("YOUR ROOMS", color = RoomGreen, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { loadRooms() }) { Text("REFRESH", color = RoomGreen) }
            }
            if (rooms.isEmpty()) Text("No rooms yet.", color = Color.LightGray, fontSize = 12.sp)
            rooms.forEach { room ->
                Card(onClick = { selectedRoom = room; prefs.edit().putString("selected_room_id", room.id).putString("selected_room_name", room.name).putString("selected_room_role", room.role).apply(); roomPlayers = emptyList(); loadRoomPlayers(room) }, colors = CardDefaults.cardColors(containerColor = RoomPanel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(room.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (room.description.isNotBlank()) Text(room.description, color = Color.LightGray, fontSize = 12.sp)
                        Text("ROLE: ${room.role.uppercase()}", color = RoomGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (room.role == "owner" || room.role == "admin") {
                            Text("Invite code: ${room.inviteCode}", color = Color.White, fontSize = 13.sp)
                        }
                        Text(if (room.role == "owner" || room.role == "admin") "Room management access" else "Read-only member access", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
            if (selectedRoom != null) {
                val room = selectedRoom!!
                HorizontalDivider(color = Color.DarkGray)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(onClick = { selectedRoom = null; prefs.edit().remove("selected_room_id").remove("selected_room_name").remove("selected_room_role").apply(); roomPlayers = emptyList(); message = "" }) {
                        Text("← BACK TO ROOMS", color = RoomGreen)
                    }
                }
                Text(room.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (room.role == "owner" || room.role == "admin") Text("INVITATION CODE: ${room.inviteCode}", color = RoomGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(onClick = onRoomEntered, modifier = Modifier.fillMaxWidth()) { Text("ENTER THIS ROOM") }
                Text("ROOM SQUAD", color = RoomGreen, fontWeight = FontWeight.Bold)
                if (room.role == "owner" || room.role == "admin") {
                    OutlinedTextField(value = newPlayerName, onValueChange = { newPlayerName = it }, label = { Text("Player name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                    Text("PLAYER ROLE", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Batsman", "Bowler", "All-rounder", "Wicketkeeper").forEach { role ->
                            FilterChip(selected = newPlayerRole == role, onClick = { newPlayerRole = role }, label = { Text(role, fontSize = 10.sp) })
                        }
                    }
                    Button(onClick = {
                        scope.launch {
                            busy = true
                            try {
                                SupabaseRoomsApi.addRoomPlayer(accessToken, room.id, newPlayerName.trim(), newPlayerRole)
                                newPlayerName = ""
                                roomPlayers = SupabaseRoomsApi.roomPlayers(accessToken, room.id)
                                message = "Player add ho gaya."
                            } catch (e: Exception) { message = e.message ?: "Player add nahi hua." }
                            busy = false
                        }
                    }, enabled = !busy && newPlayerName.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("ADD PLAYER")
                    }
                } else {
                    Text("Read-only access: squad dekh sakte ho, players add nahi kar sakte.", color = Color.LightGray, fontSize = 12.sp)
                }
                Text("${roomPlayers.size} PLAYERS", color = RoomGreen, fontWeight = FontWeight.Bold)
                if (roomPlayers.isEmpty()) Text("Abhi squad empty hai. Pehla player add karo.", color = Color.LightGray, fontSize = 12.sp)
                roomPlayers.forEachIndexed { index, player ->
                    Card(colors = CardDefaults.cardColors(containerColor = RoomPanel), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("${index + 1}.", color = RoomGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(player.role, color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                TextButton(onClick = { loadRoomPlayers(room) }) { Text("REFRESH SQUAD", color = RoomGreen) }
            }
        }
        if (message.isNotBlank()) Text(message, color = if (message.contains("nahi") || message.contains("failed", true) || message.contains("Invalid", true)) Color(0xFFFF8A80) else RoomGreen, fontSize = 12.sp)
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RoomGreen)
        Text("Security: room access aur editing permissions database RLS se enforce hoti hain.", color = Color.LightGray, fontSize = 11.sp)
    }
}
