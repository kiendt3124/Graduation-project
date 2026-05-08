package com.uet.expensetracker.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for currency conversion using exchange rates from exchanger.json
 */
@Singleton
class CurrencyConverter @Inject constructor(@ApplicationContext private val context: Context) {

    private var exchangeRates: ExchangeRateData? = null
    
    /**
     * Initialize the converter by loading exchange rates from JSON file
     */
    suspend fun initialize() {
        if (exchangeRates == null) {
            loadExchangeRates()
        }
    }

    /**
     * Convert amount from one currency to another
     * @param amount The amount to convert
     * @param fromCurrency The source currency code (e.g., "USD")
     * @param toCurrency The target currency code (e.g., "EUR")
     * @return The converted amount or null if conversion failed
     */
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double? {
        if (exchangeRates == null) {
            loadExchangeRates()
        }
        
        // If rates couldn't be loaded, return null
        val rates = exchangeRates?.rates ?: return null
        
        // If source and target currencies are the same, no conversion needed
        if (fromCurrency == toCurrency) {
            return amount
        }
        
        // Get the exchange rates for the source and target currencies
        val fromRate = rates[fromCurrency] ?: return null
        val toRate = rates[toCurrency] ?: return null
        
        // Convert to USD first (base currency), then to target currency
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }
    
    /**
     * Get the exchange rate between two currencies
     * @param fromCurrency The source currency code
     * @param toCurrency The target currency code
     * @return The exchange rate or null if not available
     */
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String): Double? {
        if (exchangeRates == null) {
            loadExchangeRates()
        }
        
        val rates = exchangeRates?.rates ?: return null
        
        // If source and target currencies are the same, rate is 1
        if (fromCurrency == toCurrency) {
            return 1.0
        }
        
        // Get the exchange rates for the source and target currencies
        val fromRate = rates[fromCurrency] ?: return null
        val toRate = rates[toCurrency] ?: return null
        
        // Calculate the exchange rate
        return toRate / fromRate
    }

    /**
     * Load exchange rates from the JSON file
     */
    private suspend fun loadExchangeRates() = withContext(Dispatchers.IO) {
        try {
            context.assets.open("exchanger.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                exchangeRates = Gson().fromJson(reader, ExchangeRateData::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exchangeRates = null
        }
    }
    
    /**
     * Check if exchange rates are available
     */
    fun hasExchangeRates(): Boolean {
        return exchangeRates != null
    }
    
    /**
     * Get a formatted string representation of the exchange rate
     */
    suspend fun getFormattedExchangeRate(fromCurrency: String, toCurrency: String): String? {
        val rate = getExchangeRate(fromCurrency, toCurrency) ?: return null
        return String.format(java.util.Locale.US, "1 %s = %.4f %s", fromCurrency, rate, toCurrency)
    }
}

/**
 * Data class for parsing exchange rate data from JSON
 */
data class ExchangeRateData(
    @SerializedName("disclaimer") val disclaimer: String,
    @SerializedName("license") val license: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("base") val base: String,
    @SerializedName("rates") val rates: Map<String, Double>
) 