# 📱 DLog - Ứng dụng chia sẻ ảnh trên Android

DLog là ứng dụng Android dành cho việc chia sẻ và khám phá hình ảnh. Người dùng có thể đăng một hoặc nhiều ảnh, theo dõi tài khoản khác, tương tác trên bảng tin, trò chuyện riêng và quản lý bộ sưu tập đã lưu. Ứng dụng sử dụng Firebase cho tài khoản và dữ liệu, Cloudinary để lưu ảnh, đồng thời có bộ nhớ SQLite hỗ trợ xem lại nội dung đã tải khi mất kết nối.

## ✨ Chức năng nổi bật

### 🔐 Tài khoản và bảo mật

- Đăng ký, đăng nhập bằng email và mật khẩu.
- Đăng nhập và đăng ký bằng tài khoản Google.
- Gửi email đặt lại mật khẩu.
- Duy trì phiên đăng nhập trên thiết bị.
- Đăng xuất và kiểm tra tài khoản bị quản trị viên khóa.

### 👤 Hồ sơ và kết nối người dùng

- Xem và chỉnh sửa hồ sơ cá nhân.
- Cập nhật tên hiển thị, tên tài khoản, phần giới thiệu, ảnh đại diện và ảnh bìa.
- Xem hồ sơ của người dùng khác.
- Theo dõi hoặc bỏ theo dõi tài khoản.
- Xem danh sách người theo dõi và danh sách đang theo dõi.
- Mở cuộc trò chuyện trực tiếp từ trang hồ sơ.

### 🖼️ Chia sẻ và quản lý ảnh

- Đăng một hoặc nhiều ảnh trong cùng một nội dung chia sẻ.
- Chọn ảnh từ thiết bị hoặc thư viện ảnh mẫu có sẵn trong ứng dụng.
- Tải ảnh lên Cloudinary và lưu đường dẫn trong Firestore.
- Chỉnh ảnh trước khi đăng: xoay, cắt theo tỉ lệ và áp dụng bộ lọc màu.
- Gắn mô tả ngắn và hashtag cho ảnh.
- Sửa hoặc xóa nội dung do chính người dùng đăng.
- Hiển thị nhiều ảnh theo bố cục collage và chỉ báo số ảnh còn lại.
- Xem ảnh toàn màn hình, chuyển qua lại giữa các ảnh, phóng to và vuốt xuống để đóng.
- Xem thông tin ảnh và tải ảnh đang xem về thiết bị.
- Báo cáo ảnh để quản trị viên kiểm tra.

### 📰 Bảng tin và khám phá

- Bảng tin ưu tiên nội dung từ các tài khoản đang theo dõi.
- Tải thêm nội dung theo từng trang.
- Thu gọn mô tả dài và mở rộng bằng thao tác "Xem thêm".
- Tìm kiếm tài khoản và hashtag.
- Lọc kết quả ảnh theo nội dung mới nhất, cũ nhất hoặc phổ biến.
- Hiển thị lưới ảnh trên trang hồ sơ.
- Nút quay nhanh lên đầu bảng tin.

### ❤️ Tương tác

- Thích và bỏ thích ảnh.
- Bình luận và xóa bình luận của chính mình.
- Xem bình luận ngay bên dưới nội dung trên bảng tin.
- Lưu hoặc bỏ lưu nội dung.
- Xem danh sách nội dung đã lưu trên trang cá nhân.
- Chia sẻ nội dung qua các ứng dụng khác trên thiết bị.

### 🔔 Thông báo và tin nhắn

- Nhận thông báo khi có người theo dõi, thích hoặc bình luận.
- Hiển thị tên người gửi, trạng thái đã đọc và số thông báo chưa đọc.
- Làm mới và đánh dấu thông báo đã đọc.
- Xem danh sách cuộc trò chuyện.
- Tìm người dùng để bắt đầu trò chuyện.
- Gửi và nhận tin nhắn riêng.

### 🌙 Giao diện và trải nghiệm sử dụng

- Hỗ trợ giao diện sáng và tối.
- Màu sắc, biểu tượng và thanh điều hướng được đồng bộ giữa các màn hình.
- Hỗ trợ bố cục dọc và ngang.
- Khi xoay ngang, thanh điều hướng chuyển sang cạnh phải; bảng tin và hồ sơ được chia thành hai vùng để tận dụng chiều rộng màn hình.
- Chuyển động giao diện tuân theo thiết lập giảm chuyển động của thiết bị.
- Hiển thị trạng thái ngoại tuyến và hạn chế các thao tác cần kết nối mạng.

### 🗄️ Dữ liệu ngoại tuyến

- Lưu các trang đã tải vào cơ sở dữ liệu SQLite cục bộ.
- Có bộ nhớ đệm cho bảng tin, hồ sơ, nội dung đã lưu và bình luận đã xem.
- Hiển thị lại dữ liệu đã lưu khi thiết bị mất mạng.
- Cập nhật bản lưu cục bộ sau khi tải được dữ liệu mới từ máy chủ.

### 🛡️ Quản trị hệ thống

