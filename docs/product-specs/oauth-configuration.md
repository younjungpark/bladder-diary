# OAuth 설정 기준

## 목적

이 문서는 배뇨일기(BladderDiary)의 Google/Kakao OAuth 설정 기준을 한곳에 고정한다. OAuth 기능은 Android 앱이 Google/Kakao SDK를 직접 붙이는 방식이 아니라 Supabase Auth Providers를 통해 동작한다.

## 현재 기준

- 제공자: Google, Kakao
- 인증 중계: Supabase Auth Providers
- 앱 패키지명: `com.chausoft.bladderdiary`
- 앱 딥링크 리다이렉트 URI: `bladderdiary://auth/callback`
- Supabase Redirect URLs: `bladderdiary://auth/callback`

## 인증 흐름

```text
앱
-> Supabase Auth authorize
-> Google 또는 Kakao OAuth
-> Supabase Auth callback
-> bladderdiary://auth/callback
-> 앱 복귀 및 세션 저장
```

앱은 OAuth 제공자별 Android SDK를 직접 호출하지 않는다. 앱은 Supabase Auth authorize URL을 열고, 인증 완료 후 커스텀 스킴 딥링크로 돌아오는 콜백을 처리한다.

## Supabase 설정

Supabase Dashboard에서 아래 항목을 유지한다.

```text
Authentication
URL Configuration
Redirect URLs
```

필수 값:

```text
bladderdiary://auth/callback
```

Google/Kakao provider는 Supabase Auth Providers에서 활성화한다. Provider별 Client ID, Client Secret, 앱 키 같은 외부 콘솔 값은 Supabase에 등록하고, Android 앱에는 넣지 않는다.

## Google 설정 기준

Google Cloud Console에서는 Supabase Auth callback URL을 OAuth 웹 클라이언트의 승인된 리다이렉트 URI로 등록한다.

```text
https://<SUPABASE_PROJECT_REF>.supabase.co/auth/v1/callback
```

현재 OAuth가 Supabase 웹 콜백을 경유하므로, 일반적인 로그인 동작에는 Android 패키지명이나 SHA 인증서 등록이 직접 관여하지 않는다. Google 로그인 방식을 Android 네이티브 SDK나 App Links 기반 흐름으로 바꾸는 경우에만 패키지명과 SHA-1/SHA-256 설정을 별도로 검토한다.

## Kakao 설정 기준

Kakao Developers에서는 Supabase Auth callback URL과 Supabase Provider에서 요청하는 동의항목을 맞춘다.

확인 항목:

- 카카오 로그인 사용 설정
- Supabase Provider scope와 Kakao 동의항목의 일치 여부
- `account_email`, `profile_nickname`, `profile_image` 같은 요청 항목의 동의 설정

현재 OAuth가 Supabase callback을 경유하므로, 일반적인 로그인 동작에는 Android 패키지명이나 키 해시 등록이 직접 관여하지 않는다. Kakao Android SDK를 직접 쓰거나 App Links 기반 흐름으로 바꾸는 경우에만 패키지명과 릴리즈 키 해시 설정을 별도로 검토한다.

## Android 앱 설정 기준

`local.properties`에는 아래 값을 둔다.

```properties
SUPABASE_REDIRECT_URI=bladderdiary://auth/callback
```

`AndroidManifest.xml`은 아래 딥링크를 수신할 수 있어야 한다.

```text
scheme: bladderdiary
host: auth
pathPrefix: /callback
```

OAuth 복귀 안정성을 위해 `MainActivity`는 딥링크 콜백을 받을 수 있어야 하며, 동일 콜백 중복 처리에 유의한다.

## 새 패키지명 전환 영향

Google Play 새 앱 등록을 위해 `applicationId`는 아래 값을 사용한다.

```text
com.chausoft.bladderdiary
```

OAuth 콜백 URI는 패키지명이 아니라 커스텀 스킴 기준으로 유지되므로, 현재 구조에서는 패키지명 변경만으로 Supabase Redirect URLs를 바꾸지 않는다.

향후 HTTPS App Links로 전환하면 `assetlinks.json`의 `package_name`과 릴리즈 SHA-256 지문을 새 패키지명 기준으로 작성한다.

```json
{
  "package_name": "com.chausoft.bladderdiary",
  "sha256_cert_fingerprints": [
    "RELEASE_CERT_SHA256_FINGERPRINT"
  ]
}
```

## 검증 절차

릴리즈 빌드 실기기 기준으로 확인한다.

1. Google 로그인 후 앱으로 정상 복귀하는지 확인한다.
2. Google 로그인 후 Supabase 데이터가 복구되는지 확인한다.
3. 로그아웃 후 Kakao 로그인으로 앱에 정상 복귀하는지 확인한다.
4. Kakao 로그인 후 Supabase 데이터가 복구되는지 확인한다.
5. 실패 시 앱 로그와 Supabase Auth 로그를 함께 확인한다.

## 문제 해결 기준

- 앱 복귀 실패: Supabase Redirect URLs와 `SUPABASE_REDIRECT_URI`가 `bladderdiary://auth/callback`으로 일치하는지 확인한다.
- Google 로그인 실패: Google OAuth 웹 클라이언트의 승인된 리다이렉트 URI가 Supabase Auth callback URL인지 확인한다.
- Kakao 로그인 실패: Supabase Provider scope와 Kakao Developers 동의항목이 일치하는지 확인한다.
- 특정 브라우저에서만 복귀 실패: Chrome Custom Tab, 기본 브라우저, `MainActivity` 딥링크 수신 흐름을 실기기에서 확인한다.
