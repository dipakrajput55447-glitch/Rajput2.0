cat << 'EOF' > app/src/main/java/com/rajput/info/MainActivity.kt
package com.rajput.info

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.random.Random

val DarkBg = Color(0xFF050505)
val SurfaceBg = Color(0xFF111111)
val GoldAccent = Color(0xFFD4AF37)
val TextWhite = Color(0xFFF5F5F5)
val ErrorRed = Color(0xFFE53935)

interface ApiService {
    @GET("tgv2") suspend fun telegramLookup(@Query("key") key: String = "Rajput", @Query("q") query: String): Response<JsonObject>
    @GET("id") suspend fun getTelegramId(@Query("key") key: String = "Rajput", @Query("q") username: String): Response<JsonObject>
    @GET("https://deeposint.felixrdx.xyz/api/developer/Cyb3rB4nn3r/") suspend fun phoneLookup(@Query("key") key: String = "9c8c8ebe8355255bb2c4cd2e002476cf", @Query("query") phone: String): Response<JsonObject>
}

object RetrofitClient {
    val api: ApiService by lazy { Retrofit.Builder().baseUrl("https://osint.invalidayushh.workers.dev/").addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java) }
}

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("RajputAuth", Context.MODE_PRIVATE)
    init { if (!prefs.contains("user_RAJPUT9555")) { prefs.edit().putString("user_RAJPUT9555", "RAJPUT9555").putInt("credits_RAJPUT9555", 9999).apply() } }
    fun createUser(u: String, p: String) = prefs.edit().putString("user_$u", p).putInt("credits_$u", 10).apply()
    fun isValidUser(u: String, p: String): Boolean = prefs.getString("user_$u", "") == p
    fun isLoggedIn(): Boolean = prefs.getBoolean("isLoggedIn", false)
    fun setLoggedIn(status: Boolean) = prefs.edit().putBoolean("isLoggedIn", status).apply()
    fun getCurrentUser(): String = prefs.getString("currentUser", "") ?: ""
    fun setCurrentUser(u: String) = prefs.edit().putString("currentUser", u).apply()
    fun getCredits(u: String): Int = prefs.getInt("credits_$u", 0)
    fun deductCredit(u: String) { val c = getCredits(u); if (c > 0) prefs.edit().putInt("credits_$u", c - 1).apply() }
    fun addCredits(u: String, amount: Int) { prefs.edit().putInt("credits_$u", getCredits(u) + amount).apply() }
    fun hasJoinedTelegram(): Boolean = prefs.getBoolean("joinedTg", false)
    fun setJoinedTelegram() = prefs.edit().putBoolean("joinedTg", true).apply()
}

class AppViewModel : ViewModel() {
    private val _tgState = MutableStateFlow<UiState>(UiState.Idle)
    val tgState: StateFlow<UiState> = _tgState
    private val _phoneState = MutableStateFlow<UiState>(UiState.Idle)
    val phoneState: StateFlow<UiState> = _phoneState

    fun searchTelegram(query: String, isUsername: Boolean) {
        if (query.isBlank()) return
        _tgState.value = UiState.Loading
        viewModelScope.launch {
            try {
                var finalId = query
                if (isUsername) {
                    val idRes = RetrofitClient.api.getTelegramId(query)
                    finalId = idRes.body()?.getAsJsonObject("result")?.getAsJsonObject("data")?.get("id")?.asString ?: ""
                    if (finalId.isEmpty()) { _tgState.value = UiState.Error("Username not found"); return@launch }
                }
                val res = RetrofitClient.api.telegramLookup(query = finalId)
                if (res.isSuccessful && res.body() != null) _tgState.value = UiState.Success(res.body()!!) else _tgState.value = UiState.Error("No data")
            } catch (e: Exception) { _tgState.value = UiState.Error(e.message ?: "Error") }
        }
    }

