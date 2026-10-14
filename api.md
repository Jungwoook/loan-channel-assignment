# 사용자 API

### POST `/api/auth/login`

- 사용자 로그인

요청 예시

```json
{
    "login_id": "사용자 ID",
    "password": "********"
}
```

응답 예시

```json
{
    "status": "success",
    "token": "jwt access token"
}
```

### GET `/api/terms`

- 약관 조회

응답 예시

```json
{
    "terms": "약관"
}
```

### POST `/api/terms/consent`

- 약관 동의

요청 예시

```json
{
  
}
```

응답 예시

```json
{
  
}
```

### POST `/api/check-limit`

- 한도 조회

```json
{
    "income": "연소득",
    "job": "직업"
}
```

응답 예시

```json
{
    "limit": "한도"
}
```

### POST `/api/application`

- 신청서 작성 시작

```json
{
    "대출 금액": "",
    "대출 기간": ""
}
```

응답 예시

```json
{
    
}
```

### PATCH `/api/application`

- 신청서 수정

```json
{
    "대출 금액": "",
    "대출 기간": ""
}
```

응답 예시

```json
{
    
}
```

### POST `/api/application/submit`

- 신청서 제출

```json
{
    "대출 금액": "",
    "대출 기간": ""
}
```

응답 예시

```json
{
    
}
```

### GET `/api/application/{application_id}`

- 신청 내용 조회

응답 예시

```json
{
    
}
```