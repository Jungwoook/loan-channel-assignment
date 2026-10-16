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

## 3. LoanApplication — 대출 신청

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| application_id | PK | INTEGER | | 개별 대출 신청 식별자 |
| user_id | FK → User.user_id | VARCHAR2 | 50 CHAR | NOT NULL. 신청한 사용자 |
| product_key | FK → Product.product_key | INTEGER | | NOT NULL. 신청 대상 대출 상품 |
| requested_amount | 신청 금액 | NUMBER(15,0) | | 고객이 신청한 금액, 원 단위 |
| loan_term | 신청 기간 | NUMBER(3,0) | | 고객이 선택한 대출 기간, 개월 단위 |
| annual_income | 연소득 | NUMBER(15,0) | | 신청 당시 연소득, 원 단위 |
| employment_type | 직업 구분 | VARCHAR2 | 30 CHAR | 신청 당시 직업 구분. 허용값은 입력 화면과 통일 |
| status | 신청 상태 | VARCHAR2 | 20 CHAR | NOT NULL. 기본값 `작성중`. `작성중`, `제출완료`, `심사중`, `승인`, `거절` |
| created_dt | 생성일 | TIMESTAMP | | NOT NULL. 기본값 CURRENT_TIMESTAMP. 신청 생성 시 설정하며 변경하지 않음 |
| updated_dt | 갱신일 | TIMESTAMP | | NOT NULL. 기본값 CURRENT_TIMESTAMP. 임시저장 및 상태 변경 시 갱신 |
| submitted_dt | 최초 제출일 | TIMESTAMP | | 최초 제출 성공 시 설정. 제출 전에는 NULL, 이후 변경하지 않음 |

- 신청 입력값은 임시저장을 위해 NULL을 허용하고, 제출 시 필수값과 상품 조건을 검증한다. 제출 이후 고객의 신청 내용 수정은 허용하지 않는다.
- 금액·기간은 값이 있을 때 양수, 연소득·재직기간·기존 부채는 0 이상이 되도록 CHECK 제약을 둔다. 직업별 재직기간 필수 여부는 제출 검증 정책으로 정한다.
- `status`는 CHECK 제약으로 위 다섯 값만 허용한다. 현재 업무 흐름은 `작성중 → 제출완료 → 심사중 → 승인 / 거절`이다. 취소 기능은 현재 요구사항에 포함되지 않아 제외한다.
- 기존 `s_date`, `a_date`는 각각 `created_dt`, `submitted_dt`로 명확히 이름을 변경한다. 기존 `e_date`의 결과 수신 시각은 아래 심사 테이블에서 회차별로 관리한다.
- 승인 금액·금리·거절 사유는 LoanReview에만 저장한다. LoanApplication.status는 목록 조회를 위한 현재 업무 상태이며, 현재 회차의 심사 결과와 같은 트랜잭션에서 갱신한다.

## 4. LoanReview — 심사 요청 및 결과 이력

신청과 심사는 `LoanApplication 1 : N LoanReview` 관계다. 한 행은 한 번의 논리적 심사 회차를 의미하며, 요청 생성부터 접수·결과 수신까지 같은 행을 갱신한다. 새 심사를 시작하면 새 행을 추가하고 완료된 과거 회차는 보존한다.

