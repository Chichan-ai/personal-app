package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File

class FinanceRepository(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val expenseListType = Types.newParameterizedType(List::class.java, ExpenseItem::class.java)
    private val debtListType = Types.newParameterizedType(List::class.java, DebtItem::class.java)

    private val expenseAdapter = moshi.adapter<List<ExpenseItem>>(expenseListType)
    private val debtAdapter = moshi.adapter<List<DebtItem>>(debtListType)

    private val expensesFile = File(context.filesDir, "expenses_v1.json")
    private val debtsFile = File(context.filesDir, "debts_v1.json")

    // Retrieve from BuildConfig gracefully
    private val supabaseUrl: String = try {
        val url = BuildConfig.SUPABASE_URL
        if (url.endsWith("/")) url else "$url/"
    } catch (e: Exception) {
        "https://placeholder.supabase.co/"
    }

    private val supabaseKey: String = try {
        BuildConfig.SUPABASE_KEY
    } catch (e: Exception) {
        ""
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $supabaseKey")
                    .header("Content-Type", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    private val api: SupabaseApi? by lazy {
        if (supabaseUrl.startsWith("https://placeholder") || supabaseKey.isEmpty()) {
            null
        } else {
            try {
                Retrofit.Builder()
                    .baseUrl(supabaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseApi::class.java)
            } catch (e: Exception) {
                Log.e("FinanceRepository", "Failed to construct Retrofit: ${e.message}")
                null
            }
        }
    }

    // --- LOCAL DB ACCESSORS ---
    fun getLocalExpenses(): List<ExpenseItem> {
        if (!expensesFile.exists()) {
            val seeded = getSeededExpenses()
            saveLocalExpenses(seeded)
            return seeded
        }
        return try {
            val json = expensesFile.readText()
            expenseAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error reading local expenses: ${e.message}")
            emptyList()
        }
    }

    fun saveLocalExpenses(list: List<ExpenseItem>) {
        try {
            val json = expenseAdapter.toJson(list)
            expensesFile.writeText(json)
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error writing local expenses: ${e.message}")
        }
    }

    fun getLocalDebts(): List<DebtItem> {
        if (!debtsFile.exists()) {
            val seeded = getSeededDebts()
            saveLocalDebts(seeded)
            return seeded
        }
        return try {
            val json = debtsFile.readText()
            debtAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error reading local debts: ${e.message}")
            emptyList()
        }
    }

    fun saveLocalDebts(list: List<DebtItem>) {
        try {
            val json = debtAdapter.toJson(list)
            debtsFile.writeText(json)
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error writing local debts: ${e.message}")
        }
    }

    // --- SUPABASE SYNCHRONIZATION ---
    suspend fun syncFromSupabase(): Result<SyncOutcome> {
        val currentApi = api ?: return Result.failure(Exception("Supabase credentials are missing or invalid in Secrets."))

        return try {
            val remoteExpenses = currentApi.getExpenses()
            saveLocalExpenses(remoteExpenses)

            val remoteDebts = currentApi.getDebts()
            saveLocalDebts(remoteDebts)

            Result.success(SyncOutcome(remoteExpenses, remoteDebts))
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Supabase sync error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadExpense(item: ExpenseItem): Result<ExpenseItem> {
        val currentApi = api ?: return saveLocalExpenseAndMockSuccess(item, "Supabase API client unavailable.")
        return try {
            val resList = currentApi.createExpense(item)
            if (resList.isNotEmpty()) {
                val updatedLocal = getLocalExpenses().toMutableList()
                val returned = resList[0]
                updatedLocal.removeAll { it.id == returned.id }
                updatedLocal.add(returned)
                saveLocalExpenses(updatedLocal)
                Result.success(returned)
            } else {
                saveLocalExpenseAndMockSuccess(item, "No data returned on Insert.")
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Supabase post expense error: ${e.message}")
            saveLocalExpenseAndMockSuccess(item, e.message ?: "Supabase table write failed.")
        }
    }

    private fun saveLocalExpenseAndMockSuccess(item: ExpenseItem, reason: String): Result<ExpenseItem> {
        val localItem = if (item.id == null) {
            item.copy(id = System.currentTimeMillis())
        } else item
        val updatedLocal = getLocalExpenses().toMutableList()
        updatedLocal.add(localItem)
        saveLocalExpenses(updatedLocal)
        return Result.success(localItem) // Succeed locally so app flow stays smooth
    }

    suspend fun removeExpense(id: Long): Result<Unit> {
        val currentApi = api
        val updatedLocal = getLocalExpenses().filterNot { it.id == id }
        saveLocalExpenses(updatedLocal)

        if (currentApi == null) {
            return Result.success(Unit)
        }
        return try {
            val res = currentApi.deleteExpense("eq.$id")
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.success(Unit) // Local delete is sufficient
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "API delete error: ${e.message}")
            Result.success(Unit) // Keep UX responsive even if server link is broken
        }
    }

    suspend fun uploadDebt(item: DebtItem): Result<DebtItem> {
        val currentApi = api ?: return saveLocalDebtAndMockSuccess(item)
        return try {
            val resList = currentApi.createDebt(item)
            if (resList.isNotEmpty()) {
                val updatedLocal = getLocalDebts().toMutableList()
                val returned = resList[0]
                updatedLocal.removeAll { it.id == returned.id }
                updatedLocal.add(returned)
                saveLocalDebts(updatedLocal)
                Result.success(returned)
            } else {
                saveLocalDebtAndMockSuccess(item)
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Supabase post debt error: ${e.message}")
            saveLocalDebtAndMockSuccess(item)
        }
    }

    private fun saveLocalDebtAndMockSuccess(item: DebtItem): Result<DebtItem> {
        val localItem = if (item.id == null) {
            item.copy(id = System.currentTimeMillis())
        } else item
        val updatedLocal = getLocalDebts().toMutableList()
        updatedLocal.add(localItem)
        saveLocalDebts(updatedLocal)
        return Result.success(localItem)
    }

    suspend fun removeDebt(id: Long): Result<Unit> {
        val currentApi = api
        val updatedLocal = getLocalDebts().filterNot { it.id == id }
        saveLocalDebts(updatedLocal)

        if (currentApi == null) {
            return Result.success(Unit)
        }
        return try {
            val res = currentApi.deleteDebt("eq.$id")
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "API delete debt error: ${e.message}")
            Result.success(Unit)
        }
    }

    private fun getSeededExpenses(): List<ExpenseItem> {
        return listOf(
            ExpenseItem(1, 1200.00, "Housing", "Monthly Appartment Rent", "2026-05-01", "Expense"),
            ExpenseItem(2, 65.50, "Food", "Supermarket Groceries & Ingredients", "2026-05-18", "Expense"),
            ExpenseItem(3, 15.50, "Transport", "City Subway Pass Ticket", "2026-05-20", "Expense"),
            ExpenseItem(4, 3800.00, "Salary", "Main Job Monthly Salary", "2026-05-01", "Income"),
            ExpenseItem(5, 88.00, "Bills", "Electricity and Heating Bill", "2026-05-10", "Expense"),
            ExpenseItem(6, 45.00, "Entertainment", "Movie and Dinner with Friends", "2026-05-21", "Expense"),
            ExpenseItem(7, 450.00, "Freelance", "Mobile App UI Design Consulting", "2026-05-15", "Income")
        )
    }

    private fun getSeededDebts(): List<DebtItem> {
        return listOf(
            DebtItem(1, "Chase Sapphire Reserve", 1450.00, 10000.00, 21.49, "Every 12th", "Credit Card"),
            DebtItem(2, "Federal Student Loan", 18200.00, 25000.00, 4.53, "Every 1st", "Loan", 6800.00),
            DebtItem(3, "Amex Gold Card", 820.50, 15000.00, 24.24, "Every 25th", "Credit Card"),
            DebtItem(4, "Kia Auto Loan", 8400.00, 12000.00, 3.99, "Every 14th", "Loan", 3600.00)
        )
    }
}

data class SyncOutcome(
    val expenses: List<ExpenseItem>,
    val debts: List<DebtItem>
)
