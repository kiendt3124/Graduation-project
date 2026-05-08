package com.uet.expensetracker.features.money.data.data_source.local.db

import androidx.room.TypeConverter
import com.uet.expensetracker.features.money.domain.model.TransactionType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromByteArray(byteArray: ByteArray?): String? {
        return byteArray?.toString(Charsets.ISO_8859_1)
    }

    @TypeConverter
    fun toByteArray(string: String?): ByteArray? {
        return string?.toByteArray(Charsets.ISO_8859_1)
    }
    
    @TypeConverter
    fun fromDate(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.valueOf(it) }
    }
}