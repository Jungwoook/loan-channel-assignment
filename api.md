# 사용자 API

## 공통 규칙

- 아래 요청·응답은 현재 erd.md를 기준으로 한 API 설계 예시다. 금액은 원, 기간은 개월, 금리는 연이율 % 단위의 JSON 숫자로 전달한다.
- 로그인 외 사용자 API는 `Authorization: Bearer <access_token>` 헤더로 인증한다. 신청의 `user_id`는 인증 정보에서 결정하며 요청 본문으로 받지 않는다.
- JSON 필드는 ERD와 동일한 snake_case를 사용한다. 로그인 요청의 기존 `login_id`는 `user_id`로 변경한다.
- 시각은 ISO 8601 형식으로 반환한다. 예시의 `+09:00`은 한국 표준시이며, Oracle TIMESTAMP에 시간대가 없으므로 저장·변환 시간대를 구현 시 통일한다.
- 고객은 본인 신청만 조회·수정할 수 있다. 타인 신청 접근은 403, 존재하지 않는 대상은 404로 응답한다.
- 입력 오류는 400, 인증 실패는 401, 허용되지 않는 상태 변경은 409로 응답한다. 오류 응답은 `code`, `message`, `traceId`를 포함한다.
- 클라이언트는 신청·심사 상태, 심사 회차, 승인 결과, 생성·수정 시각을 직접 지정할 수 없다.

### POST `/api/auth/login`

- 사용자 로그인

요청 예시

```json
{
    "user_id": "customer01",
    "password": "********"
}
```

응답 예시

```json
{
    "status": "success",
    "access_token": "jwt access token",
    "token_type": "Bearer",
    "user_id": "customer01",
    "role": "사용자"
}
```

- 응답의 기존 `token`은 `access_token`으로 변경한다. 토큰은 User 테이블에 저장하지 않는다.
- `role`은 `사용자` 또는 `운영자`다. 비밀번호는 BCrypt 해시와 비교하고 응답에 포함하지 않는다.

### GET `/api/products`

- 신청 가능한 상품 목록 조회. `status=ACTIVE`인 상품만 반환한다.
- 성공: 200 OK. 상품이 없으면 `products`는 빈 배열이다.

응답 예시

```json
{
    "products": [
        {
            "product_key": 1,
            "product_name": "직장인 신용대출",
            "product_description": "상품 안내",
            "min_loan_amount": 1000000,
            "max_loan_amount": 100000000,
            "min_loan_term": 12,
            "max_loan_term": 60,
            "status": "ACTIVE"
        }
    ]
}
```

### GET `/api/terms`

- 동의 화면의 약관 목록과 현재 버전 조회. 성공: 200 OK.
- 각 약관의 현재 버전(TermsVersion.current_yn = `Y`)만 반환한다. `display_order` 오름차순이다.

응답 예시

```json
{
    "terms": [
        {
            "term_id": 1,
            "term_name": "개인(신용)정보 수집·이용 동의",
            "required_yn": "Y",
            "display_order": 1,
            "version_id": 11,
            "version_no": 3
        },
        {
            "term_id": 4,
            "term_name": "마케팅 정보 수신 동의",
            "required_yn": "N",
            "display_order": 4,
            "version_id": 14,
            "version_no": 1
        }
    ]
}
```

### GET `/api/terms/{term_id}/versions/{version_id}/file`

- 약관 PDF 열람(FR-13). 성공: 200 OK, `Content-Type: application/pdf`.
- 고객은 현재 버전만 열람할 수 있다. 이전 버전 요청은 404로 응답한다. `version_id`가 `term_id`에 속하지 않아도 404다.

### POST `/api/terms/consent`

- 약관 동의 저장. 성공: 201 Created.
- 모든 약관 항목을 한 번에 보낸다. 항목이 빠졌거나, 필수 약관이 `false`이면 400으로 거부한다.
- `version_id`가 현재 버전이 아니면 409로 거부한다. 클라이언트는 목록을 다시 조회해 새 버전을 확인한 뒤 다시 동의한다.
- 항목별 TermsConsent 행을 한 트랜잭션으로 저장한다. 동의 일시는 서버가 정한다.

요청 예시

```json
{
    "consents": [
        { "term_id": 1, "version_id": 11, "agreed": true },
        { "term_id": 2, "version_id": 12, "agreed": true },
        { "term_id": 3, "version_id": 13, "agreed": true },
        { "term_id": 4, "version_id": 14, "agreed": false }
    ]
}
```

응답 예시

```json
{
    "user_id": "customer01",
    "consented_dt": "2026-10-16T09:58:00+09:00",
    "required_consented": true
}
```

- 한도 조회·신청 제출은 모든 필수 약관의 현재 버전에 동의한 고객만 허용하며, 아니면 403과 함께 약관 동의 필요 코드를 반환한다.

### POST `/api/check-limit`

