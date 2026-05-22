package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DebtItem
import com.example.data.ExpenseItem
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FinanceViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showSqlSchemaDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "FINTRACK",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "SUPABASE CONNECTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerSync() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Manual sync with Supabase Server"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.Home, "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.List, "All Transactions") },
                    label = { Text("Expenses") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.Info, "Loans & Credit") },
                    label = { Text("Debts & Loans") }
                )
            }
        },
        floatingActionButton = {
            if (activeTab != 0) {
                FloatingActionButton(
                    onClick = {
                        if (activeTab == 1) {
                            showAddExpenseDialog = true
                        } else {
                            showAddDebtDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (activeTab == 1) "Add Expense" else "Add Debt/Loan"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Show any Supabase sync alerts
            syncError?.let { errorText ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning Info",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Database Synced Status Alert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            errorText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            TextButton(
                                onClick = { showSqlSchemaDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("SETUP DB SQL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.triggerSync() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("RETRY SYNC", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> DashboardTab(
                        expenses = expenses,
                        debts = debts,
                        onNavigateToExpenses = { viewModel.setTab(1) },
                        onNavigateToDebts = { viewModel.setTab(2) },
                        onShowSql = { showSqlSchemaDialog = true }
                    )
                    1 -> ExpensesTab(
                        expenses = expenses,
                        onDeleteExpense = { id -> viewModel.deleteExpenseItem(id) }
                    )
                    2 -> DebtsTab(
                        debts = debts,
                        onDeleteDebt = { id -> viewModel.deleteDebtItem(id) }
                    )
                }
            }
        }
    }

    // Modal dialogs
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amount, category, desc, date, type ->
                viewModel.addNewExpense(amount, category, desc, date, type)
                showAddExpenseDialog = false
                Toast.makeText(context, "Transaction saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { name, amount, limit, rate, due, type, paid ->
                viewModel.addNewDebt(name, amount, limit, rate, due, type, paid)
                showAddDebtDialog = false
                Toast.makeText(context, "$type added successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSqlSchemaDialog) {
        SqlSchemaGuideDialog(
            onDismiss = { showSqlSchemaDialog = false }
        )
    }
}

// --- TAB BUILDERS ---

@Composable
fun DashboardTab(
    expenses: List<ExpenseItem>,
    debts: List<DebtItem>,
    onNavigateToExpenses: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onShowSql: () -> Unit
) {
    val decimalFormat = remember { DecimalFormat("$#,##0.00") }

    // Computations
    val totalIncome = expenses.filter { it.type == "Income" }.sumOf { it.amount }
    val totalExpense = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
    val remainingBudget = totalIncome - totalExpense

    val totalCreditCardDebt = debts.filter { it.type == "Credit Card" }.sumOf { it.amount }
    val totalLoans = debts.filter { it.type == "Loan" }.sumOf { it.amount }
    val totalOutstandingDebt = totalCreditCardDebt + totalLoans

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core financial matrix card
        Text(
            "Financial Snapshot",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Monthly Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        decimalFormat.format(totalIncome),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0EA5E9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        decimalFormat.format(totalExpense),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Remaining Budget", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        decimalFormat.format(remainingBudget),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (remainingBudget >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Outstanding Debt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        decimalFormat.format(totalOutstandingDebt),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- CUSTOM TREND CHART ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Monthly Spending Trend",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Interactive visual tracker",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "May 2026",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (expenses.none { it.type == "Expense" }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No expenses logged relative to May trend yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Draw custom weekly spending trend curve
                    WeeklySpendingTrendChart(expenses = expenses)
                }
            }
        }

        // --- CATEGORICAL EXPENDITURES MAP ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Expense Share by Category",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val expenseItems = expenses.filter { it.type == "Expense" }
                if (expenseItems.isEmpty()) {
                    Text(
                        "Add transaction expenses to visualize breakdown.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    val grouped = expenseItems.groupBy { it.category }
                    val totalExpensesSum = expenseItems.sumOf { it.amount }

                    grouped.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }
                        .forEach { (catName, itemsList) ->
                            val catSum = itemsList.sumOf { it.amount }
                            val perc = if (totalExpensesSum > 0) catSum / totalExpensesSum else 0.0
                            val emoji = getCategoryEmoji(catName)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(emoji, fontSize = 16.sp)
                                        Text(
                                            catName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        "${decimalFormat.format(catSum)} (${(perc * 100).toInt()}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { perc.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                }
            }
        }

        // --- DEBT SUMMARY PANELS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToDebts
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Debt Outlines",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "View Details →",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Credit Cards", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            decimalFormat.format(totalCreditCardDebt),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Loans", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            decimalFormat.format(totalLoans),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // Setup tutorial help Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Database Setup Help", fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Database Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Initial launch seeds offline-first data. To finalize cloud sync with your free Supabase, compile tables using our ready SQL.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Button(
                    onClick = onShowSql,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("SQL", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WeeklySpendingTrendChart(expenses: List<ExpenseItem>) {
    val expenseItems = expenses.filter { it.type == "Expense" }
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorText = MaterialTheme.colorScheme.onSurfaceVariant

    // Partition expenses into 4 weeks of the month for visual representation
    val weeklyAmounts = FloatArray(4) { 0f }
    expenseItems.forEach { item ->
        try {
            // Safe parse date "YYYY-MM-DD"
            val parts = item.date.split("-")
            val day = if (parts.size >= 3) parts[2].toIntOrNull() ?: 15 else 15
            val weekIndex = max(0, ((day - 1) / 7).coerceIn(0, 3))
            weeklyAmounts[weekIndex] += item.amount.toFloat()
        } catch (e: Exception) {
            weeklyAmounts[2] += item.amount.toFloat() // fallback index
        }
    }

    val maxVal = max(50f, weeklyAmounts.maxOrNull() ?: 100f) * 1.15f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High fidelity custom drawing canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / 5f

            // 1. Draw horizontal division lines
            val divisions = 3
            for (i in 0..divisions) {
                val y = height * (i.toFloat() / divisions)
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // 2. Build mathematical points mapping
            val points = arrayListOf<Offset>()
            for (i in 0..3) {
                val x = spacing * (i + 1)
                val y = height - (height * (weeklyAmounts[i] / maxVal))
                points.add(Offset(x, y))
            }

            // 3. Draw gradient background fill under path
            if (points.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(spacing, height)
                    lineTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        // Curved bezier representation
                        val prev = points[i - 1]
                        val curr = points[i]
                        cubicTo(
                            (prev.x + curr.x) / 2f, prev.y,
                            (prev.x + curr.x) / 2f, curr.y,
                            curr.x, curr.y
                        )
                    }
                    lineTo(spacing * 4f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(colorPrimary.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // 4. Draw outer colored stroke line
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        cubicTo(
                            (prev.x + curr.x) / 2f, prev.y,
                            (prev.x + curr.x) / 2f, curr.y,
                            curr.x, curr.y
                        )
                    }
                }
                drawPath(
                    path = strokePath,
                    color = colorPrimary,
                    style = Stroke(width = 6f)
                )

                // 5. Draw interactive bullet dots
                points.forEachIndexed { idx, point ->
                    drawCircle(
                        color = Color.Black,
                        radius = 11f,
                        center = point
                    )
                    drawCircle(
                        color = colorPrimary,
                        radius = 7.dp.toPx() / 2,
                        center = point
                    )
                }
            }
        }

        // Draw weekly horizontal names
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val decimalFormat = DecimalFormat("$#,##0")
            val weekNames = listOf("Week 1", "Week 2", "Week 3", "Week 4")
            weekNames.forEachIndexed { index, name ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorText)
                    Text(decimalFormat.format(weeklyAmounts[index]), fontSize = 9.sp, color = colorPrimary)
                }
            }
        }
    }
}

