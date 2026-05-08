package com.uet.expensetracker

import android.content.Context
import android.util.Log
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.SessionName
import com.google.cloud.dialogflow.v2.SessionsClient
import com.google.cloud.dialogflow.v2.SessionsSettings
import com.google.cloud.dialogflow.v2.TextInput
import java.io.InputStream

private const val DEFAULT_AGENT_PROJECT_ID = "savemoneyproject-452104"

fun processWithDialogflow(context: Context, text: String, onResult: (String, Int, String) -> Unit) {
    // Khởi tạo credentials
    val credentialsStream: InputStream = context.resources.openRawResource(0) //R.raw.credentials
    val sessionsClient = SessionsClient.create(
        SessionsSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(credentialsStream)))
            .build()
    )
    val session = SessionName.of(DEFAULT_AGENT_PROJECT_ID, "unique-session-id") // Thay "your-project-id" bằng Project ID

    // Gửi yêu cầu tới Dialogflow
    val queryInput = QueryInput.newBuilder()
        .setText(TextInput.newBuilder().setText(text).setLanguageCode("en-US").build())
        .build()
    val response = sessionsClient.detectIntent(session, queryInput)

    Log.i("dialog_flow", "processWithDialogflow: response: $response")
    // Lấy kết quả
    val intent = response.queryResult.intent.displayName
    if (intent == "ExpenseIntent") {
        val params = response.queryResult.parameters.fieldsMap
        val category = params["category"]?.stringValue ?: "Unknown"
        var cost = params["cost"]?.stringValue?.toInt() ?: 0
        val date = params["date"]?.stringValue ?: "Unknown"

        // Xử lý "thousand"
        if (text.contains("thousand")) cost *= 1000

        // Truyền kết quả qua callback
        onResult(category, cost, date)
    } else {
        onResult("Unknown", 0, "Unknown") // Intent không khớp
    }
}


fun addExpense(category: String, cost: Int, date: String) {
    Log.d("ExpenseTracker", "Added: $category - $cost - $date")
    // Thêm vào database hoặc state của bạn
}