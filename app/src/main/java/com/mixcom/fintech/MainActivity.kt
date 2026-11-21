package com.mixcom.fintech

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

// === உங்கள் Supabase விவரங்களை இங்கே மாற்றவும் ===
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FintechMainApp()
            }
        }
    }
}

data class ServiceItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val provider: String,
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
fun FintechMainApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeService by remember { mutableStateOf<ServiceItem?>(null) }
    var walletBalance by remember { mutableDoubleStateOf(78.00) }
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
            if (dbBalance != null) {
                walletBalance = dbBalance
            }
            if (dbTxns != null && dbTxns.isNotEmpty()) {
                txnList.clear()
                txnList.addAll(dbTxns)
            }
            isSyncing = false
            if (showToast) {
                Toast.makeText(context, if (dbBalance != null) "Synced with Supabase!" else "Check Supabase URL/Key", Toast.LENGTH_SHORT).show()
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
            onTransactionSuccess = { srv, title, sub, amt, isCredit ->
                val newBal = if (isCredit) walletBalance + amt else walletBalance - amt
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
                    SupabaseClient.updateWalletBalance(newBal)
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
                    isSyncing = isSyncing,
                    onRefresh = { syncFromSupabase(showToast = true) },
                    onServiceClick = { service -> activeService = service }
                )
                1 -> HistoryScreen(txns = txnList, onBack = { selectedTab = 0 })
                2 -> HelpScreen()
                3 -> AccountShopScreen(onActionClick = { service -> activeService = service })
            }
        }
    }
}

@Composable
fun ServicesHomeScreen(
    walletBalance: Double,
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
                                if (isSyncing) "Syncing Supabase..." else "Wallet Balance (Live)",
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
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh Supabase", tint = Color.White)
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
                            Text("Start buying Gold from ₹10", color = Color.White, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    "Buy Gold Now",
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹7,850/gm", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("99.9% Pure", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            ServiceGridCard(
                title = "Banking & Gold Services",
                tag = "Eko + Oropocket",
                items = bankingServices,
                columns = 3,
                onServiceClick = onServiceClick
            )
        }

        item {
            ServiceGridCard(
                title = "Recharges and Bills",
                tag = "BBPS",
                items = billsServices,
                columns = 4,
                onServiceClick = onServiceClick
            )
        }
    }
}

@Composable
fun ServiceActionRouter(
    service: ServiceItem,
    currentBalance: Double,
    onBack: () -> Unit,
    onTransactionSuccess: (ServiceItem, String, String, Double, Boolean) -> Unit
) {
    val context = LocalContext.current
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

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
                        Text("API Provider: ${service.provider}", color = Color(0xFFFFD54F), fontSize = 11.sp)
                    }
                }
                Text("₹${"%.2f".format(currentBalance)}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(SoftBg)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        service.provider == "OROPOCKET" -> {
                            Text("Oropocket 24K Gold (99.9% Purity)", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Enter Amount in ₹") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        service.category == "AEPS" -> {
                            Text("Eko AePS Service", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { if (it.length <= 12) field1 = it },
                                label = { Text("12-Digit Aadhaar Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (service.id == "aeps_withdraw") {
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    label = { Text("Withdrawal Amount (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        service.category == "DMT" -> {
                            Text("Eko Money Transfer (DMT)", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Beneficiary Account Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Transfer Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        service.id == "add_wallet" -> {
                            Text("Add Money to Wallet", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            Text("Eko Recharge & Bill Payment", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Mobile / Consumer Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (service.id != "aeps_mini" && service.id != "aeps_balance" && amt <= 0) {
                                Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isCredit = (service.id == "add_wallet" || service.id == "aeps_withdraw" || service.id == "sell_gold")
                            if (!isCredit && amt > currentBalance) {
                                Toast.makeText(context, "Insufficient Wallet Balance!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onTransactionSuccess(
                                service,
                                "${service.title} | ${service.provider}",
                                if (field1.isNotEmpty()) "Ref: $field1" else "Saved to Supabase",
                                amt,
                                isCredit
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Proceed & Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(tag, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
            }
            Spacer(Modifier.height(14.dp))
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ServiceIconBox(item, onServiceClick)
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceIconBox(item: ServiceItem, onClick: (ServiceItem) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick(item) }
    ) {
        if (item.badge != null) {
            Surface(color = item.badgeColor, shape = RoundedCornerShape(4.dp)) {
                Text(
                    item.badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
        } else {
            Spacer(Modifier.height(15.dp))
        }
        Icon(item.icon, contentDescription = item.title, tint = BrandRed, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun HistoryScreen(txns: List<TxnItem>, onBack: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("Wallet") }
    val tabs = listOf("Wallet", "AEPS", "DMT", "Recharge", "GOLD")

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.background(RoyalBlue)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("History (Supabase)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                items(tabs) { tab ->
                    Column(
                        modifier = Modifier
                            .clickable { selectedFilter = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            tab,
                            color = if (selectedFilter == tab) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        if (selectedFilter == tab) {
                            Box(Modifier.width(40.dp).height(3.dp).background(BrandRed))
                        }
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
                            Text(
                                txn.amount,
                                color = if (txn.isCredit) Color(0xFF2E7D32) else BrandRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
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
fun AccountShopScreen(onActionClick: (ServiceItem) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = RoyalBlue, shape = RoundedCornerShape(8.dp)) {
            Text("MiXCOM Fintech", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = VipGold, shape = RoundedCornerShape(6.dp)) {
            Text("VIP", fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Agent ID: $AGENT_USER_ID", color = Color.DarkGray, fontSize = 13.sp)
    }
}

@Composable
fun HelpScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Help & Support", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
        Spacer(Modifier.height(12.dp))
        Text("Providers: Eko (Banking/Recharge) & Oropocket (Digital Gold)", color = Color.DarkGray)
    }
}
