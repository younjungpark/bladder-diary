# Google Drive Backup Restore Design

## 목적

이 문서는 BLA-2 Google Drive 검토의 1단계 설계 결정을 고정한다. 기능 이름은 논의 중 `구글 드라이브 동기화`로 시작했지만, 실제 제품 범위는 Google Drive 기반 암호화 백업/복원이다. Supabase는 여러 기기 동기화가 필요한 사용자를 위한 선택 기능으로 유지하고, Google Drive는 재설치와 기기 변경에 대비한 사용자 소유 백업 저장소로만 사용한다.

## 배경

현재 앱은 로컬 우선 저장 구조이며, Supabase 클라우드 동기화는 기본적으로 꺼져 있다. 사용자가 동기화를 켠 경우에만 WorkManager 기반 업로드, 원격 다운로드, 병합 흐름이 동작한다. Supabase에 저장되는 기록은 날짜와 식별자, 삭제/동기화 메타데이터를 제외한 기록 본문을 `E2EE_RECORD_V1` 페이로드로 암호화한다.

Google Drive 백업은 이 동기화 모델을 대체하지 않는다. 사용자가 체감하는 주요 가치는 여러 기기 실시간 정합성보다 앱 삭제, 재설치, 기기 변경 후 로컬 기록을 되살릴 수 있다는 점이다. 따라서 sync queue, Supabase RLS, 원격 soft delete 전파 같은 동기화 문제를 Google Drive 쪽으로 확장하지 않는다.

## 결정 요약

| 항목 | 결정 |
| --- | --- |
| 기능 범위 | Google Drive 암호화 백업/복원 |
| 비목표 | Google Drive 기반 실시간/양방향 동기화 |
| Drive 저장 위치 | Google Drive API `appDataFolder` |
| Drive 권한 | `https://www.googleapis.com/auth/drive.appdata` |
| Android 권한 흐름 | Google Identity Services `AuthorizationClient` |
| 백업 단위 | 앱 전용 숨김 폴더의 최신 스냅샷 파일 1개 |
| 백업 암호화 | 백업 전용 비밀번호에서 KEK를 파생하고, 백업 DEK를 감싸는 방식 |
| 자동 백업 | 로컬에 보관된 백업 DEK가 있을 때만 WorkManager로 수행 |
| 복원 방식 | 기본은 병합, 사용자가 명시하면 교체 |
| fallback | 수동 암호화 백업 파일 내보내기/가져오기 |

## 근거

Google Drive `appDataFolder`는 앱 전용 숨김 폴더이며, 사용자가 직접 조작할 필요가 없는 앱 데이터를 저장하는 용도에 맞다. 이 폴더에 접근하려면 `drive.appdata` scope가 필요하고, 이 scope는 Drive API scope 표에서 recommended, non-sensitive로 분류된다. Android에서는 Google API 데이터 접근을 인증과 분리된 authorization flow로 처리하고, 사용자가 해당 기능을 실행할 때 필요한 scope만 요청하는 흐름이 권장된다. `AuthorizationClient`는 기존 grant가 있으면 token을 반환하고, 동의가 필요하면 resolution pending intent를 제공한다.

이 설계에서 Drive 앱 설치 여부는 요구 조건으로 두지 않는다. 앱은 Drive 앱을 호출하지 않고 Google Play services 기반 authorization과 Drive REST API를 사용한다. 다만 Google 계정, Google Play services 사용 가능 상태, 네트워크, 사용자 권한 동의는 필요하다.

## 범위와 비목표

### 범위

- Google 계정 권한 동의 후 `appDataFolder`에 암호화 백업을 저장한다.
- 현재 로컬 Room 기록과 삭제 상태를 스냅샷으로 내보낸다.
- 새 기기나 재설치 후 Drive 백업 파일을 다운로드하고, 백업 비밀번호로 복호화해 로컬 DB에 복원한다.
- 사용자가 Google Drive를 사용할 수 없거나 권한을 거부한 경우 수동 암호화 백업 파일 경로로 안내한다.

### 비목표

- Google Drive 변경 감지
- 여러 기기 동시 수정 충돌 해결
- record 단위 Drive sync log
- Drive 파일을 사용한 삭제 이벤트 양방향 전파
- Drive 전체 파일 접근 권한 요청
- Google Drive 앱 UI에서 백업 파일을 직접 관리하는 UX

## 계정과 권한 설계

