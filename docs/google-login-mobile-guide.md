# 📱 Hướng Dẫn Tích Hợp Đăng Nhập Google – Mobile Developer

> **Dành cho:** Android (Kotlin/Java)  
> **Backend:** Spring Boot – JWT Authentication  
> **Phiên bản tài liệu:** 1.0  

---

## 📋 Mục Lục

1. [Tổng Quan Luồng Đăng Nhập](#1-tổng-quan-luồng-đăng-nhập)
2. [Cấu Hình Phía Mobile](#2-cấu-hình-phía-mobile)
3. [API Endpoints](#3-api-endpoints)
4. [Luồng Đăng Nhập Chi Tiết](#4-luồng-đăng-nhập-chi-tiết)
5. [Quản Lý Token](#5-quản-lý-token)
6. [Gọi API Sau Khi Đăng Nhập](#6-gọi-api-sau-khi-đăng-nhập)
7. [Xử Lý Lỗi](#7-xử-lý-lỗi)
8. [Checklist Triển Khai](#8-checklist-triển-khai)

---

## 1. Tổng Quan Luồng Đăng Nhập

```
Mobile App                    Google                   Backend Server
    |                            |                           |
    |-- [1] Trigger Sign-In ---> |                           |
    |                            |                           |
    |<-- [2] Google ID Token --- |                           |
    |                            |                           |
    |-- [3] POST /google (idToken) -----------------------> |
    |                            |                           |-- verify với Google
    |                            |                           |-- tìm/tạo user trong DB
    |<-- [4] { accessToken, refreshToken, email, ... } ---- |
    |                            |                           |
    |-- [5] Lưu tokens vào Secure Storage                   |
    |                            |                           |
    |-- [6] GET /any-api (Bearer accessToken) ------------> |
    |<-- [7] Response ----------------------------------------|
```

### Điểm quan trọng

- Backend **không** redirect hay mở trình duyệt OAuth — Mobile tự lấy **Google ID Token** rồi gửi thẳng lên.
- Backend trả về **accessToken** (JWT, hết hạn sau **30 phút**) và **refreshToken** (JWT, hết hạn sau **30 ngày**).
- **Mỗi lần refresh token thành công**, backend cấp cặp token mới và vô hiệu hóa cặp cũ — hãy lưu lại ngay.

---

## 2. Cấu Hình Phía Mobile

### 2.1 Chuẩn Bị Trên Google Cloud Console

Project dùng **GCC thuần** (không Firebase) — không cần file `google-services.json`.

**Yêu cầu backend team cung cấp:**
- **Web Client ID** đang dùng trong server (dạng `xxxxxxxxx.apps.googleusercontent.com`)

**Đăng ký Android app trên GCC** (làm 1 lần):
1. Vào [console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials)
2. **+ Create Credentials** → **OAuth client ID** → chọn **Android**
3. Điền **Package name** (phải khớp với `applicationId` trong `build.gradle`)
4. Điền **SHA-1** fingerprint (lấy bằng `./gradlew signingReport`)
5. Nhấn **Create**

> ⚠️ Bước đăng ký Android Client trên GCC chỉ để Google xác minh app hợp lệ. Mobile **không cần copy** Client ID Android — vẫn dùng **Web Client ID** của backend trong code.

### 2.2 Android (Jetpack / Credential Manager)

Thêm dependency vào `build.gradle`:

```groovy
// build.gradle (app)
dependencies {
    implementation "androidx.credentials:credentials:1.3.0"
    implementation "androidx.credentials:credentials-play-services-auth:1.3.0"
    implementation "com.google.android.libraries.identity.googleid:googleid:1.1.1"
}
```

Cấu hình Google Sign-In:

```kotlin
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// Lấy Web Client ID từ backend team (không cần google-services.json)
val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)   // Cho phép chọn tài khoản mới
    .setServerClientId(WEB_CLIENT_ID)
    .setAutoSelectEnabled(false)
    .build()

val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()
```

Kích hoạt Sign-In và lấy ID Token:

```kotlin
val credentialManager = CredentialManager.create(context)

try {
    val result = credentialManager.getCredential(context, request)
    val credential = result.credential

    if (credential is GoogleIdTokenCredential) {
        val idToken = credential.idToken
        // Gửi idToken này lên backend
        sendIdTokenToBackend(idToken)
    }
} catch (e: GetCredentialException) {
    // Xử lý lỗi: người dùng huỷ, không có tài khoản, v.v.
    handleGoogleSignInError(e)
}
```



---

## 3. API Endpoints

### Base URL

```
https://<your-server-domain>
```
> Hỏi backend team để lấy URL production chính xác.

---

### 3.1 Đăng Nhập Bằng Google

```
POST /google
```

**Không cần Authorization header.**

**Request Body:**

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtp..."
}
```

| Field     | Type   | Bắt buộc | Mô tả                                          |
|-----------|--------|----------|------------------------------------------------|
| `idToken` | String | ✅        | Google ID Token lấy từ Google Sign-In SDK      |

**Response 200 – Thành công:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@gmail.com",
  "role": "USER",
  "tier": "BASIC"
}
```

| Field          | Type   | Mô tả                                    |
|----------------|--------|------------------------------------------|
| `accessToken`  | String | JWT dùng để gọi API (hết hạn sau **30 phút**) |
| `refreshToken` | String | Dùng để cấp token mới (hết hạn sau **30 ngày**) |
| `email`        | String | Email tài khoản Google                   |
| `role`         | String | Quyền: `USER` hoặc `ADMIN`               |
| `tier`         | String | Gói tài khoản: `BASIC` hoặc `PREMIUM`    |

**Response 400 – Lỗi:**

```json
"Google Token không hợp lệ hoặc đã hết hạn!"
```

---

### 3.2 Làm Mới Access Token

```
POST /refresh-token
```

**Không cần Authorization header.**

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response 200 – Thành công:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...(mới)...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...(mới)...",
  "email": "user@gmail.com",
  "role": "USER",
  "tier": "BASIC"
}
```

> ⚠️ **Quan trọng:** Backend cấp cặp token **mới hoàn toàn** — phải lưu lại cả `accessToken` lẫn `refreshToken` mới, không dùng lại token cũ.

**Response 400 – Lỗi:**

```json
"Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!"
```

---

### 3.3 Đăng Xuất

```
POST /logout
```

**Yêu cầu Authorization header** với `accessToken` HOẶC gửi `refreshToken` trong body.

**Request Body (dùng accessToken hoặc refreshToken đều được):**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Headers (nếu dùng accessToken):**
```
Authorization: Bearer <accessToken>
```

**Response 200 – Thành công:**

```json
"đăng xuất thành công"
```

---

### 3.4 Kiểm Tra Token Còn Hiệu Lực

```
GET /me
```

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response 200:** `"me"` — Token còn hợp lệ  
**Response 401/403:** Token hết hạn hoặc không hợp lệ

---

## 4. Luồng Đăng Nhập Chi Tiết

### Bước 1 – Người Dùng Nhấn "Đăng Nhập Bằng Google"

Kích hoạt Google Sign-In SDK để hiển thị picker chọn tài khoản Google.

### Bước 2 – Nhận Google ID Token

Sau khi người dùng chọn tài khoản, SDK trả về `GoogleIdToken` (chuỗi JWT của Google, khác với JWT của backend).

### Bước 3 – Gửi ID Token Lên Backend

```kotlin
// Android - Kotlin example
suspend fun sendIdTokenToBackend(idToken: String): AuthResponse? {
    val url = "https://<your-server>/google"
    val body = JSONObject().apply { put("idToken", idToken) }

    val response = httpClient.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body.toString())
    }

    return if (response.status == HttpStatusCode.OK) {
        response.body<AuthResponse>()
    } else {
        null
    }
}
```



### Bước 4 – Lưu Tokens Vào Secure Storage

> ❌ **Không** lưu token vào `SharedPreferences` / `UserDefaults` thông thường — dễ bị đọc trộm.  
> ✅ Dùng **Encrypted Storage** để bảo mật.

**Android – EncryptedSharedPreferences:**

```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

fun saveTokens(accessToken: String, refreshToken: String, email: String) {
    sharedPrefs.edit()
        .putString("access_token", accessToken)
        .putString("refresh_token", refreshToken)
        .putString("user_email", email)
        .apply()
}

fun getAccessToken(): String? = sharedPrefs.getString("access_token", null)
fun getRefreshToken(): String? = sharedPrefs.getString("refresh_token", null)
fun clearTokens() = sharedPrefs.edit().clear().apply()
```



---

## 5. Quản Lý Token

### Thời Hạn Token

| Token         | Thời hạn  | Ghi chú                             |
|---------------|-----------|-------------------------------------|
| `accessToken` | 30 phút   | Dùng cho mọi API call               |
| `refreshToken`| 30 ngày   | Chỉ dùng để lấy accessToken mới     |

### Chiến Lược Refresh Token (Khuyên dùng)

Triển khai **HTTP Interceptor** để tự động refresh khi `accessToken` hết hạn (nhận HTTP 401):

**Android – OkHttp Authenticator:**

```kotlin
class TokenAuthenticator(private val tokenStorage: TokenStorage) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Bị 401 → thử refresh
        val refreshToken = tokenStorage.getRefreshToken() ?: return null

        val newTokens = runBlocking {
            apiService.refreshToken(RefreshTokenRequest(refreshToken))
        }

        return if (newTokens != null) {
            tokenStorage.saveTokens(newTokens)
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        } else {
            tokenStorage.clearTokens()
            null // Dừng retry, chuyển về màn đăng nhập
        }
    }
}
```

---

## 6. Gọi API Sau Khi Đăng Nhập

Mọi API (trừ `/google`, `/refresh-token`, `/logout`) đều cần header:

```
Authorization: Bearer <accessToken>
```

**Ví dụ gọi API lấy danh sách giao dịch:**

```kotlin
// Android - Retrofit
@GET("transactions")
suspend fun getTransactions(
    @Header("Authorization") token: String,
    @Query("walletId") walletId: Long
): List<TransactionResponse>

// Gọi:
val transactions = apiService.getTransactions(
    token = "Bearer ${tokenStorage.getAccessToken()}",
    walletId = selectedWalletId
)
```

---

## 7. Xử Lý Lỗi

| HTTP Code | Tình huống                           | Xử lý phía Mobile                               |
|-----------|--------------------------------------|--------------------------------------------------|
| `200`     | Thành công                           | Lưu token, chuyển vào app                        |
| `400`     | idToken Google không hợp lệ/hết hạn  | Hiển thị lỗi, yêu cầu đăng nhập lại             |
| `401`     | accessToken hết hạn                  | Gọi `/refresh-token`, retry request              |
| `403`     | Không có quyền truy cập              | Hiển thị thông báo không đủ quyền               |
| `400` (refresh)| refreshToken hết hạn            | Xóa token, điều hướng về màn đăng nhập           |

**Luồng xử lý 401 cụ thể:**

```
Nhận HTTP 401
    ↓
Có refreshToken không?
    ├── Không → Xóa dữ liệu, về màn Login
    └── Có → POST /refresh-token
                ↓
            Thành công (200)?
                ├── Có → Lưu token mới, retry API call
                └── Không (400) → Xóa dữ liệu, về màn Login
```

---

## 8. Checklist Triển Khai

### Cấu Hình Ban Đầu
- [ ] Nhận **Web Client ID** từ backend team
- [ ] Đăng ký Android Client trên GCC: package name + SHA-1 (làm **1 lần duy nhất**)
- [ ] Hardcode `WEB_CLIENT_ID` vào code (không cần `google-services.json`)
- [ ] Thêm các dependency cần thiết vào `build.gradle`

### Luồng Đăng Nhập
- [ ] Tích hợp Google Sign-In SDK và lấy được `idToken`
- [ ] Gọi `POST /google` với `idToken`
- [ ] Lưu `accessToken`, `refreshToken`, `email`, `role`, `tier` vào Secure Storage
- [ ] Xử lý trường hợp người dùng huỷ đăng nhập

### Quản Lý Token
- [ ] Đính kèm `Authorization: Bearer <accessToken>` vào mọi API call
- [ ] Triển khai interceptor tự động refresh khi gặp 401
- [ ] Xử lý đúng khi cả `refreshToken` cũng hết hạn → về màn Login
- [ ] Cập nhật **cả 2 token** sau mỗi lần refresh thành công

### Đăng Xuất
- [ ] Gọi `POST /logout` khi người dùng đăng xuất
- [ ] Xóa toàn bộ token khỏi Secure Storage
- [ ] Đăng xuất khỏi Google Sign-In SDK (`credentialManager.clearCredentialState()`)
- [ ] Điều hướng về màn Login

### Bảo Mật
- [ ] Không lưu token vào SharedPreferences thường / plain text
- [ ] Không log token ra console trong bản production
- [ ] Xử lý timeout và lỗi mạng cho tất cả API call

---

## 📞 Liên Hệ Backend Team

Nếu cần hỗ trợ, hãy cung cấp:
- Log lỗi đầy đủ (status code + response body)
- Loại token đang dùng (access hay refresh)
- Thời điểm token được cấp

> Swagger UI (nếu được bật): `https://<your-server>/swagger-ui.html`
