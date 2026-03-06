# 선택적 종단간 암호화(E2EE) 설계 문서 (memo 민감 정보 보호)

## 1. 문서 목적
- `voiding_events.memo`와 같은 민감 텍스트를 **클라이언트에서 암호화 후 서버에 저장**하여, 서버 관리자/DB 접근자가 원문을 볼 수 없도록 합니다.
- 기존 앱의 핵심 가치(오프라인 기록, 자동 동기화, 일일 횟수 통계)는 유지합니다.
- 본 문서는 **설계 기준 문서**이며, 실제 구현은 단계적으로 진행합니다.

## 2. 요구사항 요약
- 선택적으로 E2EE를 켜고 끌 수 있어야 합니다(사용자별 설정).
- E2EE 활성화 시 memo 원문은 네트워크/서버에 저장되지 않아야 합니다.
- 서버에는 일시(`voided_at`), 날짜(`local_date`), 삭제 여부(`deleted_at`) 등 통계용 메타데이터는 평문으로 유지합니다.
- 로그인 토큰 만료/재시도/오프라인 큐 기반 동기화 흐름(`VoidingRepositoryImpl`)과 충돌 없이 동작해야 합니다.

## 3. 범위
### 포함
- memo 필드 암호화/복호화
- 키 생성/보관/복구(멀티 디바이스 지원)
- Supabase/Room 데이터 모델 변경
- 동기화 파이프라인 수정

### 제외 (후속)
- 첨부 이미지/파일 E2EE
- 로컬 DB 전체 암호화(SQLCipher 등)
- 검색 인덱스 암호화(서버 검색 기능 자체를 제한)

## 4. 위협 모델과 보안 목표
- 보호 대상: memo 원문, 사용자 증상/생활 습관 등 민감 메모
- 공격자 가정:
  - Supabase DB 조회 권한을 얻은 내부자/외부 공격자
  - 네트워크 패킷 열람자(TLS 외 추가 보호)
- 보안 목표:
  - 서버 저장 데이터만으로 memo 원문 복원 불가
  - 암호문 위변조 감지 가능(AEAD)
- 비목표:
  - 사용자 단말이 완전히 탈취된 경우(루팅/메모리 덤프)까지 완전 방어

## 5. 핵심 설계
## 5.1 암호화 스킴 (E2EE_V1)
- 알고리즘: `AES-256-GCM`
- 단위: memo 1건당 독립 암호화
- 랜덤 nonce: 12 bytes (레코드마다 새로 생성)
- AAD(인증 추가 데이터): `user_id + event_id + local_date` 직렬화 문자열
- 직렬화 포맷(JSON 후 Base64URL):

```json
{
  "v": 1,
  "alg": "A256GCM",
  "kid": "user-key-v1",
  "nonce": "...",
  "ct": "...",
  "tag": "..."
}
```

## 5.2 키 계층
- DEK(Data Encryption Key): memo 암복호화 실키(256bit, 사용자별 1개)
- KEK(Key Encryption Key): 사용자가 입력한 E2EE 비밀문구(passphrase)에서 파생
  - KDF 권장: `Argon2id` (초기 구현 난이도 시 `PBKDF2-HMAC-SHA256` 임시 허용)
- 서버 저장: `wrapped_dek`만 저장(평문 DEK 금지)
- 로컬 캐시: Android Keystore 키로 DEK를 재래핑해 단말 내 안전 저장(재입력 빈도 완화)

## 5.3 선택적 활성화 UX
- 기본값: 비활성화
- 활성화 시:
  - 사용자에게 복구 문구/주의사항 명시(분실 시 복호화 불가)
  - 기존 로컬 memo를 백그라운드로 재암호화 후 업로드
- 비활성화 시:
  - 정책 옵션 A(권장): 기존 암호문 유지 + 새 메모만 평문 저장 금지(보안 일관성)
  - 정책 옵션 B: 재평문화 업로드 허용(명시적 경고 필요)
- 본 프로젝트 권장: **옵션 A**

## 6. 데이터 모델 변경
## 6.1 Supabase
### `voiding_events` 테이블
- `memo_ciphertext text null` 추가
- `memo_encryption text not null default 'NONE'` 추가 (`NONE`, `E2EE_V1`)
- `memo_ciphertext`는 `memo_encryption` 값에 따라 평문(`NONE`) 또는 암호문(`E2EE_V1`)을 저장
- 현재는 단독 테스트 단계이므로 기존 `memo` 컬럼은 이번 마이그레이션에서 바로 제거

