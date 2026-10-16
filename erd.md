# 엔터티와 필드

## 1. User — 사용자

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| user_id | PK | VARCHAR2 | 50 CHAR | 사용자 및 운영자 계정 식별자. 로그인 ID로 사용하며 변경하지 않음 |
| password | 비밀번호 | VARCHAR2 | 60 BYTE | NOT NULL. BCrypt 비밀번호 해시 저장 |
| role | 계정 권한 | VARCHAR2 | 20 CHAR | NOT NULL. `사용자`, `운영자` 구분 |

- `role`은 CHECK 제약으로 `사용자`, `운영자`만 허용한다.
- JWT access token은 로그인 성공 시 발급하며 사용자 테이블에 저장하지 않는다.
- 미리 제공된 계정으로 로그인하는 범위이므로 이름·연락처·회원가입 관련 필드는 포함하지 않는다.

## 2. Product — 대출 상품

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| product_key | PK | INTEGER | | 대출 상품 식별자 |
| product_name | 상품명 | VARCHAR2 | 100 CHAR | NOT NULL. 사용자에게 표시할 대출 상품 이름 |
| product_description | 상품 설명 | VARCHAR2 | 1000 CHAR | 사용자에게 표시할 대출 상품 설명. NULL 허용 |
| min_loan_amount | 최소 대출 금액 | NUMBER(15,0) | | NOT NULL. 상품에서 허용하는 최소 신청 금액, 원 단위 |
| max_loan_amount | 최대 대출 금액 | NUMBER(15,0) | | NOT NULL. 상품에서 허용하는 최대 신청 금액, 원 단위. 고객별 예상 한도와 구분 |
| min_loan_term | 최소 대출 기간(월) | NUMBER(3,0) | | NOT NULL. 신청 가능한 최소 기간, 개월 단위 |
| max_loan_term | 최대 대출 기간(월) | NUMBER(3,0) | | NOT NULL. 신청 가능한 최대 기간, 개월 단위 |
| status | 상품 상태 | VARCHAR2 | 20 CHAR | NOT NULL. `ACTIVE`(신청 가능), `SUSPENDED`(일시 중단), `DISCONTINUED`(판매 종료) |
| created_dt | 생성일 | TIMESTAMP | | NOT NULL. 기본값 CURRENT_TIMESTAMP. 상품 등록 시 설정 |
| updated_dt | 갱신일 | TIMESTAMP | | NOT NULL. 기본값 CURRENT_TIMESTAMP. 상품 수정 시 애플리케이션에서 갱신 |

- 금액과 기간은 CHECK 제약으로 양수만 허용하고, 각각 최소값이 최대값 이하가 되도록 한다.
- `status`는 CHECK 제약으로 위 세 값만 허용한다. 신규 신청 제출 시 상품 상태가 `ACTIVE`인지 확인한다.
- 기간은 최소~최대 범위 내 정수 개월을 허용하는 전제다. 12·24·36개월처럼 특정 기간만 허용한다면 별도 상품별 허용 기간 테이블이 필요하다.
- 예상 금리는 요구사항의 산출식으로 계산하므로 상품별 기준 금리 필드는 현재 포함하지 않는다. 고객별 예상 한도·금리와 최종 승인 조건은 별도 조회·신청·심사 데이터로 보존한다.
- 과거 신청의 상품 참조를 유지하기 위해 판매 종료 상품은 삭제 대신 `DISCONTINUED`로 관리한다.

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
