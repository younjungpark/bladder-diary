# SMOKE TEST

이 문서는 smoke test 빠른 진입 문서다.
상세 절차와 수동 확인 항목은 `testing/smoke-tests.md`에서 관리한다.

## 빠른 실행 순서

1. `.\gradlew.bat spotlessCheck`
2. `.\gradlew.bat :app:testDebugUnitTest`
3. `.\gradlew.bat :app:assembleDebug`
4. 필요하면 에뮬레이터 또는 실기기에서 수동 smoke 진행

## 함께 보는 문서

- `testing/smoke-tests.md`
- `RELIABILITY.md`
- `product-specs/configuration-reference.md`
