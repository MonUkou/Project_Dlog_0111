# 📱 DLog — Ứng dụng Mạng Xã Hội & Chia sẻ Ảnh trên Android

**DLog** là một ứng dụng mạng xã hội và chia sẻ hình ảnh hiện đại được phát triển trên nền tảng Android (Java 11). Ứng dụng cung cấp đầy đủ các tính năng tương tác cộng đồng như đăng tải nhiều ảnh, bình luận, thả tim, nhắn tin chat realtime, theo dõi người dùng, trang cá nhân, chế độ sáng/tối (Dark Mode), giao diện thích ứng xoay ngang màn hình (Responsive/Landscape), cùng hệ thống quản trị hệ thống (Admin Dashboard) và khả năng hoạt động ngoại tuyến (Offline Caching).

---

## ✨ Các Chức năng Nổi bật (38 Chức năng F01 - F38)

### 🔐 1. Tài khoản & Xác thực (F01 – F03)
* **Đăng ký & Đăng nhập:** Xác thực bằng Email/Mật khẩu hoặc tài khoản Google thông qua Android Credential Manager API.
* **Quên mật khẩu:** Gửi email khôi phục mật khẩu bảo mật qua Firebase Auth.
* **Quản lý phiên:** Duy trì trạng thái đăng nhập, kiểm tra trạng thái khóa tài khoản từ phía quản trị viên.

### 👤 2. Hồ sơ & Mạng xã hội (F04 – F09)
* **Trang cá nhân & Chỉnh sửa:** Cập nhật tên hiển thị, tên tài khoản, tiểu sử (bio), ảnh đại diện và ảnh bìa.
* **Mối quan hệ Follow:** Theo dõi hoặc bỏ theo dõi tài khoản khác, xem danh sách người theo dõi (`followers`) và đang theo dõi (`following`).
* **Khám phá:** Bảng tin ưu tiên nội dung từ các tài khoản đã follow và trang khám phá toàn cầu.

### 🖼️ 3. Bài viết & Bảng tin (F10 – F17)
* **Đăng bài đa phương tiện:** Đăng tải một hoặc nhiều ảnh, nén ảnh tối ưu hóa dung lượng trước khi upload lên Cloudinary / Firebase Storage.
* **Chỉnh ảnh & Carousel:** Cắt, xoay, áp dụng bộ lọc màu trước khi đăng; hiển thị ảnh dạng collage và carousel nhiều ảnh.
* **Tương tác bảng tin:** Gắn caption, hashtag, mở rộng/thu gọn mô tả dài, bộ lọc sắp xếp (mới nhất, cũ nhất, phổ biến), và nút cuộn nhanh lên đầu bảng tin.
* **Lưu & Chia sẻ:** Lưu bài viết vào bộ sưu tập cá nhân (`SavedPosts`) và chia sẻ nội dung qua các ứng dụng khác.

### ❤️ 4. Tương tác & Media (F18 – F22)
* **Tương tác trực tiếp:** Thích/bỏ thích bài viết, gửi và xóa bình luận.
* **Trình xem ảnh nâng cao (`PhotoViewer` / `PhotoEditor`):** Xem ảnh toàn màn hình, phóng to/thu nhỏ, vuốt xuống để đóng, xem thông số EXIF và tải ảnh về thiết bị.

### 🔔 5. Thông báo & Tin nhắn Chat (F23 – F25)
* **Trung tâm Thông báo:** Nhận thông báo thời gian thực khi có người like, comment hoặc follow.
* **Nhắn tin Realtime:** Xem danh sách cuộc trò chuyện, tìm kiếm người dùng và nhắn tin trực tiếp 1-1 qua `ChatService`.

### 🌙 6. Giao diện & Trải nghiệm Ngoại tuyến (F26 – F31)
* **Dark Mode & Responsive:** Hỗ trợ giao diện Sáng/Tối đồng bộ, tự động chuyển đổi bố cục thanh điều hướng sang dạng cạnh phải (Navigation Rail) khi thiết bị xoay ngang (Landscape).
* **Cache Ngoại tuyến:** Tận dụng bộ nhớ đệm ngoại tuyến của Firestore kết hợp SQLite để người dùng vẫn có thể xem lại các trang, bài viết và bình luận đã tải khi mất kết nối mạng.

