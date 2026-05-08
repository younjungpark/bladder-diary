# Offline Sync Flow

## 목적

이 문서는 bladder-diary의 로컬 우선 저장과 원격 동기화 흐름을 설명한다.

## 핵심 구성요소

- `data/local/VoidingEventDao.kt`
- `data/local/SyncQueueDao.kt`
- `data/local/VoidingEventEntity.kt`
- `data/local/SyncQueueEntity.kt`
- `data/repository/VoidingRepositoryImpl.kt`
- `worker/SyncScheduler.kt`
- `worker/SyncWorker.kt`
- `data/remote/SupabaseApi.kt`

## 기본 흐름

1. 사용자가 기록을 추가, 수정, 삭제한다.
2. 저장소는 먼저 Room에 변경 내용을 반영한다.
3. 원격 반영이 필요한 작업은 sync queue에 적재한다.
4. 클라우드 동기화가 꺼져 있으면 여기서 멈추고 원격 호출이나 WorkManager 예약을 수행하지 않는다.
5. 클라우드 동기화가 켜져 있으면 즉시 동기화를 시도하고, 실패하면 WorkManager 재시도를 예약한다.
6. 원격 다운로드가 필요한 경우 `fetchAndSyncAll()`로 최신 데이터를 받아 로컬 DB에 병합한다.

## 설계 메모

- 앱은 로컬 우선 동작을 기준으로 한다.
- 클라우드 동기화는 기본적으로 꺼져 있으며, 로그인 후 사용자가 로컬만 사용하거나 동기화를 켜도록 선택한다.
- 동기화를 켜면 기존 로컬 활성 기록을 다시 업로드 대상으로 표시한 뒤 원격 기록과 병합한다.
- 동기화를 끄면 예정된 백그라운드 동기화를 취소하고 이후 원격 업로드와 다운로드를 중지한다. 이미 클라우드에 저장된 데이터는 자동 삭제하지 않는다.
- 삭제는 원격에 soft delete 형태로 반영된다.
- 날짜 기준은 기기 로컬 타임존 자정이다.
- 메모는 E2EE 활성화 여부에 따라 ciphertext와 평문 처리 경로가 달라진다.

## 검증 포인트

- 오프라인 상태에서 기록 추가 후 온라인 복구 시 재업로드가 되는지
- 동기화가 꺼진 상태에서 기록 추가, 수정, 삭제 시 원격 호출과 WorkManager 예약이 발생하지 않는지
- 동기화를 켰을 때 기존 로컬 활성 기록이 업로드 대상으로 재큐잉되고 원격 기록이 병합되는지
- 수정과 삭제가 queue를 통해 재시도되는지
- 원격 병합 후 일별 목록과 개수가 의도대로 유지되는지
