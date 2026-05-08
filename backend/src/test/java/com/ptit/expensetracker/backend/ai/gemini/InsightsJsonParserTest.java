package com.ptit.expensetracker.backend.ai.gemini;

import com.ptit.expensetracker.backend.ai.dto.InsightsResponse;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsightsJsonParserTest {

    private final InsightsJsonParser parser = new InsightsJsonParser();

    @Test
    void parse_validJson_returnsLists() {
        String json = """
                {
                  "alerts": ["A1", "A2"],
                  "tips": ["T1", "T2"]
                }
                """;
        InsightsResponse resp = parser.parse(json);
        assertEquals(2, resp.getAlerts().size());
        assertEquals("A1", resp.getAlerts().get(0));
        assertEquals(2, resp.getTips().size());
    }

    @Test
    void parse_codeFenceJson_works() {
        String json = """
                ```json
                {"alerts":["A"],"tips":["B"]}
                ```
                """;
        InsightsResponse resp = parser.parse(json);
        assertEquals(1, resp.getAlerts().size());
        assertEquals(1, resp.getTips().size());
    }

    @Test
    void parse_invalidJson_throws() {
        assertThrows(ApiException.class, () -> parser.parse("not json"));
    }
}


