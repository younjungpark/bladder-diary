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
