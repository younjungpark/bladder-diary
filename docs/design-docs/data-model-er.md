# Data Model ER

## 목적

이 문서는 bladder-diary가 현재 사용 중인 주요 테이블 구조를 빠르게 파악하기 위한 ER 다이어그램 문서다.
로컬 Room DB와 원격 Supabase DB를 분리해서 보여준다.

## 범위

- 로컬 DB
  - `voiding_events`
  - `sync_queue`
- 원격 DB
  - `public.voiding_events`
  - `public.user_e2ee_keys`
  - 외부 참조 `auth.users`

## 로컬 DB ER

```mermaid
erDiagram
    VOIDING_EVENTS_LOCAL {
        string local_id PK
        string user_id
        long voided_at_epoch_ms
        string local_date
        boolean is_deleted
        string sync_state
        long updated_at_epoch_ms
        string memo
        int volume_ml
        int urgency
        boolean has_incontinence
        boolean is_nocturia
        string memo_ciphertext
        string memo_encryption
    }

    SYNC_QUEUE {
        string queue_id PK
        string event_local_id FK
        string action
        int retry_count
        string last_error
    }

    VOIDING_EVENTS_LOCAL ||--o{ SYNC_QUEUE : "queued by local_id"
```

## 원격 DB ER

```mermaid
erDiagram
    AUTH_USERS {
        uuid id PK
    }

    VOIDING_EVENTS_REMOTE {
        uuid id PK
        uuid user_id FK
        timestamptz voided_at
        date local_date
        text client_ref UK
        int volume_ml
        int urgency
        boolean has_incontinence
        boolean is_nocturia
        text memo_ciphertext
        text memo_encryption
        timestamptz created_at
        timestamptz deleted_at
    }

    USER_E2EE_KEYS {
        uuid user_id PK,FK
        text kdf
        text kdf_salt
        jsonb kdf_params
        text wrapped_dek
        int key_version
        timestamptz created_at
        timestamptz updated_at
    }

    AUTH_USERS ||--o{ VOIDING_EVENTS_REMOTE : "owns"
    AUTH_USERS ||--o| USER_E2EE_KEYS : "has at most one"
```

## 로컬-원격 매핑 메모

- 로컬 `voiding_events.local_id`는 원격 `public.voiding_events.id`와 같은 이벤트 식별자로 사용된다.
- 원격 `client_ref`도 현재 구현상 로컬 `local_id`와 동일한 값을 담는다.
- 로컬 `sync_queue.event_local_id`는 로컬 `voiding_events.local_id`를 참조하는 논리적 FK다.
- 로컬 `memo`는 앱에서 사용하는 평문 또는 복호화 결과 캐시 성격이고, 원격에는 `memo_ciphertext`와 `memo_encryption`이 저장된다.
- 원격 `user_e2ee_keys`는 사용자별 E2EE 메타데이터와 wrapped DEK를 1건만 유지한다.

## 참고 기준 파일

- `app/src/main/java/com/bladderdiary/app/data/local/AppDatabase.kt`
- `app/src/main/java/com/bladderdiary/app/data/local/VoidingEventEntity.kt`
- `app/src/main/java/com/bladderdiary/app/data/local/SyncQueueEntity.kt`
- `app/src/main/java/com/bladderdiary/app/data/remote/dto/VoidingEventRemoteDto.kt`
- `app/src/main/java/com/bladderdiary/app/data/remote/dto/UserE2eeKeyRemoteDto.kt`
- `supabase/sql/001_init.sql`
- `supabase/sql/002_e2ee_memo.sql`
- `supabase/sql/003_add_volume_ml.sql`
- `supabase/sql/004_add_urgency.sql`
- `supabase/sql/005_add_has_incontinence.sql`
- `supabase/sql/006_add_is_nocturia.sql`
