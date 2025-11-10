# PHỤ LỤC — EShop Micro: Hướng dẫn Build & Run (Windows PowerShell)

Tài liệu này tổng hợp **đầy đủ câu lệnh CLI** và **thứ tự khởi chạy** khuyến nghị cho dự án microservices ECom của bạn. Ngôn ngữ: **Tiếng Việt**.  
Áp dụng cho **Docker Desktop (Compose)** + **Maven** + **JDK 17**.

> Mẹo: nên dùng **PowerShell**. Nếu thấy các flag `-D...` bị PowerShell hiểu nhầm, hãy thêm `--%` ngay sau `mvn` để PowerShell **không parse** tham số.

---

## 0) Yêu cầu cài đặt (one-time)

- **JDK 17+**, **Maven 3.9+**, **Docker Desktop** (có Compose), **Git**.
- Kiểm tra nhanh:
```powershell
java -version
mvn -v
docker -v
docker compose version
```

---

## 1) Cứng hoá Maven parent (khuyến nghị 1 lần)

Trong **`pom.xml` root** (parent), đảm bảo ép dùng Java 17 và plugin hiện đại:

```xml
<properties>
  <java.version>17</java.version>
  <maven.compiler.release>17</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

  <spring-boot.version>3.3.4</spring-boot.version>
  <spring-cloud.version>2023.0.3</spring-cloud.version>
</properties>

<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <release>${maven.compiler.release}</release>
          <encoding>${project.build.sourceEncoding}</encoding>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
      </plugin>
    </plugins>
  </pluginManagement>
</build>
```

> Lưu ý:
> - **common-service** là **library** ⇒ **không** chạy `repackage`. Dùng JAR “mỏng” chuẩn (không có `BOOT-INF/classes`).
> - Các **service chạy được** (gateway, eureka, config-server, …) **có** `spring-boot-maven-plugin` + goal `repackage`.

---

## 2) Build bằng Maven (trên máy host)

### 2.1 Build nhanh theo module
```powershell
# Build common-service (library) + settings-service (và các module phụ thuộc)
mvn -pl common-service,settings-service -am clean install -DskipTests
```

### 2.2 Build toàn bộ
```powershell
mvn clean install -DskipTests
# Nếu PowerShell làm khó các flag -D, dùng:
mvn --% clean install -DskipTests
```

### 2.3 Lỗi “Source option 5 / Target option 5”
- Thêm/đảm bảo **Java 17** như phần 1.
- Hoặc tạm thời ép ở module:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.11.0</version>
  <configuration><release>17</release></configuration>
</plugin>
```

### 2.4 Dọn artifact cũ của common-service (nếu từng “repackage”)
```powershell
Remove-Item "$env:USERPROFILE\.m2\repository\com\ducnhu\common-service" -Recurse -Force
# hoặc
mvn dependency:purge-local-repository -DmanualInclude="com.ducnhu:common-service" -DreResolve=false
```

---

## 3) Docker Compose: Khởi động hạ tầng trước

> **Chú ý:** Khoá `version:` ở đầu file `docker-compose.yml` đã lỗi thời → có thể bỏ.

### 3.1 Kafka, Redis, UI
```powershell
docker compose up -d zookeeper kafka kafka-ui redis redisinsight
docker compose ps
docker compose logs -f kafka
```

**Kafka kiểm tra nhanh:**
```powershell
# Liệt kê topics
docker exec -it <kafka-container> kafka-topics --bootstrap-server kafka:9092 --list

# Tạo topic test
docker exec -it <kafka-container> kafka-topics --bootstrap-server kafka:9092 --create --topic test-topic --partitions 1 --replication-factor 1

# Producer
docker exec -it <kafka-container> kafka-console-producer --broker-list kafka:9092 --topic test-topic

# Consumer
docker exec -it <kafka-container> kafka-console-consumer --bootstrap-server kafka:9092 --topic test-topic --from-beginning
```

**Redis kiểm tra nhanh:**
```powershell
docker exec -it <redis-container> redis-cli -a ducnhu1234 ping   # Kỳ vọng: PONG
```

### 3.2 MySQL — chọn 1 trong 2

**A) Dùng MySQL trong Docker (mặc định trong compose):**
```powershell
docker compose up -d mysql
docker compose logs -f mysql
```
- JDBC URL **bên trong container**: `jdbc:mysql://mysql:3306/<db>`

**B) Dùng MySQL cài sẵn trên Windows (tránh trùng cổng 3306):**
- **Không** start `mysql` trong compose.
- Set URL cho service sang host: `jdbc:mysql://host.docker.internal:3306/eshopdb`
- Khởi động hạ tầng **không có mysql**:
```powershell
docker compose up -d zookeeper kafka kafka-ui redis redisinsight
```