### 🛡️ 7. Quản trị hệ thống & Kiểm thử (F32 – F38)
* **Admin Dashboard (`AdminActivity`):** Phân quyền quản trị, thống kê tổng quan hệ thống, quản lý/khóa tài khoản người dùng, ẩn/hiện bài viết và bình luận vi phạm.
* **Kiểm thử tự động:** Bộ Unit Tests kiểm thử model, tuần tự hóa dữ liệu và xử lý hashtag (`src/test/java/`).

---

## 🧰 Công nghệ Sử dụng

| Thành phần | Công nghệ / Thư viện |
| --- | --- |
| **Ngôn ngữ lập trình** | Java 11 |
| **Nền tảng Android** | Android SDK (minSdk 30, targetSdk 37) |
| **Giao diện (UI)** | XML Layouts, Material Design 3, AppCompat |
| **Xác thực (Auth)** | Firebase Authentication, Google Identity (Credential Manager) |
| **Cơ sở dữ liệu đám mây** | Cloud Firestore |
| **Lưu trữ ảnh (Storage)** | Cloudinary & Firebase Storage |
| **Hiển thị ảnh** | Glide (với `circleCrop` & `centerCrop`) |
| **Dữ liệu cục bộ / Cache** | SQLite & Firestore Offline Persistence |
| **Kiểm thử (Testing)** | JUnit, AndroidX Test, Espresso |
| **Hệ thống Build** | Gradle Kotlin DSL |

---

## 📂 Cấu trúc Thư mục Dự án

```text
app/src/main/
├── java/com/example/myapplication/
│   ├── adapter/       # Các Adapter hiển thị danh sách (Post, Comment, Chat, Notification, v.v.)
│   ├── model/         # Các lớp thực thể dữ liệu (Post, User, Comment, Like, Notification, v.v.)
│   ├── service/       # Tầng xử lý nghiệp vụ & kết nối Firebase (Auth, Post, Chat, User, v.v.)
│   ├── ui/            # Các View thành phần tùy chỉnh
│   ├── util/          # Các lớp tiện ích (BottomNav, GoogleSignIn, HashtagParser, v.v.)
│   └── view/          # Custom Views (ZoomDismissImageView, PhotoCollageView, v.v.)
├── res/
│   ├── drawable/      # Tài nguyên đồ họa, vector assets & background shapes
│   ├── layout/        # Giao diện XML chuẩn dọc (Portrait)
│   ├── layout-land/   # Giao diện XML tối ưu cho chế độ xoay ngang (Landscape)
│   ├── values/        # Màu sắc, kích thước, chuỗi văn bản & Styles (Light Mode)
│   └── values-night/  # Cấu hình màu sắc & Styles cho chế độ tối (Dark Mode)
└── AndroidManifest.xml
```

---

## 🚀 Hướng dẫn Cài đặt & Chạy Ứng dụng

### Yêu cầu hệ thống
* Android Studio (phiên bản mới nhất).
* JDK tương thích Java 11.
* Thiết bị thật hoặc máy ảo Android (Emulator) chạy từ Android 11 (API 30) trở lên.
* Kết nối Internet để đồng bộ Firebase & Cloudinary.

### Các bước thực hiện
1. **Clone mã nguồn:**
   ```bash
   git clone https://github.com/MonUkou/Project_Dlog_0111.git
   cd Project_Dlog_0111
   ```
2. **Mở dự án trong Android Studio:**
   * Chọn **Open** -> Chọn thư mục `Project_Dlog_0111`.
   * Chờ quá trình **Gradle Sync** hoàn tất.
3. **Chạy ứng dụng:**
   * Chọn thiết bị ảo hoặc thiết bị thật.
   * Nhấn nút **Run (▶)** để biên dịch và khởi chạy ứng dụng.

---

## 🧪 Kiểm thử (Testing)

Dự án tích hợp bộ kiểm thử tự động cho model dữ liệu, tuần tự hóa, phân trang bảng tin và xử lý hashtag.
Chạy unit tests thông qua Terminal:
```bash
./gradlew testDebugUnitTest
```
*(On Windows:* `gradlew.bat testDebugUnitTest`*)*

---
*Sản phẩm đồ án môn Lập trình Thiết bị Di động — **DLog Android App***
