# Configuration Reference

## 개발 환경

- JDK 17
- Android SDK 35
- Android Studio 최신 안정 버전 권장

## `local.properties`

아래 값은 개인 환경 전용이므로 버전 관리에 포함하지 않는다. OAuth 세부 설정 기준은 `oauth-configuration.md`를 따른다.

```properties
sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
SUPABASE_REDIRECT_URI=bladderdiary://auth/callback
RELEASE_STORE_FILE=your-release-keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

## Google Drive 백업 설정

- Google Drive 백업/복원 엔진은 Drive REST API의 `appDataFolder`와 `https://www.googleapis.com/auth/drive.appdata` scope를 기준으로 한다.
- Android 권한 흐름은 Google Identity Services `AuthorizationClient`를 사용한다. 앱은 사용자가 설정 화면에서 백업/복원을 명시적으로 실행할 때만 Drive 권한을 요청한다.
- Google Cloud Console에는 Google Drive 백업 권한을 요청할 Android OAuth 클라이언트를 등록해야 한다. 패키지명은 `com.chausoft.bladderdiary`이며, 실기기에 설치하는 빌드의 서명 인증서 SHA-1 지문을 함께 등록한다.
- 디버그 APK 실기기 검증 시 SHA-1은 아래 명령으로 확인한다.

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -keypass android
```

- 릴리즈 APK를 sideload해 검증할 때는 실제 릴리즈 keystore의 SHA-1 또는 APK 서명 인증서 SHA-1을 Android OAuth 클라이언트로 별도 등록한다. Play Console 배포본을 검증할 때는 Play App Signing 인증서의 SHA-1을 등록해야 한다.
- OAuth 앱 게시 상태가 Testing이면 디버그/릴리즈 Android OAuth 클라이언트가 같은 테스트 사용자 목록을 공유한다. 테스트에 사용할 Google 계정은 Google Cloud Console의 Google 인증 플랫폼 Audience 설정에서 테스트 사용자로 추가한다.
- 앱이 `UNREGISTERED_ON_API_CONSOLE`을 반환하면 현재 설치된 APK의 패키지명과 SHA-1 조합이 Google Cloud Console Android OAuth 클라이언트에 등록되지 않은 상태로 본다.
- Drive access token은 `local.properties`나 DataStore에 장기 저장하지 않는다. Android 권한 화면에서 받은 짧은 수명 token을 호출 시점에 사용한다.
- 백업 암호화 비밀번호와 backup DEK는 Supabase E2EE 설정값과 독립적으로 관리한다.
- 자동 백업 상태, 마지막 성공/실패 시각, 로컬 backup DEK envelope는 사용자별 DataStore/Android Keystore에 저장된다. 백업 파일 자체는 Drive `appDataFolder` 최신 스냅샷 1개이며, 수동 fallback은 같은 JSON envelope를 `.bdbackup` 파일로 저장한다.

## Supabase SQL 초기화 순서

아래 파일을 번호 순서대로 적용한다.

1. `supabase/sql/001_init.sql`
2. `supabase/sql/002_e2ee_memo.sql`
3. `supabase/sql/003_add_volume_ml.sql`
4. `supabase/sql/004_add_urgency.sql`
5. `supabase/sql/005_add_has_incontinence.sql`
6. `supabase/sql/006_add_is_nocturia.sql`
7. `supabase/sql/007_account_data_deletion.sql`
8. `supabase/sql/008_account_deletion_requests.sql`
9. `supabase/sql/009_record_e2ee.sql`

## 자주 쓰는 명령

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

## 갱신 규칙

- Gradle, SDK, 서명 방식이 바뀌면 이 문서를 갱신한다.
- 리다이렉트 URI나 Supabase 설정 키가 바뀌면 README, 이 문서, `oauth-configuration.md`를 함께 갱신한다.
- 릴리즈 체크리스트가 바뀌면 `../../doc/store_release_checklist.md`도 함께 점검한다.