Google Drive 백업 계정은 Supabase 로그인 계정과 독립적으로 취급한다. 현재 앱의 사용자 데이터는 `user_id`를 기준으로 로컬에 저장되므로, 백업 payload에도 앱 내부 `userId`와 record `localId`를 포함한다. Drive 계정은 백업 저장 위치를 제공할 뿐이며, 앱 사용자 식별자나 Supabase user id를 대체하지 않는다.

MVP에서는 Drive account email을 표시하지 않는다. `drive.appdata`만 요청하면 권한 응답에서 선택된 계정의 email을 직접 알 수 없고, email 표시를 위해 `openid`, `profile`, `userinfo` 계열 scope를 추가하면 최소 권한 원칙이 흐려진다. 화면은 `Google Drive 백업 연결됨`과 마지막 백업 시각을 표시하고, 특정 Google 계정명을 표시하는 것은 후속 검토로 둔다.

권한 요청은 설정 화면에서 사용자가 `Google Drive 백업 켜기`, `지금 백업`, `Google Drive에서 복원` 같은 명시적 행동을 할 때 시작한다. 앱 시작 시점이나 로그인 직후에 Drive 권한을 선요청하지 않는다.

## 백업 암호화 키 정책

Google Drive 백업은 Supabase E2EE 비밀문구와 독립된 백업 전용 비밀번호를 사용한다. 이유는 다음과 같다.

- Google Drive 백업은 local-only 사용자도 사용할 수 있어야 한다.
- Supabase E2EE 키 상태나 원격 `user_e2ee_keys`에 복원을 의존하면 Google Drive 백업의 독립성이 약해진다.
- 재설치 후 로컬 Android Keystore 값은 사라질 수 있으므로, 백업 파일 자체에 복원 가능한 wrapped backup key가 필요하다.

키 구조는 현재 `MemoCrypto`의 패턴을 재사용한다.

1. 사용자가 백업 비밀번호를 설정한다.
2. 앱은 PBKDF2-HMAC-SHA256으로 KEK를 파생한다.
3. 앱은 무작위 256-bit backup DEK를 생성한다.
4. 백업 파일에는 `wrappedBackupDek`, KDF salt, KDF parameters를 저장한다.
5. 실제 기록 payload는 backup DEK로 AES-GCM 암호화한다.
6. 기기 로컬에는 자동 백업을 위해 backup DEK를 Android Keystore로 감싼 뒤 DataStore에 저장한다.

사용자가 백업 비밀번호를 잊으면 새 기기나 재설치 환경에서 백업을 복원할 수 없다. 기존 기기에 로컬 backup DEK가 남아 있으면 비밀번호 변경이나 새 백업 생성은 가능하지만, 이 동작은 2단계 구현에서 명확히 검증해야 한다.

## 백업 파일 포맷

Drive `appDataFolder`에는 최신 백업 스냅샷 파일 1개를 둔다.

```text
bladderdiary-backup-v1.json
```

파일은 하나의 JSON envelope로 구성한다. manifest와 encrypted payload를 분리된 Drive 파일로 두지 않는 이유는 두 파일 사이의 불일치와 부분 업데이트 상태를 피하기 위해서다.

```json
{
  "type": "com.chausoft.bladderdiary.backup",
  "backupVersion": 1,
  "payloadId": "random-uuid",
  "createdAtEpochMs": 1770000000000,
  "appVersionName": "1.0.4",
  "appVersionCode": 14,
  "databaseVersion": 8,
  "encryption": {
    "scheme": "BACKUP_AES_GCM_V1",
    "kdf": "PBKDF2WithHmacSHA256",
    "kdfSalt": "...",
    "kdfParams": {
      "iterations": 210000,
      "keyLengthBits": 256
    },
    "wrappedBackupDek": {
      "nonce": "...",
      "ciphertext": "...",
      "tag": "..."
    }
  },
  "payload": {
    "nonce": "...",
    "ciphertext": "...",
    "tag": "..."
  }
}
```

Manifest에는 백업 식별과 복호화 준비에 필요한 최소 정보만 둔다. `payloadId`는 AAD 구성과 파일 식별을 위한 무작위 값이며, 사용자 기록 의미를 담지 않는다. record count, 날짜 범위, 배뇨량 통계처럼 사용 패턴을 드러낼 수 있는 값은 encrypted payload 안에 둔다. 복원 미리보기는 복호화 성공 후 보여준다.

encrypted payload의 plain model은 아래 개념을 따른다. 백업 대상은 현재 앱 사용자 1명으로 제한한다. 같은 기기에 과거 계정의 잔여 데이터가 있더라도 다른 `userId`의 기록을 한 백업 파일에 섞지 않는다.

