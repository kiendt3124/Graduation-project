package com.uet.expensetracker.features.money.domain.usecases

import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.DebtCategoryMetadata
import com.uet.expensetracker.features.money.domain.model.PaymentRecord
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting payment history for a specific debt (as Flow for live updates)
 */
class GetDebtPaymentHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<PaymentRecord>>, GetDebtPaymentHistoryUseCase.Params>() {

    companion object {
        private const val TAG = "GetDebtPaymentHistoryUseCase"
    }

    override suspend fun run(params: Params): Either<Failure, Flow<List<PaymentRecord>>> {
        return try {
            Log.d(TAG, "Getting payment history for debt: ${params.debtReference}")

            // Chọn source Flow theo debtReference; fallback parentDebtId
            val sourceFlow = if (params.debtReference.isNotBlank()) {
                transactionRepository.getTransactionsByDebtReference(params.debtReference)
            } else {
                transactionRepository.getTransactionsByParentDebtId(params.originalDebtId)
            }

            val paymentCategories = DebtCategoryMetadata.PAYABLE_REPAYMENT +
                    DebtCategoryMetadata.RECEIVABLE_REPAYMENT

            val mappedFlow = sourceFlow.map { relatedTransactions ->
                relatedTransactions
                    .filter { paymentCategories.contains(it.category.metaData) }
                    .map { transaction ->
                        PaymentRecord(
                            date = transaction.transactionDate,
                            amount = transaction.amount,
                            description = transaction.description,
                            transactionId = transaction.id
                        )
                    }
                    // Hiển thị mới nhất trước
                    .sortedByDescending { it.date }
            }

            Either.Right(mappedFlow)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting payment history", e)
            Either.Left(Failure.ServerError)
        }
    }

    data class Params(
        val originalDebtId: Int,
        val debtReference: String = ""
    )
}