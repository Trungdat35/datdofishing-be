# DatDo Fishing – Backend microservice

Backend cho cửa hàng đồ câu (tham khảo chức năng docauduli.com). Chạy hoàn toàn trên máy local bằng Docker Desktop.

- Java 21, Spring Boot 3.5, Spring Cloud 2025.0, Maven multi-module
- MySQL 8.4 (mỗi service 1 database), Redis, Kafka (KRaft), RabbitMQ, Mailpit (SMTP local)
- JWT RS256 + đăng nhập Google, 2 site: cửa hàng và admin
- order-service chạy 3 instance sau load balancer, saga đặt hàng qua Kafka, SSE realtime

## 1. Các service

| Service | Cổng | Database | Việc chính |
|---|---|---|---|
| api-gateway | 8080 | Redis | Cổng duy nhất cho frontend: định tuyến, kiểm tra JWT, CORS, rate limit, chia tải order-service |
| auth-service | 8081 | auth_db, Redis | Đăng ký, đăng nhập (khách + admin), Google, refresh token, hồ sơ, địa chỉ |
| catalog-service | 8082 | catalog_db, Redis cache | Danh mục, thương hiệu, sản phẩm, biến thể, ảnh (URL) |
| inventory-service | 8083 | inventory_db | Tồn kho theo biến thể, giữ hàng cho đơn |
| cart-service | 8084 | Redis | Giỏ hàng |
| order-service ×3 | 8085 | order_db, Redis | Đặt hàng (bắt buộc đăng nhập), saga, quản lý đơn |
| promotion-service | 8086 | promotion_db, Redis | Flash sale, đếm suất bằng Redis Lua |
| content-service | 8087 | content_db | Bài viết kinh nghiệm, trang chính sách, banner |
| notification-service | 8088 | notification_db, Redis | Email qua RabbitMQ, thông báo realtime SSE |

Công cụ quan sát khi chạy:

| Công cụ | Địa chỉ | Tài khoản |
|---|---|---|
| MySQL (DBeaver) | localhost:3306 | root / 123456 (mỗi DB còn có user riêng, vd auth_user/auth_pass, xem `infra/mysql/init.sql`) |
| Redis Insight | http://localhost:5540 | thêm database: host `redis`, port `6379` |
| Kafka UI | http://localhost:8090 | |
| RabbitMQ | http://localhost:15672 | datdofishing / 123456 |
| Mailpit (xem email đã gửi) | http://localhost:8025 | |
| Swagger từng service | http://localhost:8081/swagger-ui.html (đổi cổng theo service) | |

## 2. Chuẩn bị

1. Docker Desktop → Settings → Resources: cấp **10–12 GB RAM** (tối thiểu 8 GB).
2. Sao chép `.env.example` thành `.env`. Điền `GOOGLE_CLIENT_ID` nếu muốn thử đăng nhập Google (xem mục 6).

Không cần cài JDK hay Maven: code được build bên trong Docker.

## 3. Chạy bằng Docker Compose

Mở terminal (PowerShell) tại thư mục `be`:

```powershell
# Lần đầu: build và chạy toàn bộ (mất 5–15 phút để tải thư viện Maven)
docker compose --profile apps up -d --build

# Xem trạng thái
docker compose --profile apps ps

# Xem log một service
docker compose logs -f auth-service
```

Chỉ chạy hạ tầng để code trong IntelliJ (service chạy từ IDE sẽ kết nối localhost):

```powershell
docker compose up -d
```

Dừng: `docker compose --profile apps down` (thêm `-v` nếu muốn xóa sạch dữ liệu).

### Kết nối DBeaver
New Connection → MySQL → Host `localhost`, Port `3306`, User `root`, Password `123456`. Ở tab **Driver properties** đặt
`allowPublicKeyRetrieval=true` (và `useSSL=false`) để khỏi lỗi "Public Key Retrieval is not allowed". Khi mở rộng
connection sẽ thấy đủ 7 database: auth_db, catalog_db, inventory_db, order_db,
promotion_db, content_db, notification_db.

## 4. Thử nhanh các luồng chính

Mở file `api-test.http` bằng IntelliJ (HTTP Client) hoặc VS Code (extension REST Client) và chạy lần lượt. Hoặc dùng curl:

```bash
# Đăng ký -> mở http://localhost:8025 sẽ thấy email thông tin tài khoản
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"email":"khach@test.vn","password":"12345678","fullName":"Nguyễn Văn A","phone":"0912345678"}'

# Xem sản phẩm (không cần đăng nhập)
curl "http://localhost:8080/api/catalog/products?category=can-cau-dai"

# Đặt hàng (cần token). Chưa đăng nhập sẽ nhận 401
curl -X POST http://localhost:8080/api/orders -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" -H "Idempotency-Key: 1" \
  -d '{"items":[{"variantId":1,"quantity":1}],"shipping":{"recipientName":"A","phone":"0912345678","address":"Hà Nội"}}'
```

Tài khoản admin tạo sẵn: `admin@datdofishing.local` / `Admin@123` (đăng nhập tại `POST /api/admin/auth/login`).

### Kiểm tra load balancer của order-service
Gọi nhiều lần `GET /api/orders/instance` (có token): giá trị `instanceId` và header `X-Instance-Id` lần lượt là
`order-1`, `order-2`, `order-3`. Tắt 1 instance (`docker stop datdofishing-order-service-2`) vẫn đặt hàng bình thường.

### Kiểm tra SSE
```bash
curl -N http://localhost:8080/api/admin/notifications/stream -H "Authorization: Bearer <adminToken>"
```
Giữ terminal đó, đặt một đơn ở terminal khác: admin nhận ngay sự kiện `notification` "Đơn hàng mới".

## 5. Luồng hoạt động

