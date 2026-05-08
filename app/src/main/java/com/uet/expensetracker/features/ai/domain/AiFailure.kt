package com.uet.expensetracker.features.ai.domain

import com.uet.expensetracker.core.failure.Failure

sealed class AiFailure : Failure.FeatureFailure() {
    data class Network(val message: String) : AiFailure()
    data class Server(val message: String) : AiFailure()
    data class Parsing(val message: String) : AiFailure()
    data class Unknown(val message: String) : AiFailure()
}


