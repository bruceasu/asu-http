asu-http
==========

target：
-------
A lightweight HTTP server without third-party dependencies, very simple and fast implementation of RESTful services,
Mock services, and easy extend other features. It's not a frameworks like play! framework, servlet(tomcat, jetty),
spring mvc and other wheels.


---

# 📌 REST API 设计速查清单

## 1. 路径设计原则

* **资源名用名词**（复数），不要动词

    * ✅ `/users`
    * ❌ `/getUsers`
* **层级表示从属关系**

    * ✅ `/users/{id}/posts`
* **动作用子资源或动词化字段**

    * ✅ `POST /users/{id}/reset-password`

---

## 2. 常见操作和状态码

| 操作        | 方法     | 成功返回                        | 典型错误            |
| --------- | ------ | --------------------------- | --------------- |
| 获取资源列表    | GET    | `200 OK` + JSON 数组          | `400` / `404`   |
| 获取单个资源    | GET    | `200 OK` + JSON 对象          | `404 Not Found` |
| 创建资源      | POST   | `201 Created` + `Location`  | `400` / `409`   |
| 更新资源 (全量) | PUT    | `200 OK` / `204 No Content` | `400` / `404`   |
| 更新资源 (部分) | PATCH  | `200 OK` / `204 No Content` | `400` / `404`   |
| 删除资源      | DELETE | `204 No Content`            | `404 Not Found` |
| 提交异步任务    | POST   | `202 Accepted` + 任务 ID      | `400`           |
| 查询异步任务结果  | GET    | `200 OK` + 任务状态 JSON        | `404 Not Found` |

---

## 3. 错误返回格式 (推荐)

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User with id 123 does not exist",
  "error": {
    "id": 123
  }
}
```

* **code** → 业务错误码（方便客户端判断）
* **message** → 人类可读信息
* **details** → 额外上下文（可选）

---

## 4. 分页 & 过滤

### 分页参数

```
GET /users?page=2&size=20
```

返回：

```json
{
  "code": "ok",
  "result": {
    "data": [
      ...
    ],
    "page": 2,
    "size": 20,
    "total": 153
  }
}

```

### 过滤 & 排序

```
GET /users?role=admin&sort=-createdAt
```

---

## 5. 安全与版本

* **版本控制**：`/api/v1/...`
* **认证**：HTTP Header → `Authorization: Bearer <token>`
* **幂等性**：`PUT`、`DELETE` 应该是幂等的，`POST` 可以支持幂等键 (Idempotency-Key)。

---

## 6. Content-Type

* **JSON 请求/响应** → `application/json`
* **文件上传** → `multipart/form-data`
* **文件下载** → `application/octet-stream` 或对应 MIME 类型

--

# 📖 API 文档模板 (`API.md`)

## Overview

This document describes the REST API endpoints, including request/response formats, status codes, and error handling.

* **Base URL**: `https://api.example.com/v1`
* **Content-Type**: `application/json`
* **Authentication**: Bearer Token (`Authorization: Bearer <token>`)

---

## Authentication

### Request

```http
POST /auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "secret"
}
```

### Response

```http
200 OK
Content-Type: application/json

{
  "code": "ok",
  "result": {
    "token": "eyJhbGciOi..."
  }
}
```

---

## Users

### Get User

```http
GET /users/{id}
```

#### Response

```http
200 OK
Content-Type: application/json
{
  "code": "ok",
  "result": {
      "id": 123,
      "name": "Alice",
      "email": "alice@example.com",
      "role": "admin"
    }
}
```

**Error**

```http
404 Not Found
Content-Type: application/json

{
  "code": "USER_NOT_FOUND",
  "message": "User with id 123 does not exist"
  "error": {
    
  }
}
```

---

### Create User

```http
POST /users
Content-Type: application/json
{
  "name": "Bob",
  "email": "bob@example.com",
  "role": "user"
}

```

#### Response

```http
201 Created
Location: /users/124
Content-Type: application/json

{
  "code": "ok",
  "result": {
      "id": 124,
      "name": "Bob",
      "email": "bob@example.com",
      "role": "user"
    }
}
```

---

### Delete User

```http
DELETE /users/124
```

#### Response

```http
204 No Content
```

---

## Async Jobs

### Submit Job

```http
POST /jobs
Content-Type: application/json

{
  "task": "video-encode",
  "params": {
    "input": "s3://bucket/input.mp4",
    "preset": "1080p"
  }
}
```

#### Response

```http
202 Accepted
Location: /jobs/567
Content-Type: application/json

{
  "code": "ok",
  "result":{
      "jobId": 567,
      "status": "pending"
    }
}
```

---

### Get Job Result

```http
GET /jobs/567
```

#### Response

```http
200 OK
Content-Type: application/json

{
  "code": "ok",
  "result": {
      "jobId": 567,
      "status": "done",
      "output": "s3://bucket/output.mp4",
      "duration": 300
    }
}
```

---

## Error Format

All errors follow a consistent JSON structure:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "The requested resource does not exist",
  "error": { "id": 123 }
}
```

---

## Status Codes Quick Reference

| Code | Meaning               | Example Use Case            |
| ---- | --------------------- | --------------------------- |
| 200  | OK                    | Successful GET/PUT/PATCH    |
| 201  | Created               | Resource created (POST)     |
| 202  | Accepted              | Async job submitted         |
| 204  | No Content            | Successful DELETE/PUT       |
| 400  | Bad Request           | Invalid parameters          |
| 401  | Unauthorized          | Missing/invalid auth token  |
| 403  | Forbidden             | Authenticated but no access |
| 404  | Not Found             | Resource does not exist     |
| 409  | Conflict              | Duplicate or conflict state |
| 422  | Unprocessable Entity  | Validation failed           |
| 500  | Internal Server Error | Unexpected server issue     |

---