- **Đăng ký:** auth-service lưu user + ghi sự kiện vào bảng outbox (cùng transaction) → OutboxRelay đẩy lên Kafka
  `user.registered` → notification-service đẩy job vào RabbitMQ `email.send` → worker gửi email (lỗi thì retry 4 lần,
  vẫn lỗi thì vào `email.send.dlq`). Email chỉ chứa thông tin tài khoản + link xác thực, không chứa mật khẩu.
- **Đặt hàng (saga):** gateway kiểm tra JWT → order-service (1 trong 3 instance) lấy giá từ catalog, giữ suất flash sale
  ở promotion, tạo đơn `PENDING` + outbox `order.created` → inventory-service giữ hàng → `inventory.reserved`
  (đơn `CONFIRMED`) hoặc `inventory.rejected` (đơn `CANCELLED`, trả suất flash sale) → notification-service gửi SSE + email.
- **Ảnh sản phẩm:** admin dán URL ảnh (https, đuôi jpg/png/webp), ví dụ
  `https://bizweb.dktcdn.net/thumb/medium/100/436/612/products/can-cau-dai-v5-big-fish-cac-kich-thuoc.jpg?v=1740132741173`.
  Ảnh lấy từ docauduli chỉ dùng để học và chạy local.

Bảng đường dẫn chính (qua gateway `http://localhost:8080`):

| Nhóm | Công khai | Cần đăng nhập | Admin (`ROLE_ADMIN`) |
|---|---|---|---|
| Auth | `/api/auth/register`, `/login`, `/google`, `/refresh`, `/forgot-password`, `/reset-password`, `/verify-email` | `/api/auth/logout`, `/api/users/me/**` | `/api/admin/auth/login` (công khai), `/api/admin/users/**` |
| Catalog | `GET /api/catalog/**` | | `/api/admin/catalog/**` |
| Inventory | `GET /api/inventory/stock` | | `/api/admin/inventory/**` |
| Cart | | `/api/cart/**` | |
| Order | | `/api/orders/**` | `/api/admin/orders/**` |
| Promotion | `GET /api/promotions/flash-sales/current` | | `/api/admin/promotions/**` |
| Content | `GET /api/content/**` | | `/api/admin/content/**` |
| Notification | | `/api/notifications/**` (SSE: `/stream`) | `/api/admin/notifications/**` (SSE: `/stream`) |

## 6. Đăng nhập Google

1. Vào https://console.cloud.google.com/apis/credentials → Create credentials → OAuth client ID → Web application.
2. Authorized JavaScript origins: `http://localhost:8081`, `http://localhost:3000`, `http://localhost:3001`.
3. Chép Client ID vào `.env` (`GOOGLE_CLIENT_ID=...`) rồi `docker compose --profile apps up -d auth-service`.
4. Mở http://localhost:8081/google-test.html, bấm nút Google: trang hiện JWT của hệ thống.

## 7. Chạy trên Kubernetes của Docker Desktop

1. Docker Desktop → Settings → Kubernetes → Enable Kubernetes (chọn kiểu kubeadm để dùng chung image với Docker).
2. Cài ingress-nginx (một lần):
   ```powershell
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/cloud/deploy.yaml
   ```
3. Build image: `docker compose --profile apps build`
4. Triển khai: `kubectl apply -k k8s` rồi theo dõi `kubectl get pods -n datdofishing -w`
5. Truy cập: API `http://api.localhost`, email `http://mail.localhost`, RabbitMQ `http://rabbit.localhost`,
   MySQL cho DBeaver `localhost:30306`.
6. order-service chạy 3 pod: `kubectl get pods -n datdofishing -l app=order-service`. Xóa thử 1 pod, Kubernetes tự tạo lại.

Nếu Docker Desktop dùng kiểu cluster "kind", cần nạp image vào cluster trước khi apply.
Dừng Compose trước khi chạy Kubernetes để tránh tranh cổng và RAM.

## 8. Gửi email thật (tùy chọn)
Mặc định email đi vào Mailpit. Muốn gửi qua Gmail, thêm vào `environment` của notification-service:
`MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `SPRING_MAIL_USERNAME=...`, `SPRING_MAIL_PASSWORD=<App Password>`,
`MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`.

## 9. Cấu trúc thư mục

```
be/
├── pom.xml                 # parent
├── common/                 # lỗi chuẩn, JWT resource server, outbox, sự kiện Kafka
├── api-gateway/ … notification-service/
├── infra/mysql/init.sql # tạo 7 database + user
├── docker-compose.yml      # hạ tầng (mặc định) + profile "apps"
├── Dockerfile              # build mọi service, chọn bằng build-arg MODULE
├── k8s/                    # Kustomize cho Kubernetes
└── api-test.http           # bộ request thử nghiệm
```

## Ghi chú MySQL
- Cổng 3306: nếu máy đã cài MySQL khác chiếm cổng này, đổi `"3306:3306"` trong `docker-compose.yml` (ví dụ `"3307:3306"`)
  khi dùng DBeaver; các service trong Docker vẫn nối qua `mysql:3306`. Chạy service bằng IDE thì sửa luôn cổng trong `spring.datasource.url`.
- Script `infra/mysql/init.sql` chỉ chạy khi volume còn trống. Đổi từ bản PostgreSQL cũ: `docker compose down -v` rồi `up -d` lại.
- Mọi thời điểm lưu UTC (`DATETIME(6)`); các cột JSON (specs, tags, meta, outbox payload) dùng kiểu JSON của MySQL.
- Flyway trên MySQL không rollback được DDL. Nếu một migration lỗi giữa chừng, xóa database đó (hoặc `down -v`) rồi chạy lại.
#   d a t d o f i s h i n g - b e  
 