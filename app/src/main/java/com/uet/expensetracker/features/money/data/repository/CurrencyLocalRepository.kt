package com.uet.expensetracker.features.money.data.repository

import com.uet.expensetracker.features.money.data.data_source.local.dao.CurrencyDao
import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CurrencyLocalRepository @Inject constructor(
    private val currencyDao: CurrencyDao
) : CurrencyRepository {

    override fun getAllCurrencies(): Flow<List<CurrencyEntity>> {
        return currencyDao.getAllCurrencies()
    }

    override suspend fun insertCurrencies(currencies: List<Currency>) {
        val currencyEntities = currencies.map { it.toCurrencyEntity() }
        currencyDao.insertAllCurrencies(currencyEntities)
    }

    override suspend fun getCurrencyByCode(code: String): CurrencyEntity? {
        return currencyDao.getCurrencyByCode(code)
    }

    override suspend fun getCurrenciesCount(): Int {
        return currencyDao.getCurrenciesCount()
    }
}