- 한도 조회. 상품을 선택한 뒤 연소득·직업 구분으로 예상 한도와 금리를 계산한다.
- 기존 `income`, `job`은 `annual_income`, `employment_type`으로 변경한다. `employment_type`의 예시 값은 허용 코드 확정 시 입력 화면과 통일한다.
- 아래 금액·금리는 응답 형식 설명용이며 실제 값은 요구사항의 산출식과 상품 최대 금액을 적용한다. 요청값은 모두 필수다.

```json
{
    "product_key": 1,
    "annual_income": 50000000,
    "employment_type": "직장인"
}
```

응답 예시

```json
{
    "product_key": 1,
    "estimated_limit": 30000000,
    "estimated_interest_rate": 4.50
}
```

- 예상 값은 최종 승인 조건이 아니다. 현재 ERD에는 한도 조회 이력 테이블이 없으므로 조회 이력 저장·집계 구조는 별도 설계가 필요하다.

### POST `/api/application`

- 신청서 생성 및 최초 임시저장. 성공: 201 Created.
- `product_key`는 필수다. 나머지 입력값은 생략하거나 NULL로 저장할 수 있다. 전달된 값은 자료형·범위를 검증한다.

```json
{
    "product_key": 1,
    "requested_amount": 10000000,
    "loan_term": 24,
    "annual_income": null,
    "employment_type": null,
    "employment_months": null,
    "existing_debt": null
}
```

응답 예시

```json
{
    "application_id": 1001,
    "user_id": "customer01",
    "product_key": 1,
    "requested_amount": 10000000,
    "loan_term": 24,
    "annual_income": null,
    "employment_type": null,
    "employment_months": null,
    "existing_debt": null,
    "status": "작성중",
    "created_dt": "2026-10-16T10:00:00+09:00",
    "updated_dt": "2026-10-16T10:00:00+09:00",
    "submitted_dt": null,
    "current_review": null
}
```

### POST `/api/application/{application_id}`

- 본인의 작성중 신청 수정 및 임시저장. 성공: 200 OK.
- 수정 가능한 필드는 `product_key`, `requested_amount`, `loan_term`, `annual_income`, `employment_type`, `employment_months`, `existing_debt`이다.
- 생략한 필드는 유지하고, 명시적으로 NULL을 보낸 입력 필드는 비운다. `product_key`는 NULL로 변경할 수 없다.
- 제출 이후 수정 요청은 409로 거부한다.

```json
{
    "annual_income": 50000000,
    "employment_type": "직장인",
    "employment_months": 36,
    "existing_debt": 5000000
}
```

응답 예시

```json
{
    "application_id": 1001,
    "status": "작성중",
    "updated_dt": "2026-10-16T10:05:00+09:00"
}
```

### POST `/api/application/{application_id}/submit`

- 저장된 신청서 제출. 요청 본문은 없다. 최신 입력값은 먼저 `POST /api/application/{application_id}`로 저장한다.
- `Idempotency-Key` 헤더는 필수다. 동일 사용자가 같은 키로 같은 신청을 다시 제출하면 SubmitIdempotency에 저장한 최초 응답을 그대로 반환하며 새 심사 행을 만들지 않는다. 제출에 실패한 응답은 저장하지 않으므로 같은 키로 다시 시도할 수 있다. 같은 키를 다른 신청에 사용하면 409로 거부한다.
- 금액·기간·연소득·직업 구분·재직기간·기존 부채의 필수값, 필수 약관 현재 버전 동의 여부, 상품 상태 `ACTIVE`, 상품 금액·기간 범위와 서버에서 계산한 고객별 한도를 검증한다. 클라이언트의 예상 한도·금리를 제출값으로 신뢰하지 않는다.
- 신청의 `제출완료` 전환, `submitted_dt` 설정, 회차 1의 LoanReview 생성, ApplicationStatusHistory 추가, SubmitIdempotency 응답 저장을 하나의 트랜잭션으로 처리한다.
- 성공: 202 Accepted. 응답은 로컬 제출 접수 결과이며 외부 심사 접수 완료를 의미하지 않는다. 이후 상태는 상세 조회로 확인한다.

요청 헤더 예시

```http
Authorization: Bearer <access_token>
Idempotency-Key: 8ed4ecdb-6068-447d-b47f-c497f048bde9
```

응답 예시

```json
{
    "application_id": 1001,
    "status": "제출완료",
    "submitted_dt": "2026-10-16T10:10:00+09:00",
    "current_review": {
        "review_id": 2001,
        "review_round": 1,
        "status": "요청대기"
    }
}
```

- 외부 접수 성공 시 신청과 심사는 `심사중`이 된다. 전송 실패 시 신청은 `제출완료`, 심사는 `요청대기`로 유지하며 기존 요청을 재전송한다.
- 다른 키로 이미 제출한 신청을 다시 제출하면 409로 거부한다. 현재 범위에서는 재심사 생성 API를 제공하지 않는다.

### GET `/api/application/{application_id}`

- 본인 신청 상세와 현재 심사 조회. 성공: 200 OK.
- `current_review`는 가장 큰 `review_round`의 심사다. 작성중으로 심사가 없으면 NULL이다.
- 심사 결과는 LoanReview에서 조회해 중첩 객체로 반환한다. 내부 `request_payload`, 외부 연동 식별자는 고객 응답에 포함하지 않는다.

