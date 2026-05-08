package com.uet.expensetracker.features.money.data.data_source.local

import android.content.Context
import com.uet.expensetracker.features.money.domain.model.Currency
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

class CurrencyDataSource {

    fun loadCurrencies(context: Context): List<Currency> {
        val jsonString: String? = try {
            val inputStream = context.assets.open("currency.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            null
        }

        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            val dataArray = jsonObject.getJSONArray("data")
            val currencies = mutableListOf<Currency>()
            for (i in 0 until dataArray.length()) {
                val currencyJson = dataArray.getJSONObject(i)
                // Use getInt, getString, etc. with fallback values or checks
                val id = currencyJson.optInt("i", -1) // Use optInt for safety
                val name = currencyJson.optString("n", "")
                val code = currencyJson.optString("c", "")
                val symbol = currencyJson.optString("s", "")
                // Ensure 't' exists and is an integer before parsing
                val displayTypeInt =
                    currencyJson.optInt("t", 0) // Default to 0 if not found or not an int
                val displayType =
                    displayTypeInt.toString() // Convert to String as per Currency model

                if (id != -1 && name.isNotEmpty() && code.isNotEmpty()) {
                    currencies.add(
                        Currency(
                            id = id,
                            currencyName = name,
                            currencyCode = code,
                            symbol = symbol,
                            displayType = displayType,
                            image = null // Image loading will be handled in UI based on code
                        )
                    )
                }
            }
            return currencies
        }
        return emptyList()
    }
}
