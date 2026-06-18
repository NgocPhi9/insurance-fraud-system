# 🛡️ Hệ Thống Phát Hiện Gian Lận Bảo Hiểm (Insurance Fraud Detection System)

Dự án này là một hệ thống toàn diện hỗ trợ quản lý và tự động phát hiện các yêu cầu bồi thường bảo hiểm y tế có dấu hiệu gian lận. Hệ thống kết hợp giữa dịch vụ nghiệp vụ chính viết bằng **Spring Boot** và mô hình học máy (Machine Learning) viết bằng **Python** sử dụng thuật toán **Isolation Forest**.

---

## 📁 Cấu trúc dự án

Dự án được chia làm 2 phần chính:

1. **[claim-service](file:///d:/insurance-fraud-system/claim-service)**: Dịch vụ web quản lý yêu cầu bồi thường (Spring Boot, Thymeleaf, MySQL, Spring Security).
2. **[fraud-ml-service](file:///d:/insurance-fraud-system/fraud-ml-service)**: Dịch vụ phân tích học máy (Python, FastAPI, Streamlit, MLflow). Dịch vụ này có [README riêng](file:///d:/insurance-fraud-system/fraud-ml-service/README.md) chi tiết hơn về mô hình và pipeline. Link: https://github.com/imvhp/health-insurance-fraud-detection

---

## 🛠️ Yêu cầu môi trường

- **Java**: JDK 21 hoặc mới hơn.
- **Maven**: Dùng trực tiếp bộ cài `mvnw` đi kèm dự án.
- **MySQL**: Phiên bản 8.0 trở lên.
- **Python**: Phiên bản 3.9 trở lên.

---

## 🚀 Hướng dẫn cài đặt và khởi chạy

### Bước 1: Thiết lập Cơ sở dữ liệu (MySQL)
1. Khởi động MySQL Server của bạn.
2. Chạy file Database.sql (Mật khẩu là password với mọi tài khoản có sẵn)

### Bước 2: Thiết lập Biến môi trường
1. Sao chép tệp cấu hình mẫu ở thư mục gốc:
   Tạo tệp `.env` tại thư mục gốc từ tệp [`.env.example`](file:///d:/insurance-fraud-system/.env.example).
2. Chỉnh sửa thông tin kết nối database MySQL của bạn trong tệp `.env`:
   ```env
   DB_URL=jdbc:mysql://localhost:3306/fraud_detection
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   ```

---

### Bước 3: Khởi chạy Machine Learning Service (`fraud-ml-service`)

Dịch vụ này sử dụng môi trường ảo (`venv`) của Python và chạy API FastAPI trên cổng `8000`.

1. Clone repo tại https://github.com/imvhp/health-insurance-fraud-detection

2. Tạo môi trường ảo:

   ```bash
   python -m venv venv
   ```
3. Kích hoạt môi trường ảo (`venv`):
   - **Trên Windows (cmd / PowerShell):**
     ```powershell
     venv\Scripts\activate
     ```
   - **Trên macOS / Linux:**
     ```bash
     source venv/bin/activate
     ```
4. Cài đặt các thư viện cần thiết (nếu chưa cài):
   ```bash
   pip install pandas numpy scikit-learn mlflow streamlit fastapi uvicorn requests
   ```
5. Khởi chạy FastAPI Backend:
   ```bash
   python -m uvicorn src.app.api:app --reload --port 8000
   ```
   > [!NOTE]
   > API sẽ khả dụng tại `http://localhost:8000`. Điểm cuối phục vụ dự đoán là `/predict`.

---

### Bước 4: Khởi chạy Claim Service (`claim-service`)

Dịch vụ Spring Boot sẽ tự động nạp cấu hình từ tệp `.env` ở thư mục gốc nhờ thư viện `spring-dotenv`.

1. Mở một terminal mới và di chuyển vào thư mục `claim-service`:
   ```bash
   cd claim-service
   ```
2. Chạy lệnh Maven để khởi tạo và chạy ứng dụng:
   - **Trên Windows:**
     ```cmd
     mvnw.cmd spring-boot:run
     ```
   - **Trên macOS / Linux:**
     ```bash
     ./mvnw spring-boot:run
     ```
3. Truy cập ứng dụng qua trình duyệt tại địa chỉ:
   ```
   http://localhost:8080
   ```

---

## 📈 Kiểm thử hệ thống

- **Trực quan hóa mô hình (Streamlit)**: 
  Nếu muốn chạy giao diện thử nghiệm độc lập của mô hình Python:
  ```bash
  # Tại thư mục fraud-ml-service (đã activate venv)
  streamlit run src/app/app.py
  ```
  Giao diện sẽ chạy tại `http://localhost:8501`.

- **Quản lý thực nghiệm (MLflow)**:
  Xem các phiên huấn luyện mô hình và tham số:
  ```bash
  mlflow ui
  ```
  Truy cập tại `http://localhost:5000`.