```text
BackupPayloadV1
- exportedAtEpochMs
- sourceAppVersionName
- sourceAppVersionCode
- sourceDatabaseVersion
- userId
- records[]
  - localId
  - voidedAtEpochMs
  - localDate
  - isDeleted
  - updatedAtEpochMs
  - memo
  - volumeMl
  - urgency
  - hasIncontinence
  - isNocturia
```

`syncState`, Supabase access token, refresh token, PIN, 로컬 E2EE runtime key, WorkManager 상태는 백업 payload에 포함하지 않는다. 복원된 기록은 로컬 데이터로 취급하고, Supabase 동기화가 켜져 있을 때만 별도 정책에 따라 업로드 대상으로 requeue할지 결정한다. 이 requeue 정책은 2단계 구현에서 안전하게 다룬다.

## 백업 흐름

1. 사용자가 `Google Drive 백업 켜기`를 누른다.
2. 앱은 `drive.appdata` 권한을 요청한다.
3. 사용자가 동의하면 백업 비밀번호 설정 또는 기존 로컬 backup DEK 확인을 진행한다.
4. Room에서 현재 사용자 기록을 export한다.
5. 앱은 payload를 backup DEK로 암호화한다.
6. Drive `appDataFolder`에서 기존 `bladderdiary-backup-v1.json`을 찾고, 있으면 업데이트하고 없으면 생성한다.
7. 마지막 백업 시각과 성공/실패 상태를 로컬 DataStore에 저장한다.

자동 백업은 사용자가 백업을 켠 뒤 기록이 추가, 수정, 삭제될 때 예약한다. 자동 백업은 Supabase sync queue와 별도 WorkManager 작업명으로 관리한다. 네트워크 연결이 없거나 Drive 권한이 없거나 로컬 backup DEK가 없으면 백업을 수행하지 않고 재시도 가능한 상태로 남긴다.

## 복원 흐름

1. 사용자가 `Google Drive에서 복원`을 누른다.
2. 앱은 `drive.appdata` 권한을 확인하거나 요청한다.
3. Drive `appDataFolder`에서 최신 백업 파일을 다운로드한다.
4. 사용자는 백업 비밀번호를 입력한다.
5. 앱은 `wrappedBackupDek`를 풀고 payload를 복호화한다.
6. 복호화 후 백업 생성 시각, 앱 버전, 기록 수 같은 미리보기 정보를 표시한다.
7. 로컬 기록이 없으면 바로 복원한다.
8. 로컬 기록이 있으면 `백업과 합치기`, `백업으로 교체`, `취소`를 제공한다.

기본 복원 정책은 `백업과 합치기`다. 병합 기준은 `localId`이며, 동일 record가 있으면 `updatedAtEpochMs`가 더 최신인 쪽을 사용한다. `isDeleted = true` record는 tombstone으로 취급한다. `백업으로 교체`는 현재 사용자 로컬 기록을 transaction 안에서 백업 스냅샷으로 교체한다.

복원은 반드시 transaction으로 처리한다. 중간에 복호화, schema validation, DB write가 실패하면 기존 로컬 데이터가 깨지지 않아야 한다.

## 수동 파일 fallback

Google Drive를 사용할 수 없는 경우에도 같은 backup envelope를 `.bdbackup` 파일로 내보내고 가져올 수 있게 한다. 이 경로는 Android Storage Access Framework 기반으로 구현한다. 수동 파일도 동일한 백업 비밀번호, 동일한 payload 포맷, 동일한 복원 정책을 사용한다.

fallback이 필요한 대표 상황은 다음과 같다.

- Google 계정이 없음
- Google Play services 사용 불가
- Drive 권한 거부
- 네트워크 장기 실패
- 회사/학교 계정 정책으로 Drive API 권한 차단

## Supabase 동기화와의 경계

Google Drive 백업은 Supabase 동기화 상태를 켜거나 끄지 않는다. Supabase 동기화가 꺼진 사용자는 계속 로컬 우선 + Drive 백업으로 사용할 수 있다. Supabase 동기화가 켜진 사용자는 기존처럼 sync queue와 WorkManager 기반 동기화를 유지하되, Drive 백업은 별도 스냅샷으로 동작한다.