*(Tuỳ chọn)* ELK (Elastic, Logstash, Kibana) — chỉ dùng khi đã cấu hình:
```powershell
docker compose up -d elasticsearch logstash kibana
```

---

## 4) Nhóm Spring core: Thứ tự & lệnh

1) **Eureka**
```powershell
docker compose up -d --build eureka
docker compose logs -f eureka
# UI: http://localhost:8761
```

2) **Config Server** (chạy chế độ **native**/classpath)
```powershell
docker compose up -d --build --no-deps config-server
docker compose up -d --build config-server
docker compose logs -f config-server
# Health: http://localhost:8888/actuator/health
# Ví dụ endpoint native classpath:
# http://localhost:8888/gateway/default
```

3) **Gateway**
```powershell
docker compose up -d --build --no-deps gateway
docker compose up -d --build gateway
docker compose logs -f gateway
# Health: http://localhost:8080/actuator/health
```

> Nếu gateway log cảnh báo “Could not locate configserver via discovery”, không sao — miễn config-server **đã lên** và endpoint `/gateway/{profile}` trả về OK.

---

## 5) Nhóm business services: Thứ tự khuyến nghị

> Start theo **chuỗi phụ thuộc** này để các luồng Kafka request-reply có đủ backend.

```powershell
# 1) settings-service (cung cấp cấu hình email/paypal qua Kafka)
docker compose up -d --build --no-deps settings-service
docker compose up -d --build settings-service
docker compose logs -f settings-service

# 2) auth-service
docker compose up -d --build --no-deps auth-service
docker compose up -d --build auth-service
docker compose logs -f auth-service

# 3) customer-service
docker compose up -d --build --no-deps customer-service
docker compose up -d --build customer-service

# 4) catalog-service
#  (Nếu CHƯA dùng Elasticsearch, hãy vô hiệu auto-config ES của service này hoặc để elasticsearch container chạy sẵn)
docker compose up -d --build --no-deps catalog-service
docker compose up -d --build catalog-service

# 5) cart-service
docker compose up -d --build --no-deps cart-service
docker compose up -d --build cart-service

# 6) shipping-service
docker compose up -d --build --no-deps shipping-service
docker compose up -d --build shipping-service

# 7) order-service
docker compose up -d --build --no-deps order-service
docker compose up -d --build order-service

# 8) payment-service
docker compose up -d --build --no-deps payment-service
docker compose up -d --build payment-service

# 9) checkout-service
docker compose up -d --build --no-deps checkout-service
docker compose up -d --build checkout-service

```

**Kiểm tra đăng ký Eureka:** mở `http://localhost:8761` và quan sát các service báo **UP**.

---

## 6) Endpoint/UI hữu ích

- **Eureka UI**: http://localhost:8761  
- **Gateway health**: http://localhost:8080/actuator/health  
- **Config Server health**: http://localhost:8888/actuator/health  
- **Config by app/profile**: `http://localhost:8888/{app}/{profile}` (vd: `/gateway/default`)  
- **Kafka UI**: http://localhost:8085  (Cluster name: `local`)  
- **RedisInsight**: http://localhost:5540  
- **Kibana**: http://localhost:5601  

---

## 7) Quy trình hằng ngày (sau khi đổi code)

### 7.1 Build lại **đúng module** trên host
```powershell
# Ví dụ: bạn sửa common-service + settings-service
mvn -pl common-service,settings-service -am install -DskipTests
```

### 7.2 Rebuild container **không kéo lại dependency**
```powershell
docker compose up -d --no-deps --build settings-service
docker compose logs -f settings-service
```

### 7.3 Lệnh Docker thường dùng
```powershell
docker compose ps
docker compose logs -f <service>
docker compose restart <service>
docker compose rm -sf <service>

# Dọn rác image/container không dùng
docker system prune -f
```

---

## 8) Khắc phục sự cố nhanh

- **`no main manifest attribute, in /opt/app/app.jar`**  
  Dockerfile phải chạy **`spring-boot:repackage`** và copy đúng **fat jar**:
  ```dockerfile
  # stage build:
  RUN mvn -q -DskipTests package spring-boot:repackage
  # stage run:
  COPY --from=build /app/<module>/target/<module>-*.jar /opt/app/app.jar
  ```

- **Config Server: “You need to configure a uri for the git repository.”**  
  Dùng profile **native** với classpath:
  ```properties
  spring.profiles.active=docker
  spring.cloud.config.server.native.search-locations=classpath:/config
  ```

- **Cổng MySQL 3306 bận**  
  Dùng **Option B** (MySQL cài sẵn) và chỉnh URL của service sang `host.docker.internal`.

