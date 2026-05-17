# RELIABILITY

이 문서는 안정성과 검증 관련 상위 진입 문서다.
`docs/` 루트에서는 빠른 라우팅만 제공하고, 테스트 상세 기준은 `testing/` 아래에서 관리한다.

## 안정성 관점의 핵심 영역

- 로컬 우선 저장과 재시도 가능한 동기화 큐
- 사용자가 동기화를 켠 경우에만 동작하는 WorkManager 기반 백그라운드 동기화
- Supabase 동기화 queue와 분리된 Google Drive 암호화 백업/복원 엔진
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
- `app/src/main/java/com/bladderdiary/app/data/backup/`
- `app/src/main/java/com/bladderdiary/app/data/drive/`
- `app/src/main/java/com/bladderdiary/app/data/repository/VoidingRepositoryImpl.kt`
- `app/src/main/java/com/bladderdiary/app/worker/`
- `app/src/main/java/com/bladderdiary/app/export/`
- `app/src/main/java/com/bladderdiary/app/presentation/main/`
- `app/src/test/java/com/bladderdiary/app/`

## 갱신 포인트

- 동기화 재시도 기준이나 큐 모델이 바뀌면 `product-specs/sync-behavior.md`와 이 문서를 함께 갱신한다.
- Google Drive 백업/복원 정책이 바뀌면 `design-docs/google-drive-backup-restore.md`와 이 문서를 함께 갱신한다.
- smoke 실행 기준이 바뀌면 `testing/smoke-tests.md`와 `SMOKE_TEST.md`를 함께 갱신한다.
- 테스트 전략이나 coverage 기준이 바뀌면 `testing/coverage.md`와 `TEST_COVERAGE.md`를 함께 갱신한다.
- 날짜 계산, 타임존 기준, PDF 범위 계산이 바뀌면 설계 문서와 제품 문서를 함께 점검한다.

## Google Drive 백업 안정성 메모

- Google Drive 백업은 최신 스냅샷 1개를 생성/업로드/다운로드하는 엔진이며, Supabase `sync_queue`에 백업 작업을 넣지 않는다.
- 자동 백업은 `google_drive_backup_work` unique WorkManager 작업으로 예약하며 Supabase `voiding_sync_work`와 분리한다. 기록 추가, 수정, 삭제 후 약 30분 지연으로 다시 예약해 짧은 연속 변경을 하나의 백업으로 합친다.
- 복원은 먼저 백업 비밀번호 복호화와 payload validation을 끝낸 뒤 Room transaction 안에서 반영한다.
- 병합 복원은 같은 `localId`에서 `updatedAtEpochMs`가 더 최신인 백업 기록만 반영하고, 더 최신인 로컬 기록은 유지한다. 방금 삭제한 기록처럼 로컬 tombstone이 백업 기록보다 최신이면 병합에서는 되살리지 않으므로, 백업 시점으로 되돌리는 복구 테스트에는 `백업으로 교체`를 사용한다.
- 교체 복원은 현재 사용자 로컬 기록을 백업 스냅샷으로 바꾸며, 복원된 기록은 `SYNCED` 상태의 로컬 데이터로 취급한다.
- 수동 `.bdbackup` 내보내기/가져오기는 Drive가 없거나 권한을 거부한 환경에서 같은 envelope와 같은 복원 정책을 사용하는 fallback이다.
