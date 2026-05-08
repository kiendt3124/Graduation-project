package com.ptit.expensetracker.backend.ai.gemini;

import com.ptit.expensetracker.backend.ai.dto.ParsedTransactionDto;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransactionJsonParserTest {

    private final TransactionJsonParser parser = new TransactionJsonParser();

    @Test
    void parse_validJson_returnsDto() {
        String json = """
                {
                  "amount": 50000,
                  "currencyCode": "VND",
                  "categoryName": "Ăn uống",
                  "description": "Trưa ăn cơm",
                  "date": "2025-01-01",
                  "walletName": "Ví chính"
                }
                """;

        ParsedTransactionDto dto = parser.parse(json);
        assertEquals(50000d, dto.getAmount());
        assertEquals("VND", dto.getCurrencyCode());
        assertEquals("Ăn uống", dto.getCategoryName());
        assertEquals("Trưa ăn cơm", dto.getDescription());
        assertEquals(LocalDate.parse("2025-01-01"), dto.getDate());
        assertEquals("Ví chính", dto.getWalletName());
    }

    @Test
    void parse_codeFenceJson_parsesSuccessfully() {
        String json = """
                ```json
                {
                  "amount": 100000,
                  "currencyCode": "VND",
                  "categoryName": "Mua sắm",
                  "description": "Mua áo",
                  "date": "2025-02-10",
                  "walletName": "Thẻ tín dụng"
                }
                ```
                """;

        ParsedTransactionDto dto = parser.parse(json);
        assertEquals(100000d, dto.getAmount());
        assertEquals("Mua sắm", dto.getCategoryName());
    }

    @Test
    void parse_invalidJson_throws() {
        assertThrows(ApiException.class, () -> parser.parse("not json"));
    }
}