- **`package ... does not exist` với class trong common-service**  
  Lý do: `common-service` từng bị **repackage** (fat jar) ⇒ compile-time không thấy class.  
  Cách xử lý: **tắt repackage** ở common-service, **xoá artifact** cũ trong `~/.m2`, rồi **build lại** (mục 2.4).

- **Kafka/Redis chưa sẵn sàng**  
  Mở **Kafka UI** / **RedisInsight** để xác nhận hoạt động trước khi khởi động các orchestrator.

---

## 9) Tắt hệ thống

```powershell
# Tắt container
docker compose down

# Tắt & xoá volumes (CẨN TRỌNG: xoá dữ liệu MySQL…)
docker compose down -v
```

---

## 10) Ghi chú cấu hình DB cho container vs máy host

- **Dùng MySQL trong Docker**:  
  `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/eshopdb`
- **Dùng MySQL trên Windows**:  
  `SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/eshopdb`

Các biến khác (ví dụ settings-service):
```
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka
SPRING_REDIS_HOST=redis
SPRING_REDIS_PASSWORD=ducnhu1234
```

---

### Thứ tự chuẩn để chạy toàn bộ hệ thống

1) **Hạ tầng**: `zookeeper, kafka, kafka-ui, redis, redisinsight` (+ `mysql` nếu dùng trong Docker)  
2) **Core Spring**: `eureka` → `config-server` → `gateway`  
3) **Business**: `settings-service` → `auth-service` → `customer-service` → `catalog-service` → `cart-service` → `shipping-service` → `order-service` → `payment-service` → `checkout-service`

> Làm theo đúng thứ tự trên sẽ hạn chế tối đa lỗi “không tìm thấy service” hoặc “kết nối Kafka/Redis/DB thất bại”.

---

## 11) Bỏ qua `depends_on` khi dev (không đụng file gốc)

### 11.1 Cách nhanh bằng CLI (khuyên dùng hàng ngày)
- **Rebuild & (re)start đúng 1 service** mà **không** đụng dependencies:
```powershell
docker compose up -d --build --no-deps <service>
```

- **Giữ nguyên** các container đang chạy, **không tạo lại**:
```powershell
docker compose up -d --no-recreate
```

- Ví dụ thường dùng:
```powershell
# Chỉ build & restart shipping-service, không kéo eureka/config/gateway
docker compose up -d --build --no-deps shipping-service

# Xem logs realtime của service đó
docker compose logs -f shipping-service

# Nếu chỉ muốn restart nhẹ (không rebuild image)
docker compose restart shipping-service
```

> Mẹo kiểm tra có thực sự restart không:
```powershell
docker inspect -f '{{.Name}} RestartCount={{.RestartCount}} StartedAt={{.State.StartedAt}}' <container_name>
```

---

### 11.2 Dùng file override để “vô hiệu” `depends_on` (không sửa file gốc)
Tạo thêm file **`docker-compose.override.nodeps.yml`** (song song với `docker-compose.yml`), nội dung:

```yaml
# docker-compose.override.nodeps.yml
# Mục tiêu: override để depends_on trống (bỏ qua phụ thuộc) + thêm env dev cho Spring

x-dev-env: &dev_env
  SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
  SPRING_CLOUD_CONFIG_RETRY_MAX_ATTEMPTS: "1"
  SPRING_CLOUD_CONFIG_RETRY_INITIAL_INTERVAL: "500"
  SPRING_MAIN_LAZY_INITIALIZATION: "true"

services:
  gateway:
    depends_on: []   # bỏ phụ thuộc khi up riêng gateway
    environment:
      <<: *dev_env

  auth-service:
    depends_on: []
    environment:
      <<: *dev_env

  catalog-service:
    depends_on: []
    environment:
      <<: *dev_env

  cart-service:
    depends_on: []
    environment:
      <<: *dev_env

  customer-service:
    depends_on: []
    environment:
      <<: *dev_env

  settings-service:
    depends_on: []
    environment:
      <<: *dev_env

  shipping-service:
    depends_on: []
    environment:
      <<: *dev_env

  order-service:
    depends_on: []
    environment:
      <<: *dev_env

  checkout-service:
    depends_on: []
    environment:
      <<: *dev_env

  payment-service:
    depends_on: []
    environment:
      <<: *dev_env
```

**Cách chạy với override (bỏ qua phụ thuộc):**
```powershell
# Khởi động hạ tầng + core 1 lần (theo mục 3 & 4)
docker compose up -d zookeeper kafka kafka-ui redis redisinsight
docker compose up -d --build eureka config-server gateway

# Sau đó dev từng service bằng override bỏ deps:
docker compose -f docker-compose.yml -f docker-compose.override.nodeps.yml `
  up -d --build --no-deps shipping-service
