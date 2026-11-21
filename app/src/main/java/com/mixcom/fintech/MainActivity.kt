package com.mixcom.fintech

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun FintechMainApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

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
                0 -> ServicesHomeScreen()
                1 -> HistoryScreen { selectedTab = 0 }
                2 -> HelpScreen()
                3 -> AccountShopScreen()
            }
        }
    }
}

data class ServiceItem(val title: String, val icon: ImageVector, val badge: String? = null, val badgeColor: Color = BadgeYellow)
data class TxnItem(val id: String, val date: String, val title: String, val subtitle: String, val amount: String, val balance: String, val isCredit: Boolean)

@Composable
fun ServicesHomeScreen() {
    val context = LocalContext.current
    var showBalance by remember { mutableStateOf(true) }
    val walletBalance = "₹ 78.00"

    val bankingServices = listOf(
        ServiceItem("Cash Withdraw", Icons.Outlined.AccountBalanceWallet),
        ServiceItem("Mini Statement", Icons.Outlined.Description),
        ServiceItem("Balance Enquiry", Icons.Outlined.Savings),
        ServiceItem("UPI Cash Point", Icons.Outlined.QrCode2, "NEW", BadgeGreen),
        ServiceItem("Micro ATM", Icons.Outlined.PointOfSale),
        ServiceItem("Money Transfer", Icons.Outlined.SwapHoriz),
        ServiceItem("Loans", Icons.Outlined.MonetizationOn),
        ServiceItem("Suraksha Plans", Icons.Outlined.VerifiedUser, "NEW", BadgeGreen),
        ServiceItem("Credit Cards", Icons.Outlined.CreditCard, "FREE", BadgeYellow)
    )

    val billsServices = listOf(
        ServiceItem("Mobile Prepaid", Icons.Outlined.Smartphone, "Upto 2%"),
        ServiceItem("Loan Repayment", Icons.Outlined.RequestQuote, "Flat 0.15%"),
        ServiceItem("Electricity", Icons.Outlined.Lightbulb, "Flat ₹1"),
        ServiceItem("DTH", Icons.Outlined.SettingsInputAntenna, "Upto 1.50%"),
        ServiceItem("Agent Collection", Icons.Outlined.Storefront, "Flat 0.11%"),
        ServiceItem("Credit Card", Icons.Outlined.CreditScore, "Flat ₹0.5"),
        ServiceItem("FASTag", Icons.Outlined.DirectionsCar, "Flat 0.15%"),
        ServiceItem("Mobile Postpaid", Icons.Outlined.PhoneAndroid)
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Top Blue Header
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
                                text = if (showBalance) walletBalance else "₹ •••••",
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
                            onClick = { Toast.makeText(context, "Add Wallet Money", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .size(38.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Outlined.AddCard, contentDescription = "Add Money", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = { Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Alerts", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Promo Banner
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A3A9C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vyapar Loan Becho", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Extra Commission Kamao", color = Color.White, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    "Refer & Earn",
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Earn 0.7%", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Commission", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Banking Services Section
        item {
            ServiceGridCard(title = "Banking Services", tag = "AePS", items = bankingServices, columns = 3)
        }

        // Recharges & Bills Section
        item {
            ServiceGridCard(title = "Recharges and Bills", tag = "View More", items = billsServices, columns = 4)
        }

        // Accept Payments & Govt Services Row
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
                        ServiceIconBox(ServiceItem("MPOS+", Icons.Outlined.PointOfSale))
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
                            ServiceIconBox(ServiceItem("Apply PAN", Icons.Outlined.Badge))
                            ServiceIconBox(ServiceItem("Activate ePAN", Icons.Outlined.ContactPage, "Instant"))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ServiceGridCard(title: String, tag: String, items: List<ServiceItem>, columns: Int) {
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
                Text(tag, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue)
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
                            ServiceIconBox(item)
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
fun ServiceIconBox(item: ServiceItem) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            Toast.makeText(context, "${item.title} selected", Toast.LENGTH_SHORT).show()
        }
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
fun HistoryScreen(onBack: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("Wallet") }
    val tabs = listOf("Wallet", "AEPS", "DMT", "Recharge", "BBPS")
    val txns = listOf(
        TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Wallet | Refund", "GST Amount Refunded", "+ Rs 0.76", "Balance Rs 77.50", true),
        TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Move to Bank | Refund", "Move to Bank charges refunded", "+ Rs 4.24", "Balance Rs 76.74", true),
        TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Wallet | GST", "GST Amount Debited", "- Rs 0.76", "Balance Rs 72.50", false),
        TxnItem("31793865614910", "05-Nov-2026 | 01:30 PM", "Move to Bank | Charges", "Move to Bank charges deducted", "- Rs 4.24", "Balance Rs 73.26", false)
    )

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

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {}, shape = RoundedCornerShape(8.dp)) {
                    Text("Date", color = Color.DarkGray)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.DarkGray)
                }
                OutlinedButton(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Download, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download Statement", color = Color.DarkGray, fontSize = 13.sp)
                }
            }
            HorizontalDivider()

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
fun AccountShopScreen() {
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
                    // Blue Header Bar + Red & White Shop Awning
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

            // 4 Quick Shop Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val shopActions = listOf(
                    "Manage\nWallet" to Icons.Outlined.AccountBalanceWallet,
                    "Bank\nSettlement" to Icons.Outlined.AccountBalance,
                    "Request\nFunds" to Icons.Outlined.VolunteerActivism,
                    "My\nEarnings" to Icons.Outlined.Savings
                )
                shopActions.forEach { (label, icon) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

            // Complete e-sign Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Draw, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Complete e-sign >", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("To continue services", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // VIP Banner
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = VipGold, shape = RoundedCornerShape(4.dp)) {
                            Text("VIP", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Congratulations! You are now a VIP Adhikari", color = Color.White, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            // Loans & Health Cards
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FD))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Grow with Loans", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Loans just @1.2% interest", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(12.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RoyalBlue)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECE3))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Secure your Health", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Free Shop Cover", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(12.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrandRed)
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
                Text("For AePS, DMT, Wallet settlement or e-sign queries, reach out to support.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