| 필드 | 역할 | 데이터 형식 (Oracle) | 길이 | 설명 |
| --- | --- | --- | --- | --- |
| review_id | PK | INTEGER | | 내부 심사 식별자 |
| application_id | FK → LoanApplication.application_id | INTEGER | | NOT NULL. 심사 대상 신청 |
| review_round | 심사 회차 | NUMBER(5,0) | | NOT NULL. 신청별 1부터 증가하는 회차 |
| request_id | 외부 심사 요청 식별자 | VARCHAR2 | 100 CHAR | NOT NULL, UNIQUE. Mock 서버의 requestId. 같은 심사 재전송 시 유지 |
| result_id | 외부 심사 결과 식별자 | VARCHAR2 | 100 CHAR | UNIQUE. Mock 서버의 resultId. 결과 수신 전에는 NULL |
| status | 심사 상태 | VARCHAR2 | 20 CHAR | NOT NULL. 기본값 `요청대기`. `요청대기`, `심사중`, `승인`, `거절` |
| request_payload | 요청 당시 데이터 | CLOB | | NOT NULL. 실제 심사 요청에 사용한 금액·기간·소득·재직기간·부채 등을 JSON으로 보존. 재전송 시 동일 데이터 사용 |
| approved_amount | 승인 금액 | NUMBER(15,0) | | 승인 시 필수, 양수. 그 외에는 NULL |
| approved_interest_rate | 승인 금리 | NUMBER(5,2) | | 승인 시 필수, 0 이상. 연이율 % 단위이며 4.50은 연 4.50%. 그 외에는 NULL |
| rejection_reason | 거절 사유 | VARCHAR2 | 1000 CHAR | 거절 시 외부 서버가 제공한 사유. 그 외에는 NULL |
| created_dt | 심사 요청 생성일 | TIMESTAMP | | NOT NULL. 기본값 CURRENT_TIMESTAMP. 전송할 요청을 DB에 생성한 시각 |
| accepted_dt | 심사 접수일 | TIMESTAMP | | 외부 서버의 정상 접수를 최초 확인한 시각. 접수 전에는 NULL |
| result_received_dt | 결과 수신일 | TIMESTAMP | | 유효한 최종 결과를 최초 수신한 시각. 결과 수신 전에는 NULL |
| updated_dt | 갱신일 | TIMESTAMP | | NOT NULL. 기본값 CURRENT_TIMESTAMP. 접수·결과 반영 시 갱신 |

- `(application_id, review_round)`에 복합 UNIQUE 제약을 두고, `review_round > 0` 및 허용 상태·결과 필드 조건에 CHECK 제약을 둔다. 외부 식별자의 길이는 Mock API 계약 확정 시 맞춘다.
- 작성중 신청은 심사 행이 없다. 제출 시 신청 상태 변경과 최초 심사 행 생성을 같은 트랜잭션으로 처리한다. 전송 실패 시 신청은 `제출완료`, 심사는 `요청대기`로 유지한다.
- 현재 심사는 해당 신청의 가장 큰 `review_round`로 정의한다. 회차 생성 시 신청 행 잠금 등으로 동시 생성을 직렬화하고, 진행 중인 심사가 있으면 새 회차를 생성하지 않는다.
- 재전송은 새 심사가 아니므로 기존 `review_id`, `review_round`, `request_id`, 요청 데이터를 유지한다. 개별 전송 시도와 응답 로그는 이 테이블의 새 행으로 쌓지 않고 별도 연동 로그로 관리한다.
- 콜백은 `request_id`로 심사를 찾고 `result_id`로 중복을 확인한다. 이미 반영한 결과는 다시 반영하지 않으며 최초 결과 수신 시각을 유지한다. 동일 ID의 상충하는 결과는 덮어쓰지 않는다.
- 심사 결과 저장과 현재 회차에 대한 신청 상태 변경은 같은 트랜잭션으로 처리한다. 과거 회차의 지연·중복 콜백으로 현재 신청 상태를 변경하지 않는다.
- 과거 심사 행과 요청 데이터·완료 결과는 보존한다. 이 테이블은 회차별 이력이며, 각 회차 안의 모든 상태 변경·송수신 사건을 기록하는 로그는 아니다. 전체 진행 이력은 별도 상태 이력 테이블이 필요하다.
- 현재 docs.md는 승인·거절을 최종 상태로 정의하므로 현 기능에서는 신청당 최초 심사 한 건만 생성한다. 1:N 구조는 이후 재심사 이력 보존을 지원하기 위한 설계다. 재심사를 실제 허용하려면 시작 권한·조건, 신청 상태 전이, 현재 회차 선택 정책을 함께 확정해야 한다.