    fun searchPhone(phone: String) {
        if (phone.isBlank()) return
        _phoneState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val res = RetrofitClient.api.phoneLookup(phone = phone)
                if (res.isSuccessful && res.body() != null) {
                    val json = res.body()!!
                    if (json.get("status")?.asBoolean == true) _phoneState.value = UiState.Success(json.getAsJsonObject("data")) else _phoneState.value = UiState.Error("No data found")
                } else _phoneState.value = UiState.Error("API Error")
            } catch (e: Exception) { _phoneState.value = UiState.Error(e.message ?: "Error") }
        }
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val data: JsonObject) : UiState()
    data class Error(val msg: String) : UiState()
}

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { mediaPlayer = MediaPlayer.create(this, R.raw.song); mediaPlayer?.isLooping = true; mediaPlayer?.start() } catch (e: Exception) { }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = GoldAccent, background = DarkBg, surface = SurfaceBg)) {
                val isMaintenance = false 
                if (isMaintenance) MaintenanceScreen() else Box(modifier = Modifier.fillMaxSize().background(DarkBg)) { HackerBackground(); AppContent() }
            }
        }
    }
    override fun onDestroy() { super.onDestroy(); mediaPlayer?.stop(); mediaPlayer?.release() }
}

@Composable
fun HackerBackground() {
    val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.GREEN; textSize = 35f; alpha = 50; fontFamily = FontFamily.Monospace }
    Canvas(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        for (i in 0..25) {
            val x = Random.nextInt(0, size.width.toInt()).toFloat()
            val y = Random.nextInt(0, size.height.toInt()).toFloat()
            drawContext.canvas.nativeCanvas.drawText("01ABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%^".random().toString(), x, y, textPaint)
        }
    }
}

@Composable
fun MaintenanceScreen() {
    Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, "Maintenance", tint = GoldAccent, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Under Maintenance", color = GoldAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Hum jald wapas aayenge!", color = TextWhite)
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    var isLoggedIn by remember { mutableStateOf(session.isLoggedIn()) }
    var hasJoinedTg by remember { mutableStateOf(session.hasJoinedTelegram()) }
    if (!hasJoinedTg) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Join Telegram", color = GoldAccent) },
            text = { Text("App use karne ke liye channel join karein.", color = TextWhite) },
            confirmButton = { Button(onClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AnonymouschatbotR"))); hasJoinedTg = true; session.setJoinedTelegram() } catch (e: Exception) {} }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) { Text("Join", color = Color.Black) } }
        )
    } else if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { u -> isLoggedIn = true; session.setLoggedIn(true); session.setCurrentUser(u) }, session = session)
    } else {
        MainScreen(session)
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, session: SessionManager) {
    var id by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(DarkBg).padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("LOGIN", color = GoldAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, focusedLabelColor = GoldAccent))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, focusedLabelColor = GoldAccent))
        Spacer(Modifier.height(24.dp))
        Button(onClick = { if (session.isValidUser(id, pass)) onLoginSuccess(id) else Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)) { Text("LOGIN") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(session: SessionManager) {
    var tab by remember { mutableStateOf(0) }
    var showAdmin by remember { mutableStateOf(false) }
    var credits by remember { mutableStateOf(session.getCredits(session.getCurrentUser())) }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rajput Info | Credits: $credits", color = GoldAccent, fontWeight = FontWeight.Bold) }, actions = { IconButton(onClick = { showAdmin = true }) { Icon(Icons.Default.Lock, "Admin", tint = GoldAccent) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)) },
        bottomBar = { NavigationBar(containerColor = SurfaceBg) { NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Send, null) }, label = { Text("Telegram") }); NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Phone, null) }, label = { Text("Phone") }) } }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(DarkBg)) {
            if (showAdmin) {
                AdminPanel(session, onClose = { showAdmin = false })
            } else if (tab == 0) {
                TelegramTab(session = session, onCreditUsed = { credits = session.getCredits(session.getCurrentUser()) })
            } else {
                PhoneTab(session = session, onCreditUsed = { credits = session.getCredits(session.getCurrentUser()) })
            }
        }
    }
}