### 신규 `user_e2ee_keys` 테이블
- 목적: 사용자별 키 메타데이터/래핑된 DEK 저장
- 컬럼(예시):
  - `user_id uuid primary key`
  - `kdf text not null` (`ARGON2ID`, `PBKDF2_SHA256`)
  - `kdf_salt text not null`
  - `kdf_params jsonb not null`
  - `wrapped_dek text not null`
  - `key_version int not null default 1`
  - `created_at timestamptz default now()`
  - `updated_at timestamptz default now()`
- RLS: `auth.uid() = user_id`로 select/insert/update 허용

## 6.2 Android Room
- `VoidingEventEntity` 변경:
  - `memo: String?` 유지 (UI 표시용 평문 캐시)
  - `memoCiphertext: String?` 추가 (서버 저장 payload 캐시)
  - `memoEncryption: String` 추가 (기본 `NONE`)
- `AppDatabase` 버전: `2 -> 3`
- 마이그레이션:
  - `ALTER TABLE voiding_events ADD COLUMN memo_ciphertext TEXT`
  - `ALTER TABLE voiding_events ADD COLUMN memo_encryption TEXT NOT NULL DEFAULT 'NONE'`
  - 서버 스키마에서는 `memo` 컬럼 제거 반영

## 7. 동기화/도메인 흐름 변경
## 7.1 업로드(`syncCreate`)
- E2EE 비활성:
  - `memo` 평문을 `memo_ciphertext`로 전송
  - `memo_encryption='NONE'`
- E2EE 활성:
  - `memo` 평문 -> 암호화 -> `memo_ciphertext` 전송
  - `memo_encryption='E2EE_V1'`

## 7.2 다운로드(`fetchAndSyncAll`)
- `memo_encryption='NONE'`이고 `memo_ciphertext` 존재 시 평문으로 간주하고 Room `memo`에 캐시
- `memo_encryption='E2EE_V1'`이고 `memo_ciphertext` 존재 시 복호화 시도
- 복호화 성공: Room `memo`에 평문 캐시
- 복호화 실패(키 잠김/문구 미입력/손상):
  - `memo`는 `null` 또는 플레이스홀더 저장
  - UI에 "복호화 필요" 상태 표시

## 7.3 메모 수정(`updateMemo`)
- 로컬에서는 평문 편집 유지
- 업로드 직전 암호화하는 현재 큐 구조를 유지하여 오프라인 UX 보존

## 8. 실패/예외 처리
- passphrase 오류: 재시도 허용, 지수 백오프 없음(로컬 동작)
- 복호화 실패율이 일정 횟수 초과 시 손상 경고 이벤트 로깅
- JWT 만료 재시도 로직(`syncWithAutoRefresh`)은 그대로 사용
- 암호화 모듈 예외는 동기화 실패로 큐에 남겨 재시도

## 9. 단계별 구현 계획
1. 서버 스키마 마이그레이션 SQL 추가 (`supabase/sql/002_e2ee_memo.sql`)
2. Room v3 마이그레이션 + 엔티티/DTO 필드 확장
3. `data/security`에 `MemoCrypto` 모듈 추가(AES-GCM, 직렬화)
4. E2EE 키 저장소(`E2eeKeyStore`) 추가
5. `VoidingRepositoryImpl` 업/다운로드 경로에 암복호화 연결
6. UI 상태 추가(활성화, 잠김, 복호화 필요)
7. 기존 메모 데이터 재암호화 워커(1회성)
8. 통합 테스트 및 릴리스 체크리스트 반영

## 10. 테스트 전략
- 단위 테스트:
  - 암복호화 왕복 일치
  - AAD 불일치 시 복호화 실패
  - 잘못된 nonce/tag 손상 검출
- 저장소 테스트:
  - E2EE ON/OFF별 업로드 payload 검증
  - 다운로드 후 복호화/플레이스홀더 처리 검증
- 마이그레이션 테스트:
  - Room v2 -> v3 데이터 보존
  - Supabase에서 `memo` 제거 후 `memo_ciphertext`/`memo_encryption` 조합 해석 검증
- E2E 테스트:
  - 오프라인 작성 -> 온라인 동기화 -> 재설치/타기기 복호화

## 11. 운영/정책 반영 포인트
- 개인정보 처리방침에 "클라이언트 측 암호화" 및 "복구 문구 분실 시 복구 불가" 명시
- Play Console Data Safety 작성 시:
  - 건강 데이터 처리
  - 전송 중 암호화 + 저장 전 클라이언트 암호화(선택 기능)

## 12. 오픈 이슈
- KDF를 Argon2id로 갈지, PBKDF2로 1차 릴리스할지 결정 필요
- "E2EE 비활성화 시 재평문화 허용 여부" 정책 확정 필요
- 복구 UX(문구 백업, 분실 경고, 키 회전 주기) 상세안 확정 필요
