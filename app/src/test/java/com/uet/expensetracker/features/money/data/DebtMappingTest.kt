package com.uet.expensetracker.features.money.data

import com.uet.expensetracker.features.money.data.mapper.DebtMapper
import com.uet.expensetracker.features.money.data.mapper.DebtSortBy
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.DebtCategoryMetadata
import com.uet.expensetracker.features.money.domain.model.DebtInfo
import com.uet.expensetracker.features.money.domain.model.DebtPerson
import com.uet.expensetracker.features.money.domain.model.DebtType
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

/**
 * Unit tests for debt mapping functionality
 * Tests JSON parsing, debt calculations, and data transformations
 */
class DebtMappingTest {

    private val testWallet = Wallet(
        id = 1,
        name = "Test Wallet",
        balance = 1000.0,
        currency = Currency(1, "USD", "$"),
        isSelected = true
    )

    private val debtCategory = Category(
        id = 1,
        name = "Debt",
        metaData = DebtCategoryMetadata.DEBT,
        icon = "debt_icon",
        type = TransactionType.INCOME
    )

    private val loanCategory = Category(
        id = 2,
        name = "Loan",
        metaData = DebtCategoryMetadata.LOAN,
        icon = "loan_icon",
        type = TransactionType.EXPENSE
    )

    @Test
    fun `parseWithPersonJson should parse valid JSON correctly`() {
        val json = """[{"id":"1","name":"John Doe","initial":"J"},{"id":"2","name":"Mary Smith","initial":"M"}]"""
        
        val result = DebtMapper.parseWithPersonJson(json)
        
        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("John Doe", result[0].name)
        assertEquals("J", result[0].initial)
        assertEquals("2", result[1].id)
        assertEquals("Mary Smith", result[1].name)
        assertEquals("M", result[1].initial)
    }