@Composable
fun TelegramTab(session: SessionManager, onCreditUsed: () -> Unit, viewModel: AppViewModel = viewModel()) {
    var subTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    val state by viewModel.tgState.collectAsState()
    val context = LocalContext.current
    Column(Modifier.padding(16.dp)) {
        TabRow(selectedTabIndex = subTab, containerColor = SurfaceBg, contentColor = GoldAccent) { Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("ID") }); Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Username") }); Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Phone") }) }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Enter Query") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent))
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val u = session.getCurrentUser()
            if (session.getCredits(u) <= 0) Toast.makeText(context, "Credits khatam! Owner se add karwayein.", Toast.LENGTH_LONG).show()
            else { session.deductCredit(u); onCreditUsed(); viewModel.searchTelegram(query, subTab == 1) }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)) { Text("SEARCH") }
        Spacer(Modifier.height(16.dp))
        when (val s = state) {
            is UiState.Loading -> CircularProgressIndicator(color = GoldAccent, modifier = Modifier.align(Alignment.CenterHorizontally))
            is UiState.Error -> Text(s.msg, color = ErrorRed)
            is UiState.Success -> {
                val target = s.data.getAsJsonObject("data")?.getAsJsonObject("_TARGET_ACQUIRED_")
                val results = s.data.getAsJsonObject("data")?.getAsJsonObject("results")
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceBg), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Name: ${target?.get("OPERATIVE_NAME")?.asString ?: "N/A"}", color = TextWhite)
                        Text("Country: ${results?.get("country")?.asString ?: "N/A"}", color = TextWhite)
                        Text("Number: ${results?.get("number")?.asString ?: "N/A"}", color = TextWhite)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun PhoneTab(session: SessionManager, onCreditUsed: () -> Unit, viewModel: AppViewModel = viewModel()) {
    var phone by remember { mutableStateOf("+91") }
    val state by viewModel.phoneState.collectAsState()
    val context = LocalContext.current
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent))
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val u = session.getCurrentUser()
            if (session.getCredits(u) <= 0) Toast.makeText(context, "Credits khatam!", Toast.LENGTH_LONG).show()
            else { session.deductCredit(u); onCreditUsed(); viewModel.searchPhone(phone) }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)) { Text("SEARCH") }
        Spacer(Modifier.height(16.dp))
        when (val s = state) {
            is UiState.Loading -> CircularProgressIndicator(color = GoldAccent, modifier = Modifier.align(Alignment.CenterHorizontally))
            is UiState.Error -> Text(s.msg, color = ErrorRed)
            is UiState.Success -> Card(colors = CardDefaults.cardColors(containerColor = SurfaceBg), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Data Found", color = GoldAccent, fontWeight = FontWeight.Bold); Text(s.data.toString(), color = TextWhite, fontSize = 12.sp) } }
            else -> {}
        }
    }
}

@Composable
fun AdminPanel(session: SessionManager, onClose: () -> Unit) {
    val context = LocalContext.current
    var showAddUser by remember { mutableStateOf(false) }
    var showAddCredits by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Admin Panel", color = GoldAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { showAddUser = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) { Text("Create User (10 Credits)", color = Color.Black) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showAddCredits = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) { Text("Add Credits to User", color = Color.Black) }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text("Back", color = Color.White) }
    }
    if (showAddUser) {
        var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddUser = false }, title = { Text("Create User") }, text = { Column { OutlinedTextField(value = u, onValueChange = { u = it }, label = { Text("Username") }); OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Password") }) } },
            confirmButton = { Button(onClick = { if (u.isNotBlank() && p.isNotBlank()) { session.createUser(u, p); Toast.makeText(context, "User Created with 10 Credits", Toast.LENGTH_SHORT).show(); showAddUser = false } }) { Text("Create") } })
    }
    if (showAddCredits) {
        var u by remember { mutableStateOf("") }; var c by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddCredits = false }, title = { Text("Add Credits") }, text = { Column { OutlinedTextField(value = u, onValueChange = { u = it }, label = { Text("Username") }); OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("Credits Amount") }) } },
            confirmButton = { Button(onClick = { val amt = c.toIntOrNull() ?: 0; if (u.isNotBlank() && amt > 0) { session.addCredits(u, amt); Toast.makeText(context, "Credits Added", Toast.LENGTH_SHORT).show(); showAddCredits = false } }) { Text("Add") } })
    }
}
EOF
