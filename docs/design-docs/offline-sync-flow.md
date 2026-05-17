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
5. 클라우드 동기화가 켜져 있고 기록 E2EE가 준비되어 있으면 WorkManager 동기화를 예약한다.
6. 원격 다운로드가 필요한 경우 `fetchAndSyncAll()`로 최신 데이터를 받아 로컬 DB에 병합한다.

## 설계 메모

- 앱은 로컬 우선 동작을 기준으로 한다.
- 클라우드 동기화는 기본적으로 꺼져 있으며, 로그인 후 사용자가 로컬만 사용하거나 동기화를 켜도록 선택한다.
- 동기화를 켜면 기존 pending 작업의 백그라운드 업로드를 예약한 뒤 원격 기록과 병합한다. 이미 동기화 완료된 로컬 기록은 다시 pending으로 바꾸지 않는다. 설정 변경 UI는 네트워크 동기화 완료를 기다리지 않는다.
- 동기화가 켜진 상태의 기록 추가, 수정, 삭제도 로컬 저장 완료만 기다리고, 원격 업로드는 백그라운드 작업으로 분리한다.
- 원격 기록 병합 시 같은 `local_id`의 로컬 pending/failed 변경이 있으면 로컬 변경을 우선하고 원격 데이터로 덮어쓰지 않는다.
- sync queue 처리는 현재 로그인 사용자에 속한 기록만 대상으로 하며, 동기화를 다시 켤 때 이전 오류 문구는 새 재시도 결과가 나오기 전까지 초기화한다.
- 동기화를 끄면 예정된 백그라운드 동기화를 취소하고 이후 원격 업로드와 다운로드를 중지한다. 이미 클라우드에 저장된 데이터는 자동 삭제하지 않는다.
- 삭제는 원격에 soft delete 형태로 반영된다.
- 날짜 기준은 기기 로컬 타임존 자정이다.
- 클라우드 동기화를 켠 기록은 날짜만 평문으로 유지하고 정확한 시각, 배뇨량, 절박감, 요실금 여부, 야간뇨 여부, 메모를 `E2EE_RECORD_V1` 페이로드로 암호화한다.
- 기존 평문 클라우드 기록은 E2EE 설정 또는 잠금 해제 후 내려받아 로컬에 병합하고, 같은 기록 ID로 암호문을 재업로드한다.
- Google Drive 백업/복원은 Supabase 동기화와 별도의 스냅샷 엔진이다. 백업 파일에는 `sync_queue`를 포함하지 않고, 복원 작업도 새 Supabase queue 항목을 만들지 않는다.
- 복원 중 백업 기록으로 대체되는 localId의 기존 queue 항목은 stale upload를 피하기 위해 제거한다. 더 최신인 로컬 pending 기록은 병합 복원에서 유지한다.

## 검증 포인트

- 오프라인 상태에서 기록 추가 후 온라인 복구 시 재업로드가 되는지
- 동기화가 꺼진 상태에서 기록 추가, 수정, 삭제 시 원격 호출과 WorkManager 예약이 발생하지 않는지
- 동기화를 켰을 때 기존 pending 기록만 업로드 대상으로 유지되고 원격 기록이 병합되는지
- 로컬 pending/failed 기록이 있는 상태에서 원격 다운로드가 로컬 변경을 덮어쓰지 않는지
- 수정과 삭제가 queue를 통해 재시도되는지
- 원격 병합 후 일별 목록과 개수가 의도대로 유지되는지
- Google Drive 복원 후 Supabase 동기화 ON/OFF 상태가 백업 엔진 성공 여부를 직접 바꾸지 않는지
