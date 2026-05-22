package com.example.data

import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {
    @GET("rest/v1/expenses")
    suspend fun getExpenses(
        @Query("select") select: String = "*"
    ): List<ExpenseItem>

    @POST("rest/v1/expenses")
    suspend fun createExpense(
        @Body expense: ExpenseItem,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<ExpenseItem>

    @DELETE("rest/v1/expenses")
    suspend fun deleteExpense(
        @Query("id") idFilter: String // e.g., "eq.10"
    ): Response<Unit>

    @GET("rest/v1/debts")
    suspend fun getDebts(
        @Query("select") select: String = "*"
    ): List<DebtItem>

    @POST("rest/v1/debts")
    suspend fun createDebt(
        @Body debt: DebtItem,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<DebtItem>

    @DELETE("rest/v1/debts")
    suspend fun deleteDebt(
        @Query("id") idFilter: String // e.g., "eq.10"
    ): Response<Unit>
}
