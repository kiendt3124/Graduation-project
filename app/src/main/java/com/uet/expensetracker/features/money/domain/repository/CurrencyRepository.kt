package com.uet.expensetracker.features.money.domain.repository

import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun getAllCurrencies(): Flow<List<CurrencyEntity>>
    suspend fun insertCurrencies(currencies: List<Currency>)
    suspend fun getCurrencyByCode(code: String): CurrencyEntity?
    suspend fun getCurrenciesCount(): Int
}