복원 후 Supabase 동기화가 켜져 있는 경우에도 즉시 원격 merge를 수행하지 않는다. BLA-4 엔진 구현에서는 복원된 백업 기록을 `SYNCED` 상태의 로컬 데이터로 반영하고 새 `sync_queue` 항목을 만들지 않는다. 백업 기록으로 대체되는 localId의 기존 queue 항목은 stale upload를 피하기 위해 제거한다. 더 최신인 로컬 pending 기록은 병합 복원에서 유지하며, 원격 업로드나 병합은 기존 `fetchAndSyncAll()` 또는 명시적인 requeue 흐름으로 분리한다.

## 2단계 구현 지침

- 새 패키지는 `data/backup` 또는 `data/drive` 하위로 분리한다.
- Drive 권한, Drive REST 호출, 백업 암호화, Room export/import를 작은 interface로 나누고 테스트에서는 fake를 사용한다.
- 기존 `MemoCrypto`의 AES-GCM envelope와 PBKDF2 기본값을 재사용하되, backup 전용 scheme 이름과 AAD를 둔다.
- backup AAD에는 app id, backup version, user id 또는 payload id를 포함해 다른 용도의 ciphertext와 혼동되지 않게 한다.
- DataStore에는 현재 앱 사용자별 Drive 연결 상태, 자동 백업 여부, 마지막 성공/실패 시각, 로컬 wrapped backup DEK만 저장한다.
- Drive access token은 장기 저장하지 않는다. 필요할 때 `AuthorizationClient.authorize()`로 짧은 수명의 token을 받아 사용하고, invalid token 오류가 나면 token cache를 clear한 뒤 재요청한다.
- 백업 payload validation은 DB write 전에 끝낸다.
- `sync_queue`를 백업 파일에 포함하지 않는다.

## BLA-4 구현 상태

- `data/backup`에 백업 envelope, 암호화, payload validation, Room export/import, 병합/교체 planner, Android Keystore 기반 backup DEK store, `BackupEngine`을 추가했다.
- `data/drive`에 Drive `appDataFolder` 백업 파일 업로드/다운로드 port와 Ktor REST 구현, 권한 흐름용 `DriveAuthorizationClient` port를 추가했다.
- Drive 파일명은 `bladderdiary-backup-v1.json` 고정값이며 사용자 기록 의미를 담지 않는다. 수동 fallback은 같은 envelope JSON을 `.bdbackup` 확장자로 내보내는 경로에서 재사용할 수 있다.
- 백업 암호화는 `BACKUP_AES_GCM_V1` scheme, PBKDF2-HMAC-SHA256, AES-GCM, backup 전용 AAD를 사용한다.
- 자동 백업은 로컬에 저장된 backup DEK와 password envelope가 있을 때만 새 payload를 만들 수 있으며, 로컬 키가 없으면 실패로 구분한다.
- 복원은 payload validation 이후 Room transaction으로 반영하며, 기본 모드는 병합이다.
- 현재 단계는 데이터/저장소 엔진 구현이며, Compose 설정 화면과 Android `AuthorizationClient` 어댑터 연결은 후속 UI 단계에서 붙인다.

## 검증 기준

- Drive 앱 미설치 상태에서도 Google Play services와 Google 계정이 있으면 권한 요청과 백업이 가능해야 한다.
- Google 계정 없음, 권한 거부, Play services 사용 불가, 네트워크 실패가 구분되어야 한다.
- 백업 파일에는 memo, volume, urgency, incontinence, nocturia, exact time 같은 민감 기록이 평문으로 남지 않아야 한다.
- 백업 비밀번호가 틀리면 복원은 실패하고 로컬 DB는 변경되지 않아야 한다.
- 빈 DB 복원, 기존 데이터와 병합, 백업으로 교체, tombstone 반영을 검증해야 한다.
- Supabase 동기화 ON/OFF 상태가 Google Drive 백업 성공 여부에 직접 영향을 주지 않아야 한다.

## 후속 문서 갱신

BLA-3에서는 설계만 확정한다. 구현이 들어가는 BLA-4 이후에는 실제 동작에 맞춰 README, `SECURITY.md`, `RELIABILITY.md`, 제품 스펙 문서, 개인정보 처리방침을 갱신한다. 특히 사용자-facing 문구에서는 `동기화`와 `백업/복원`을 분리해서 설명해야 한다.

## 참고

- Google Drive API `appDataFolder`: https://developers.google.com/workspace/drive/api/guides/appdata
- Google Drive API scope 선택: https://developers.google.com/workspace/drive/api/guides/api-specific-auth
- Android Google API authorization: https://developer.android.com/identity/authorization
