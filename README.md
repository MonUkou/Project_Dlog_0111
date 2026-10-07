# 📱 DLog – Ứng dụng mạng xã hội chia sẻ ảnh

DLog là ứng dụng mạng xã hội dành cho việc chia sẻ hình ảnh và tương tác giữa người dùng. 
Ứng dụng hỗ trợ đăng bài viết, theo dõi người dùng, thích, bình luận, lưu bài viết và nhận thông báo.

Hệ thống được tổ chức thành **5 module chính**, gồm **13 nhóm chức năng và 34 chức năng (F01–F34)**.

---

# 1. 📌 Giới thiệu

DLog tập trung vào các chức năng cơ bản của một ứng dụng mạng xã hội chia sẻ ảnh:

- Quản lý tài khoản và đăng nhập.
- Quản lý hồ sơ cá nhân.
- Theo dõi và tìm kiếm người dùng.
- Đăng, sửa, xóa và xem bài viết.
- Thích và bình luận bài viết.
- Lưu bài viết.
- Nhận và quản lý thông báo.

---

# 2. 🎯 Mục tiêu

DLog được xây dựng nhằm cung cấp một ứng dụng mạng xã hội đơn giản, trong đó người dùng có thể:

- Tạo và quản lý tài khoản.
- Chia sẻ hình ảnh và nội dung.
- Theo dõi những người dùng khác.
- Tương tác với bài viết thông qua Like và Comment.
- Lưu lại những bài viết yêu thích.
- Theo dõi các hoạt động thông qua hệ thống thông báo.

---

# 3. ✨ Tính năng

## 3.1 🔐 Tài khoản & xác thực

### Đăng ký & đăng nhập

| Mã | Chức năng |
|---|---|
| F01 | Đăng ký tài khoản |
| F02 | Đăng nhập |
| F03 | Đăng xuất |

### Phiên đăng nhập

| Mã | Chức năng |
|---|---|
| F04 | Kiểm tra trạng thái đăng nhập |
| F05 | Lấy mã người dùng hiện tại |

---

## 3.2 👤 Hồ sơ & theo dõi

### Hồ sơ cá nhân

| Mã | Chức năng |
|---|---|
| F06 | Xem hồ sơ |
| F07 | Cập nhật hồ sơ |
| F08 | Đổi ảnh đại diện |

### Theo dõi

| Mã | Chức năng |
|---|---|
| F09 | Theo dõi người dùng |
| F10 | Bỏ theo dõi |
| F11 | Kiểm tra đang theo dõi |
| F12 | Danh sách người theo dõi |
| F13 | Danh sách đang theo dõi |

### Tìm kiếm

| Mã | Chức năng |
|---|---|
| F14 | Tìm kiếm người dùng |

---

## 3.3 📝 Bài viết

### Quản lý bài viết

| Mã | Chức năng |
|---|---|
| F15 | Đăng bài viết |
| F16 | Sửa bài viết |
| F17 | Xóa bài viết |
| F18 | Xem chi tiết bài viết |

### Bảng tin & trang cá nhân

| Mã | Chức năng |
|---|---|
| F19 | Xem bảng tin |
| F20 | Xem bài viết của một người |

### Lưu bài viết

| Mã | Chức năng |
|---|---|
| F21 | Lưu bài viết |
| F22 | Bỏ lưu bài viết |
| F23 | Danh sách bài đã lưu |

### Tiện ích

| Mã | Chức năng |
|---|---|
| F24 | Lưu ảnh về thiết bị |

---

## 3.4 ❤️ Tương tác

### Thích bài viết

| Mã | Chức năng |
|---|---|
| F25 | Thích bài viết |
| F26 | Bỏ thích |
| F27 | Kiểm tra đã thích |

### Bình luận

| Mã | Chức năng |
|---|---|
| F28 | Bình luận bài viết |
| F29 | Xem bình luận |
| F30 | Xóa bình luận |

---

## 3.5 🔔 Thông báo

### Tạo thông báo

| Mã | Chức năng |
|---|---|
| F31 | Tạo thông báo |

Thông báo được tạo khi xảy ra các hoạt động:

- Theo dõi người dùng.
- Thích bài viết.
- Bình luận bài viết.

### Xem & quản lý thông báo

| Mã | Chức năng |
|---|---|
| F32 | Danh sách thông báo |
| F33 | Đánh dấu đã đọc |
| F34 | Đếm thông báo chưa đọc |

---

# 4. 🏗️ Kiến trúc hệ thống

DLog được tổ chức theo các thành phần **Entity – Service – Interface**.

```text
                         DLog
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
     Entity            Service          Interface
        │                 │                 │
        │                 ├─ AuthService    ├─ IAuthService
        │                 ├─ UserService    └─ INotificationService
        │                 ├─ PostService
        │                 ├─ InteractionService
        │                 └─ NotificationService
        │
        ├─ BaseEntity
        ├─ User
        ├─ Follow
        ├─ Post
        ├─ Comment
        ├─ Like
        ├─ SavedPost
        └─ Notification
