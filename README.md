# 배뇨일기 BladderDiary

배뇨일기(BladderDiary)는 배뇨 기록을 빠르게 남기고, 날짜별 패턴을 확인하며, 필요 시 PDF로 내보낼 수 있는 안드로이드 앱입니다. 로컬 우선 저장 구조를 기반으로 동작하며, 사용자가 클라우드 동기화를 켠 경우에만 Supabase와 동기화를 수행합니다.

## 주요 기능

- Google / Kakao 소셜 로그인
- 4자리 PIN 잠금
- 날짜별 배뇨 기록 추가, 수정, 삭제
- 기록 항목별 메모, 배뇨량, 절박감, 요실금 여부 저장
- 메인 화면 타임라인과 월간 캘린더 조회
- 오프라인 로컬 저장 후 선택형 클라우드 동기화
- 클라우드 기록 본문 종단간 암호화(E2EE) 비밀문구 설정 / 변경 / 잠금 해제
- 로그인 전 민감 건강정보 및 선택형 Supabase 클라우드 저장 안내 확인
- 로그인 후 클라우드 동기화 사용 여부 선택
- 설정 메뉴의 Google Drive 암호화 백업/복원, 자동 백업, 수동 `.bdbackup` 파일 내보내기/가져오기
- 기간 선택 기반 PDF 내보내기
- 설정 메뉴에서 경고 확인 후 회원탈퇴 및 앱 데이터 초기화

## 화면 구성

- 인증 화면: Google 또는 Kakao 계정으로 로그인
- 메인 화면: 선택 날짜 기준 기록 타임라인, 요약 정보, 동기화 상태와 로컬 저장 상태 확인
- 기록 입력 시트: 시간, 절박감, 요실금 여부, 배뇨량, 메모 입력
- 캘린더 화면: 월간 기록 밀도와 날짜별 기록 유무 확인
- 보안 화면: PIN 설정 및 잠금 해제
- 기록 암호화 화면: E2EE 비밀문구 설정, 변경, 잠금 해제

## 기술 스택

- Kotlin
- Jetpack Compose + Material 3
- Room
- DataStore
- WorkManager
- Ktor Client
- kotlinx.serialization
- Supabase REST/Auth 연동
- Google Identity Services AuthorizationClient
- Google Drive `appDataFolder` 암호화 백업/복원

## 프로젝트 구조

```text
app/src/main/java/com/bladderdiary/app
|- core/           앱 그래프 및 의존성 초기화
|- data/           로컬 DB, 원격 API, 백업 엔진, 저장소 구현, 보안 처리
|- domain/         모델 및 유스케이스
|- export/         PDF 보고서 생성
|- presentation/   인증, 메인, PIN, E2EE 화면과 ViewModel
|- ui/theme/       앱 테마, 색상, 타이포그래피
|- worker/         Supabase 동기화와 Google Drive 자동 백업 작업
```

## 개발 환경

- Android Studio 최신 안정 버전 권장
- JDK 17
- Android SDK 35
- 최소 SDK 26

## 시작하기

### 1. Supabase 초기 설정

1. Supabase 프로젝트를 생성합니다.
2. `supabase/sql` 아래 SQL 파일을 번호 순서대로 실행합니다.

```text
001_init.sql
002_e2ee_memo.sql
003_add_volume_ml.sql
004_add_urgency.sql
005_add_has_incontinence.sql
006_add_is_nocturia.sql
007_account_data_deletion.sql
008_account_deletion_requests.sql
009_record_e2ee.sql
```

3. Authentication에서 Google, Kakao provider를 활성화합니다. OAuth는 앱에서 Google/Kakao SDK를 직접 붙이지 않고 Supabase Auth Providers를 통해 처리합니다.
4. Redirect URL에 `bladderdiary://auth/callback`를 추가합니다. 상세 설정은 [docs/product-specs/oauth-configuration.md](docs/product-specs/oauth-configuration.md)를 따릅니다.
5. 프로젝트 루트의 `local.properties`에 아래 값을 설정합니다.

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
SUPABASE_REDIRECT_URI=bladderdiary://auth/callback
```

### 2. 앱 실행

- Android Studio에서 프로젝트를 연 뒤 Gradle Sync를 수행합니다.
- 에뮬레이터 또는 실기기에서 `app` 모듈을 실행합니다.

CLI로 빌드할 경우:

```bash
./gradlew :app:assembleDebug
```

## 릴리즈 APK 빌드

릴리즈 서명까지 포함해 APK를 만들려면 `local.properties`에 아래 항목을 추가합니다.

```properties
RELEASE_STORE_FILE=your-release-keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