@Composable
fun ExpensesTab(
    expenses: List<ExpenseItem>,
    onDeleteExpense: (Long) -> Unit
) {
    var filterType by remember { mutableStateOf("All") } // All, Expense, Income
    val listDecimalFormat = remember { DecimalFormat("$#,##0.00") }

    val filtered = when (filterType) {
        "Expense" -> expenses.filter { it.type == "Expense" }
        "Income" -> expenses.filter { it.type == "Income" }
        else -> expenses
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Transactions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // High contrast quick segment selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
            ) {
                listOf("All", "Expense", "Income").forEach { type ->
                    val isSelected = filterType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { filterType = type }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            type,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCC3", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No transaction records fit this filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filtered.sortedByDescending { it.date }.forEach { item ->
                    TransactionItemRow(
                        item = item,
                        format = listDecimalFormat,
                        onDelete = { item.id?.let { onDeleteExpense(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    item: ExpenseItem,
    format: DecimalFormat,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left category visual avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.type == "Income") Color(0xFF0EA5E9).copy(alpha = 0.15f)
                        else Color(0xFFEF4444).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(getCategoryEmoji(item.category), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right price info
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (item.type == "Income") "+" else "-") + format.format(item.amount),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (item.type == "Income") Color(0xFF0EA5E9) else Color(0xFFEF4444)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DebtsTab(
    debts: List<DebtItem>,
    onDeleteDebt: (Long) -> Unit
) {
    val debtFormat = remember { DecimalFormat("$#,##0.00") }

    val creditCards = debts.filter { it.type == "Credit Card" }
    val loans = debts.filter { it.type == "Loan" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CREDIT CARD TRACKER PANEL ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Credit Cards Balance",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💳 ", fontSize = 9.sp)
                    Text(
                        "${creditCards.size} Cards",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        if (creditCards.isEmpty()) {
            Text(
                "No credit card debts configured. Tap the bottom right button to tracking debt.",
                fontSize = 12.sp,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            creditCards.forEach { card ->
                CreditCardModelView(
                    card = card,
                    format = debtFormat,
                    onDelete = { card.id?.let { onDeleteDebt(it) } }
                )
            }
        }

        // --- LOANS PANELS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Personal & Home Loans",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏦 ", fontSize = 9.sp)
                    Text(
                        "${loans.size} Loans",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (loans.isEmpty()) {
            Text(
                "No active loans added yet.",
                fontSize = 12.sp,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            loans.forEach { loan ->
                LoanModelView(
                    loan = loan,
                    format = debtFormat,
                    onDelete = { loan.id?.let { onDeleteDebt(it) } }
                )
            }
        }
    }
}

// Workaround extension for custom color text inside text helper
@Composable
fun Text(text: String, fontSize: androidx.compose.ui.unit.TextUnit, textColor: Color, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = fontSize, color = textColor, modifier = modifier)
}

@Composable
fun CreditCardModelView(
    card: DebtItem,
    format: DecimalFormat,
    onDelete: () -> Unit
) {
    // Stylized luxury modern Credit Card container UI
    val utilRatio = (card.amount / card.limitOrTotal).coerceIn(0.0, 1.0)
    val remainingLimit = card.limitOrTotal - card.amount

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (utilRatio > 0.75) Color(0xFFEF4444).copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.2f
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💳", fontSize = 20.sp)
                    Column {
                        Text(
                            card.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "APR: ${card.interestRate}% • Due: ${card.dueDate}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Debt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Current Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        format.format(card.amount),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Available Limit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        format.format(remainingLimit),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Progress balance indicator
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { utilRatio.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (utilRatio > 0.75) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Utilization: ${(utilRatio * 100).toInt()}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (utilRatio > 0.75) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Limit: ${format.format(card.limitOrTotal)}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LoanModelView(
    loan: DebtItem,
    format: DecimalFormat,
    onDelete: () -> Unit
) {
    val progressRatio = if (loan.limitOrTotal > 0) (loan.paidAmount / loan.limitOrTotal).coerceIn(0.0, 1.0) else 0.0
    val remainingValue = max(0.0, loan.amount)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏦", fontSize = 20.sp)
                    Column {
                        Text(
                            loan.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Interest Rate: ${loan.interestRate}% • Due: ${loan.dueDate}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Loan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Outstanding Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        format.format(remainingValue),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Paid Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        format.format(loan.paidAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Progress towards loan payoffs
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progressRatio.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Payoff progress: ${(progressRatio * 100).toInt()}% Complete",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Original Loan Size: ${format.format(loan.limitOrTotal)}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Dialog for inserting transactions
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, description: String, date: String, type: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Food") }
    var typeSelection by remember { mutableStateOf("Expense") }
    var dateString by remember { mutableStateOf("2026-05-22") }

    val categories = listOf("Food", "Shopping", "Transport", "Bills", "Housing", "Entertainment", "Salary", "Freelance", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Add New Transaction",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Tab Type: Expense vs Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("Expense", "Income").forEach { t ->
                        val isSel = typeSelection == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSel) {
                                        if (t == "Expense") Color(0xFFEF4444) else Color(0xFF0EA5E9)
                                    } else Color.Transparent
                                )
                                .clickable { typeSelection = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category selection chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Category Selection",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = categorySelection == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { categorySelection = cat },
                                label = { Text(cat, fontSize = 11.sp, textColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val parsedAmount = amountInput.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0) {
                                return@Button
                            }
                            if (descriptionInput.isBlank()) {
                                return@Button
                            }
                            onSave(
                                parsedAmount,
                                categorySelection,
                                descriptionInput,
                                dateString,
                                typeSelection
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Dialog for adding credit cards or debt loads
@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, limit: Double, rate: Double, due: String, type: String, paid: Double) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var limitInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("") }
    var dueInput by remember { mutableStateOf("Every 15th") }
    var typeSelector by remember { mutableStateOf("Credit Card") }
    var paidAmountInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Track Debt Asset",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Select Credit Card vs Loan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("Credit Card", "Loan").forEach { t ->
                        val isSel = typeSelector == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable { typeSelector = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (typeSelector == "Credit Card") "Card Name (e.g., Amex Gold)" else "Loan Name (e.g., Auto Loan)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it },
                    label = { Text(if (typeSelector == "Credit Card") "Current Outstanding Balance ($)" else "Remaining Loan Principal ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text(if (typeSelector == "Credit Card") "Credit Card Limit ($)" else "Original Borrowed Capital ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (typeSelector == "Loan") {
                    OutlinedTextField(
                        value = paidAmountInput,
                        onValueChange = { paidAmountInput = it },
                        label = { Text("Paid Amount so far ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Annual Interest Rate (APR %)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = dueInput,
                    onValueChange = { dueInput = it },
                    label = { Text("Due Date Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val parsedBalance = balanceInput.toDoubleOrNull() ?: 0.0
                            val parsedLimit = limitInput.toDoubleOrNull() ?: parsedBalance
                            val parsedRate = rateInput.toDoubleOrNull() ?: 0.0
                            val parsedPaid = paidAmountInput.toDoubleOrNull() ?: 0.0

                            if (nameInput.isBlank() || parsedLimit <= 0) {
                                return@Button
                            }

                            onSave(
                                nameInput,
                                parsedBalance,
                                parsedLimit,
                                parsedRate,
                                dueInput,
                                typeSelector,
                                parsedPaid
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Dialog explaining how to set up files in Supabase SQL editor with raw code
@Composable
fun SqlSchemaGuideDialog(
    onDismiss: () -> Unit
) {
    val sqlCode = """
-- FinTrack SQL Setup Schema Script
-- Paste this script directly inside your Supabase.com Project SQL Editor!

-- 1. Create expenses table
create table if not exists public.expenses (
    id bigint generated by default as identity primary key,
    amount double precision not null,
    category text not null,
    description text not null,
    date text not null,
    type text not null check (type in ('Expense', 'Income')),
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 2. Create debts table (Credit Cards & Loans)
create table if not exists public.debts (
    id bigint generated by default as identity primary key,
    name text not null,
    amount double precision not null,
    limit_or_total double precision not null,
    interest_rate double precision not null,
    due_date text not null,
    type text not null check (type in ('Credit Card', 'Loan')),
    paid_amount double precision default 0.0 not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Enable reading permissions for anon access on both tables
alter table public.expenses enable row level security;
alter table public.debts enable row level security;

create policy "Allow read on expenses" on public.expenses for select using (true);
create policy "Allow insert on expenses" on public.expenses for insert with check (true);
create policy "Allow delete on expenses" on public.expenses for delete using (true);

create policy "Allow read on debts" on public.debts for select using (true);
create policy "Allow insert on debts" on public.debts for insert with check (true);
create policy "Allow delete on debts" on public.debts for delete using (true);
    """.trimIndent()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Supabase Bootstrapper",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close description")
                    }
                }

                Text(
                    "FinTrack is ready! Run this SQL script in your Supabase project to immediately link database tables for cloud sync.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // The SQL raw text
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    Text(
                        sqlCode,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFF10B981)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(sqlCode))
                            Toast.makeText(context, "SQL copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Code")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

// Helpers
private fun getCategoryEmoji(catName: String): String {
    return when (catName.lowercase()) {
        "food" -> "🍔"
        "shopping" -> "🛍️"
        "transport" -> "🚗"
        "bills" -> "⚡"
        "housing" -> "🏠"
        "entertainment" -> "🎬"
        "salary" -> "💵"
        "freelance" -> "💻"
        "credit card" -> "💳"
        "loan" -> "🏦"
        else -> "📦"
    }
}

// Custom flow-row container helper
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
