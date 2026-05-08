package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure

/**
 * Custom failure types for search operations
 */
sealed class SearchFailure : Failure.FeatureFailure() {
    object InvalidAmountRange : SearchFailure()
    object InvalidDateRange : SearchFailure()
    object EmptySearchQuery : SearchFailure()
    object SearchTextTooShort : SearchFailure()
} 