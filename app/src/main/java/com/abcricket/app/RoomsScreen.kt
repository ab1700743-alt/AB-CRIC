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

private const val SUPABASE_URL = "https://wcpzmaqwxvvowkdgapsj.supabase.co"
private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_epu8W3eriwDPTVm7TLvoOg_87fuLBQx"

private data class CricketRoom(
    val id: String,
    val name: String,
    val description: String,
    val inviteCode: String,
    val role: String
)

private object SupabaseRoomsApi {
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

    suspend fun createRoom(token: String, userId: String, name: String, description: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("name", name).put("description", description).put("owner_id", userId)
        val response = JSONArray(request("/rest/v1/cricket_circles?select=id", "POST", token, body, "return=representation"))
        response.getJSONObject(0).getString("id")
    }

    suspend fun joinRoom(token: String, code: String) = withContext(Dispatchers.IO) {
        request("/rest/v1/rpc/join_circle_by_code", "POST", token, JSONObject().put("p_code", code))
    }
}

@Composable
fun RoomsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("ab_cricket_rooms", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(prefs.getString("email", "") ?: "") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf(prefs.getString("access_token", "") ?: "") }
    var userId by remember { mutableStateOf(prefs.getString("user_id", "") ?: "") }
    var rooms by remember { mutableStateOf(emptyList<CricketRoom>()) }
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
        Text("PRIVATE CRICKET ROOMS", color = Green, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                Text(if (isSignUp) "Already have an account? Login" else "New here? Create account", color = Green)
            }
        } else {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(email, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = {
                    prefs.edit().remove("access_token").remove("user_id").apply()
                    accessToken = ""; userId = ""; rooms = emptyList(); password = ""
                    message = "Logout ho gaya."
                }) { Text("LOG OUT", color = Green) }
            }
            HorizontalDivider(color = Color.DarkGray)
            Text("CREATE A ROOM", color = Green, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = roomName, onValueChange = { roomName = it }, label = { Text("Room name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            OutlinedTextField(value = roomDescription, onValueChange = { roomDescription = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            Button(onClick = {
                scope.launch {
                    busy = true
                    try {
                        SupabaseRoomsApi.createRoom(accessToken, userId, roomName.trim(), roomDescription.trim())
                        roomName = ""; roomDescription = ""; loadRooms()
                        message = "Room create ho gaya."
                    } catch (e: Exception) { message = e.message ?: "Room create nahi hua." }
                    busy = false
                }
            }, enabled = !busy && roomName.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text("CREATE ROOM") }
            HorizontalDivider(color = Color.DarkGray)
            Text("JOIN WITH INVITE CODE", color = Green, fontWeight = FontWeight.Bold)
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
                Text("YOUR ROOMS", color = Green, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { loadRooms() }) { Text("REFRESH", color = Green) }
            }
            if (rooms.isEmpty()) Text("No rooms yet.", color = Color.LightGray, fontSize = 12.sp)
            rooms.forEach { room ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(room.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (room.description.isNotBlank()) Text(room.description, color = Color.LightGray, fontSize = 12.sp)
                        Text("ROLE: ${room.role.uppercase()}", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (room.role == "owner" || room.role == "admin") {
                            Text("Invite code: ${room.inviteCode}", color = Color.White, fontSize = 13.sp)
                        }
                        Text(if (room.role == "owner" || room.role == "admin") "Room management access" else "Read-only member access", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }
        if (message.isNotBlank()) Text(message, color = if (message.contains("nahi") || message.contains("failed", true) || message.contains("Invalid", true)) Color(0xFFFF8A80) else Green, fontSize = 12.sp)
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Green)
        Text("Security: room access aur editing permissions database RLS se enforce hoti hain.", color = Color.LightGray, fontSize = 11.sp)
    }
}
