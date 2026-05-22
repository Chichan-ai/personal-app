package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExpenseItem(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String,
    @Json(name = "date") val date: String, // format "YYYY-MM-DD"
    @Json(name = "type") val type: String // "Expense" or "Income"
)

@JsonClass(generateAdapter = true)
data class DebtItem(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String,
    @Json(name = "amount") val amount: Double, // current outstanding balance
    @Json(name = "limit_or_total") val limitOrTotal: Double, // total credit limit or original loan size
    @Json(name = "interest_rate") val interestRate: Double, // annual interest rate, e.g., 18.5
    @Json(name = "due_date") val dueDate: String, // e.g., "Every 15th" or "2026-06-15"
    @Json(name = "type") val type: String, // "Credit Card" or "Loan"
    @Json(name = "paid_amount") val paidAmount: Double = 0.0 // amount paid off (for loans)
)