    @Test
    fun `parseWithPersonJson should return empty list for invalid JSON`() {
        val invalidJson = """{"invalid": "json"}"""
        
        val result = DebtMapper.parseWithPersonJson(invalidJson)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseWithPersonJson should return empty list for null or blank input`() {
        assertEquals(emptyList<DebtPerson>(), DebtMapper.parseWithPersonJson(null))
        assertEquals(emptyList<DebtPerson>(), DebtMapper.parseWithPersonJson(""))
        assertEquals(emptyList<DebtPerson>(), DebtMapper.parseWithPersonJson("   "))
    }

    @Test
    fun `debtPersonListToJson should convert list to JSON correctly`() {
        val debtPersons = listOf(
            DebtPerson("1", "John Doe", "J"),
            DebtPerson("2", "Mary Smith", "M")
        )
        
        val result = DebtMapper.debtPersonListToJson(debtPersons)
        
        assertTrue(result.contains("John Doe"))
        assertTrue(result.contains("Mary Smith"))
        assertTrue(result.contains("\"id\":\"1\""))
        assertTrue(result.contains("\"id\":\"2\""))
    }

    @Test
    fun `parseDebtMetadata should parse valid JSON correctly`() {
        val json = """{"isPartialPayment":true,"originalDebtId":"DEBT_123","notes":"Test note"}"""
        
        val result = DebtMapper.parseDebtMetadata(json)
        
        assertNotNull(result)
        assertEquals(true, result?.isPartialPayment)
        assertEquals("DEBT_123", result?.originalDebtId)
        assertEquals("Test note", result?.notes)
    }

    @Test
    fun `parseDebtMetadata should return null for invalid JSON`() {
        val invalidJson = """{"invalid": json}"""
        
        val result = DebtMapper.parseDebtMetadata(invalidJson)
        
        assertNull(result)
    }

    @Test
    fun `debtInfoToJson should convert DebtInfo to JSON correctly`() {
        val debtInfo = DebtInfo(
            isPartialPayment = true,
            originalDebtId = "DEBT_123",
            notes = "Test note"
        )
        
        val result = DebtMapper.debtInfoToJson(debtInfo)
        
        assertTrue(result.contains("isPartialPayment"))
        assertTrue(result.contains("DEBT_123"))
        assertTrue(result.contains("Test note"))
    }

    @Test
    fun `determineDebtType should return correct type for each category`() {
        assertEquals(DebtType.RECEIVABLE, DebtMapper.determineDebtType(DebtCategoryMetadata.DEBT))
        assertEquals(DebtType.RECEIVABLE, DebtMapper.determineDebtType(DebtCategoryMetadata.DEBT_COLLECTION))
        assertEquals(DebtType.RECEIVABLE, DebtMapper.determineDebtType(DebtCategoryMetadata.COLLECT_INTEREST))
        assertEquals(DebtType.PAYABLE, DebtMapper.determineDebtType(DebtCategoryMetadata.LOAN))
        assertEquals(DebtType.PAYABLE, DebtMapper.determineDebtType(DebtCategoryMetadata.REPAYMENT))
        assertEquals(DebtType.PAYABLE, DebtMapper.determineDebtType(DebtCategoryMetadata.PAY_INTEREST))
        assertEquals(DebtType.RECEIVABLE, DebtMapper.determineDebtType("UNKNOWN"))
    }

    @Test
    fun `calculateTotalDebtAmount should calculate correctly`() {
        val transactions = listOf(
            createTestTransaction(debtCategory, 1000.0), // Debt (money lent)
            createTestTransaction(loanCategory, 500.0),  // Loan (money borrowed)
            createTestTransaction(
                debtCategory.copy(metaData = DebtCategoryMetadata.DEBT_COLLECTION),
                200.0
            ), // Collection (money received back)
            createTestTransaction(
                loanCategory.copy(metaData = DebtCategoryMetadata.REPAYMENT),
                100.0
            )  // Repayment (money paid back)
        )
        
        val result = DebtMapper.calculateTotalDebtAmount(transactions)
        
        // 1000 + 500 - 200 - 100 = 1200
        assertEquals(1200.0, result, 0.01)
    }

    @Test
    fun `calculateRemainingDebtAmount should calculate correctly`() {
        val originalAmount = 1000.0
        val paymentTransactions = listOf(
            createTestTransaction(debtCategory, 200.0),
            createTestTransaction(debtCategory, 150.0)
        )
        
        val result = DebtMapper.calculateRemainingDebtAmount(originalAmount, paymentTransactions)
        
        assertEquals(650.0, result, 0.01)
    }

    @Test
    fun `createPaymentRecords should convert transactions correctly`() {
        val transactions = listOf(
            createTestTransaction(debtCategory, 200.0).copy(description = "Payment 1"),
            createTestTransaction(debtCategory, 150.0).copy(description = "Payment 2")
        )
        
        val result = DebtMapper.createPaymentRecords(transactions)
        
        assertEquals(2, result.size)
        assertEquals(200.0, result[0].amount, 0.01)
        assertEquals("Payment 1", result[0].description)
        assertEquals(150.0, result[1].amount, 0.01)
        assertEquals("Payment 2", result[1].description)
    }

    @Test
    fun `generateDebtReference should create unique references`() {
        val personId = "12345"
        val timestamp = System.currentTimeMillis()
        
        val ref1 = DebtMapper.generateDebtReference(personId, timestamp)
        val ref2 = DebtMapper.generateDebtReference(personId, timestamp)
        
        assertNotEquals(ref1, ref2)
        assertTrue(ref1.startsWith("DEBT_${personId}_${timestamp}"))
        assertTrue(ref2.startsWith("DEBT_${personId}_${timestamp}"))
    }

    @Test
    fun `groupDebtTransactionsByPersonAndType should group correctly`() {
        val johnDebt = createTestTransaction(debtCategory, 1000.0).copy(
            withPerson = """[{"id":"john","name":"John Doe","initial":"J"}]"""
        )
        val johnLoan = createTestTransaction(loanCategory, 500.0).copy(
            withPerson = """[{"id":"john","name":"John Doe","initial":"J"}]"""
        )
        val maryDebt = createTestTransaction(debtCategory, 800.0).copy(
            withPerson = """[{"id":"mary","name":"Mary Smith","initial":"M"}]"""
        )
        
        val transactions = listOf(johnDebt, johnLoan, maryDebt)
        
        val result = DebtMapper.groupDebtTransactionsByPersonAndType(transactions)
        
        assertEquals(3, result.size)
        assertTrue(result.containsKey(Pair("john", DebtType.RECEIVABLE)))
        assertTrue(result.containsKey(Pair("john", DebtType.PAYABLE)))
        assertTrue(result.containsKey(Pair("mary", DebtType.RECEIVABLE)))
        
        assertEquals(1, result[Pair("john", DebtType.RECEIVABLE)]?.size)
        assertEquals(1, result[Pair("john", DebtType.PAYABLE)]?.size)
        assertEquals(1, result[Pair("mary", DebtType.RECEIVABLE)]?.size)
    }

    @Test
    fun `createDebtSummary should create correct summary`() {
        val originalTransaction = createTestTransaction(debtCategory, 1000.0)
        val paymentTransaction1 = createTestTransaction(
            debtCategory.copy(metaData = DebtCategoryMetadata.DEBT_COLLECTION),
            200.0
        )
        val paymentTransaction2 = createTestTransaction(
            debtCategory.copy(metaData = DebtCategoryMetadata.DEBT_COLLECTION),
            150.0
        )
        
        val transactions = listOf(originalTransaction, paymentTransaction1, paymentTransaction2)
        
        val result = DebtMapper.createDebtSummary(
            personId = "john",
            personName = "John Doe",
            transactions = transactions
        )
        
        assertEquals("john", result.personId)
        assertEquals("John Doe", result.personName)
        assertEquals(1000.0, result.totalAmount, 0.01)
        assertEquals(350.0, result.paidAmount, 0.01)
        assertEquals(650.0, result.remainingAmount, 0.01)
        assertFalse(result.isPaid)
        assertEquals(2, result.paymentHistory.size)
        assertEquals(2, result.repaymentTransactions.size)
    }

    @Test
    fun `filterDebtsByStatus should filter correctly`() {
        val paidDebt = createTestDebtSummary("john", 0.0) // Fully paid
        val unpaidDebt = createTestDebtSummary("mary", 500.0) // Unpaid
        val summaries = listOf(paidDebt, unpaidDebt)
        
        val paidResults = DebtMapper.filterDebtsByStatus(summaries, isPaid = true)
        val unpaidResults = DebtMapper.filterDebtsByStatus(summaries, isPaid = false)
        
        assertEquals(1, paidResults.size)
        assertEquals("john", paidResults[0].personId)
        
        assertEquals(1, unpaidResults.size)
        assertEquals("mary", unpaidResults[0].personId)
    }

    @Test
    fun `sortDebtSummaries should sort by amount correctly`() {
        val summary1 = createTestDebtSummary("alice", 1000.0)
        val summary2 = createTestDebtSummary("bob", 500.0)
        val summary3 = createTestDebtSummary("charlie", 750.0)
        val summaries = listOf(summary1, summary2, summary3)
        
        val sortedAsc = DebtMapper.sortDebtSummaries(summaries, DebtSortBy.AMOUNT_ASC)
        val sortedDesc = DebtMapper.sortDebtSummaries(summaries, DebtSortBy.AMOUNT_DESC)
        
        assertEquals("bob", sortedAsc[0].personId) // 500
        assertEquals("charlie", sortedAsc[1].personId) // 750
        assertEquals("alice", sortedAsc[2].personId) // 1000
        
        assertEquals("alice", sortedDesc[0].personId) // 1000
        assertEquals("charlie", sortedDesc[1].personId) // 750
        assertEquals("bob", sortedDesc[2].personId) // 500
    }

    // Helper methods
    private fun createTestTransaction(category: Category, amount: Double): Transaction {
        return Transaction(
            id = (Math.random() * 1000).toInt(),
            wallet = testWallet,
            transactionType = category.type,
            amount = amount,
            transactionDate = Date(),
            description = "Test transaction",
            category = category,
            withPerson = null,
            eventName = null,
            hasReminder = false,
            excludeFromReport = false,
            photoUri = null,
            parentDebtId = null,
            debtReference = null,
            debtMetadata = null
        )
    }

    private fun createTestDebtSummary(personId: String, remainingAmount: Double): DebtSummary {
        // Create a mock transaction for testing
        val testTransaction = createTestTransaction(debtCategory, remainingAmount)
        return DebtMapper.createDebtSummary(
            personId = personId,
            personName = "$personId Name",
            transactions = listOf(testTransaction)
        )
    }
} 