응답 예시

```json
{
    "application_id": 1001,
    "user_id": "customer01",
    "product_key": 1,
    "requested_amount": 10000000,
    "loan_term": 24,
    "annual_income": 50000000,
    "employment_type": "직장인",
    "employment_months": 36,
    "existing_debt": 5000000,
    "status": "승인",
    "created_dt": "2026-10-16T10:00:00+09:00",
    "updated_dt": "2026-10-16T10:12:00+09:00",
    "submitted_dt": "2026-10-16T10:10:00+09:00",
    "current_review": {
        "review_id": 2001,
        "review_round": 1,
        "status": "승인",
        "approved_amount": 10000000,
        "approved_interest_rate": 4.50,
        "rejection_reason": null,
        "created_dt": "2026-10-16T10:10:00+09:00",
        "accepted_dt": "2026-10-16T10:10:01+09:00",
        "result_received_dt": "2026-10-16T10:12:00+09:00",
        "updated_dt": "2026-10-16T10:12:00+09:00"
    }
}
```

- `status_history`는 ApplicationStatusHistory를 `changed_dt` 오름차순으로 반환한 진행 이력이다. 위 예시에서는 생략했으며 형식은 아래와 같다.

```json
"status_history": [
    { "from_status": null, "to_status": "작성중", "changed_by_type": "고객", "changed_dt": "2026-10-16T10:00:00+09:00" },
    { "from_status": "작성중", "to_status": "제출완료", "changed_by_type": "고객", "changed_dt": "2026-10-16T10:10:00+09:00" },
    { "from_status": "제출완료", "to_status": "심사중", "changed_by_type": "시스템", "changed_dt": "2026-10-16T10:10:01+09:00" },
    { "from_status": "심사중", "to_status": "승인", "changed_by_type": "외부심사", "changed_dt": "2026-10-16T10:12:00+09:00" }
]
```

- 승인 전 및 거절 시 승인 금액·금리는 NULL이다. 거절 사유는 외부 서버가 제공한 경우 반환한다. 결과 전에는 `result_received_dt`도 NULL이다.

### GET `/api/application/{application_id}/reviews`

- 본인 신청의 회차별 심사 이력 조회. 성공: 200 OK.
- `review_round` 내림차순으로 반환한다. 각 항목은 상세 조회의 `current_review`와 동일한 형식이다. 심사가 없으면 빈 배열을 반환한다.

응답 예시

```json
{
    "application_id": 1001,
    "reviews": [
        {
            "review_id": 2001,
            "review_round": 1,
            "status": "승인",
            "approved_amount": 10000000,
            "approved_interest_rate": 4.50,
            "rejection_reason": null,
            "created_dt": "2026-10-16T10:10:00+09:00",
            "accepted_dt": "2026-10-16T10:10:01+09:00",
            "result_received_dt": "2026-10-16T10:12:00+09:00",
            "updated_dt": "2026-10-16T10:12:00+09:00"
        }
    ]
}
```

- 회차별 심사 이력이며 개별 상태 변경·전송 시도 로그는 아니다. 상태 변경 이력은 상세 조회의 `status_history`로 확인한다. 현재 업무 규칙에서는 신청당 최초 심사 한 건만 생성한다.

## 외부 심사 연동에 반영할 규칙

- 외부 요청 `POST /mock/review-requests`의 `requestId`는 LoanReview.request_id, `applicationId`는 LoanApplication.application_id에 대응한다. 외부 계약의 필드명은 내부 API의 snake_case 규칙과 별개다.
- `POST /api/callback/review-results`는 고객용 API가 아닌 서버 간 콜백이다. 외부 서버 인증 방식과 상세 요청·응답 스키마는 Mock 계약에 맞춰 별도 확정한다.
- 콜백의 `requestId`로 심사를 식별하고 `resultId`를 LoanReview.result_id에 저장한다. 승인 금액·금리·거절 사유는 LoanReview에 반영하며, 현재 회차인 경우 신청 상태도 같은 트랜잭션에서 변경한다.
- 같은 결과의 중복 콜백은 결과·이력·수신 시각을 다시 갱신하지 않는다. 동일 ID의 상충하는 결과나 과거 회차의 콜백으로 현재 결과를 덮어쓰지 않는다.

## ERD 및 연동 계약 확인 필요 사항

- 재직기간·기존 부채는 LoanApplication의 `employment_months`, `existing_debt`로 추가했다. Mock 요청의 재직기간 단위(개월·년)는 Mock 계약 확인 후 변환 여부를 정한다.
- 상태 이력은 ApplicationStatusHistory, 제출 멱등성 응답은 SubmitIdempotency, 약관은 Terms·TermsVersion·TermsConsent로 ERD에 추가했다.
- 한도 조회 이력(KPI·퍼널)과 외부 연동 로그(TR-09)의 저장 구조는 아직 ERD에 없다.
