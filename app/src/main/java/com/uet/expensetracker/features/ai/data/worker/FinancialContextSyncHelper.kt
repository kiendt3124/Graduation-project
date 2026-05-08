package com.uet.expensetracker.features.ai.data.worker

import com.uet.expensetracker.features.ai.data.context.FinancialContextBuilder
import com.uet.expensetracker.features.ai.data.remote.AiApiService
import com.uet.expensetracker.features.ai.data.remote.dto.context.FinancialContextDto

object FinancialContextSyncHelper {
    private var builder: FinancialContextBuilder? = null
    private var apiService: AiApiService? = null
    
    fun initialize(builder: FinancialContextBuilder, apiService: AiApiService) {
        this.builder = builder
        this.apiService = apiService
    }
    
    suspend fun syncContext(): FinancialContextDto? {
        val contextBuilder = builder ?: return null
        val api = apiService ?: return null
        
        val context = contextBuilder.build()
        api.syncContext(context)
        return context
    }
}
