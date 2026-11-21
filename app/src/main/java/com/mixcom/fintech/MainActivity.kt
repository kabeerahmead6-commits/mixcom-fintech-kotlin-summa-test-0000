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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

@Composable
fun FintechMainApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeService by remember { mutableStateOf<ServiceItem?>(null) }
    var walletBalance by remember { mutableDoubleStateOf(78.00) }
    val txnList = remember {
        mutableStateListOf(
            TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Wallet | Refund", "GST Amount Refunded", "+ Rs 0.76", "Balance Rs 77.50", true),
            TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Move to Bank | Refund", "Move to Bank charges refunded", "+ Rs 4.24", "Balance Rs 76.74", true),
            TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Wallet | GST", "GST Amount Debited", "- Rs 0.76", "Balance Rs 72.50", false)
        )
    }

    // Back button handler when inside a service screen
    BackHandler(enabled = activeService != null) {
        activeService = null
    }

    if (activeService != null) {
        ServiceActionRouter(
            service = activeService!!,
            currentBalance = walletBalance,
            onBack = { activeService = null },
            onTransactionSuccess = { title, sub, amt, isCredit ->
                if (isCredit) walletBalance += amt else walletBalance -= amt
                val prefix = if (isCredit) "+ Rs" else "- Rs"
                txnList.add(
                    0,
                    TxnItem(
                        id = (10000000000000..99999999999999).random().toString(),
                        date = "21-Nov-2026 | Just now",
                        title = title,
                        subtitle = sub,
                        amount = "$prefix ${"%.2f".format(amt)}",
                        balance = "Balance Rs ${"%.2f".format(walletBalance)}",
                        isCredit = isCredit
                    )
                )
                activeService = null
                selectedTab = 1 // Redirect to History tab to see result
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
fun ServicesHomeScreen(walletBalance: Double, onServiceClick: (ServiceItem) -> Unit) {
    var showBalance by remember { mutableStateOf(true) }

    val bankingServices = listOf(
        ServiceItem("aeps_withdraw", "Cash Withdraw", Icons.Outlined.AccountBalanceWallet, "EKO"),
        ServiceItem("aeps_mini", "Mini Statement", Icons.Outlined.Description, "EKO"),
        ServiceItem("aeps_balance", "Balance Enquiry", Icons.Outlined.Savings, "EKO"),
        ServiceItem("dmt", "Money Transfer", Icons.Outlined.SwapHoriz, "EKO"),
        ServiceItem("buy_gold", "Digital Gold", Icons.Outlined.Diamond, "OROPOCKET", "24K LIVE", BadgeYellow),
        ServiceItem("sell_gold", "Sell Gold", Icons.Outlined.MonetizationOn, "OROPOCKET", "INSTANT", BadgeGreen),
        ServiceItem("upi_cash", "UPI Cash Point", Icons.Outlined.QrCode2, "EKO", "NEW", BadgeGreen),
        ServiceItem("matm", "Micro ATM", Icons.Outlined.PointOfSale, "EKO"),
        ServiceItem("credit_card", "Credit Cards", Icons.Outlined.CreditCard, "EKO", "FREE", BadgeYellow)
    )

    val billsServices = listOf(
        ServiceItem("mobile_prepaid", "Mobile Prepaid", Icons.Outlined.Smartphone, "EKO", "Upto 2%"),
        ServiceItem("loan_repay", "Loan Repayment", Icons.Outlined.RequestQuote, "EKO", "Flat 0.15%"),
        ServiceItem("electricity", "Electricity", Icons.Outlined.Lightbulb, "EKO", "Flat ₹1"),
        ServiceItem("dth", "DTH", Icons.Outlined.SettingsInputAntenna, "EKO", "Upto 1.50%"),
        ServiceItem("agent_coll", "Agent Collection", Icons.Outlined.Storefront, "EKO", "Flat 0.11%"),
        ServiceItem("cc_bill", "Credit Card Bill", Icons.Outlined.CreditScore, "EKO", "Flat ₹0.5"),
        ServiceItem("fastag", "FASTag", Icons.Outlined.DirectionsCar, "EKO", "Flat 0.15%"),
        ServiceItem("postpaid", "Mobile Postpaid", Icons.Outlined.PhoneAndroid, "EKO")
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
                            Text("Wallet Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
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
                                onServiceClick(ServiceItem("add_wallet", "Add Money to Wallet", Icons.Outlined.AddCard, "WALLET"))
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Outlined.AddCard, contentDescription = "Add Money", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Alerts", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Gold & Loan Promo Banner
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A3A9C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onServiceClick(ServiceItem("buy_gold", "Digital Gold", Icons.Outlined.Diamond, "OROPOCKET"))
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

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Accept Payments", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(14.dp))
                        ServiceIconBox(ServiceItem("mpos", "MPOS+", Icons.Outlined.PointOfSale, "EKO"), onServiceClick)
                    }
                }
                Card(
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Govt. Services", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceIconBox(ServiceItem("pan_apply", "Apply PAN", Icons.Outlined.Badge, "EKO"), onServiceClick)
                            ServiceIconBox(ServiceItem("epan", "Activate ePAN", Icons.Outlined.ContactPage, "EKO", "Instant"), onServiceClick)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ServiceActionRouter(
    service: ServiceItem,
    currentBalance: Double,
    onBack: () -> Unit,
    onTransactionSuccess: (String, String, Double, Boolean) -> Unit
) {
    val context = LocalContext.current
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("State Bank of India") }

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
                            Text("Live Buy Price: ₹7,850.00 / gram", fontSize = 13.sp, color = Color(0xFF2E7D32))
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Enter Amount in ₹ (Min ₹10)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            val grams = (amountStr.toDoubleOrNull() ?: 0.0) / 7850.0
                            Text("Estimated Gold Weight: ${"%.4f".format(grams)} grams", fontWeight = FontWeight.SemiBold)
                        }

                        service.id.startsWith("aeps") -> {
                            Text("Eko AePS Biometric Service", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { if (it.length <= 12) field1 = it },
                                label = { Text("Customer 12-Digit Aadhaar Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { if (it.length <= 10) field2 = it },
                                label = { Text("Customer Mobile Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = selectedOption,
                                onValueChange = { selectedOption = it },
                                label = { Text("Bank Name") },
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

                        service.id == "dmt" -> {
                            Text("Eko Direct Money Transfer (IMPS/NEFT)", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Beneficiary Bank Account Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { field2 = it.uppercase() },
                                label = { Text("IFSC Code (e.g. CIUB0000001)") },
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
                            Text("Instant Wallet Top-up", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Enter Amount to Add (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> {
                            Text("Eko BBPS & Recharge Service", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Mobile Number / Consumer ID / Vehicle No") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { field2 = it },
                                label = { Text("Operator / Biller Name (e.g. Jio, Airtel, TANGEDCO)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Bill / Recharge Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (service.id != "aeps_mini" && service.id != "aeps_balance" && amt <= 0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isCredit = (service.id == "add_wallet" || service.id == "aeps_withdraw" || service.id == "sell_gold")
                            if (!isCredit && amt > currentBalance) {
                                Toast.makeText(context, "Insufficient Wallet Balance!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            Toast.makeText(context, "${service.title} Request Submitted via ${service.provider}", Toast.LENGTH_SHORT).show()
                            onTransactionSuccess(
                                "${service.title} | ${service.provider}",
                                if (field1.isNotEmpty()) "Ref: $field1" else "Processed Successfully",
                                amt,
                                isCredit
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (service.id.startsWith("aeps")) "Capture Fingerprint & Proceed" else "Proceed & Pay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1A1A1A))
                Text(tag, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
            }
            Spacer(Modifier.height(14.dp))
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
            lineHeight = 14.sp,
            color = Color(0xFF222222)
        )
    }
}

@Composable
fun HistoryScreen(txns: List<TxnItem>, onBack: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("Wallet") }
    val tabs = listOf("Wallet", "AEPS", "DMT", "Recharge", "GOLD")

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                containerColor = RoyalBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Summary", fontWeight = FontWeight.Bold)
            }
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.background(RoyalBlue)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).border(1.dp, Color.LightGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₹", color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(txn.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(txn.subtitle, color = Color.Gray, fontSize = 12.sp)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }
    }
}

@Composable
fun AccountShopScreen(onActionClick: (ServiceItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = RoyalBlue,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        modifier = Modifier.width(230.dp)
                    ) {
                        Text(
                            "MiXCOM Fintech",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Canvas(modifier = Modifier.width(250.dp).height(45.dp)) {
                        val stripes = 12
                        val stripeW = size.width / stripes
                        for (i in 0 until stripes) {
                            drawRect(
                                color = if (i % 2 == 0) BrandRed else Color(0xFFF2F2F2),
                                topLeft = Offset(i * stripeW, 0f),
                                size = Size(stripeW, size.height)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(color = VipGold, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "VIP",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Powered by Kabeer Ahamed", color = Color.DarkGray, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val shopActions = listOf(
                    Triple("Manage\nWallet", Icons.Outlined.AccountBalanceWallet, ServiceItem("add_wallet", "Add Money to Wallet", Icons.Outlined.AddCard, "WALLET")),
                    Triple("Bank\nSettlement", Icons.Outlined.AccountBalance, ServiceItem("dmt", "Bank Settlement", Icons.Outlined.AccountBalance, "EKO")),
                    Triple("Buy\nGold", Icons.Outlined.Diamond, ServiceItem("buy_gold", "Digital Gold", Icons.Outlined.Diamond, "OROPOCKET")),
                    Triple("My\nEarnings", Icons.Outlined.Savings, ServiceItem("aeps_balance", "Balance Enquiry", Icons.Outlined.Savings, "EKO"))
                )
                shopActions.forEach { (label, icon, srv) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onActionClick(srv) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            color = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = label, tint = BrandRed, modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(label, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Help & Support", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("24x7 Merchant Support", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text("Connected APIs: Eko India Financial Services & Oropocket Digital Gold.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
