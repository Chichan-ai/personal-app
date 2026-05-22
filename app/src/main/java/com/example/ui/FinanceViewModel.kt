package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DebtItem
import com.example.data.ExpenseItem
import com.example.data.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository(application)

    private val _expenses = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val expenses: StateFlow<List<ExpenseItem>> = _expenses.asStateFlow()

    private val _debts = MutableStateFlow<List<DebtItem>>(emptyList())
    val debts: StateFlow<List<DebtItem>> = _debts.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Dashboard, 1: Expenses, 2: Debts & Loans
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    init {
        // First load from local copy so UI gets instant content
        loadLocalData()
        // Trigger sync automatically on launch
        triggerSync()
    }

    private fun loadLocalData() {
        _expenses.value = repository.getLocalExpenses()
        _debts.value = repository.getLocalDebts()
    }

    fun setTab(index: Int) {
        _activeTab.value = index
    }

    fun triggerSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncError.value = null
            
            val result = repository.syncFromSupabase()
            if (result.isSuccess) {
                val outcome = result.getOrNull()
                if (outcome != null) {
                    _expenses.value = outcome.expenses
                    _debts.value = outcome.debts
                }
            } else {
                val error = result.exceptionOrNull()
                val message = error?.message ?: "Unknown Supabase sync error"
                // Parse if 404 or connection
                _syncError.value = when {
                    message.contains("404") || message.contains("not found") -> {
                        "Supabase tables ('expenses' or 'debts') not found in your database. Click 'Setup DB' to view the quick schema script!"
                    }
                    message.contains("401") || message.contains("Unauthorized") || message.contains("JWT") -> {
                        "Supabase API key is invalid or unauthorized. Please verify your Anon Key."
                    }
                    message.contains("ConnectException") || message.contains("Unable to resolve host") -> {
                        "Offline Mode: Network connection unavailable. Using locally stored data."
                    }
                    else -> {
                        "Unable to sync with Supabase: $message. Check credentials or connection."
                    }
                }
            }
            _isSyncing.value = false
        }
    }

    fun addNewExpense(
        amount: Double,
        category: String,
        description: String,
        date: String,
        type: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newItem = ExpenseItem(
                amount = amount,
                category = category,
                description = description,
                date = date,
                type = type
            )
            repository.uploadExpense(newItem)
            loadLocalData() // Refresh live stats from local copy
        }
    }

    fun deleteExpenseItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeExpense(id)
            loadLocalData()
        }
    }

    fun addNewDebt(
        name: String,
        amount: Double,
        limitOrTotal: Double,
        interestRate: Double,
        dueDate: String,
        type: String,
        paidAmount: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newItem = DebtItem(
                name = name,
                amount = amount,
                limitOrTotal = limitOrTotal,
                interestRate = interestRate,
                dueDate = dueDate,
                type = type,
                paidAmount = paidAmount
            )
            repository.uploadDebt(newItem)
            loadLocalData()
        }
    }

    fun deleteDebtItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDebt(id)
            loadLocalData()
        }
    }
}
