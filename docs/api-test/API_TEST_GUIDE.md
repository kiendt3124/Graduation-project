# Hướng dẫn Test API – Vay Nợ / Mục Tiêu Tài Chính / Báo Cáo

## 1. Chuẩn bị

### 1.1 Import vào Postman
1. Mở **Postman** → **Import**
2. Chọn file `Loan_Goal_Report_API_Test.postman_collection.json`
3. Tạo **Environment** với 2 biến bắt buộc:

| Biến | Giá trị |
|------|---------|
| `baseUrl` | `http://localhost:8080` |
| `token` | Access token lấy từ bước đăng nhập |
| `walletId` | UUID của ví đang có số dư (lấy từ `GET /api/wallets`) |

> **Lưu ý:** `loanId`, `goalId`, `paymentId`, `contributionId` sẽ được **tự động set** bởi các test script sau mỗi lần tạo mới.

### 1.2 Lấy Token
Vì ứng dụng dùng **Google OAuth2**, cần:
1. Lấy `idToken` từ Google → gọi `POST /google` để nhận `accessToken` + `refreshToken`
2. Set `token = accessToken` vào Environment
3. Khi token hết hạn → chạy request **"0. Auth – Refresh Token"**

---

## 2. Luồng Test Module Vay Nợ (Loans)

```
POST /api/loans (BORROW) ──→ auto set {{loanId}}
  ↓
POST /api/loans (LEND)
  ↓
GET  /api/loans                    [kiểm tra danh sách ≥ 2]
GET  /api/loans?type=BORROW        [filter theo loại]
GET  /api/loans?status=ACTIVE      [filter theo status]
  ↓
GET  /api/loans/{{loanId}}         [kiểm tra accruedInterest, remainingAmount]
  ↓
PATCH /api/loans/{{loanId}}        [cập nhật note, dueDate]
  ↓
POST /api/loans/{{loanId}}/payments  (10tr) ──→ auto set {{paymentId}}
POST /api/loans/{{loanId}}/payments  (40tr) ──→ status tự động = PAID
  ↓
GET  /api/loans/{{loanId}}/payments  [lịch sử ≥ 2 lần]
  ↓
[NEGATIVE] POST payments vượt remainingAmount → 400
  ↓
DELETE /api/loans/{{loanId}}       [soft delete]
GET    /api/loans/{{loanId}}       [verify → 400]
```

### Điểm kiểm tra quan trọng
| Bước | Điều cần verify |
|------|-----------------|
| Tạo BORROW | `status = ACTIVE`, `loanType = BORROW` |
| GET chi tiết | Có `accruedInterest` (lãi tích lũy), `remainingAmount` |
| Payment đủ 50tr | `loanStatus` tự chuyển → `PAID` |
| Payment vượt | Trả `400` |
| Soft delete | GET lại → `400` (không thấy nữa) |

---

## 3. Luồng Test Module Mục Tiêu Tài Chính (Goals)

```
POST /api/goals (có deadline)    ──→ auto set {{goalId}}
POST /api/goals (không deadline)
  ↓
GET  /api/goals                  [danh sách ≥ 2]
GET  /api/goals?status=ACTIVE    [filter]
  ↓
GET  /api/goals/{{goalId}}       [kiểm tra progressPercentage, remainingAmount]
  ↓
PATCH /api/goals/{{goalId}}      [cập nhật name, note]
  ↓
POST /api/goals/{{goalId}}/contributions  (5tr) ──→ auto set {{contributionId}}
POST /api/goals/{{goalId}}/contributions  (25tr) ──→ status = COMPLETED (auto)
  ↓
GET  /api/goals/{{goalId}}/contributions  [lịch sử ≥ 2]
  ↓
DELETE contributions/{{contributionId}}  ──→ goal reset về ACTIVE
GET  /api/goals/{{goalId}}               [verify status = ACTIVE]
  ↓
PATCH /api/goals/{{goalId}}  {status: CANCELLED}
  ↓
[NEGATIVE] POST contributions → 400 (goal đã CANCELLED)
  ↓
DELETE /api/goals/{{goalId}}
GET    /api/goals?status=COMPLETED  [xem goals đã hoàn thành]
```

### Điểm kiểm tra quan trọng
| Bước | Điều cần verify |
|------|-----------------|
| Tạo goal | `status = ACTIVE`, `savedAmount = 0` |
| GET chi tiết | `progressPercentage`, `remainingAmount` |
| Góp đủ tiền | `goalStatus` tự chuyển → `COMPLETED` |
| Xoá contribution | Goal reset → `ACTIVE` |
| Góp khi CANCELLED | Trả `400` |

---

## 4. Luồng Test Module Báo Cáo (Reports)

```
GET /api/reports/summary?month=4&year=2026
  ↓
GET /api/reports/expense-by-category?month=4&year=2026
  ↓
GET /api/reports/income-by-category?month=4&year=2026
  ↓
GET /api/reports/cash-flow?from=2026-04-01&to=2026-04-30
[NEGATIVE] GET cash-flow?from=2026-04-30&to=2026-04-01  → 400
  ↓
GET /api/reports/monthly?year=2026
  ↓
GET /api/reports/budget-vs-actual?month=4&year=2026
```

### Điểm kiểm tra quan trọng
| Endpoint | Điều cần verify |
|----------|-----------------|
| `/summary` | Tất cả trường có giá trị, `month`/`year` đúng |
| `/expense-by-category` | Tổng `percentage` ≈ 100% |
| `/income-by-category` | Tổng `percentage` ≈ 100% |
| `/cash-flow` | `net = income - expense` cho từng ngày |
| `/cash-flow` (negative) | `400` khi `from > to` |
| `/monthly` | Đúng **12 phần tử**, tháng 1–12 đủ |
| `/budget-vs-actual` | `remaining = limit - spent`, `isOverBudget` đúng logic |

---

## 5. Chạy tự động với Newman (CLI)

```bash
# Cài Newman
npm install -g newman

# Chạy toàn bộ collection
newman run Loan_Goal_Report_API_Test.postman_collection.json \
  --env-var "baseUrl=http://localhost:8080" \
  --env-var "token=YOUR_ACCESS_TOKEN" \
  --env-var "walletId=YOUR_WALLET_UUID" \
  --reporters cli,json \
  --reporter-json-export result.json

# Chạy chỉ 1 folder
newman run Loan_Goal_Report_API_Test.postman_collection.json \
  --folder "1. MODULE – VAY NỢ (Loans)" \
  --env-var "baseUrl=http://localhost:8080" \
  --env-var "token=YOUR_ACCESS_TOKEN" \
  --env-var "walletId=YOUR_WALLET_UUID"
```

---

## 6. Các lỗi thường gặp & cách xử lý

| Lỗi | Nguyên nhân | Cách fix |
|-----|-------------|----------|
| `401 Unauthorized` | Token hết hạn | Chạy `POST /refresh-token` → update `{{token}}` |
| `400 – User not found` | Email từ token không có trong DB | Đăng nhập lại qua Google |
| `400 – Wallet not found` | `walletId` sai hoặc thuộc user khác | Lấy đúng walletId từ `GET /api/wallets` |
| `400 – Insufficient balance` | Số dư ví không đủ để góp tiền | Nạp thêm hoặc dùng ví khác |
| `loanId` / `goalId` rỗng | Chưa chạy bước tạo mới | Chạy đủ thứ tự từ bước 1 |
