# KIẾN THỨC CƠ BẢN VÀ TRỌNG TÂM ORACLE DATABASE 12C+

## MỤC LỤC

1. [Giới Thiệu Oracle Database 12c](#1-giới-thiệu-oracle-database-12c)
2. [Kiến Trúc Oracle Database 12c](#2-kiến-trúc-oracle-database-12c)
3. [Multitenant Architecture](#3-multitenant-architecture)
4. [Cấu Trúc Bộ Nhớ](#4-cấu-trúc-bộ-nhớ)
5. [SQL và PL/SQL Mới](#5-sql-và-plsql-mới)
6. [Quản Lý Tablespace và Datafile](#6-quản-lý-tablespace-và-datafile)
7. [Quản Lý User, Role và Privilege](#7-quản-lý-user-role-và-privilege)
8. [Partitioning và Indexing](#8-partitioning-và-indexing)
9. [Backup và Recovery với RMAN](#9-backup-và-recovery-với-rman)
10. [Data Guard và RAC](#10-data-guard-và-rac)
11. [Bảo Mật (Security)](#11-bảo-mật-security)
12. [Performance Tuning](#12-performance-tuning)
13. [Cài Đặt và Cấu Hình](#13-cài-đặt-và-cấu-hình)
14. [Data Types và Constraints](#14-data-types-và-constraints)
15. [Upgrade và Migration](#15-upgrade-và-migration)

---

## 1. GIỚI THIỆU ORACLE DATABASE 12C

### 1.1 Tổng Quan
Oracle Database 12c (phát hành 2013) là bản cập nhật quan trọng với chữ "c" đại diện cho "Cloud". Phiên bản này được thiết kế để hỗ trợ kiến trúc đám mây và consolidation.

### 1.2 Các Phiên Bản
- **Oracle 12c Release 1 (12.1.0.1)** - 2013
- **Oracle 12c Release 1 (12.1.0.2)** - 2014
- **Oracle 12c Release 2 (12.2.0.1)** - 2016

### 1.3 Tính Năng Nổi Bật

#### Multitenant Architecture
- Cho phép nhiều Pluggable Databases (PDB) trong một Container Database (CDB)
- Tối ưu hóa tài nguyên và quản lý
- Giảm chi phí vận hành

#### In-Memory Column Store
- Lưu trữ dữ liệu dạng cột trong bộ nhớ
- Tăng tốc độ truy vấn phân tích lên 100 lần
- Hỗ trợ cả OLTP và OLAP đồng thời

#### Advanced Security
- Transparent Data Encryption (TDE) cải tiến
- Data Redaction
- Privilege Analysis
- Database Vault enhancements

---

## 2. KIẾN TRÚC ORACLE DATABASE 12C

### 2.1 Các Thành Phần Chính

```
┌─────────────────────────────────────────────────┐
│           ORACLE DATABASE INSTANCE              │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │     System Global Area (SGA)              │  │
│  │  ┌────────────────────────────────────┐  │  │
│  │  │  Database Buffer Cache              │  │  │
│  │  │  Shared Pool (Library + Dict Cache) │  │  │
│  │  │  Redo Log Buffer                    │  │  │
│  │  │  Large Pool                         │  │  │
│  │  │  Java Pool                          │  │  │
│  │  │  Streams Pool                       │  │  │
│  │  │  In-Memory Column Store (12c)       │  │  │
│  │  └────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  Background Processes                     │  │
│  │  - PMON, SMON, DBWn, LGWR, CKPT          │  │
│  │  - ARCn, RECO, MMON, MMNL                │  │
│  │  - LREn (Listener Registration - 12c)    │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  Program Global Area (PGA)                │  │
│  │  - Session Memory                         │  │
│  │  - Sort Areas                             │  │
│  │  - Hash Areas                             │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
          ↓           ↓           ↓
┌─────────────────────────────────────────────────┐
│            PHYSICAL STORAGE                     │
├─────────────────────────────────────────────────┤
│  - Control Files                                │
│  - Data Files                                   │
│  - Redo Log Files                               │
│  - Archive Log Files                            │
│  - Parameter Files (PFILE/SPFILE)               │
│  - Password File                                │
└─────────────────────────────────────────────────┘
```

### 2.2 Background Processes Chính

| Process | Tên Đầy Đủ | Chức Năng |
|---------|------------|-----------|
| **PMON** | Process Monitor | Dọn dẹp các process bị lỗi, đăng ký service với listener |
| **SMON** | System Monitor | Khôi phục instance, dọn dẹp temp segments |
| **DBWn** | Database Writer | Ghi dirty buffers từ buffer cache xuống data files |
| **LGWR** | Log Writer | Ghi redo log buffer xuống redo log files |
| **CKPT** | Checkpoint | Cập nhật checkpoint information vào control files và data file headers |
| **ARCn** | Archiver | Sao lưu redo log files khi chuyển sang archive log mode |
| **MMON** | Manageability Monitor | Thu thập AWR statistics |
| **MMNL** | Manageability Monitor Lite | Hỗ trợ MMON với ASH (Active Session History) |
| **LREn** | Listener Registration | Đăng ký database services với listener (12c) |

### 2.3 Memory Structures

#### System Global Area (SGA)
Vùng bộ nhớ chia sẻ cho tất cả processes:
- **Database Buffer Cache**: Lưu trữ data blocks đã đọc từ disk
- **Shared Pool**: Chứa Library Cache và Data Dictionary Cache
- **Redo Log Buffer**: Lưu trữ thông tin thay đổi trước khi ghi vào redo logs
- **Large Pool**: Dành cho parallel operations, RMAN backup
- **Java Pool**: Cho Java code và JVM
- **Streams Pool**: Cho Oracle Streams và replication

#### Program Global Area (PGA)
Vùng bộ nhớ riêng cho mỗi server process:
- Session memory
- Sort areas
- Hash areas
- Private SQL areas

---

## 3. MULTITENANT ARCHITECTURE

### 3.1 Khái Niệm

Multitenant là tính năng quan trọng nhất của Oracle 12c, cho phép:
- Một Container Database (CDB) chứa nhiều Pluggable Databases (PDB)
- Chia sẻ SGA, background processes, redo logs
- Quản lý tập trung, giảm chi phí vận hành

```
┌──────────────────────────────────────────────────────┐
│         Container Database (CDB)                     │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │         CDB$ROOT (Root Container)              │ │
│  │  - Oracle metadata                             │ │
│  │  - Common users (C##username)                  │ │
│  │  - Data dictionary                             │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │         PDB$SEED (Seed Container)              │ │
│  │  - Template để tạo PDB mới                     │ │
│  │  - READ ONLY mode                              │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │   PDB1      │  │   PDB2      │  │   PDB3      │ │
│  │  (SALESPDB) │  │  (HRPDB)    │  │  (FINPDB)   │ │
│  │             │  │             │  │             │ │
│  │ - User data │  │ - User data │  │ - User data │ │
│  │ - Local     │  │ - Local     │  │ - Local     │ │
│  │   users     │  │   users     │  │   users     │ │
│  │ - Service   │  │ - Service   │  │ - Service   │ │
│  └─────────────┘  └─────────────┘  └─────────────┘ │
│                                                      │
│  Shared: SGA, Background Processes, Redo Logs       │
└──────────────────────────────────────────────────────┘
```

### 3.2 Thành Phần

#### CDB (Container Database)
- Là "super database" chứa các PDB
- Có root container (CDB$ROOT) và seed container (PDB$SEED)
- Giới hạn: 252 PDBs trong 12.1, 4096 PDBs trong 12.2

#### Root Container (CDB$ROOT)
- Chứa Oracle metadata
- Lưu trữ common users (tên bắt đầu bằng C##)
- Quản lý data dictionary cho toàn CDB

#### Seed Container (PDB$SEED)
- Template để tạo PDB mới
- Luôn ở chế độ READ ONLY
- Không thể thay đổi

#### Pluggable Database (PDB)
- Database độc lập với user data riêng
- Có SYSTEM, SYSAUX, USERS tablespaces riêng
- Mỗi PDB có service name riêng
- Có thể plug/unplug dễ dàng

### 3.3 Quản Lý CDB và PDB

#### Tạo CDB

```sql
-- Tạo CDB khi tạo database
CREATE DATABASE cdb1
  ENABLE PLUGGABLE DATABASE
  SEED FILE_NAME_CONVERT = ('pdbseed', 'seed')
  ...;
```

#### Tạo PDB

```sql
-- Tạo PDB từ seed
CREATE PLUGGABLE DATABASE pdb1
  ADMIN USER pdb_admin IDENTIFIED BY password
  FILE_NAME_CONVERT = ('/u01/seed/', '/u01/pdb1/');

-- Tạo PDB từ PDB khác (clone)
CREATE PLUGGABLE DATABASE pdb2 FROM pdb1
  FILE_NAME_CONVERT = ('/u01/pdb1/', '/u01/pdb2/');
```

#### Quản Lý PDB

```sql
-- Mở PDB
ALTER PLUGGABLE DATABASE pdb1 OPEN;

-- Đóng PDB
ALTER PLUGGABLE DATABASE pdb1 CLOSE;

-- Xem danh sách PDB
SELECT con_id, name, open_mode FROM v$pdbs;

-- Chuyển đổi giữa containers
ALTER SESSION SET CONTAINER = pdb1;

-- Unplug PDB
ALTER PLUGGABLE DATABASE pdb1 CLOSE;
ALTER PLUGGABLE DATABASE pdb1 UNPLUG INTO '/tmp/pdb1.xml';

-- Plug PDB
CREATE PLUGGABLE DATABASE pdb1 USING '/tmp/pdb1.xml'
  NOCOPY;
```

### 3.4 Common vs Local Users

#### Common Users
- Tạo trong CDB$ROOT
- Tên bắt đầu bằng C## (hoặc c##)
- Tồn tại trong tất cả containers
- Dùng cho DBA quản trị

```sql
-- Tạo common user
CREATE USER c##admin IDENTIFIED BY password CONTAINER=ALL;
GRANT DBA TO c##admin CONTAINER=ALL;
```

#### Local Users
- Tạo trong PDB cụ thể
- Chỉ tồn tại trong PDB đó
- Dùng cho application users

```sql
ALTER SESSION SET CONTAINER = pdb1;
CREATE USER app_user IDENTIFIED BY password;
GRANT CONNECT, RESOURCE TO app_user;
```

### 3.5 Lợi Ích Multitenant

| Lợi Ích | Mô Tả |
|---------|-------|
| **Consolidation** | Nhiều databases trên một instance, giảm overhead |
| **Resource Sharing** | Chia sẻ SGA, processes, giảm memory footprint |
| **Patch Management** | Patch một lần cho CDB, áp dụng cho tất cả PDBs |
| **Backup Efficiency** | Backup CDB hoặc từng PDB riêng lẻ |
| **Portability** | Plug/unplug PDB dễ dàng giữa các CDBs |
| **Multi-tenancy** | Hỗ trợ SaaS model với isolation tốt |

---

## 4. CẤU TRÚC BỘ NHỚ

### 4.1 System Global Area (SGA)

#### Components

```sql
-- Xem cấu trúc SGA
SELECT * FROM v$sgainfo;
SELECT * FROM v$sgastat;
```

**Database Buffer Cache**
- Lưu trữ data blocks từ data files
- Giảm I/O bằng cách cache data
- Quản lý theo LRU algorithm

**Shared Pool**
- Library Cache: SQL statements, PL/SQL code
- Data Dictionary Cache: Metadata
- Server Result Cache: Query results

**Redo Log Buffer**
- Lưu trữ redo entries trước khi ghi vào redo log files
- Ghi tuần tự, hiệu suất cao

**Large Pool**
- Parallel query operations
- RMAN backup/restore
- Shared server sessions

**Java Pool**
- Java code và JVM data

**In-Memory Column Store (12c)**
- Lưu trữ dữ liệu dạng cột trong memory
- Tăng tốc analytical queries
- Dual-format architecture (row + column)

### 4.2 Program Global Area (PGA)

```sql
-- Xem PGA statistics
SELECT * FROM v$pgastat;
SELECT * FROM v$process ORDER BY pga_used_mem DESC;
```

**Components:**
- Session memory
- Private SQL areas
- Sort areas
- Hash areas
- Bitmap merge areas

### 4.3 Memory Management

#### Automatic Memory Management (AMM)
Oracle tự động quản lý toàn bộ SGA và PGA:

```sql
-- Thiết lập AMM
ALTER SYSTEM SET memory_target = 4G SCOPE=SPFILE;
ALTER SYSTEM SET memory_max_target = 8G SCOPE=SPFILE;
```

#### Automatic Shared Memory Management (ASMM)
Tự động quản lý SGA, PGA riêng biệt (khuyến nghị):

```sql
-- Thiết lập ASMM
ALTER SYSTEM SET sga_target = 3G SCOPE=SPFILE;
ALTER SYSTEM SET sga_max_size = 4G SCOPE=SPFILE;
ALTER SYSTEM SET pga_aggregate_target = 1G SCOPE=BOTH;
ALTER SYSTEM SET pga_aggregate_limit = 2G SCOPE=BOTH; -- 12c only
```

#### Manual Memory Management
Thiết lập thủ công từng component:

```sql
ALTER SYSTEM SET db_cache_size = 2G;
ALTER SYSTEM SET shared_pool_size = 800M;
ALTER SYSTEM SET large_pool_size = 200M;
```

### 4.4 Monitoring Memory

```sql
-- Kiểm tra memory parameters
SHOW PARAMETER memory;
SHOW PARAMETER sga;
SHOW PARAMETER pga;

-- Memory allocation
SELECT component, current_size/1024/1024 MB
FROM v$sga_dynamic_components;

-- PGA usage
SELECT name, value/1024/1024 MB
FROM v$pgastat
WHERE name IN ('total PGA allocated', 'maximum PGA allocated');
```

---

## 5. SQL VÀ PL/SQL MỚI

### 5.1 SQL Enhancements

#### Identity Columns (Auto-Increment)

```sql
-- Tạo bảng với IDENTITY column
CREATE TABLE employees (
  emp_id NUMBER GENERATED ALWAYS AS IDENTITY,
  emp_name VARCHAR2(100),
  hire_date DATE
);

-- Hoặc tùy chỉnh
CREATE TABLE orders (
  order_id NUMBER GENERATED BY DEFAULT AS IDENTITY 
    (START WITH 1000 INCREMENT BY 1),
  order_date DATE
);
```

#### Row Limiting Clause (Top-N Queries)

```sql
-- Lấy 10 dòng đầu (cú pháp mới, dễ hơn ROWNUM)
SELECT emp_name, salary
FROM employees
ORDER BY salary DESC
FETCH FIRST 10 ROWS ONLY;

-- Với offset (pagination)
SELECT emp_name, salary
FROM employees
ORDER BY salary DESC
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;

-- Phần trăm
SELECT emp_name, salary
FROM employees
ORDER BY salary DESC
FETCH FIRST 5 PERCENT ROWS ONLY;
```

#### Extended Data Types

```sql
-- Mở rộng VARCHAR2, NVARCHAR2, RAW lên 32KB
-- (mặc định vẫn là 4000/2000 bytes)

-- Enable extended data types
ALTER SYSTEM SET max_string_size = EXTENDED;
-- Chạy script
@?/rdbms/admin/utl32k.sql

-- Sử dụng
CREATE TABLE large_text (
  id NUMBER,
  description VARCHAR2(32767)  -- Trước đây tối đa 4000
);
```

#### Invisible Columns

```sql
-- Tạo cột ẩn
CREATE TABLE products (
  product_id NUMBER,
  product_name VARCHAR2(100),
  internal_code VARCHAR2(50) INVISIBLE
);

-- Cột invisible không xuất hiện trong SELECT *
SELECT * FROM products;  -- Không hiện internal_code

-- Phải gọi tên rõ ràng
SELECT product_id, product_name, internal_code FROM products;
```

#### Default Values on Sequence

```sql
-- Sử dụng sequence làm default value
CREATE SEQUENCE emp_seq START WITH 1;

CREATE TABLE employees (
  emp_id NUMBER DEFAULT emp_seq.NEXTVAL,
  emp_name VARCHAR2(100)
);

-- Insert không cần chỉ định emp_id
INSERT INTO employees (emp_name) VALUES ('John Doe');
```

#### Lateral Inline Views

```sql
-- Sử dụng LATERAL để tham chiếu outer query
SELECT d.department_name, e.emp_name, e.salary
FROM departments d,
     LATERAL (SELECT emp_name, salary
              FROM employees
              WHERE department_id = d.department_id
              ORDER BY salary DESC
              FETCH FIRST 3 ROWS ONLY) e;
```

#### Pattern Matching (MATCH_RECOGNIZE)

```sql
-- Tìm patterns trong dữ liệu
SELECT *
FROM stock_prices
MATCH_RECOGNIZE (
  PARTITION BY symbol
  ORDER BY price_date
  MEASURES 
    FIRST(A.price) AS start_price,
    LAST(Z.price) AS end_price
  PATTERN (A B+ C+ Z)
  DEFINE
    B AS B.price < PREV(B.price),
    C AS C.price > PREV(C.price)
);
```

#### Approximate Query Processing

```sql
-- Ước lượng nhanh với APPROX_COUNT_DISTINCT
SELECT APPROX_COUNT_DISTINCT(customer_id) 
FROM sales;

-- APPROX_PERCENTILE
SELECT APPROX_PERCENTILE(0.95, salary) 
FROM employees;
```

### 5.2 PL/SQL Enhancements

#### Accessible By Clause (White Lists)

```sql
-- Giới hạn ai có thể gọi procedure/function
CREATE PACKAGE secure_pkg
ACCESSIBLE BY (PROCEDURE authorized_proc, PACKAGE auth_pkg)
AS
  PROCEDURE sensitive_operation;
END;
```

#### PL/SQL Function Results in SQL

```sql
-- Sử dụng PL/SQL function trong WITH clause
WITH
  FUNCTION calculate_tax(amount NUMBER) RETURN NUMBER IS
  BEGIN
    RETURN amount * 0.1;
  END;
SELECT product_name, price, calculate_tax(price) AS tax
FROM products;
```

#### UTL_CALL_STACK Package

```sql
-- Lấy thông tin chi tiết về call stack
DECLARE
  v_depth PLS_INTEGER;
BEGIN
  v_depth := UTL_CALL_STACK.dynamic_depth;
  
  FOR i IN 1..v_depth LOOP
    DBMS_OUTPUT.put_line(
      'Unit: ' || UTL_CALL_STACK.unit_type(i) ||
      ' Owner: ' || UTL_CALL_STACK.owner(i) ||
      ' Line: ' || UTL_CALL_STACK.unit_line(i)
    );
  END LOOP;
END;
```

#### Implicit Statement Results

```sql
-- Trả về multiple result sets (như SQL Server)
CREATE PROCEDURE get_reports AS
BEGIN
  DBMS_SQL.RETURN_RESULT(
    CURSOR(SELECT * FROM employees)
  );
  DBMS_SQL.RETURN_RESULT(
    CURSOR(SELECT * FROM departments)
  );
END;
```

#### DEPRECATE Pragma (12.2)

```sql
-- Đánh dấu code là deprecated
PROCEDURE old_procedure
  DEPRECATE('Use new_procedure instead')
AS
BEGIN
  -- Old logic
END;
```

### 5.3 JSON Support

```sql
-- Lưu trữ JSON
CREATE TABLE customers (
  id NUMBER,
  data CLOB CHECK (data IS JSON)
);

-- Insert JSON
INSERT INTO customers VALUES (1, 
  '{"name": "John", "age": 30, "city": "Hanoi"}');

-- Query JSON
SELECT json_value(data, '$.name') AS customer_name
FROM customers;

-- JSON_TABLE
SELECT jt.*
FROM customers c,
     JSON_TABLE(c.data, '$'
       COLUMNS (
         name VARCHAR2(100) PATH '$.name',
         age NUMBER PATH '$.age',
         city VARCHAR2(100) PATH '$.city'
       )
     ) jt;
```

---

## 6. QUẢN LÝ TABLESPACE VÀ DATAFILE

### 6.1 Tablespace Types

| Type | Mô Tả | Sử Dụng |
|------|-------|---------|
| **PERMANENT** | Lưu trữ dữ liệu vĩnh viễn | User data, objects |
| **TEMPORARY** | Lưu trữ tạm thời | Sort operations, joins |
| **UNDO** | Lưu trữ undo data | Transaction rollback, read consistency |

### 6.2 Tạo và Quản Lý Tablespace

#### Tạo Tablespace

```sql
-- Tạo tablespace cơ bản
CREATE TABLESPACE data01
  DATAFILE '/u01/oradata/data01.dbf'
  SIZE 100M
  AUTOEXTEND ON NEXT 10M MAXSIZE 1G;

-- Tạo bigfile tablespace
CREATE BIGFILE TABLESPACE big_data
  DATAFILE '/u01/oradata/big_data.dbf'
  SIZE 10G
  AUTOEXTEND ON;

-- Tạo temporary tablespace
CREATE TEMPORARY TABLESPACE temp01
  TEMPFILE '/u01/oradata/temp01.dbf'
  SIZE 500M;

-- Tạo undo tablespace
CREATE UNDO TABLESPACE undo02
  DATAFILE '/u01/oradata/undo02.dbf'
  SIZE 1G;
```

#### Thêm Datafile

```sql
-- Thêm datafile vào tablespace
ALTER TABLESPACE data01
  ADD DATAFILE '/u01/oradata/data01_02.dbf'
  SIZE 100M
  AUTOEXTEND ON NEXT 10M MAXSIZE 2G;
```

#### Resize Datafile

```sql
-- Tăng kích thước datafile
ALTER DATABASE
  DATAFILE '/u01/oradata/data01.dbf'
  RESIZE 500M;

-- Enable autoextend
ALTER DATABASE
  DATAFILE '/u01/oradata/data01.dbf'
  AUTOEXTEND ON NEXT 50M MAXSIZE 5G;
```

#### Drop Tablespace

```sql
-- Drop tablespace và xóa datafiles
DROP TABLESPACE data01
  INCLUDING CONTENTS AND DATAFILES
  CASCADE CONSTRAINTS;
```

#### Tablespace Status

```sql
-- Chuyển sang READ ONLY
ALTER TABLESPACE data01 READ ONLY;

-- Chuyển về READ WRITE
ALTER TABLESPACE data01 READ WRITE;

-- Offline tablespace
ALTER TABLESPACE data01 OFFLINE;

-- Online tablespace
ALTER TABLESPACE data01 ONLINE;
```

### 6.3 Monitoring Tablespace

```sql
-- Xem tất cả tablespaces
SELECT tablespace_name, status, contents, extent_management
FROM dba_tablespaces;

-- Kiểm tra dung lượng
SELECT 
  tablespace_name,
  ROUND(SUM(bytes)/1024/1024/1024, 2) AS size_gb,
  ROUND(SUM(maxbytes)/1024/1024/1024, 2) AS max_size_gb
FROM dba_data_files
GROUP BY tablespace_name;

-- Kiểm tra free space
SELECT 
  tablespace_name,
  ROUND(SUM(bytes)/1024/1024, 2) AS free_mb
FROM dba_free_space
GROUP BY tablespace_name;

-- Utilization percentage
SELECT 
  df.tablespace_name,
  ROUND((df.total_bytes - NVL(fs.free_bytes, 0)) / df.total_bytes * 100, 2) AS used_pct
FROM (
  SELECT tablespace_name, SUM(bytes) AS total_bytes
  FROM dba_data_files
  GROUP BY tablespace_name
) df
LEFT JOIN (
  SELECT tablespace_name, SUM(bytes) AS free_bytes
  FROM dba_free_space
  GROUP BY tablespace_name
) fs ON df.tablespace_name = fs.tablespace_name;
```

### 6.4 Quản Lý trong Multitenant

```sql
-- Tạo tablespace trong CDB
CREATE TABLESPACE cdb_data
  DATAFILE '/u01/oradata/cdb1/cdb_data.dbf'
  SIZE 500M;

-- Tạo tablespace trong PDB
ALTER SESSION SET CONTAINER = pdb1;
CREATE TABLESPACE pdb1_data
  DATAFILE '/u01/oradata/cdb1/pdb1/pdb1_data.dbf'
  SIZE 500M;

-- Xem tablespaces trong container hiện tại
SELECT tablespace_name FROM dba_tablespaces;

-- Xem tablespaces của tất cả containers
SELECT con_id, tablespace_name 
FROM cdb_tablespaces
ORDER BY con_id, tablespace_name;
```

---

## 7. QUẢN LÝ USER, ROLE VÀ PRIVILEGE

### 7.1 User Management

#### Tạo User

```sql
-- Tạo local user trong PDB
CREATE USER app_user IDENTIFIED BY password
  DEFAULT TABLESPACE users
  TEMPORARY TABLESPACE temp
  QUOTA 100M ON users;

-- Tạo common user trong CDB
CREATE USER c##admin IDENTIFIED BY password
  CONTAINER = ALL;

-- Lock/Unlock user
ALTER USER app_user ACCOUNT LOCK;
ALTER USER app_user ACCOUNT UNLOCK;

-- Đổi password
ALTER USER app_user IDENTIFIED BY new_password;

-- Drop user
DROP USER app_user CASCADE;
```

#### User Profiles

```sql
-- Tạo profile
CREATE PROFILE dev_profile LIMIT
  SESSIONS_PER_USER 5
  CPU_PER_SESSION UNLIMITED
  CONNECT_TIME 120
  IDLE_TIME 30
  FAILED_LOGIN_ATTEMPTS 3
  PASSWORD_LIFE_TIME 90
  PASSWORD_REUSE_TIME 365;

-- Gán profile cho user
ALTER USER app_user PROFILE dev_profile;
```

### 7.2 Privilege Management

#### System Privileges

```sql
-- Grant system privileges
GRANT CREATE SESSION TO app_user;
GRANT CREATE TABLE TO app_user;
GRANT CREATE PROCEDURE TO app_user;
GRANT UNLIMITED TABLESPACE TO app_user;

-- Grant với ADMIN OPTION
GRANT CREATE USER TO c##admin WITH ADMIN OPTION;

-- Revoke
REVOKE CREATE TABLE FROM app_user;
```

#### Object Privileges

```sql
-- Grant object privileges
GRANT SELECT ON employees TO app_user;
GRANT INSERT, UPDATE, DELETE ON departments TO app_user;
GRANT ALL ON products TO app_user;

-- Grant với GRANT OPTION
GRANT SELECT ON customers TO app_user WITH GRANT OPTION;

-- Grant column-level
GRANT UPDATE (salary) ON employees TO hr_user;

-- Revoke
REVOKE SELECT ON employees FROM app_user;
```

### 7.3 Role Management

#### Tạo và Sử Dụng Role

```sql
-- Tạo role
CREATE ROLE app_role;
CREATE ROLE manager_role;

-- Grant privileges đến role
GRANT CREATE SESSION TO app_role;
GRANT SELECT ANY TABLE TO app_role;
GRANT INSERT, UPDATE ON hr.employees TO manager_role;

-- Grant role đến user
GRANT app_role TO app_user;
GRANT manager_role TO hr_manager;

-- Grant role đến role khác
GRANT app_role TO manager_role;

-- Set default role
ALTER USER app_user DEFAULT ROLE app_role, manager_role;

-- Drop role
DROP ROLE app_role;
```

#### Predefined Roles

| Role | Mô Tả |
|------|-------|
| **CONNECT** | Basic connection privileges |
| **RESOURCE** | Create schema objects |
| **DBA** | Full administrative privileges |
| **SELECT_CATALOG_ROLE** | SELECT on data dictionary |
| **EXECUTE_CATALOG_ROLE** | EXECUTE on data dictionary |

### 7.4 New Privileges in 12c

```sql
-- SYSBACKUP - Backup operations
GRANT SYSBACKUP TO c##backup_admin;
CONNECT c##backup_admin/password AS SYSBACKUP;

-- SYSDG - Data Guard operations
GRANT SYSDG TO c##dg_admin;

-- SYSKM - Key management (TDE)
GRANT SYSKM TO c##key_admin;

-- READ privilege (12c) - không lock như SELECT
GRANT READ ON employees TO reporting_user;
```

### 7.5 Monitoring Users và Privileges

```sql
-- Danh sách users
SELECT username, account_status, default_tablespace, profile
FROM dba_users
ORDER BY created DESC;

-- System privileges của user
SELECT grantee, privilege, admin_option
FROM dba_sys_privs
WHERE grantee = 'APP_USER';

-- Object privileges của user
SELECT grantee, owner, table_name, privilege
FROM dba_tab_privs
WHERE grantee = 'APP_USER';

-- Roles của user
SELECT grantee, granted_role, admin_option, default_role
FROM dba_role_privs
WHERE grantee = 'APP_USER';

-- Privileges trong role
SELECT role, privilege
FROM role_sys_privs
WHERE role = 'APP_ROLE';
```

---

## 8. PARTITIONING VÀ INDEXING

### 8.1 Table Partitioning

#### Range Partitioning

```sql
CREATE TABLE sales (
  sale_id NUMBER,
  sale_date DATE,
  amount NUMBER
)
PARTITION BY RANGE (sale_date) (
  PARTITION sales_q1_2024 VALUES LESS THAN (TO_DATE('01-04-2024', 'DD-MM-YYYY')),
  PARTITION sales_q2_2024 VALUES LESS THAN (TO_DATE('01-07-2024', 'DD-MM-YYYY')),
  PARTITION sales_q3_2024 VALUES LESS THAN (TO_DATE('01-10-2024', 'DD-MM-YYYY')),
  PARTITION sales_q4_2024 VALUES LESS THAN (TO_DATE('01-01-2025', 'DD-MM-YYYY'))
);
```

#### List Partitioning

```sql
CREATE TABLE customers (
  customer_id NUMBER,
  country VARCHAR2(50),
  name VARCHAR2(100)
)
PARTITION BY LIST (country) (
  PARTITION asia VALUES ('Vietnam', 'Thailand', 'Singapore'),
  PARTITION europe VALUES ('France', 'Germany', 'UK'),
  PARTITION americas VALUES ('USA', 'Canada', 'Brazil')
);
```

#### Hash Partitioning

```sql
CREATE TABLE orders (
  order_id NUMBER,
  order_date DATE,
  customer_id NUMBER
)
PARTITION BY HASH (customer_id)
PARTITIONS 8;
```

#### Composite Partitioning

```sql
-- Range-Hash
CREATE TABLE transactions (
  trans_id NUMBER,
  trans_date DATE,
  customer_id NUMBER,
  amount NUMBER
)
PARTITION BY RANGE (trans_date)
SUBPARTITION BY HASH (customer_id) SUBPARTITIONS 4 (
  PARTITION p_2024_q1 VALUES LESS THAN (TO_DATE('01-04-2024', 'DD-MM-YYYY')),
  PARTITION p_2024_q2 VALUES LESS THAN (TO_DATE('01-07-2024', 'DD-MM-YYYY'))
);
```

#### Reference Partitioning (12c Enhancement)

```sql
-- Parent table
CREATE TABLE orders (
  order_id NUMBER PRIMARY KEY,
  order_date DATE
)
PARTITION BY RANGE (order_date) (
  PARTITION p_2024_q1 VALUES LESS THAN (TO_DATE('01-04-2024', 'DD-MM-YYYY'))
);

-- Child table partitioned by reference
CREATE TABLE order_items (
  item_id NUMBER,
  order_id NUMBER,
  product_id NUMBER,
  CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
)
PARTITION BY REFERENCE (fk_order);
```

### 8.2 Partition Operations

```sql
-- Add partition
ALTER TABLE sales ADD PARTITION sales_q1_2025 
  VALUES LESS THAN (TO_DATE('01-04-2025', 'DD-MM-YYYY'));

-- Drop partition
ALTER TABLE sales DROP PARTITION sales_q1_2024;

-- Truncate partition
ALTER TABLE sales TRUNCATE PARTITION sales_q2_2024;

-- Split partition (12c online)
ALTER TABLE sales SPLIT PARTITION sales_q4_2024
  AT (TO_DATE('15-11-2024', 'DD-MM-YYYY'))
  INTO (PARTITION sales_nov_2024, PARTITION sales_dec_2024)
  ONLINE;

-- Merge partitions (12c online)
ALTER TABLE sales MERGE PARTITIONS sales_q1_2024, sales_q2_2024
  INTO PARTITION sales_h1_2024
  ONLINE;

-- Move partition (12c online)
ALTER TABLE sales MOVE PARTITION sales_q1_2024
  TABLESPACE new_tablespace
  ONLINE;
```

### 8.3 Indexing

#### B-Tree Index

```sql
-- Standard index
CREATE INDEX idx_emp_name ON employees(emp_name);

-- Unique index
CREATE UNIQUE INDEX idx_emp_email ON employees(email);

-- Composite index
CREATE INDEX idx_emp_dept_salary ON employees(department_id, salary);

-- Function-based index
CREATE INDEX idx_emp_upper_name ON employees(UPPER(emp_name));
```

#### Bitmap Index

```sql
-- Cho low-cardinality columns
CREATE BITMAP INDEX idx_emp_gender ON employees(gender);
```

#### Invisible Index (12c)

```sql
-- Tạo invisible index
CREATE INDEX idx_emp_dept INVISIBLE ON employees(department_id);

-- Test index trước khi visible
ALTER SESSION SET optimizer_use_invisible_indexes = TRUE;
-- Test queries...

-- Make visible
ALTER INDEX idx_emp_dept VISIBLE;
```

#### Multiple Indexes on Same Column (12c)

```sql
-- Tạo cả B-tree và Bitmap trên cùng column
CREATE INDEX idx_status_btree ON orders(status);
CREATE BITMAP INDEX idx_status_bitmap ON orders(status);
```

### 8.4 Partitioned Indexes

#### Local Partitioned Index

```sql
-- Mỗi partition của index tương ứng với partition của table
CREATE INDEX idx_sales_date ON sales(sale_date) LOCAL;

-- Với storage options
CREATE INDEX idx_sales_amount ON sales(amount) LOCAL
  (PARTITION p1 TABLESPACE ts1,
   PARTITION p2 TABLESPACE ts2);
```

#### Global Partitioned Index

```sql
-- Index partition khác với table partition
CREATE INDEX idx_sales_customer ON sales(customer_id)
  GLOBAL PARTITION BY RANGE (customer_id) (
    PARTITION idx_p1 VALUES LESS THAN (1000),
    PARTITION idx_p2 VALUES LESS THAN (2000),
    PARTITION idx_p3 VALUES LESS THAN (MAXVALUE)
  );
```

#### Partial Indexes (12c)

```sql
-- Chỉ index một số partitions
ALTER TABLE sales MODIFY PARTITION sales_old INDEXING OFF;
ALTER TABLE sales MODIFY PARTITION sales_current INDEXING ON;

-- Tạo partial index
CREATE INDEX idx_sales_partial ON sales(amount)
  LOCAL INDEXING PARTIAL;
```

### 8.5 Index Monitoring

```sql
-- Xem indexes
SELECT index_name, index_type, uniqueness, status
FROM user_indexes
WHERE table_name = 'EMPLOYEES';

-- Check index usage
ALTER INDEX idx_emp_name MONITORING USAGE;
-- Sau một thời gian...
SELECT * FROM v$object_usage;

-- Rebuild index
ALTER INDEX idx_emp_name REBUILD ONLINE;

-- Drop index
DROP INDEX idx_emp_name;
```

---

## 9. BACKUP VÀ RECOVERY VỚI RMAN

### 9.1 RMAN Basics

#### Kết Nối RMAN

```bash
# Kết nối local
rman TARGET /

# Kết nối remote
rman TARGET sys/password@database

# Với catalog
rman TARGET / CATALOG rman/password@catalog_db
```

#### Cấu Hình RMAN

```sql
-- Configure retention policy
CONFIGURE RETENTION POLICY TO RECOVERY WINDOW OF 7 DAYS;
CONFIGURE RETENTION POLICY TO REDUNDANCY 2;

-- Configure backup location
CONFIGURE CHANNEL DEVICE TYPE DISK FORMAT '/backup/%U';

-- Configure controlfile autobackup
CONFIGURE CONTROLFILE AUTOBACKUP ON;
CONFIGURE CONTROLFILE AUTOBACKUP FORMAT FOR DEVICE TYPE DISK TO '/backup/cf_%F';

-- Configure backup optimization
CONFIGURE BACKUP OPTIMIZATION ON;

-- Configure compression
CONFIGURE COMPRESSION ALGORITHM 'MEDIUM';

-- Show all configurations
SHOW ALL;
```

### 9.2 Backup Operations

#### Full Database Backup

```sql
-- Full backup
BACKUP DATABASE;

-- Full backup với compression
BACKUP AS COMPRESSED BACKUPSET DATABASE;

-- Full backup với tag
BACKUP DATABASE TAG 'FULL_BACKUP_WEEKLY';

-- Include archive logs
BACKUP DATABASE PLUS ARCHIVELOG;

-- Incremental backup
BACKUP INCREMENTAL LEVEL 0 DATABASE;
BACKUP INCREMENTAL LEVEL 1 DATABASE;
```

#### Tablespace Backup

```sql
-- Backup specific tablespace
BACKUP TABLESPACE users, data01;

-- Backup all tablespaces except system
BACKUP DATABASE SKIP TABLESPACE system, sysaux;
```

#### Datafile Backup

```sql
-- Backup specific datafile
BACKUP DATAFILE 4;
BACKUP DATAFILE '/u01/oradata/users01.dbf';
```

#### Archive Log Backup

```sql
-- Backup all archive logs
BACKUP ARCHIVELOG ALL;

-- Backup archive logs và delete sau đó
BACKUP ARCHIVELOG ALL DELETE INPUT;

-- Backup archive logs trong khoảng thời gian
BACKUP ARCHIVELOG FROM TIME 'SYSDATE-1' UNTIL TIME 'SYSDATE';
```

#### Controlfile và SPFile Backup

```sql
-- Backup controlfile
BACKUP CURRENT CONTROLFILE;

-- Backup spfile
BACKUP SPFILE;

-- Include trong database backup
BACKUP DATABASE INCLUDE CURRENT CONTROLFILE;
```

### 9.3 Advanced Backup Features (12c)

#### Table Recovery

```sql
-- Backup để hỗ trợ table recovery
BACKUP DATABASE PLUS ARCHIVELOG;

-- Recover single table (12c feature)
RMAN> RECOVER TABLE hr.employees 
      UNTIL TIME "TO_DATE('2024-01-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS')"
      AUXILIARY DESTINATION '/tmp/aux'
      REMAP TABLE hr.employees:employees_recovered;
```

#### Network Backup

```sql
-- Backup qua network (cho standby)
BACKUP DATABASE SECTION SIZE 10G
  FROM SERVICE standby_service;
```

### 9.4 Recovery Operations

#### Complete Recovery

```sql
-- Restore và recover database
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
RESTORE DATABASE;
RECOVER DATABASE;
ALTER DATABASE OPEN;
```

#### Point-in-Time Recovery

```sql
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
RUN {
  SET UNTIL TIME "TO_DATE('2024-01-01 12:00:00', 'YYYY-MM-DD HH24:MI:SS')";
  RESTORE DATABASE;
  RECOVER DATABASE;
}
ALTER DATABASE OPEN RESETLOGS;
```

#### Tablespace Point-in-Time Recovery

```sql
RECOVER TABLESPACE users
  UNTIL TIME "TO_DATE('2024-01-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS')"
  AUXILIARY DESTINATION '/tmp/aux';
```

#### Block Media Recovery

```sql
-- Recover corrupted blocks
RECOVER DATAFILE 4 BLOCK 100, 101, 102;
```

### 9.5 Monitoring và Maintenance

```sql
-- List backups
LIST BACKUP SUMMARY;
LIST BACKUP OF DATABASE;
LIST BACKUP OF TABLESPACE users;
LIST ARCHIVELOG ALL;

-- Report obsolete backups
REPORT OBSOLETE;

-- Report need backup
REPORT NEED BACKUP DAYS 7;

-- Delete obsolete backups
DELETE OBSOLETE;

-- Delete specific backup
DELETE BACKUP TAG 'OLD_BACKUP';

-- Crosscheck backups
CROSSCHECK BACKUP;
CROSSCHECK ARCHIVELOG ALL;

-- Validate backups
VALIDATE BACKUPSET 123;
RESTORE DATABASE VALIDATE;
```

### 9.6 RMAN Scripts

```sql
-- Tạo RMAN script
CREATE SCRIPT full_backup {
  BACKUP AS COMPRESSED BACKUPSET 
    DATABASE PLUS ARCHIVELOG
    TAG 'FULL_BACKUP';
  DELETE NOPROMPT OBSOLETE;
}

-- Chạy script
RUN { EXECUTE SCRIPT full_backup; }

-- List scripts
LIST SCRIPT NAMES;

-- Delete script
DELETE SCRIPT full_backup;
```

---

## 10. DATA GUARD VÀ RAC

### 10.1 Oracle Data Guard

#### Khái Niệm
Data Guard cung cấp high availability, data protection và disaster recovery bằng cách:
- Duy trì standby databases (bản sao của primary)
- Tự động đồng bộ dữ liệu
- Hỗ trợ switchover và failover

#### Types of Standby

| Type | Mô Tả | Use Case |
|------|-------|----------|
| **Physical Standby** | Block-for-block copy | Disaster recovery, reporting |
| **Logical Standby** | Logical copy, có thể query | Reporting, rolling upgrades |
| **Snapshot Standby** | Updateable copy | Testing |

#### Cấu Hình Physical Standby - Bước Cơ Bản

```sql
-- 1. Primary: Enable force logging
ALTER DATABASE FORCE LOGGING;

-- 2. Primary: Enable archivelog mode
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
ALTER DATABASE ARCHIVELOG;
ALTER DATABASE OPEN;

-- 3. Primary: Tạo standby redo logs
ALTER DATABASE ADD STANDBY LOGFILE 
  ('/u01/oradata/standby_redo01.log') SIZE 100M;
-- Tạo thêm các standby redo logs...

-- 4. Primary: Configure parameters
ALTER SYSTEM SET log_archive_config='DG_CONFIG=(PROD,PRODDR)';
ALTER SYSTEM SET log_archive_dest_1=
  'LOCATION=/archive/ VALID_FOR=(ALL_LOGFILES,ALL_ROLES) DB_UNIQUE_NAME=PROD';
ALTER SYSTEM SET log_archive_dest_2=
  'SERVICE=PRODDR ASYNC VALID_FOR=(ONLINE_LOGFILES,PRIMARY_ROLE) DB_UNIQUE_NAME=PRODDR';
ALTER SYSTEM SET fal_server='PRODDR';
ALTER SYSTEM SET standby_file_management='AUTO';

-- 5. Standby: Create auxiliary instance và restore
STARTUP NOMOUNT;
RMAN> DUPLICATE TARGET DATABASE FOR STANDBY FROM ACTIVE DATABASE;

-- 6. Standby: Start managed recovery
ALTER DATABASE RECOVER MANAGED STANDBY DATABASE DISCONNECT FROM SESSION;

-- 7. Active Data Guard (license required)
ALTER DATABASE OPEN READ ONLY;
ALTER DATABASE RECOVER MANAGED STANDBY DATABASE DISCONNECT FROM SESSION;
```

#### Data Guard Broker

```bash
# Enable broker
dgmgrl /
DGMGRL> CREATE CONFIGURATION dg_config AS
        PRIMARY DATABASE IS prod CONNECT IDENTIFIER IS prod;
DGMGRL> ADD DATABASE proddr AS CONNECT IDENTIFIER IS proddr;
DGMGRL> ENABLE CONFIGURATION;

# Check status
DGMGRL> SHOW CONFIGURATION;
DGMGRL> SHOW DATABASE prod;

# Switchover
DGMGRL> SWITCHOVER TO proddr;

# Failover
DGMGRL> FAILOVER TO proddr;
```

#### Fast-Start Failover

```bash
# Enable FSFO
DGMGRL> EDIT DATABASE prod SET PROPERTY FastStartFailoverTarget='proddr';
DGMGRL> EDIT DATABASE proddr SET PROPERTY FastStartFailoverTarget='prod';
DGMGRL> ENABLE FAST_START FAILOVER;

# Start observer
DGMGRL> START OBSERVER;
```

### 10.2 Oracle RAC (Real Application Clusters)

#### Khái Niệm
RAC cho phép nhiều instances truy cập cùng một database:
- High availability
- Scalability
- Load balancing

#### Kiến Trúc RAC

```
┌─────────────────────────────────────────────────┐
│                 SCAN Listeners                  │
│         (Single Client Access Name)             │
└─────────────────────────────────────────────────┘
                     ↓
    ┌────────────────┴────────────────┐
    ↓                                 ↓
┌─────────┐                      ┌─────────┐
│ Node 1  │←──── Interconnect ──→│ Node 2  │
│         │      (Cache Fusion)   │         │
│Instance1│                       │Instance2│
│ (SGA)   │                       │ (SGA)   │
└────┬────┘                       └────┬────┘
     │                                 │
     └────────────┬────────────────────┘
                  ↓
         ┌────────────────┐
         │ Shared Storage │
         │  (ASM/CFS)     │
         │  - Data Files  │
         │  - Control     │
         │  - Redo Logs   │
         │  - OCR/Voting  │
         └────────────────┘
```

#### RAC Components

**Cluster Ready Services (CRS)**
- Oracle Cluster Registry (OCR): Cluster configuration
- Voting Disk: Cluster membership
- High Availability Services (HAS)

**ASM (Automatic Storage Management)**
- Quản lý storage cho RAC
- Striping và mirroring tự động
- Dynamic rebalancing

**Cache Fusion**
- Chia sẻ data blocks giữa instances
- Sử dụng high-speed interconnect
- Global Cache Service (GCS)
- Global Enqueue Service (GES)

#### RAC Administration

```sql
-- Xem tất cả instances
SELECT inst_id, instance_name, status 
FROM gv$instance;

-- Xem services
SELECT name, network_name 
FROM dba_services;

-- Tạo service
BEGIN
  DBMS_SERVICE.CREATE_SERVICE(
    service_name => 'APP_SERVICE',
    network_name => 'app_service.example.com'
  );
  DBMS_SERVICE.START_SERVICE('APP_SERVICE');
END;
/

-- Add service to instance
srvctl add service -d prod -s APP_SERVICE -r prod1,prod2 -P BASIC

-- Start/Stop instance
srvctl stop instance -d prod -i prod1
srvctl start instance -d prod -i prod1

-- Check cluster status
crsctl check crs
crsctl stat res -t
```

#### Data Guard với RAC

```
Primary Site (RAC)          Standby Site (RAC)
┌────────────────┐         ┌────────────────┐
│  Node 1        │         │  Node 1        │
│  Instance 1 ───┼────────→│  Instance 1    │
└────────────────┘         └────────────────┘
┌────────────────┐         ┌────────────────┐
│  Node 2        │         │  Node 2        │
│  Instance 2 ───┼────────→│  Instance 2    │
└────────────────┘         └────────────────┘
```

---

## 11. BẢO MẬT (SECURITY)

### 11.1 Transparent Data Encryption (TDE)

#### Setup TDE

```sql
-- 1. Tạo wallet directory
mkdir -p $ORACLE_BASE/admin/$ORACLE_SID/wallet

-- 2. Configure sqlnet.ora
ENCRYPTION_WALLET_LOCATION =
  (SOURCE = (METHOD = FILE)
    (METHOD_DATA =
      (DIRECTORY = /u01/app/oracle/admin/PROD/wallet)))

-- 3. Tạo wallet
ALTER SYSTEM SET ENCRYPTION KEY IDENTIFIED BY "wallet_password";

-- 4. Open wallet (phải làm sau mỗi lần restart)
ALTER SYSTEM SET ENCRYPTION WALLET OPEN IDENTIFIED BY "wallet_password";
```

#### Column Encryption

```sql
-- Encrypt column
CREATE TABLE employees (
  emp_id NUMBER,
  emp_name VARCHAR2(100),
  salary NUMBER ENCRYPT,
  ssn VARCHAR2(20) ENCRYPT USING 'AES256'
);

-- Encrypt existing column
ALTER TABLE employees MODIFY (salary ENCRYPT);
```

#### Tablespace Encryption

```sql
-- Tạo encrypted tablespace
CREATE TABLESPACE secure_data
  DATAFILE '/u01/oradata/secure_data01.dbf'
  SIZE 100M
  ENCRYPTION USING 'AES256'
  DEFAULT STORAGE(ENCRYPT);

-- Encrypt existing tablespace (12c online)
ALTER TABLESPACE users ENCRYPTION ONLINE ENCRYPT;
```

### 11.2 Data Redaction

```sql
-- Full redaction
BEGIN
  DBMS_REDACT.ADD_POLICY(
    object_schema => 'HR',
    object_name   => 'EMPLOYEES',
    policy_name   => 'REDACT_SALARY',
    column_name   => 'SALARY',
    function_type => DBMS_REDACT.FULL,
    expression    => 'SYS_CONTEXT(''USERENV'', ''SESSION_USER'') != ''HR_ADMIN'''
  );
END;
/

-- Partial redaction (mask first 12 digits of credit card)
BEGIN
  DBMS_REDACT.ADD_POLICY(
    object_schema     => 'SALES',
    object_name       => 'CUSTOMERS',
    policy_name       => 'MASK_CC',
    column_name       => 'CREDIT_CARD',
    function_type     => DBMS_REDACT.PARTIAL,
    function_parameters => 'VVVVFVVVVVVVVVVV,VVVVFVVVVVVVV1234,X,1,12',
    expression        => '1=1'
  );
END;
/
```

### 11.3 Database Vault

```sql
-- Enable Database Vault (cần license)
-- 1. Install
@?/rdbms/admin/catmac.sql

-- 2. Tạo realm để protect data
BEGIN
  DVSYS.DBMS_MACADM.CREATE_REALM(
    realm_name    => 'HR_REALM',
    description   => 'Protect HR data',
    enabled       => DBMS_MACUTL.G_YES,
    audit_options => DBMS_MACUTL.G_REALM_AUDIT_FAIL
  );
  
  -- Add objects to realm
  DVSYS.DBMS_MACADM.ADD_OBJECT_TO_REALM(
    realm_name   => 'HR_REALM',
    object_owner => 'HR',
    object_name  => 'EMPLOYEES',
    object_type  => 'TABLE'
  );
  
  -- Authorize user
  DVSYS.DBMS_MACADM.ADD_AUTH_TO_REALM(
    realm_name => 'HR_REALM',
    grantee    => 'HR_ADMIN'
  );
END;
/
```

### 11.4 Unified Auditing (12c)

```sql
-- Enable unified auditing (chỉ cần làm 1 lần)
-- Relink binary
cd $ORACLE_HOME/rdbms/lib
make -f ins_rdbms.mk uniaud_on ioracle

-- Tạo audit policy
CREATE AUDIT POLICY sensitive_data_access
  ACTIONS SELECT ON hr.employees,
          UPDATE ON hr.salaries,
          DELETE ON hr.terminations;

-- Enable policy
AUDIT POLICY sensitive_data_access;

-- Audit specific users
AUDIT POLICY sensitive_data_access BY hr_user, finance_user;

-- Xem audit records
SELECT event_timestamp, dbusername, sql_text
FROM unified_audit_trail
WHERE unified_audit_policies = 'SENSITIVE_DATA_ACCESS'
ORDER BY event_timestamp DESC;
```

### 11.5 Privilege Analysis (12c)

```sql
-- Tạo capture
BEGIN
  DBMS_PRIVILEGE_CAPTURE.CREATE_CAPTURE(
    name        => 'APP_USER_ANALYSIS',
    type        => DBMS_PRIVILEGE_CAPTURE.G_DATABASE,
    description => 'Analyze privileges for app_user'
  );
END;
/

-- Start capture
BEGIN
  DBMS_PRIVILEGE_CAPTURE.ENABLE_CAPTURE('APP_USER_ANALYSIS');
END;
/

-- Sau thời gian chạy application...

-- Stop capture
BEGIN
  DBMS_PRIVILEGE_CAPTURE.DISABLE_CAPTURE('APP_USER_ANALYSIS');
END;
/

-- Generate report
BEGIN
  DBMS_PRIVILEGE_CAPTURE.GENERATE_RESULT('APP_USER_ANALYSIS');
END;
/

-- Xem unused privileges
SELECT username, sys_priv 
FROM dba_unused_sysprivs_path;

SELECT username, object_owner, object_name, object_type, priv
FROM dba_unused_objprivs_path;
```

### 11.6 Network Encryption

```bash
# Cấu hình sqlnet.ora (Server)
SQLNET.ENCRYPTION_SERVER = REQUIRED
SQLNET.ENCRYPTION_TYPES_SERVER = (AES256, AES192, AES128)
SQLNET.CRYPTO_CHECKSUM_SERVER = REQUIRED
SQLNET.CRYPTO_CHECKSUM_TYPES_SERVER = (SHA256, SHA384, SHA512)

# Client
SQLNET.ENCRYPTION_CLIENT = REQUIRED
SQLNET.ENCRYPTION_TYPES_CLIENT = (AES256, AES192, AES128)
```

---

## 12. PERFORMANCE TUNING

### 12.1 Automatic Workload Repository (AWR)

#### AWR Configuration

```sql
-- Xem retention và interval
SELECT * FROM dba_hist_wr_control;

-- Thay đổi retention (60 days) và interval (30 min)
BEGIN
  DBMS_WORKLOAD_REPOSITORY.MODIFY_SNAPSHOT_SETTINGS(
    retention => 60*24*60,  -- Minutes
    interval  => 30          -- Minutes
  );
END;
/

-- Tạo manual snapshot
EXEC DBMS_WORKLOAD_REPOSITORY.CREATE_SNAPSHOT;
```

#### Generate AWR Report

```sql
-- Text format
@$ORACLE_HOME/rdbms/admin/awrrpt.sql

-- HTML format
@$ORACLE_HOME/rdbms/admin/awrrpti.sql

-- Compare periods
@$ORACLE_HOME/rdbms/admin/awrddrpt.sql

-- RAC report
@$ORACLE_HOME/rdbms/admin/awrgrpt.sql
```

#### AWR Report Analysis - Key Sections

**1. Load Profile**
- Redo size per second
- Logical/Physical reads
- Transactions per second

**2. Top 5 Timed Events**
```
Event                          Waits    Time(s)  % Total
------------------------------ -------- -------- --------
db file sequential read        1,234K      456      45%
CPU time                                   234      23%
log file sync                    567K      123      12%
```

**3. SQL Statistics**
- Top SQL by elapsed time
- Top SQL by CPU time
- Top SQL by buffer gets

**4. Instance Efficiency**
- Buffer cache hit ratio
- Library cache hit ratio
- Parse CPU to Parse Elapsed
- Execute to Parse ratio

### 12.2 Active Session History (ASH)

```sql
-- Generate ASH report
@$ORACLE_HOME/rdbms/admin/ashrpt.sql

-- Query ASH data
SELECT sample_time, session_id, sql_id, event, wait_time
FROM v$active_session_history
WHERE sample_time > SYSDATE - 1/24  -- Last hour
  AND event IS NOT NULL
ORDER BY sample_time;

-- Top events in last hour
SELECT event, COUNT(*) AS samples
FROM v$active_session_history
WHERE sample_time > SYSDATE - 1/24
GROUP BY event
ORDER BY samples DESC;
```

### 12.3 SQL Tuning

#### SQL Tuning Advisor

```sql
-- Tạo tuning task
DECLARE
  v_task_name VARCHAR2(30);
BEGIN
  v_task_name := DBMS_SQLTUNE.CREATE_TUNING_TASK(
    sql_id      => 'abc123xyz',
    scope       => 'COMPREHENSIVE',
    time_limit  => 300,
    task_name   => 'TUNE_SQL_ABC123',
    description => 'Tune slow query'
  );
  
  -- Execute task
  DBMS_SQLTUNE.EXECUTE_TUNING_TASK('TUNE_SQL_ABC123');
END;
/

-- Xem report
SELECT DBMS_SQLTUNE.REPORT_TUNING_TASK('TUNE_SQL_ABC123') FROM DUAL;

-- Accept recommendations
EXEC DBMS_SQLTUNE.ACCEPT_SQL_PROFILE(task_name => 'TUNE_SQL_ABC123');
```

#### SQL Plan Management

```sql
-- Tạo SQL plan baseline
DECLARE
  v_plans PLS_INTEGER;
BEGIN
  v_plans := DBMS_SPM.LOAD_PLANS_FROM_CURSOR_CACHE(
    sql_id => 'abc123xyz'
  );
END;
/

-- Xem baselines
SELECT sql_handle, plan_name, enabled, accepted
FROM dba_sql_plan_baselines;

-- Accept plan
EXEC DBMS_SPM.ALTER_SQL_PLAN_BASELINE(
  sql_handle => 'SQL_abcd1234',
  plan_name  => 'SQL_PLAN_xyz',
  attribute_name => 'ACCEPTED',
  attribute_value => 'YES'
);
```

#### Adaptive Query Optimization (12c)

```sql
-- Enable adaptive features
ALTER SESSION SET optimizer_adaptive_features = TRUE;

-- Adaptive plans
ALTER SESSION SET optimizer_adaptive_plans = TRUE;

-- Adaptive statistics
ALTER SESSION SET optimizer_adaptive_statistics = TRUE;
```

### 12.4 Memory Tuning

```sql
-- Xem PGA usage
SELECT name, value/1024/1024 AS mb
FROM v$pgastat
WHERE name IN ('total PGA allocated', 'maximum PGA allocated');

-- Xem PGA advice
SELECT pga_target_for_estimate/1024/1024 AS target_mb,
       estd_pga_cache_hit_percentage AS cache_hit_pct
FROM v$pga_target_advice
ORDER BY pga_target_for_estimate;

-- Xem SGA components
SELECT component, current_size/1024/1024 AS current_mb,
       min_size/1024/1024 AS min_mb,
       max_size/1024/1024 AS max_mb
FROM v$sga_dynamic_components;

-- SGA advice
SELECT size_for_estimate/1024/1024 AS size_mb,
       estd_db_time_factor
FROM v$db_cache_advice
WHERE name = 'DEFAULT'
ORDER BY size_for_estimate;
```

### 12.5 I/O Tuning

```sql
-- Xem I/O statistics
SELECT file_name, phyrds, phywrts, readtim, writetim
FROM v$filestat f, dba_data_files d
WHERE f.file# = d.file_id
ORDER BY phyrds + phywrts DESC;

-- Top segments by I/O
SELECT owner, object_name, object_type,
       logical_reads, physical_reads, physical_writes
FROM v$segment_statistics
WHERE statistic_name IN ('logical reads', 'physical reads', 'physical writes')
ORDER BY logical_reads DESC
FETCH FIRST 20 ROWS ONLY;
```

### 12.6 Wait Events Analysis

```sql
-- System wait events
SELECT event, total_waits, time_waited/100 AS time_sec,
       average_wait*10 AS avg_wait_ms
FROM v$system_event
WHERE wait_class != 'Idle'
ORDER BY time_waited DESC
FETCH FIRST 20 ROWS ONLY;

-- Session wait events
SELECT sid, event, seconds_in_wait, state
FROM v$session_wait
WHERE wait_class != 'Idle';
```

---

## 13. CÀI ĐẶT VÀ CẤU HÌNH

### 13.1 System Requirements

#### Hardware Requirements (Minimum)

| Component | Requirement |
|-----------|-------------|
| **RAM** | 1 GB (minimum), 2 GB+ recommended |
| **Swap** | RAM × 1.5 (if RAM < 2GB), RAM × 1 (if RAM 2-16GB) |
| **Disk Space** | 6.5 GB cho software, 2 GB cho database |
| **CPU** | x86-64 architecture |

#### Software Requirements (Linux)

- Oracle Linux 6/7
- Red Hat Enterprise Linux 6/7
- Required packages: binutils, compat-libcap1, gcc, glibc, ksh, etc.

### 13.2 Pre-Installation (Linux)

#### Kernel Parameters

```bash
# /etc/sysctl.conf
fs.file-max = 6815744
kernel.sem = 250 32000 100 128
kernel.shmmni = 4096
kernel.shmall = 1073741824
kernel.shmmax = 4398046511104
net.core.rmem_default = 262144
net.core.rmem_max = 4194304
net.core.wmem_default = 262144
net.core.wmem_max = 1048576
fs.aio-max-nr = 1048576
net.ipv4.ip_local_port_range = 9000 65500

# Apply
sysctl -p
```

#### Resource Limits

```bash
# /etc/security/limits.conf
oracle soft nproc 2047
oracle hard nproc 16384
oracle soft nofile 1024
oracle hard nofile 65536
oracle soft stack 10240
oracle hard stack 32768
```

#### Create Oracle User và Groups

```bash
groupadd -g 54321 oinstall
groupadd -g 54322 dba
groupadd -g 54323 oper
useradd -u 54321 -g oinstall -G dba,oper oracle

# Set password
passwd oracle
```

#### Create Directories

```bash
mkdir -p /u01/app/oracle/product/12.2.0/dbhome_1
mkdir -p /u01/app/oraInventory
chown -R oracle:oinstall /u01
chmod -R 775 /u01
```

### 13.3 Installation

#### Environment Variables

```bash
# .bash_profile cho oracle user
export ORACLE_BASE=/u01/app/oracle
export ORACLE_HOME=$ORACLE_BASE/product/12.2.0/dbhome_1
export ORACLE_SID=PROD
export PATH=$ORACLE_HOME/bin:$PATH
export LD_LIBRARY_PATH=$ORACLE_HOME/lib:/lib:/usr/lib
```

#### Silent Installation

```bash
# response file: db_install.rsp
oracle.install.option=INSTALL_DB_SWONLY
ORACLE_HOSTNAME=server.example.com
UNIX_GROUP_NAME=oinstall
INVENTORY_LOCATION=/u01/app/oraInventory
SELECTED_LANGUAGES=en
ORACLE_HOME=/u01/app/oracle/product/12.2.0/dbhome_1
ORACLE_BASE=/u01/app/oracle
oracle.install.db.InstallEdition=EE
oracle.install.db.DBA_GROUP=dba
oracle.install.db.OPER_GROUP=oper
DECLINE_SECURITY_UPDATES=true

# Run installer
./runInstaller -silent -responseFile /tmp/db_install.rsp
```

### 13.4 Database Creation

#### Using DBCA (GUI)

```bash
dbca
```

#### Silent Mode

```bash
dbca -silent -createDatabase \
  -templateName General_Purpose.dbc \
  -gdbname PROD \
  -sid PROD \
  -sysPassword sys_password \
  -systemPassword system_password \
  -datafileDestination /u01/oradata \
  -recoveryAreaDestination /u01/fra \
  -storageType FS \
  -characterSet AL32UTF8 \
  -nationalCharacterSet AL16UTF16 \
  -memoryPercentage 40 \
  -emConfiguration NONE
```

#### Manual Database Creation

```sql
-- 1. Tạo password file
orapwd file=$ORACLE_HOME/dbs/orapwPROD password=sys_password entries=5

-- 2. Tạo init parameter file
# $ORACLE_HOME/dbs/initPROD.ora
db_name=PROD
memory_target=1G
processes=150
control_files=/u01/oradata/control01.ctl,/u01/oradata/control02.ctl
undo_tablespace=UNDOTBS1
compatible=12.2.0

-- 3. Start instance
sqlplus / as sysdba
STARTUP NOMOUNT PFILE='$ORACLE_HOME/dbs/initPROD.ora';

-- 4. CREATE DATABASE
CREATE DATABASE PROD
  USER SYS IDENTIFIED BY sys_password
  USER SYSTEM IDENTIFIED BY system_password
  LOGFILE 
    GROUP 1 '/u01/oradata/redo01.log' SIZE 100M,
    GROUP 2 '/u01/oradata/redo02.log' SIZE 100M,
    GROUP 3 '/u01/oradata/redo03.log' SIZE 100M
  MAXLOGFILES 5
  MAXLOGHISTORY 1
  DATAFILE '/u01/oradata/system01.dbf' SIZE 500M AUTOEXTEND ON
  SYSAUX DATAFILE '/u01/oradata/sysaux01.dbf' SIZE 300M AUTOEXTEND ON
  DEFAULT TABLESPACE users
    DATAFILE '/u01/oradata/users01.dbf' SIZE 100M AUTOEXTEND ON
  DEFAULT TEMPORARY TABLESPACE temp
    TEMPFILE '/u01/oradata/temp01.dbf' SIZE 100M
  UNDO TABLESPACE undotbs1
    DATAFILE '/u01/oradata/undotbs01.dbf' SIZE 200M AUTOEXTEND ON
  CHARACTER SET AL32UTF8
  NATIONAL CHARACTER SET AL16UTF16;

-- 5. Run catalog scripts
@?/rdbms/admin/catalog.sql
@?/rdbms/admin/catproc.sql

-- 6. Create SPFILE
CREATE SPFILE FROM PFILE;
```

### 13.5 Listener Configuration

#### listener.ora

```bash
# $ORACLE_HOME/network/admin/listener.ora
LISTENER =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCP)(HOST = server.example.com)(PORT = 1521))
    )
  )

SID_LIST_LISTENER =
  (SID_LIST =
    (SID_DESC =
      (GLOBAL_DBNAME = PROD)
      (ORACLE_HOME = /u01/app/oracle/product/12.2.0/dbhome_1)
      (SID_NAME = PROD)
    )
  )
```

#### tnsnames.ora

```bash
# $ORACLE_HOME/network/admin/tnsnames.ora
PROD =
  (DESCRIPTION =
    (ADDRESS = (PROTOCOL = TCP)(HOST = server.example.com)(PORT = 1521))
    (CONNECT_DATA =
      (SERVER = DEDICATED)
      (SERVICE_NAME = PROD)
    )
  )
```

#### Start/Stop Listener

```bash
# Start
lsnrctl start

# Stop
lsnrctl stop

# Status
lsnrctl status

# Reload configuration
lsnrctl reload
```

---

## 14. DATA TYPES VÀ CONSTRAINTS

### 14.1 Data Types

#### Character Data Types

| Data Type | Max Size (Standard) | Max Size (Extended 12c) | Mô Tả |
|-----------|---------------------|-------------------------|-------|
| **VARCHAR2(size)** | 4000 bytes | 32767 bytes | Variable-length character |
| **CHAR(size)** | 2000 bytes | 2000 bytes | Fixed-length character |
| **NVARCHAR2(size)** | 4000 bytes | 32767 bytes | Variable-length Unicode |
| **NCHAR(size)** | 2000 bytes | 2000 bytes | Fixed-length Unicode |
| **CLOB** | 4 GB | 4 GB | Character large object |
| **NCLOB** | 4 GB | 4 GB | Unicode large object |

#### Numeric Data Types

| Data Type | Range | Mô Tả |
|-----------|-------|-------|
| **NUMBER(p,s)** | p: 1-38, s: -84 to 127 | Variable precision |
| **FLOAT(p)** | p: 1-126 | Binary precision |
| **BINARY_FLOAT** | 32-bit | Single precision |
| **BINARY_DOUBLE** | 64-bit | Double precision |

#### Date/Time Data Types

| Data Type | Mô Tả |
|-----------|-------|
| **DATE** | Date and time (7 bytes) |
| **TIMESTAMP** | Date and time with fractional seconds |
| **TIMESTAMP WITH TIME ZONE** | TIMESTAMP with timezone |
| **TIMESTAMP WITH LOCAL TIME ZONE** | TIMESTAMP normalized to database timezone |
| **INTERVAL YEAR TO MONTH** | Period of years and months |
| **INTERVAL DAY TO SECOND** | Period of days, hours, minutes, seconds |

#### Binary Data Types

| Data Type | Max Size (Standard) | Max Size (Extended 12c) |
|-----------|---------------------|-------------------------|
| **RAW(size)** | 2000 bytes | 32767 bytes |
| **BLOB** | 4 GB | 4 GB |

#### Other Data Types

- **ROWID**: Physical row address
- **UROWID**: Universal rowid
- **XMLTYPE**: XML data
- **JSON** (12c): JSON documents

### 14.2 Constraints

#### Primary Key

```sql
-- Column level
CREATE TABLE employees (
  emp_id NUMBER PRIMARY KEY,
  emp_name VARCHAR2(100)
);

-- Table level
CREATE TABLE employees (
  emp_id NUMBER,
  emp_name VARCHAR2(100),
  CONSTRAINT pk_employees PRIMARY KEY (emp_id)
);

-- Composite primary key
CREATE TABLE order_items (
  order_id NUMBER,
  item_id NUMBER,
  product_id NUMBER,
  CONSTRAINT pk_order_items PRIMARY KEY (order_id, item_id)
);
```

#### Foreign Key

```sql
CREATE TABLE orders (
  order_id NUMBER PRIMARY KEY,
  customer_id NUMBER,
  order_date DATE,
  CONSTRAINT fk_customer FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id)
    ON DELETE CASCADE
);

-- ON DELETE options:
-- CASCADE: Delete child rows
-- SET NULL: Set child FK to NULL
-- NO ACTION: Prevent deletion (default)
```

#### Unique Constraint

```sql
CREATE TABLE employees (
  emp_id NUMBER PRIMARY KEY,
  email VARCHAR2(100) UNIQUE,
  emp_name VARCHAR2(100)
);

-- Table level
ALTER TABLE employees
  ADD CONSTRAINT uk_email UNIQUE (email);
```

#### Check Constraint

```sql
CREATE TABLE employees (
  emp_id NUMBER PRIMARY KEY,
  emp_name VARCHAR2(100),
  salary NUMBER CHECK (salary > 0),
  age NUMBER,
  CONSTRAINT chk_age CHECK (age BETWEEN 18 AND 65)
);

-- Complex check
ALTER TABLE orders
  ADD CONSTRAINT chk_dates 
  CHECK (ship_date >= order_date);
```

#### NOT NULL Constraint

```sql
CREATE TABLE employees (
  emp_id NUMBER NOT NULL,
  emp_name VARCHAR2(100) NOT NULL,
  email VARCHAR2(100)
);

-- Modify column
ALTER TABLE employees
  MODIFY email VARCHAR2(100) NOT NULL;
```

#### Default Values (12c Enhancement)

```sql
-- Static default
CREATE TABLE employees (
  emp_id NUMBER,
  hire_date DATE DEFAULT SYSDATE,
  status VARCHAR2(20) DEFAULT 'ACTIVE'
);

-- Sequence default (12c)
CREATE TABLE employees (
  emp_id NUMBER DEFAULT emp_seq.NEXTVAL,
  emp_name VARCHAR2(100)
);

-- ON NULL default (12c)
CREATE TABLE employees (
  emp_id NUMBER,
  created_date DATE DEFAULT ON NULL SYSDATE
);
```

### 14.3 Managing Constraints

```sql
-- Disable constraint
ALTER TABLE employees DISABLE CONSTRAINT pk_employees;

-- Enable constraint
ALTER TABLE employees ENABLE CONSTRAINT pk_employees;

-- Drop constraint
ALTER TABLE employees DROP CONSTRAINT pk_employees;

-- Add constraint
ALTER TABLE employees
  ADD CONSTRAINT pk_employees PRIMARY KEY (emp_id);

-- Defer constraint checking (for deferrable constraints)
CREATE TABLE test (
  id NUMBER,
  CONSTRAINT pk_test PRIMARY KEY (id) DEFERRABLE
);

SET CONSTRAINT pk_test DEFERRED;
```

### 14.4 Viewing Constraints

```sql
-- All constraints
SELECT constraint_name, constraint_type, table_name, status
FROM user_constraints
WHERE table_name = 'EMPLOYEES';

-- Constraint columns
SELECT constraint_name, column_name, position
FROM user_cons_columns
WHERE table_name = 'EMPLOYEES'
ORDER BY constraint_name, position;

-- Foreign key relationships
SELECT a.constraint_name, a.table_name, a.column_name,
       b.constraint_name, b.table_name, b.column_name
FROM user_cons_columns a
JOIN user_constraints c ON a.constraint_name = c.constraint_name
JOIN user_cons_columns b ON c.r_constraint_name = b.constraint_name
WHERE c.constraint_type = 'R';
```

---

## 15. UPGRADE VÀ MIGRATION

### 15.1 Upgrade Methods

#### Method 1: Database Upgrade Assistant (DBUA)
- GUI-based tool
- Automated upgrade process
- Performs pre-upgrade checks
- Creates restore point
- Khuyến nghị cho hầu hết trường hợp

#### Method 2: Manual Upgrade
- Command-line scripts
- Linh hoạt hơn
- Dùng cho automation

#### Method 3: Data Pump Export/Import
- Full Transportable Export/Import
- Cho phép thay đổi platform
- Có thể upgrade và migrate đồng thời

#### Method 4: Transportable Tablespaces
- Nhanh nhất cho large databases
- Có thể cross-platform

### 15.2 Upgrade từ 11g/12c đến 12c

#### Pre-Upgrade Steps

```bash
# 1. Backup database
RMAN> BACKUP DATABASE PLUS ARCHIVELOG;

# 2. Run Pre-Upgrade Information Tool
cd $ORACLE_HOME/rdbms/admin
java -jar preupgrd.jar -c "jdbc:oracle:thin:@localhost:1521:PROD" \
  -u sys -p password

# Hoặc trong SQL*Plus
@preupgrd.sql

# 3. Review output
cat preupgrade.log
cat preupgrade_fixups.sql

# 4. Run fixup scripts
sqlplus / as sysdba
@preupgrade_fixups.sql
```

#### Using DBUA

```bash
# Start DBUA
dbua

# Follow wizard:
# 1. Select database
# 2. Review prerequisites
# 3. Configure upgrade options
# 4. Run upgrade
# 5. Review results
```

#### Manual Upgrade

```bash
# 1. Stop database
sqlplus / as sysdba
SHUTDOWN IMMEDIATE;

# 2. Copy parameter file
cp $OLD_ORACLE_HOME/dbs/spfilePROD.ora $NEW_ORACLE_HOME/dbs/

# 3. Start from new home
export ORACLE_HOME=/u01/app/oracle/product/12.2.0/dbhome_1
sqlplus / as sysdba
STARTUP UPGRADE;

# 4. Run upgrade scripts
cd $ORACLE_HOME/rdbms/admin
@catupgrd.sql

# 5. Run post-upgrade scripts
@utlrp.sql
@postupgrade_fixups.sql

# 6. Restart database
SHUTDOWN IMMEDIATE;
STARTUP;

# 7. Verify
SELECT version FROM v$instance;
```

### 15.3 Migrate to Pluggable Database

#### Unplug/Plug Method

```sql
-- 1. Non-CDB: Shutdown database
SHUTDOWN IMMEDIATE;

-- 2. Non-CDB: Start in READ ONLY
STARTUP OPEN READ ONLY;

-- 3. Non-CDB: Generate manifest file
EXEC DBMS_PDB.DESCRIBE(pdb_descr_file => '/tmp/noncdb.xml');

-- 4. Non-CDB: Shutdown
SHUTDOWN IMMEDIATE;

-- 5. CDB: Check compatibility
SET SERVEROUTPUT ON
DECLARE
  compatible BOOLEAN;
BEGIN
  compatible := DBMS_PDB.CHECK_PLUG_COMPATIBILITY(
    pdb_descr_file => '/tmp/noncdb.xml'
  );
  IF compatible THEN
    DBMS_OUTPUT.PUT_LINE('Compatible');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Incompatible - check PDB_PLUG_IN_VIOLATIONS');
  END IF;
END;
/

-- 6. CDB: Create PDB from non-CDB
CREATE PLUGGABLE DATABASE pdb1 USING '/tmp/noncdb.xml'
  NOCOPY
  FILE_NAME_CONVERT = ('/u01/oradata/NONCDB/', '/u01/oradata/CDB1/pdb1/');

-- 7. Open PDB
ALTER PLUGGABLE DATABASE pdb1 OPEN;

-- 8. Run noncdb_to_pdb script
ALTER SESSION SET CONTAINER = pdb1;
@?/rdbms/admin/noncdb_to_pdb.sql
```

#### Full Transportable Export/Import

```bash
# 1. Source: Set tablespaces to READ ONLY
sqlplus / as sysdba
ALTER TABLESPACE users READ ONLY;
ALTER TABLESPACE data01 READ ONLY;

# 2. Export metadata
expdp system/password FULL=Y TRANSPORTABLE=ALWAYS \
  DIRECTORY=dpump_dir DUMPFILE=full_tts.dmp LOGFILE=full_tts.log

# 3. Copy datafiles to target
cp /u01/oradata/source/*.dbf /u01/oradata/target/pdb1/

# 4. Target CDB: Import into PDB
impdp system/password@pdb1 FULL=Y \
  DIRECTORY=dpump_dir DUMPFILE=full_tts.dmp LOGFILE=full_tts_imp.log \
  TRANSPORT_DATAFILES='/u01/oradata/target/pdb1/users01.dbf',
                      '/u01/oradata/target/pdb1/data01.dbf'
```

### 15.4 Post-Upgrade Tasks

```sql
-- Recompile invalid objects
@?/rdbms/admin/utlrp.sql

-- Update optimizer statistics
EXEC DBMS_STATS.GATHER_DICTIONARY_STATS;
EXEC DBMS_STATS.GATHER_FIXED_OBJECTS_STATS;

-- Upgrade timezone file (if needed)
SELECT * FROM v$timezone_file;

-- Check registry
SELECT comp_name, version, status 
FROM dba_registry
ORDER BY comp_name;

-- Check for violations (PDB)
SELECT name, cause, type, message
FROM pdb_plug_in_violations
WHERE status != 'RESOLVED';
```

### 15.5 Rollback Strategy

```sql
-- Before upgrade: Create guaranteed restore point
CREATE RESTORE POINT before_upgrade GUARANTEE FLASHBACK DATABASE;

-- If upgrade fails, flashback database
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
FLASHBACK DATABASE TO RESTORE POINT before_upgrade;
ALTER DATABASE OPEN RESETLOGS;

-- Drop restore point when done
DROP RESTORE POINT before_upgrade;
```

---

## PHỤ LỤC

### A. Các View Quan Trọng

#### Data Dictionary Views

| View | Mô Tả |
|------|-------|
| **DBA_TABLES** | Tất cả tables trong database |
| **DBA_USERS** | Tất cả users |
| **DBA_TABLESPACES** | Tất cả tablespaces |
| **DBA_DATA_FILES** | Tất cả datafiles |
| **DBA_SEGMENTS** | Storage segments |
| **DBA_EXTENTS** | Extents |
| **DBA_INDEXES** | Indexes |
| **DBA_CONSTRAINTS** | Constraints |

#### Dynamic Performance Views (V$)

| View | Mô Tả |
|------|-------|
| **V$DATABASE** | Database information |
| **V$INSTANCE** | Instance information |
| **V$SESSION** | Current sessions |
| **V$SQL** | SQL statements in shared pool |
| **V$SQLAREA** | Statistics for SQL statements |
| **V$SYSSTAT** | System statistics |
| **V$PARAMETER** | Init parameters |
| **V$DATAFILE** | Datafile information |
| **V$TABLESPACE** | Tablespace information |

#### Multitenant Views

| View | Mô Tả |
|------|-------|
| **V$PDBS** | Pluggable databases |
| **CDB_TABLES** | Tables across all containers |
| **CDB_USERS** | Users across all containers |
| **V$CONTAINERS** | Container information |

### B. Useful Commands Reference

```sql
-- Database info
SELECT name, created, log_mode, open_mode FROM v$database;
SELECT instance_name, version, status, startup_time FROM v$instance;

-- Session info
SELECT sid, serial#, username, status, program FROM v$session;

-- Kill session
ALTER SYSTEM KILL SESSION 'sid,serial#' IMMEDIATE;

-- Archive log info
SELECT sequence#, first_time, next_time FROM v$archived_log;

-- Redo log info
SELECT group#, thread#, sequence#, bytes, members, status FROM v$log;

-- Controlfile info
SELECT name FROM v$controlfile;

-- Temp tablespace usage
SELECT tablespace_name, total_blocks, used_blocks, free_blocks
FROM v$sort_segment;

-- Lock info
SELECT l.sid, s.serial#, l.type, l.id1, l.id2, l.lmode, l.request
FROM v$lock l, v$session s
WHERE l.sid = s.sid;
```

### C. Best Practices

1. **Backup Strategy**
   - Daily incremental backups
   - Weekly full backups
   - Archive log backups every 15-30 minutes
   - Test restore regularly

2. **Security**
   - Enable TDE for sensitive data
   - Use unified auditing
   - Regular privilege analysis
   - Strong password policies

3. **Performance**
   - Monitor AWR reports weekly
   - Keep statistics current
   - Use SQL tuning advisor
   - Partition large tables
   - Use appropriate indexes

4. **Maintenance**
   - Regular patching (quarterly)
   - Recompile invalid objects
   - Purge audit records
   - Monitor tablespace usage
   - Archive old data

5. **High Availability**
   - Implement Data Guard for DR
   - Use RAC for scalability
   - Regular failover testing
   - Document recovery procedures

---

## KẾT LUẬN

Oracle Database 12c mang đến nhiều tính năng quan trọng:

### Điểm Nổi Bật
- **Multitenant Architecture**: Consolidation hiệu quả
- **In-Memory Column Store**: Performance boost cho analytics
- **Enhanced Security**: TDE, Data Redaction, Privilege Analysis
- **JSON Support**: NoSQL capabilities
- **Improved SQL**: Identity columns, row limiting, extended data types
- **Better Manageability**: Unified auditing, online operations

### Khi Nào Sử Dụng 12c
- Cần consolidate nhiều databases
- Yêu cầu high performance cho analytics
- Cần advanced security features
- Migrate lên cloud
- Modernize infrastructure

### Tài Nguyên Học Tập
- Oracle Documentation: docs.oracle.com
- Oracle-Base: oracle-base.com
- My Oracle Support: support.oracle.com
- Oracle Learning Library
- Community forums

**Lưu ý**: Oracle 12c đã end of premier support (2019). Nên xem xét upgrade lên 19c hoặc 21c cho production systems mới.

---

*Document này tổng hợp kiến thức cơ bản và trọng tâm của Oracle Database 12c+. Để biết thêm chi tiết, tham khảo Oracle Official Documentation.*