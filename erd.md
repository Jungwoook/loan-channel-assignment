# 엔터티와 필드

## 1. User — 사용자

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| user_id | PK | VARCHAR2 | | 사용자 및 운영자 계정 식별자. 로그인 ID로도 사용 |
| password | 비밀번호 | VARCHAR2 | 60 BYTE | BCrypt 비밀번호 해시 저장 |
| type | 계정 유형 | VARCHAR2 | | `사용자`, `운영자` 구분 |
| token | 인증 토큰 | | | 실제 DB 저장 여부와 관리 방식은 추후 확정 |

## 2. Product — 대출 상품

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| product_key | PK | INTEGER | | 대출 상품 식별자 |
| product_name | 상품명 | VARCHAR2 | | 사용자에게 표시할 대출 상품 이름 |

## 3. LoanApplication — 대출 신청 및 심사 상태

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| application_id | PK | INTEGER | | 개별 대출 신청 식별자 |
| user_id | FK → User.user_id | VARCHAR2 | | 신청한 사용자. 길이는 User.user_id와 동일하게 지정 |
| product_key | FK → Product.product_key | INTEGER | | 신청 대상 대출 상품. Product.product_key와 동일한 형식 |
| status | 신청 상태 | VARCHAR2 | | `작성중`, `승인 대기`, `승인`, `거절`, `취소` |
| s_date | 시작일 | TIMESTAMP | | 정보 입력 시작 시 생성. 이후 수정 시 변경하지 않음 |
| a_date | 신청일 | TIMESTAMP | | 작성 완료 후 신청 제출 시 생성. 제출 전에는 NULL |
| e_date | 종료일 | TIMESTAMP | | 심사 결과(승인·거절)가 나오거나 제출 이후 취소한 경우 생성. 해당 사건 발생 전에는 NULL |