그다음 아래 명령으로 릴리즈 APK를 생성할 수 있습니다.

```bash
./gradlew :app:assembleRelease
```

생성 위치:

```text
app/build/outputs/apk/release/app-release.apk
```

## 동작 메모

- 일별 기준은 기기 로컬 타임존의 자정입니다.
- 클라우드 기록 암호화는 클라우드 동기화 사용 전 필요한 기능입니다.
- E2EE 비밀문구를 잊어버리면 암호화된 클라우드 기록 본문을 복구할 수 없습니다.
- 클라우드 동기화는 기본적으로 꺼져 있으며, 로그인 후 사용자가 로컬만 사용하거나 동기화를 켤 수 있습니다.
- 동기화를 켠 경우 정확한 배뇨 시각, 배뇨량, 절박감, 요실금 여부, 야간뇨 여부, 메모는 기기에서 암호화된 뒤 서버에 저장됩니다.
- 동기화를 켠 경우에도 날짜, 계정/기록 식별자, 삭제/동기화 메타데이터는 서버에 남을 수 있습니다.
- 기존 평문 클라우드 기록은 최신 앱에서 암호화 설정 또는 잠금 해제 후 암호문으로 재업로드되지만, 과거 Supabase 백업이나 로그에 남은 평문까지 즉시 삭제된다고 보장할 수는 없습니다.
- Google Drive 백업/복원은 실시간 동기화가 아니라 백업 전용 비밀번호로 암호화한 최신 스냅샷 1개를 `appDataFolder`에 저장하는 별도 엔진입니다.
- 설정 메뉴의 `백업 및 복원`에서 Google Drive 백업/복원, 자동 백업, 수동 백업 파일 내보내기/가져오기를 사용할 수 있습니다.
- Google Drive 백업/복원에는 Google 계정 선택과 `drive.appdata` 권한 동의가 필요합니다. Google Drive 앱 설치는 필수가 아니지만 Google Play services, Google 계정, 네트워크 사용 가능 상태가 필요할 수 있습니다.
- Google Drive를 사용할 수 없거나 권한을 거부한 경우에도 같은 암호화 포맷의 `.bdbackup` 수동 파일 내보내기/가져오기를 사용할 수 있습니다.
- 자동 백업은 백업 비밀번호로 최초 백업을 만든 뒤 로컬에 보관된 backup DEK가 있을 때만 동작하며, 기록 변경 후 약 30분 뒤 Supabase 동기화 queue와 별도 WorkManager 작업으로 예약됩니다.
- Google Drive 백업 파일에는 Supabase access token, refresh token, PIN, WorkManager 상태, `sync_queue`가 포함되지 않습니다.
- 회원탈퇴는 기기 로컬 백업 설정과 자동 백업 키를 초기화하지만, 이미 Google Drive `appDataFolder`에 저장된 암호화 백업 파일이나 사용자가 내보낸 `.bdbackup` 파일은 별도 저장소에 남을 수 있습니다.
- 배뇨 기록은 건강 관련 민감정보일 수 있으며, 앱은 로그인 전과 기존 사용자 업데이트 진입 시 민감정보 및 클라우드 저장 안내를 표시합니다.
- 동기화를 켠 경우 삭제는 소프트 삭제 기반으로 원격 반영됩니다.
- 오프라인 상태에서 저장한 기록은 로컬에 보관되며, 동기화를 켠 상태라면 연결이 복구될 때 다시 동기화됩니다.
- 회원탈퇴는 운영자 수동 Auth 삭제를 위한 탈퇴 요청을 먼저 기록하고, 클라우드 앱 기록과 E2EE 키를 삭제한 뒤 기기 로컬 DB, PIN, 세션 정보를 초기화합니다.
- PDF는 개인 참고용 기록이며 의료적 진단이나 치료 판단을 대체하지 않습니다.

## 관련 문서

- [docs/README.md](docs/README.md)
- [docs/CODING_RULES.md](docs/CODING_RULES.md)
- [docs/product-specs/oauth-configuration.md](docs/product-specs/oauth-configuration.md)
- [LICENSE](LICENSE)
- [TRADEMARK_POLICY.md](TRADEMARK_POLICY.md)
- [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- [MEDICAL_DISCLAIMER.md](MEDICAL_DISCLAIMER.md)

## 라이선스

이 프로젝트는 Apache License 2.0을 따릅니다.  
단, `배뇨일기`, `BladderDiary` 이름, 로고, 아이콘 등 브랜드 자산은 별도 정책의 적용을 받을 수 있습니다.