- Phân quyền tài khoản người dùng và quản trị viên.
- Xem thống kê tổng quan về người dùng, nội dung, bình luận, báo cáo và tương tác.
- Khóa hoặc mở khóa tài khoản.
- Ẩn hoặc hiện nội dung và bình luận.
- Xem danh sách báo cáo ảnh do người dùng gửi.

## 🧰 Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 11 |
| Nền tảng | Android SDK, minSdk 30, targetSdk 37 |
| Giao diện | XML Layout, Material Components, AppCompat |
| Xác thực | Firebase Authentication, Google Identity |
| Cơ sở dữ liệu trực tuyến | Cloud Firestore |
| Lưu trữ ảnh | Cloudinary; Firebase Storage được cấu hình trong dự án |
| Dữ liệu cục bộ | SQLite |
| Tải và hiển thị ảnh | Glide |
| Kiểm thử | JUnit, AndroidX Test, Espresso |
| Hệ thống build | Gradle Kotlin DSL |

## 🏗️ Kiến trúc ứng dụng

Mã nguồn được tổ chức theo các lớp `Activity`, `Adapter`, `Model`, `Service`, `Interface`, `Util` và `View`.

```text
DLog
├── Activity
│   ├── Xác thực và hồ sơ
│   ├── Bảng tin và khám phá
│   ├── Đăng, chỉnh sửa và xem ảnh
│   ├── Thông báo và tin nhắn
│   └── Quản trị hệ thống
├── Adapter
│   ├── PostAdapter
│   ├── CommentAdapter
│   ├── NotificationAdapter
│   ├── ChatMessageAdapter
│   └── FollowUserAdapter
├── Model
│   ├── User
│   ├── Follow
│   ├── Post
│   ├── Comment
│   ├── Like
│   ├── SavedPost
│   └── Notification
├── Service
│   ├── AuthService
│   ├── UserService
│   ├── PostService
│   ├── InteractionService
│   ├── NotificationService
│   ├── ChatService
│   ├── CloudinaryUploader
│   └── OfflinePages
├── Interface
│   ├── IAuthService
│   └── INotificationService
├── Util
│   ├── HashtagParser
│   ├── FeedPageSelector
│   ├── PhotoLibrary
│   ├── ImagePickerDialog
│   ├── BottomNavHelper
│   └── UiMotion
└── View
    ├── PhotoCollageView
    └── ZoomDismissImageView
```

Luồng dữ liệu chính:

```text
Giao diện → Activity/Adapter → Service → Firebase hoặc Cloudinary
                              ↓
                         SQLite cache
```

## 📂 Cấu trúc thư mục

```text
app/src/main/
├── java/com/example/myapplication/
│   ├── adapter/
│   ├── model/
│   ├── service/
│   ├── ui/
│   ├── util/
│   └── view/
├── res/
│   ├── drawable/
│   ├── drawable-nodpi/
│   ├── layout/
│   ├── layout-land/
│   ├── values/
│   └── values-night/
└── AndroidManifest.xml
```

## 🚀 Cài đặt và chạy ứng dụng

### Yêu cầu

- Android Studio có Android SDK phù hợp.
- JDK đi kèm Android Studio hoặc JDK tương thích Java 11.
- Thiết bị thật hoặc máy ảo chạy Android 11 (API 30) trở lên.
- Kết nối Internet để sử dụng Firebase, Google Sign-In và Cloudinary.

### Tải mã nguồn

```bash
git clone https://github.com/MonUkou/Project_Dlog_0111.git
cd Project_Dlog_0111
git switch main
```

### Chạy trong Android Studio

1. Chọn **Open** và mở thư mục `Project_Dlog_0111`.
2. Chọn **Trust Project** khi Android Studio yêu cầu.
3. Chờ Gradle Sync hoàn tất.
4. Chọn thiết bị hoặc máy ảo Android.
5. Chọn cấu hình `app` và nhấn **Run**.

File `local.properties` không được lưu trên GitHub. Android Studio sẽ tạo file này theo đường dẫn Android SDK trên từng máy.

## 🧪 Kiểm thử

Dự án có kiểm thử cho model người dùng, model nội dung, tuần tự hóa dữ liệu, xử lý hashtag và phân trang bảng tin.

Chạy unit test bằng lệnh:

```bash
./gradlew testDebugUnitTest
```

Trên Windows:

```cmd
gradlew.bat testDebugUnitTest
```

## 🔒 Lưu ý về cấu hình

- `app/google-services.json` dùng để kết nối ứng dụng với Firebase.
- Cấu hình Cloudinary sử dụng cloud name và unsigned upload preset của dự án.
- Không đưa mật khẩu, private key, service-account key hoặc `local.properties` lên repository.
- Các thao tác tải dữ liệu mới, đăng ảnh, thích, bình luận, theo dõi và nhắn tin cần kết nối mạng.

## 📄 Trạng thái dự án

Phiên bản hiện tại là sản phẩm đồ án Android hoàn chỉnh, tập trung vào trải nghiệm chia sẻ ảnh, tương tác cộng đồng, khả năng sử dụng khi ngoại tuyến và giao diện thích ứng theo hướng màn hình.
