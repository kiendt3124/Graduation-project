package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionsByCategoryAndWalletUseCase
import com.uet.expensetracker.utils.CurrencyConverter
import javax.inject.Inject

class GetBudgetTransactionsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val getTxnsByCategoryAndWallet: GetTransactionsByCategoryAndWalletUseCase,
    private val converter: CurrencyConverter,
) : UseCase<Flow<List<DailyTransactions>>, GetBudgetTransactionsUseCase.Params>() {

    data class Params(val budgetId: Int)

    override suspend fun run(params: Params): Either<Failure, Flow<List<DailyTransactions>>> {
        return try {
            converter.initialize()
            val budget = budgetRepository.getBudgetById(params.budgetId)
                ?: return Either.Left(Failure.NotFound)

            when (val eitherFlow = getTxnsByCategoryAndWallet.run(
                GetTransactionsByCategoryAndWalletUseCase.Params(
                    categoryId = budget.category.id,
                    walletId = if(budget.wallet.id == -1) {
                        null // null for all wallet
                    } else {
                        budget.wallet.id
                    }
                )
            )) {
                is Either.Left -> Either.Left(eitherFlow.left)
                is Either.Right -> {
                    val txnsFlow = eitherFlow.right
                    val groupedFlow = txnsFlow.map { txns ->
                        // filter by budget date range
                        txns.filter { txn ->
                            txn.transactionDate.time >= budget.fromDate.time &&
                            txn.transactionDate.time <= budget.endDate.time
                        }
                    }.map { filteredTxns ->
                        val calendar = Calendar.getInstance()
                        filteredTxns.groupBy { txn ->
                            calendar.time = txn.transactionDate
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            calendar.timeInMillis
                        }.map { (dayMillis, list) ->
                            val date = Date(dayMillis)
                            val total = list.fold(0.0) { acc, tx ->

                               (if (tx.transactionType == TransactionType.INFLOW) {
                                   acc +  if(tx.wallet.currency.currencyCode == budget.wallet.currency.currencyCode) {
                                        tx.amount
                                    } else {
                                        converter.convert(tx.amount, tx.wallet.currency.currencyCode, budget.wallet.currency.currencyCode) ?: 0.0
                                    }
                                } else {
                                   acc - if(tx.wallet.currency.currencyCode == budget.wallet.currency.currencyCode) {
                                        tx.amount
                                    } else {
                                        converter.convert(tx.amount, tx.wallet.currency.currencyCode, budget.wallet.currency.currencyCode)?.minus(1) ?: 0.0
                                    }
                                } ?: 0.0)



//                                acc + (if (tx.wallet.currency.currencyCode == budget.wallet.currency.currencyCode) {
//                                    if (tx.transactionType == TransactionType.INFLOW) tx.amount else -tx.amount
//                                } else {
//                                    if (tx.transactionType == TransactionType.INFLOW) {
//                                        converter.convert(tx.amount, tx.wallet.currency.currencyCode, budget.wallet.currency.currencyCode)
//                                    } else {
//                                        converter.convert(tx.amount, tx.wallet.currency.currencyCode, budget.wallet.currency.currencyCode)?.minus(-1)
//                                    } ?: 0.0
//                                } ?: 0.0)

                            }
                            DailyTransactions.create(date, list, total)
                        }.sortedByDescending { it.date.time }
                    }
                    Either.Right(groupedFlow)
                }
            }
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
} 