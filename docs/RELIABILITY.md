# RELIABILITY

이 문서는 안정성과 검증 관련 상위 진입 문서다.
`docs/` 루트에서는 빠른 라우팅만 제공하고, 테스트 상세 기준은 `testing/` 아래에서 관리한다.

## 안정성 관점의 핵심 영역

- 로컬 우선 저장과 재시도 가능한 동기화 큐
- 사용자가 동기화를 켠 경우에만 동작하는 WorkManager 기반 백그라운드 동기화
- 날짜 경계와 기기 로컬 타임존 처리
- 원격 다운로드 후 로컬 병합 흐름
- PDF 내보내기와 공유 파일 생성
- 기본 단위 테스트와 수동 smoke 검증

## 테스트/검증 문서 빠른 진입

1. `testing/README.md`
2. `SMOKE_TEST.md`
3. `TEST_COVERAGE.md`
4. `testing/test-catalog-guide.md`
5. `product-specs/sync-behavior.md`
6. `design-docs/offline-sync-flow.md`
7. `../README.md`

## 현재 저장소 안의 관련 코드

- `app/src/main/java/com/bladderdiary/app/data/local/`
- `app/src/main/java/com/bladderdiary/app/data/repository/VoidingRepositoryImpl.kt`
- `app/src/main/java/com/bladderdiary/app/worker/`
- `app/src/main/java/com/bladderdiary/app/export/`
- `app/src/main/java/com/bladderdiary/app/presentation/main/`
- `app/src/test/java/com/bladderdiary/app/`

## 갱신 포인트

- 동기화 재시도 기준이나 큐 모델이 바뀌면 `product-specs/sync-behavior.md`와 이 문서를 함께 갱신한다.
- smoke 실행 기준이 바뀌면 `testing/smoke-tests.md`와 `SMOKE_TEST.md`를 함께 갱신한다.
- 테스트 전략이나 coverage 기준이 바뀌면 `testing/coverage.md`와 `TEST_COVERAGE.md`를 함께 갱신한다.
- 날짜 계산, 타임존 기준, PDF 범위 계산이 바뀌면 설계 문서와 제품 문서를 함께 점검한다.
