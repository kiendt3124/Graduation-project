package com.ptit.expensetracker.backend.ai.gemini;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class GeminiPromptBuilder {

    public String buildChatPrompt(String userMessage, String userId, String locale) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý quản lý chi tiêu cá nhân, trả lời ngắn gọn. ");
        sb.append("Vai trò: hỗ trợ người dùng kiểm soát chi tiêu, gợi ý hành động thực tế. ");
        sb.append("Không bịa số liệu; nếu thiếu dữ liệu, hãy hỏi lại. ");
        if (locale != null && !locale.isBlank()) {
            sb.append("Locale của người dùng: ").append(locale).append(". ");
        }
        sb.append("UserId (ẩn danh): ").append(userId).append(". ");
        sb.append("Tin nhắn của người dùng: ").append(userMessage);
        return sb.toString();
    }

    public String buildParseTransactionPrompt(String text, String locale) {
        StringBuilder sb = new StringBuilder();
        LocalDate currentDateUtc = LocalDate.now(ZoneOffset.UTC);
        sb.append("Bạn là trợ lý quản lý chi tiêu. Nhiệm vụ: trích xuất giao dịch từ câu tiếng Việt. ");
        sb.append("Phải trả về JSON THUẦN (không markdown, không giải thích). ");
        sb.append("Schema bắt buộc: {\"amount\": number, \"currencyCode\": string, \"categoryName\": string, \"description\": string, \"date\": \"YYYY-MM-DD\", \"walletName\": string}. ");
        sb.append("Nếu thiếu thông tin, để null hoặc chuỗi rỗng nhưng vẫn trả về JSON hợp lệ. ");

        String today = currentDateUtc.format(DateTimeFormatter.ISO_DATE);
        sb.append("Thời gian hiện tại (UTC) là ").append(today).append(". ");

        sb.append("Danh sách category hợp lệ trong ứng dụng (trả về đúng \"metadata\" hoặc \"name\" trong trường categoryName, không tạo mới): ");
        sb.append("{\"expense\":[");
        sb.append("{\"metadata\":\"foodndrink0\",\"name\":\"food_beverage\",\"title\":\"cate_food\"},");
        sb.append("{\"metadata\":\"utilities0\",\"name\":\"bills_utilities\",\"title\":\"cate_utilities\",\"subs\":[\"phone0\",\"water0\",\"electricity0\",\"gas0\",\"television0\",\"internet0\",\"rentals0\",\"other_bill0\"]},");
        sb.append("{\"metadata\":\"shopping0\",\"name\":\"shopping\",\"title\":\"cate_shopping\",\"subs\":[\"personal_items0\",\"houseware0\",\"makeup0\"]},");
        sb.append("{\"metadata\":\"family0\",\"name\":\"housing_family\",\"title\":\"cate_family\",\"subs\":[\"home_maintenance0\",\"home_service0\",\"pets0\"]},");
        sb.append("{\"metadata\":\"transport0\",\"name\":\"transportation\",\"title\":\"cate_transport\",\"subs\":[\"vehicle_maintenance0\"]},");
        sb.append("{\"metadata\":\"medical0\",\"name\":\"health_fitness\",\"title\":\"cate_medical\",\"subs\":[\"medical_checkup0\",\"fitness0\"]},");
        sb.append("{\"metadata\":\"education0\",\"name\":\"education\",\"title\":\"cate_education\"},");
        sb.append("{\"metadata\":\"entertainment0\",\"name\":\"entertainment\",\"title\":\"cate_entertainment\",\"subs\":[\"streaming_service0\",\"fun_money0\"]},");
        sb.append("{\"metadata\":\"gifts_donations0\",\"name\":\"gifts_donations\",\"title\":\"cate_donation\"},");
        sb.append("{\"metadata\":\"insurance0\",\"name\":\"insurance\",\"title\":\"cate_insurance\"},");
        sb.append("{\"metadata\":\"invest0\",\"name\":\"investment\",\"title\":\"cate_invest\"},");
        sb.append("{\"metadata\":\"IS_OTHER_EXPENSE\",\"name\":\"expense_other\",\"title\":\"cate_expense_other\"},");
        sb.append("{\"metadata\":\"IS_OUTGOING_TRANSFER\",\"name\":\"outgoing_transfer\",\"title\":\"cate_outgoing_transfer\"},");
        sb.append("{\"metadata\":\"IS_LOAN\",\"name\":\"loan\",\"title\":\"cate_loan\"},");
        sb.append("{\"metadata\":\"IS_REPAYMENT\",\"name\":\"repayment \",\"title\":\"cate_repayment\"},");
        sb.append("{\"metadata\":\"IS_PAY_INTEREST\",\"name\":\"pay_interest\",\"title\":\"cate_pay_interest\"}");
        sb.append("],\"income\":[");
        sb.append("{\"metadata\":\"salary0\",\"name\":\"salary\",\"title\":\"cate_salary\"},");
        sb.append("{\"metadata\":\"IS_DEBT\",\"name\":\"debt\",\"title\":\"cate_debt\"},");
        sb.append("{\"metadata\":\"IS_DEBT_COLLECTION\",\"name\":\"debt_collection\",\"title\":\"cate_debt_collection\"},");
        sb.append("{\"metadata\":\"IS_OTHER_INCOME\",\"name\":\"income_other\",\"title\":\"cate_income_other\"},");
        sb.append("{\"metadata\":\"IS_INCOMING_TRANSFER\",\"name\":\"incoming_transfer\",\"title\":\"cate_incoming_transfer\"},");
        sb.append("{\"metadata\":\"IS_COLLECT_INTEREST\",\"name\":\"collect_interest\",\"title\":\"cate_collect_interest\"}");
        sb.append("]} ");
        sb.append("LƯU Ý QUAN TRỌNG: Nếu category có 'subs' (subcategories), BẮT BUỘC phải chọn một trong các subcategory trong mảng 'subs', KHÔNG được trả về parent category. ");
        sb.append("Ví dụ: Nếu giao dịch liên quan đến giải trí/vui chơi, trả về 'streaming_service0' hoặc 'fun_money0', KHÔNG trả về 'entertainment0'. ");
        sb.append("QUY TẮC PHÂN BIỆT LOAN / DEBT: ");
        sb.append("IS_LOAN dùng khi NGƯỜI DÙNG đưa tiền cho người khác (cho vay, cho mượn). ");
        sb.append("IS_DEBT dùng khi NGƯỜI DÙNG nhận tiền từ người khác (vay, mượn tiền). ");
        sb.append("Nếu câu có 'cho tôi vay', 'vay của', 'mượn tiền từ' => IS_DEBT. ");
        sb.append("Nếu câu có 'cho người khác vay', 'đưa tiền cho', 'cho mượn tiền' => IS_LOAN. ");
        sb.append("Ưu tiên dùng metadata (ví dụ: \"foodndrink0\", \"streaming_service0\", \"IS_DEBT\", \"IS_LOAN\" ) trong categoryName. ");

        if (locale != null && !locale.isBlank()) {
            sb.append("Locale: ").append(locale).append(". ");
        }
        sb.append("Câu người dùng: ").append(text);
        return sb.toString();
    }

    public String buildInsightsPrompt(
            double totalIncome,
            double totalExpense,
            double totalDebt,
            String recentPattern,
            String timeRange,
            LocalDate currentDateUtc
    ) {
        String today = currentDateUtc != null
                ? currentDateUtc.format(DateTimeFormatter.ISO_DATE)
                : "unknown";

        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý tài chính cá nhân, trả lời ngắn gọn bằng tiếng Việt. ");
        sb.append("Nhiệm vụ: sinh cảnh báo (alerts) và gợi ý (tips) dựa trên số liệu chi tiêu. ");
        sb.append("Phải trả về JSON THUẦN, schema: {\"alerts\": [string], \"tips\": [string]}. ");
        sb.append("Mỗi mục ngắn gọn, hành động, không lan man, không markdown. ");
        sb.append("Ngày hiện tại (UTC): ").append(today).append(". ");
        sb.append("Tổng thu nhập: ").append(totalIncome).append(". ");
        sb.append("Tổng chi tiêu: ").append(totalExpense).append(". ");
        sb.append("Tổng nợ: ").append(totalDebt).append(". ");
        if (timeRange != null && !timeRange.isBlank()) {
            sb.append("Khoảng thời gian: ").append(timeRange).append(". ");
        }
        if (recentPattern != null && !recentPattern.isBlank()) {
            sb.append("Mô tả xu hướng gần đây: ").append(recentPattern).append(". ");
        }
        sb.append("Trả về tối đa 3 alerts và 3 tips. Nếu không có gì đáng lưu ý, để mảng trống.");
        return sb.toString();
    }

    public String buildPlanPrompt(String userPrompt, String locale, String timezone) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là hệ thống lập kế hoạch truy vấn dữ liệu cho app quản lý chi tiêu (không trả lời người dùng). ");
        sb.append("Nhiệm vụ: dựa trên yêu cầu của người dùng, chọn dữ liệu cần thiết theo schema JSON. ");
        sb.append("Chỉ được dùng các dataset trong whitelist: [\"MONTHLY_TOTALS\",\"TOP_CATEGORIES\"]. ");
        sb.append("Phải trả về JSON THUẦN theo schema: ");
        sb.append("{\"analysisType\": string, \"timeRange\": string, \"requiredDatasets\": [string], \"topK\": number}. ");
        sb.append("timeRange chỉ được là: THIS_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, LAST_12_MONTHS. ");
        sb.append("Quy tắc: ");
        sb.append("- Nếu có 'so sánh' và '3 tháng' -> analysisType=COMPARE_MONTHS, timeRange=LAST_3_MONTHS, requiredDatasets=[MONTHLY_TOTALS,TOP_CATEGORIES]. ");
        sb.append("- Nếu có 'dự đoán' hoặc 'forecast' -> analysisType=FORECAST_MONTH, timeRange=THIS_MONTH, requiredDatasets=[MONTHLY_TOTALS,TOP_CATEGORIES]. ");
        sb.append("- Mặc định -> analysisType=GENERIC, timeRange=THIS_MONTH, requiredDatasets=[MONTHLY_TOTALS]. ");
        if (locale != null && !locale.isBlank()) sb.append("Locale: ").append(locale).append(". ");
        if (timezone != null && !timezone.isBlank()) sb.append("Timezone: ").append(timezone).append(". ");
        sb.append("Yêu cầu người dùng: ").append(userPrompt);
        return sb.toString();
    }

    public String buildAnswerPrompt(String userPrompt,
                                   String locale,
                                   String timezone,
                                   String currencyCode,
                                   Object dataRequest,
                                   Object dataResponse) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý quản lý chi tiêu cá nhân. Trả lời ngắn gọn, rõ ràng, bằng tiếng Việt. ");
        sb.append("Bạn sẽ nhận yêu cầu người dùng + dữ liệu tóm tắt từ app. ");
        sb.append("Phải trả về JSON THUẦN theo schema: ");
        sb.append("{\"answerMarkdown\": string, \"actions\": [{\"type\": string, \"label\": string, \"payloadJson\": string}]}. ");
        sb.append("Trong answerMarkdown có thể dùng markdown đơn giản (bullet, **bold**, `code`). ");
        sb.append("Locale=").append(locale == null ? "vi-VN" : locale).append(". ");
        if (timezone != null && !timezone.isBlank()) sb.append("Timezone=").append(timezone).append(". ");
        if (currencyCode != null && !currencyCode.isBlank()) sb.append("Currency=").append(currencyCode).append(". ");
        sb.append("Yêu cầu: ").append(userPrompt).append(". ");
        sb.append("DataRequest: ").append(dataRequest).append(". ");
        sb.append("DataResponse: ").append(dataResponse).append(". ");
        sb.append("Actions gợi ý tối đa 3, chỉ chọn type trong: OPEN_REPORT, CREATE_BUDGET, CREATE_REMINDER. ");
        sb.append("payloadJson là JSON string (có thể rỗng).");
        return sb.toString();
    }

    public String buildMemoryUpdatePrompt(String previousSummary,
                                         String previousPinnedFactsJson,
                                         String newMessagesTextBlock) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là hệ thống cập nhật trí nhớ cho trợ lý quản lý chi tiêu. ");
        sb.append("Nhiệm vụ: cập nhật summary ngắn gọn và pinnedFacts từ các tin nhắn mới. ");
        sb.append("Phải trả về JSON THUẦN theo schema: ");
        sb.append("{\"summary\": string, \"pinnedFacts\": {\"preferredCurrency\": string?, \"timezone\": string?, \"mainWalletHint\": string?, \"goals\": [string]?}}. ");
        sb.append("Yêu cầu: summary tối đa 8-12 dòng, chỉ giữ thông tin bền vững (sở thích, mục tiêu, bối cảnh dài hạn). ");
        sb.append("Không đưa dữ liệu nhạy cảm chi tiết (số tài khoản, định danh...). ");
        if (previousSummary != null && !previousSummary.isBlank()) {
            sb.append("Previous summary: ").append(previousSummary).append(". ");
        }
        if (previousPinnedFactsJson != null && !previousPinnedFactsJson.isBlank()) {
            sb.append("Previous pinnedFacts (JSON): ").append(previousPinnedFactsJson).append(". ");
        }
        sb.append("New messages:\n").append(newMessagesTextBlock);
        return sb.toString();
    }
}


