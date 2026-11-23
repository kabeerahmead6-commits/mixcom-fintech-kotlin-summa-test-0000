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
const val AGENT_USER_ID = "MIXCOM_AGENT_01"

val RoyalBlue = Color(0xFF1D3587)
val DeepBlue = Color(0xFF0E236B)
val BrandRed = Color(0xFFD32F2F)
val SoftBg = Color(0xFFF5F6FA)
val BadgeYellow = Color(0xFFFFE59E)
val BadgeGreen = Color(0xFFC8E6C9)
val VipGold = Color(0xFFD4AF37)

fun hashMpin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
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
    val icon: ImageVector,
    val provider: String, // "EKO", "OROPOCKET", "WALLET"
    val category: String = "Wallet",
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

    suspend fun fetchSecurityState(): SecurityState? = withContext(Dispatchers.IO) {
        try {
            val req = baseRequest("agent_security?user_id=eq.$AGENT_USER_ID&select=kyc_status,mpin_hash").get().build()
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

    suspend fun saveKycAndMpin(pan: String, aadhaarLast4: String, mpinHash: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("user_id", AGENT_USER_ID)
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

    suspend fun fetchWalletBalance(): Double? = withContext(Dispatchers.IO) {
        try {
            val req = baseRequest("wallets?user_id=eq.$AGENT_USER_ID&select=balance").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val arr = JSONArray(body)
                if (arr.length() > 0) arr.getJSONObject(0).optDouble("balance", 78.0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateWalletBalance(newBalance: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("balance", newBalance).toString()
            val req = baseRequest("wallets?user_id=eq.$AGENT_USER_ID")
                .patch(payload.toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchTransactions(): List<TxnItem>? = withContext(Dispatchers.IO) {
        try {
            val req = baseRequest("transactions?user_id=eq.$AGENT_USER_ID&order=created_at.desc&limit=30").get().build()
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
                            amount = "$prefix ${"%.2f".format(amt)}",
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
                .put("user_id", AGENT_USER_ID)
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

    var isLoadingSecurity by remember { mutableStateOf(true) }
    var isUnlocked by remember { mutableStateOf(false) }
    var securityState by remember { mutableStateOf(SecurityState()) }

    LaunchedEffect(Unit) {
        val remote = SupabaseClient.fetchSecurityState()
        if (remote != null) {
            securityState = remote
        }
        isLoadingSecurity = false
    }

    if (isLoadingSecurity) {
        Box(
            modifier = Modifier.fillMaxSize().background(RoyalBlue),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("Checking KYC & MPIN...", color = Color.White)
            }
        }
        return
    }

    if (!isUnlocked) {
        KycAndMpinGateScreen(
            initialKycVerified = securityState.kycVerified,
            savedMpinHash = securityState.mpinHash,
            onSaveKycAndMpin = { pan, last4, mpinHash ->
                securityState = SecurityState(kycVerified = true, mpinHash = mpinHash)
                scope.launch {
                    val ok = SupabaseClient.saveKycAndMpin(pan, last4, mpinHash)
                    if (!ok) {
                        Toast.makeText(context, "Saved locally (check Supabase URL/Key)", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUnlocked = { isUnlocked = true }
        )
        return
    }

    FintechMainApp(onLockApp = { isUnlocked = false })
}

@Composable
fun KycAndMpinGateScreen(
    initialKycVerified: Boolean,
    savedMpinHash: String?,
    onSaveKycAndMpin: (pan: String, aadhaarLast4: String, mpinHash: String) -> Unit,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var step by remember {
        mutableStateOf(
            when {
                !initialKycVerified -> "KYC"
                savedMpinHash.isNullOrBlank() -> "SET_MPIN"
                else -> "ENTER_MPIN"
            }
        )
    }

    var panInput by remember { mutableStateOf("") }
    var aadhaarInput by remember { mutableStateOf("") }
    var mpinInput by remember { mutableStateOf("") }
    var confirmMpinInput by remember { mutableStateOf("") }

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
                "MIXCOM SECURITY",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("MiXCOM Fintech", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    "KYC" -> {
                        Text("Step 1: Aadhaar & PAN KYC", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RoyalBlue)
                        Text(
                            "Enter PAN & 12-digit Aadhaar to verify account",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = panInput,
                            onValueChange = { if (it.length <= 10) panInput = it.uppercase() },
                            label = { Text("PAN Number (e.g. ABCDE1234F)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = aadhaarInput,
                            onValueChange = { if (it.length <= 12 && it.all { ch -> ch.isDigit() }) aadhaarInput = it },
                            label = { Text("12-Digit Aadhaar Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val panRegex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
                                if (!panRegex.matches(panInput)) {
                                    Toast.makeText(context, "Enter valid PAN (e.g. ABCDE1234F)", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (aadhaarInput.length != 12) {
                                    Toast.makeText(context, "Enter 12-digit Aadhaar number", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                step = "SET_MPIN"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Verify & Set MPIN", fontWeight = FontWeight.Bold)
                        }
                    }

                    "SET_MPIN" -> {
                        Text("Step 2: Create 6-Digit MPIN", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RoyalBlue)
                        OutlinedTextField(
                            value = mpinInput,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) mpinInput = it },
                            label = { Text("Enter 6-Digit MPIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmMpinInput,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) confirmMpinInput = it },
                            label = { Text("Confirm 6-Digit MPIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (mpinInput.length != 6) {
                                    Toast.makeText(context, "MPIN must be 6 digits", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (mpinInput != confirmMpinInput) {
                                    Toast.makeText(context, "MPIN mismatch", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val last4 = if (aadhaarInput.length >= 4) aadhaarInput.takeLast(4) else "0000"
                                onSaveKycAndMpin(panInput, last4, hashMpin(mpinInput))
                                onUnlocked()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save MPIN & Enter App", fontWeight = FontWeight.Bold)
                        }
                    }

                    "ENTER_MPIN" -> {
                        Text("Enter 6-Digit MPIN", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RoyalBlue)
                        OutlinedTextField(
                            value = mpinInput,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) mpinInput = it },
                            label = { Text("6-Digit MPIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (hashMpin(mpinInput) == savedMpinHash) {
                                    onUnlocked()
                                } else {
                                    Toast.makeText(context, "Invalid 6-Digit MPIN", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Unlock App", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FintechMainApp(onLockApp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeService by remember { mutableStateOf<ServiceItem?>(null) }
    var walletBalance by remember { mutableDoubleStateOf(78.00) }
    var goldRate by remember { mutableStateOf(GoldRateInfo()) }
    var isSyncing by remember { mutableStateOf(false) }

    val txnList = remember {
        mutableStateListOf(
            TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Wallet | Refund", "GST Amount Refunded", "+ Rs 0.76", "Balance Rs 77.50", true),
            TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Move to Bank | Refund", "Move to Bank charges refunded", "+ Rs 4.24", "Balance Rs 76.74", true),
            TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Wallet | GST", "GST Amount Debited", "- Rs 0.76", "Balance Rs 72.50", false)
        )
    }

    fun syncFromSupabase(showToast: Boolean = false) {
        scope.launch {
            isSyncing = true
            val dbBalance = SupabaseClient.fetchWalletBalance()
            val dbTxns = SupabaseClient.fetchTransactions()
            val liveGold = SupabaseClient.fetchGoldRate()

            if (dbBalance != null) walletBalance = dbBalance
            if (dbTxns != null && dbTxns.isNotEmpty()) {
                txnList.clear()
                txnList.addAll(dbTxns)
            }
            if (liveGold != null) {
                goldRate = liveGold
            }
            isSyncing = false
            if (showToast) {
                Toast.makeText(context, "Synced with Supabase & Edge Functions!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        syncFromSupabase(showToast = false)
    }

    BackHandler(enabled = activeService != null) {
        activeService = null
    }

    if (activeService != null) {
        ServiceActionRouter(
            service = activeService!!,
            currentBalance = walletBalance,
            onBack = { activeService = null },
            onTransactionSuccess = { srv, title, sub, amt, isCredit, updateBalance ->
                val newBal = if (!updateBalance) {
                    walletBalance
                } else if (isCredit) {
                    walletBalance + amt
                } else {
                    walletBalance - amt
                }
                val transId = (10000000000000..99999999999999).random().toString()
                val prefix = if (isCredit) "+ Rs" else "- Rs"

                walletBalance = newBal
                txnList.add(
                    0,
                    TxnItem(
                        id = transId,
                        date = "Just now",
                        title = title,
                        subtitle = sub,
                        amount = "$prefix ${"%.2f".format(amt)}",
                        balance = "Balance Rs ${"%.2f".format(newBal)}",
                        isCredit = isCredit
                    )
                )
                activeService = null
                selectedTab = 1

                scope.launch {
                    if (updateBalance) {
                        SupabaseClient.updateWalletBalance(newBal)
                    }
                    SupabaseClient.insertTransaction(
                        transId = transId,
                        category = srv.category,
                        provider = srv.provider,
                        title = title,
                        subtitle = sub,
                        amount = amt,
                        balanceAfter = newBal,
                        isCredit = isCredit
                    )
                }
            }
        )
        return
    }

    Scaffold(
        containerColor = SoftBg,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val items = listOf(
                    Triple("Services", Icons.Outlined.Home, Icons.Filled.Home),
                    Triple("History", Icons.Outlined.ReceiptLong, Icons.Filled.ReceiptLong),
                    Triple("Help", Icons.AutoMirrored.Filled.HelpOutline, Icons.AutoMirrored.Filled.HelpOutline),
                    Triple("Account", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
                )
                items.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isSelected) {
                                    Box(
                                        Modifier
                                            .width(36.dp)
                                            .height(3.dp)
                                            .background(BrandRed, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                Icon(
                                    imageVector = if (isSelected) item.third else item.second,
                                    contentDescription = item.first,
                                    tint = if (isSelected) RoyalBlue else Color.Gray
                                )
                            }
                        },
                        label = {
                            Text(
                                item.first,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) RoyalBlue else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ServicesHomeScreen(
                    walletBalance = walletBalance,
                    goldRate = goldRate,
                    isSyncing = isSyncing,
                    onRefresh = { syncFromSupabase(showToast = true) },
                    onServiceClick = { service -> activeService = service }
                )
                1 -> HistoryScreen(txns = txnList, onBack = { selectedTab = 0 })
                2 -> HelpScreen()
                3 -> AccountShopScreen(onLockApp = onLockApp)
            }
        }
    }
}

@Composable
fun ServicesHomeScreen(
    walletBalance: Double,
    goldRate: GoldRateInfo,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onServiceClick: (ServiceItem) -> Unit
) {
    var showBalance by remember { mutableStateOf(true) }

    val bankingServices = listOf(
        ServiceItem("aeps_withdraw", "Cash Withdraw", Icons.Outlined.AccountBalanceWallet, "EKO", "AEPS"),
        ServiceItem("aeps_mini", "Mini Statement", Icons.Outlined.Description, "EKO", "AEPS"),
        ServiceItem("aeps_balance", "Balance Enquiry", Icons.Outlined.Savings, "EKO", "AEPS"),
        ServiceItem("dmt", "Money Transfer", Icons.Outlined.SwapHoriz, "EKO", "DMT"),
        ServiceItem("buy_gold", "Digital Gold", Icons.Outlined.Diamond, "OROPOCKET", "GOLD", "24K LIVE", BadgeYellow),
        ServiceItem("sell_gold", "Sell Gold", Icons.Outlined.MonetizationOn, "OROPOCKET", "GOLD", "INSTANT", BadgeGreen),
        ServiceItem("upi_cash", "UPI Cash Point", Icons.Outlined.QrCode2, "EKO", "Wallet", "NEW", BadgeGreen),
        ServiceItem("matm", "Micro ATM", Icons.Outlined.PointOfSale, "EKO", "AEPS"),
        ServiceItem("credit_card", "Credit Cards", Icons.Outlined.CreditCard, "EKO", "Wallet", "FREE", BadgeYellow)
    )

    val billsServices = listOf(
        ServiceItem("mobile_prepaid", "Mobile Prepaid", Icons.Outlined.Smartphone, "EKO", "Recharge", "Upto 2%"),
        ServiceItem("loan_repay", "Loan Repayment", Icons.Outlined.RequestQuote, "EKO", "Recharge", "Flat 0.15%"),
        ServiceItem("electricity", "Electricity", Icons.Outlined.Lightbulb, "EKO", "Recharge", "Flat ₹1"),
        ServiceItem("dth", "DTH", Icons.Outlined.SettingsInputAntenna, "EKO", "Recharge", "Upto 1.50%"),
        ServiceItem("agent_coll", "Agent Collection", Icons.Outlined.Storefront, "EKO", "Wallet", "Flat 0.11%"),
        ServiceItem("cc_bill", "Credit Card Bill", Icons.Outlined.CreditScore, "EKO", "Recharge", "Flat ₹0.5"),
        ServiceItem("fastag", "FASTag", Icons.Outlined.DirectionsCar, "EKO", "Recharge", "Flat 0.15%"),
        ServiceItem("postpaid", "Mobile Postpaid", Icons.Outlined.PhoneAndroid, "EKO", "Recharge")
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DeepBlue, RoyalBlue)))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = if (showBalance) "₹ ${"%.2f".format(walletBalance)}" else "₹ •••••",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isSyncing) "Syncing..." else "Wallet Balance (KYC Verified)",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { showBalance = !showBalance },
                            modifier = Modifier
                                .size(38.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                if (showBalance) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle Balance",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                onServiceClick(ServiceItem("add_wallet", "Add Money to Wallet", Icons.Outlined.AddCard, "WALLET", "Wallet"))
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Outlined.AddCard, contentDescription = "Add Money", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A3A9C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onServiceClick(ServiceItem("buy_gold", "Digital Gold", Icons.Outlined.Diamond, "OROPOCKET", "GOLD"))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("24K Digital Gold (Oropocket)", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Buy: ₹${"%.0f".format(goldRate.buyPrice)} | Sell: ₹${"%.0f".format(goldRate.sellPrice)}/gm", color = Color.White, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${"%.2f".format(goldRate.finalBuyPrice)}/gm", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("Incl. GST", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            ServiceGridCard("Banking & Gold Services", "Eko + Oropocket", bankingServices, 3, onServiceClick)
        }

        item {
            ServiceGridCard("Recharges and Bills", "BBPS", billsServices, 4, onServiceClick)
        }
    }
}

@Composable
fun ServiceActionRouter(
    service: ServiceItem,
    currentBalance: Double,
    onBack: () -> Unit,
    onTransactionSuccess: (ServiceItem, String, String, Double, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var field1 by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val isEnquiryOnly = service.id == "aeps_balance" || service.id == "aeps_mini"

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoyalBlue)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text(service.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Provider: ${service.provider}", color = Color(0xFFFFD54F), fontSize = 11.sp)
                    }
                }
                Text("₹${"%.2f".format(currentBalance)}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().background(SoftBg).padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = field1,
                        onValueChange = { field1 = it },
                        label = { Text("Mobile / Account / Ref No") },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isEnquiryOnly) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (!isEnquiryOnly && amt <= 0.0) {
                                Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isCredit = (service.id == "add_wallet" || service.id == "aeps_withdraw" || service.id == "sell_gold")
                            val shouldUpdateBalance = (service.id == "add_wallet")
                            if (!isEnquiryOnly && shouldUpdateBalance && !isCredit && amt > currentBalance) {
                                Toast.makeText(context, "Insufficient Wallet Balance", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isProcessing = true
                            scope.launch {
                                var gatewayMsg = "Processed"
                                var gatewayOk = true

                                if (service.provider == "EKO") {
                                    val payload = JSONObject()
                                        .put("action", service.id)
                                        .put("customer_id", if (field1.isNotBlank()) field1 else "9000000000")
                                        .put("method", "POST")
                                        .put(
                                            "payload",
                                            JSONObject()
                                                .put("initiator_id", "9962981729")
                                                .put("client_ref_id", "MX_${System.currentTimeMillis()}")
                                                .put("user_code", "10000001")
                                                .put("bank_code", "HDFC")
                                                .put("aadhar", "TEST_ENCRYPTED")
                                                .put("latlong", "10.9596,79.3766")
                                                .put("piddata", "")
                                                .put("amount", amt)
                                        )
                                    val (ok, respJson) = SupabaseClient.callEdgeFunction("eko-service", payload)
                                    gatewayOk = ok
                                    gatewayMsg = respJson.optJSONObject("data")?.optString("message")
                                        ?: respJson.optString("message", if (ok) "Eko Success" else "Eko Failed")
                                } else if (service.provider == "OROPOCKET") {
                                    val oroAction = if (service.id == "sell_gold") "sell_gold" else "buy_gold"
                                    val payload = JSONObject()
                                        .put("action", oroAction)
                                        .put("path", if (service.id == "sell_gold") "/sell" else "/buy")
                                        .put("method", "POST")
                                        .put("payload", JSONObject().put("amount", amt).put("asset", "gold"))
                                    val (ok, respJson) = SupabaseClient.callEdgeFunction("oropocket-gold", payload)
                                    gatewayOk = ok
                                    gatewayMsg = respJson.optJSONObject("data")?.optString("message")
                                        ?: respJson.optString("message", if (ok) "Oropocket Success" else "Oropocket Failed")
                                }

                                isProcessing = false
                                Toast.makeText(context, gatewayMsg, Toast.LENGTH_LONG).show()

                                if (gatewayOk || service.provider == "WALLET") {
                                    onTransactionSuccess(
                                        service,
                                        "${service.title} | ${service.provider}",
                                        gatewayMsg,
                                        amt,
                                        isCredit,
                                        shouldUpdateBalance
                                    )
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Proceed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceGridCard(
    title: String,
    tag: String,
    items: List<ServiceItem>,
    columns: Int,
    onServiceClick: (ServiceItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(tag, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
            }
            Spacer(Modifier.height(14.dp))
            items.chunked(columns).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ServiceIconBox(item, onServiceClick)
                        }
                    }
                    repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun ServiceIconBox(item: ServiceItem, onClick: (ServiceItem) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick(item) }) {
        if (item.badge != null) {
            Surface(color = item.badgeColor, shape = RoundedCornerShape(4.dp)) {
                Text(item.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
            }
            Spacer(Modifier.height(2.dp))
        } else {
            Spacer(Modifier.height(15.dp))
        }
        Icon(item.icon, contentDescription = item.title, tint = BrandRed, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(item.title, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
fun HistoryScreen(txns: List<TxnItem>, onBack: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("Wallet") }
    val tabs = listOf("Wallet", "AEPS", "DMT", "Recharge", "GOLD")

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.background(RoyalBlue)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Text("History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                items(tabs) { tab ->
                    Column(
                        modifier = Modifier.clickable { selectedFilter = tab }.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tab, color = if (selectedFilter == tab) Color.White else Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (selectedFilter == tab) Box(Modifier.width(40.dp).height(3.dp).background(BrandRed))
                    }
                }
            }
        }
        LazyColumn {
            items(txns) { txn ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Trans ID: ${txn.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(txn.date, color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(txn.amount, color = if (txn.isCredit) Color(0xFF2E7D32) else BrandRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(txn.balance, color = Color(0xFF2E7D32), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(txn.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(txn.subtitle, color = Color.Gray, fontSize = 12.sp)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
        }
    }
}

@Composable
fun AccountShopScreen(onLockApp: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = RoyalBlue, shape = RoundedCornerShape(8.dp)) {
            Text("MiXCOM Fintech", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = VipGold, shape = RoundedCornerShape(6.dp)) {
            Text("VIP VERIFIED", fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onLockApp, colors = ButtonDefaults.buttonColors(containerColor = BrandRed)) {
            Text("Lock App (6-Digit MPIN)")
        }
    }
}

@Composable
fun HelpScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Help & Support", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
        Spacer(Modifier.height(12.dp))
        Text("Eko Banking/Recharge + Oropocket Digital Gold", color = Color.DarkGray)
    }
}
