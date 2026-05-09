# Smoke Tests

## 목적

이 문서는 기본 CLI 검증과 수동 앱 smoke 절차를 정리한다.

## 사전 조건

- `local.properties`에 `sdk.dir`가 설정되어 있어야 한다.
- 앱 기능 smoke를 위해서는 `../product-specs/oauth-configuration.md` 기준의 Supabase Auth Providers 설정과 로그인 제공자 구성이 되어 있어야 한다.

## 기본 CLI smoke

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## 수동 앱 smoke 체크리스트

1. 앱이 실행되고 로그인 화면이 정상 표시되는지 확인한다.
2. Google 로그인 후 앱으로 복귀하고 메인 화면으로 진입하는지 확인한다.
3. 로그아웃 후 Kakao 로그인으로 앱에 복귀하고 메인 화면으로 진입하는지 확인한다.
4. 기록을 추가하고 목록과 일일 집계가 즉시 반영되는지 확인한다.
5. 기록을 수정한 뒤 시간, 절박감, 메모, 배뇨량이 정상 반영되는지 확인한다.
6. 기록 삭제 후 목록과 집계가 정상 갱신되는지 확인한다.
7. 캘린더 화면에서 월간 집계와 날짜 이동이 정상 동작하는지 확인한다.
8. PIN 설정, 잠금 해제, 오입력 잠금이 정상 동작하는지 확인한다.
9. E2EE 설정 또는 잠금 해제 후 클라우드 기록 암호화와 기존 기록 재동기화 흐름이 깨지지 않는지 확인한다.
10. PDF 내보내기에서 기록 존재 / 미존재 케이스가 정상 처리되는지 확인한다.

## 참고

- 현재 `app/src/androidTest`에는 별도 instrumentation 테스트가 없다.
- 기기 의존 동작은 수동 smoke를 기준으로 본다.