```

> Ghi chú:
> - `depends_on: []` trong override **không xoá** file gốc; chỉ áp dụng khi bạn truyền **kèm** file override.
> - Bộ env `*dev_env` giúp app **không fail-fast** khi config-server chưa sẵn và **khởi động nhanh** hơn với lazy init.

---

### 11.3 Tuỳ chọn: Compose **profiles** để tách nhóm (chỉ thêm, không xoá)
Bạn có thể thêm một override nữa **gắn profile** cho các nhóm (infra/core/app) để bật/tắt nhanh theo nhóm.

Tạo file **`docker-compose.override.profiles.yml`**:

```yaml
# docker-compose.override.profiles.yml
services:
  zookeeper:     { profiles: ["infra"] }
  kafka:         { profiles: ["infra"] }
  kafka-ui:      { profiles: ["infra"] }
  redis:         { profiles: ["infra"] }
  redisinsight:  { profiles: ["infra"] }

  eureka:        { profiles: ["core"] }
  config-server: { profiles: ["core"] }
  gateway:       { profiles: ["core"] }

  settings-service: { profiles: ["app"] }
  auth-service:     { profiles: ["app"] }
  customer-service: { profiles: ["app"] }
  catalog-service:  { profiles: ["app"] }
  cart-service:     { profiles: ["app"] }
  shipping-service: { profiles: ["app"] }
  order-service:    { profiles: ["app"] }
  payment-service:  { profiles: ["app"] }
  checkout-service: { profiles: ["app"] }
```

**Cách dùng profiles kết hợp bỏ deps:**
```powershell
# Lên infra + core 1 lần
docker compose -f docker-compose.yml -f docker-compose.override.profiles.yml `
  --profile infra --profile core up -d

# Dev 1 service (không kéo deps):
docker compose -f docker-compose.yml `
  -f docker-compose.override.nodeps.yml `
  -f docker-compose.override.profiles.yml `
  --profile app up -d --build --no-deps shipping-service
```

---

### 11.4 (Tuỳ chọn) Compose **Watch** để “rebuild khi có thay đổi”
Nếu Docker Desktop của bạn hỗ trợ `docker compose watch`, bạn có thể thêm một override để tự rebuild khi đổi code.

Tạo file **`docker-compose.override.watch.yml`**:
```yaml
# docker-compose.override.watch.yml
services:
  shipping-service:
    develop:
      watch:
        - action: rebuild
          path: ./shipping-service/src
        - action: rebuild
          path: ./shipping-service/pom.xml
```

Chạy:
```powershell
docker compose -f docker-compose.yml `
  -f docker-compose.override.nodeps.yml `
  -f docker-compose.override.watch.yml `
  watch -d shipping-service
```

> Gợi ý: Với Java, `action: rebuild` phù hợp hơn `sync` (vì cần compile). Nếu bạn có cơ chế hot-swap (Spring DevTools + chạy app qua IDE), hãy dùng `sync` tới `target/classes` thay vì rebuild toàn image.

---

### 11.5 PowerShell helpers (cho nhanh tay)
Thêm vào file `profile.ps1` của PowerShell (hoặc tạo `dev.ps1` trong root repo):

```powershell
function dc-up-nodeps {
  param([Parameter(Mandatory=$true)][string]$svc)
  docker compose up -d --build --no-deps $svc
}

function dc-up-nodeps-ovr {
  param([Parameter(Mandatory=$true)][string]$svc)
  docker compose -f docker-compose.yml -f docker-compose.override.nodeps.yml `
    up -d --build --no-deps $svc
}

function dc-no-recreate {
  docker compose up -d --no-recreate
}
```

Dùng:
```powershell
dc-up-nodeps shipping-service
# hoặc
dc-up-nodeps-ovr shipping-service
```

---

### 11.6 Checklist vòng lặp dev “nhanh – không kéo cụm”
1. **Lên infra + core 1 lần**, để đó (mục 3 & 4).
2. Mỗi lần sửa 1 service:
   ```powershell
   mvn -pl <module> -am install -DskipTests
   docker compose -f docker-compose.yml -f docker-compose.override.nodeps.yml `
     up -d --build --no-deps <service>
   docker compose logs -f <service>
   ```
3. Khi cần đảm bảo không container nào khác bị recreate:
   ```powershell
   docker compose up -d --no-recreate
   ```

---

### 11.7 Bonus: giảm thời gian khởi động Spring Boot
Thêm vào `application-docker.yml` hoặc env (đã có trong `x-dev-env` ở trên):
```
spring:
  main:
    lazy-initialization: true
  cloud:
    config:
      fail-fast: false
      retry:
        max-attempts: 1
        initial-interval: 500
```

---

*Hết Phụ lục.*
