package com.mixcom.fintech

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

// === இங்கே உங்கள் Supabase URL மற்றும் Anon Key-ஐ மாற்றவும் ===
const val SUPABASE_URL = "https://bwowvsrcehixusbkhctv.supabase.co"
const val SUPABASE_ANON_KEY = "sb_publishable_ohZfbMRrAR2GwR-GtytDhA_WZ0ZknTK"
const val DEFAULT_AGENT_ID = "MXA-100001"

val RoyalBlue = Color(0xFF1D3587)
val DeepBlue = Color(0xFF0E236B)
val BrandRed = Color(0xFFD32F2F)
val SoftBg = Color(0xFFF5F6FA)
val BadgeYellow = Color(0xFFFFE59E)
val BadgeGreen = Color(0xFFC8E6C9)
val VipGold = Color(0xFFD4AF37)

fun hashSecret(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun generateAgentId(phoneOrIdentifier: String): String {
    val digitsOnly = phoneOrIdentifier.filter { it.isDigit() }
    val suffix = if (digitsOnly.length >= 4) {
        val last4 = digitsOnly.takeLast(4)
        val rand2 = (10..99).random()
        "$last4$rand2"
    } else {
        (100000..999999).random().toString()
    }
    return "MXA-$suffix"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

data class AuthUser(
    val agentId: String,
    val fullName: String,
    val identifier: String,
    val passwordHash: String
)

data class SecurityState(
    val kycVerified: Boolean = false,
    val mpinHash: String? = null
)

data class GoldRateInfo(
    val buyPrice: Double = 13007.0,
    val sellPrice: Double = 12630.84,
    val finalBuyPrice: Double = 13397.21
)

data class ServiceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val provider: String, // "EKO", "OROPOCKET", "WALLET"
    val category: String = "Wallet",
    val isAeps: Boolean = false,
    val badge: String? = null,
    val badgeColor: Color = BadgeYellow
)

data class TxnItem(
    val id: String,
    val date: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val balance: String,
    val isCredit: Boolean
)

object SupabaseClient {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun baseRequest(endpoint: String): Request.Builder {
        return Request.Builder()
            .url("$SUPABASE_URL/rest/v1/$endpoint")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
    }

    suspend fun callEdgeFunction(
        functionName: String,
        payloadJson: JSONObject
    ): Pair<Boolean, JSONObject> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/$functionName")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payloadJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                val json = try { JSONObject(rawBody) } catch (_: Exception) { JSONObject() }
                val ok = resp.isSuccessful && json.optBoolean("success", true)
                Pair(ok, json)
            }
        } catch (e: Exception) {
            Pair(false, JSONObject().put("message", e.message ?: "Network error"))
        }
    }

    suspend fun fetchGoldRate(): GoldRateInfo? = withContext(Dispatchers.IO) {
        try {
            val (ok, json) = callEdgeFunction("oropocket-gold", JSONObject().put("action", "get_gold_rate"))
            if (!ok) return@withContext null
            val goldObj = json.optJSONObject("data")
                ?.optJSONObject("data")
                ?.optJSONObject("prices")
                ?.optJSONObject("gold")
            if (goldObj != null) {
                GoldRateInfo(
                    buyPrice = goldObj.optDouble("buy", 13007.0),
                    sellPrice = goldObj.optDouble("sell", 12630.84),
                    finalBuyPrice = goldObj.optDouble("final_buy_price", 13397.21)
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createInitialWalletIfMissing(agentId: String, initialBalance: Double = 0.0): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("user_id", agentId)
                    .put("balance", initialBalance)
                    .toString()

                val req = baseRequest("wallets?on_conflict=user_id")
                    .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }

    suspend fun fetchSecurityState(agentId: String): SecurityState? = withContext(Dispatchers.IO) {
        try {
            val req = baseRequest("agent_security?user_id=eq.$agentId&select=kyc_status,mpin_hash").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val status = obj.optString("kyc_status", "PENDING")
                    val hash = obj.optString("mpin_hash", "")
                    SecurityState(
                        kycVerified = status == "VERIFIED",
                        mpinHash = hash.ifBlank { null }
                    )
                } else {
                    SecurityState(kycVerified = false, mpinHash = null)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveKycAndMpin(agentId: String, pan: String, aadhaarLast4: String, mpinHash: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("user_id", agentId)
                    .put("pan_number", pan)
                    .put("aadhaar_last4", aadhaarLast4)
                    .put("kyc_status", "VERIFIED")
                    .put("mpin_hash", mpinHash)
                    .toString()

                val req = baseRequest("agent_security?on_conflict=user_id")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }

    suspend fun fetchWalletBalance(agentId: String): Double? = withContext(Dispatchers.IO) {
        try {
            val req = baseRequest("wallets?user_id=eq.$agentId&select=balance").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val arr = JSONArray(body)
                if (arr.length() > 0) arr.getJSONObject(0).optDouble("balance", 0.0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateWalletBalance(agentId: String, newBalance: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("balance", newBalance).toString()
            val req = baseRequest("wallets?user_id=eq.$agentId")
                .patch(payload.toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchTransactions(agentId: String): List<TxnItem>? = withContext(Dispatchers.IO) {
        try {
            val req = baseRequest("transactions?user_id=eq.$agentId&order=created_at.desc&limit=30").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val arr = JSONArray(body)
                val list = mutableListOf<TxnItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val amt = obj.optDouble("amount", 0.0)
                    val balAfter = obj.optDouble("balance_after", 0.0)
                    val isCredit = obj.optBoolean("is_credit", false)
                    val prefix = if (isCredit) "+ Rs" else "- Rs"
                    list.add(
                        TxnItem(
                            id = obj.optString("trans_id", "000000"),
                            date = obj.optString("created_at", "").take(16).replace("T", " | "),
                            title = obj.optString("title", ""),
                            subtitle = obj.optString("subtitle", ""),
                            amount = "$prefix${"%.2f".format(amt)}",
                            balance = "Balance Rs ${"%.2f".format(balAfter)}",
                            isCredit = isCredit
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun insertTransaction(
        agentId: String,
        transId: String,
        category: String,
        provider: String,
        title: String,
        subtitle: String,
        amount: Double,
        balanceAfter: Double,
        isCredit: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("trans_id", transId)
                .put("user_id", agentId)
                .put("service_category", category)
                .put("api_provider", provider)
                .put("title", title)
                .put("subtitle", subtitle)
                .put("amount", amount)
                .put("balance_after", balanceAfter)
                .put("is_credit", isCredit)
                .toString()

            val req = baseRequest("transactions")
                .addHeader("Prefer", "return=minimal")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var registeredUser by remember { mutableStateOf<AuthUser?>(null) }
    var isAuthenticated by remember { mutableStateOf(false) }

    var isLoadingSecurity by remember { mutableStateOf(false) }
    var isUnlocked by remember { mutableStateOf(false) }
    var securityState by remember { mutableStateOf(SecurityState()) }

    val activeAgentId = registeredUser?.agentId ?: DEFAULT_AGENT_ID

    LaunchedEffect(isAuthenticated, activeAgentId) {
        if (!isAuthenticated) return@LaunchedEffect
        isLoadingSecurity = true
        SupabaseClient.createInitialWalletIfMissing(activeAgentId, 0.0)
        val remote = SupabaseClient.fetchSecurityState(activeAgentId)
        if (remote != null) {
            securityState = remote
        }
        isLoadingSecurity = false
    }

    if (!isAuthenticated) {
        AuthSignInSignUpScreen(
            registeredUser = registeredUser,
            onSignUpSuccess = { newUser ->
                registeredUser = newUser
                isAuthenticated = true
                Toast.makeText(
                    context,
                    "Registered! Agent ID: ${newUser.agentId}",
                    Toast.LENGTH_LONG
                ).show()
            },
            onSignInSuccess = { user ->
                registeredUser = user
                isAuthenticated = true
                Toast.makeText(
                    context,
                    "Signed in (${user.agentId})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        return
    }

    if (isLoadingSecurity) {
        Box(
            modifier = Modifier.fillMaxSize().background(RoyalBlue),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("Initializing Agent $activeAgentId...", color = Color.White)
            }
        }
        return
    }

    if (!isUnlocked) {
        KycAndMpinGateScreen(
            agentId = activeAgentId,
            initialKycVerified = securityState.kycVerified,
            savedMpinHash = securityState.mpinHash,
            onSaveKycAndMpin = { pan, last4, mpinHash ->
                securityState = SecurityState(kycVerified = true, mpinHash = mpinHash)
                scope.launch {
                    val ok = SupabaseClient.saveKycAndMpin(activeAgentId, pan, last4, mpinHash)
                    if (!ok) {
                        Toast.makeText(context, "Saved locally (check Supabase URL/Key)", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUnlocked = { isUnlocked = true }
        )
        return
    }

    FintechMainApp(
        currentUser = registeredUser,
        onLockApp = { isUnlocked = false },
        onSignOut = {
            isUnlocked = false
            isAuthenticated = false
        }
    )
}

@Composable
fun AuthSignInSignUpScreen(
    registeredUser: AuthUser?,
    onSignUpSuccess: (AuthUser) -> Unit,
    onSignInSuccess: (AuthUser) -> Unit
) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(true) }

    var fullName by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf(registeredUser?.identifier ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepBlue, RoyalBlue)))
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = VipGold, shape = RoundedCornerShape(6.dp)) {
            Text(
                "MIXCOM AGENT ONBOARDING",
                color = Color.Black,
                fontWeight =
