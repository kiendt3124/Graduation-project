package com.uet.expensetracker.features.money.ui.transactions

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit test for Month Tab Logic
 */
class MonthTabLogicTest {

    @Test
    fun `buildMonthItems should create correct number of months`() {
        // Arrange
        val monthsBack = 6
        
        // Act
        val months = MonthItem.buildMonthItems(monthsBack)
        
        // Assert
        // Should have 6 past months + current month + future = 8 total
        assertEquals(8, months.size)
    }

    @Test
    fun `buildMonthItems should have current month labeled as This month`() {
        // Arrange
        val monthsBack = 6
        
        // Act
        val months = MonthItem.buildMonthItems(monthsBack)
        
        // Assert
        val thisMonthItems = months.filter { it.label == "This month" }
        assertEquals(1, thisMonthItems.size)
        
        val thisMonth = thisMonthItems.first()
        val now = LocalDate.now()
        assertEquals(now.year, thisMonth.year)
        assertEquals(now.monthValue, thisMonth.month)
        assertFalse(thisMonth.isFuture)
    }

    @Test
    fun `buildMonthItems should have future tab at the end`() {
        // Arrange
        val monthsBack = 6
        
        // Act
        val months = MonthItem.buildMonthItems(monthsBack)
        
        // Assert
        val lastMonth = months.last()
        assertEquals("Future", lastMonth.label)
        assertTrue(lastMonth.isFuture)
    }

    @Test
    fun `findThisMonthIndex should return correct index`() {
        // Arrange
        val months = MonthItem.buildMonthItems(6)
        
        // Act
        val thisMonthIndex = MonthItem.findThisMonthIndex(months)
        
        // Assert
        assertTrue(thisMonthIndex >= 0)
        assertTrue(thisMonthIndex < months.size)
        assertEquals("This month", months[thisMonthIndex].label)
    }

    @Test
    fun `month items should be in chronological order`() {
        // Arrange
        val monthsBack = 12
        
        // Act
        val months = MonthItem.buildMonthItems(monthsBack)
        
        // Assert
        // Past months should be in ascending order (oldest to newest)
        for (i in 0 until months.size - 2) { // Skip "This month" and "Future"
            val current = months[i]
            val next = months[i + 1]
            
            if (current.label != "This month" && next.label != "This month" && 
                current.label != "Future" && next.label != "Future") {
                
                val currentDate = LocalDate.of(current.year, current.month, 1)
                val nextDate = LocalDate.of(next.year, next.month, 1)
                
                assertTrue("Months should be in chronological order", 
                    currentDate.isBefore(nextDate) || currentDate.isEqual(nextDate))
            }
        }
    }

    @Test
    fun `month formatting should be correct`() {
        // Arrange
        val monthsBack = 3
        
        // Act
        val months = MonthItem.buildMonthItems(monthsBack)
        
        // Assert
        months.forEach { month ->
            if (month.label != "This month" && month.label != "Future") {
                // Should be in MM/yyyy format
                assertTrue("Month label should match MM/yyyy pattern", 
                    month.label.matches(Regex("\\d{2}/\\d{4}")))
            }
        }
    }

    @Test
    fun `default monthsBack should be 18`() {
        // Act
        val months = MonthItem.buildMonthItems()
        
        // Assert
        // Should have 18 past months + current month + future = 20 total
        assertEquals(20, months.size)
    }
} 