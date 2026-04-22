# Sync Behavior

## 목적

이 문서는 기록 저장과 동기화의 사용자 관점 계약을 설명한다.

## 핵심 동작

- 기록 추가, 수정, 삭제는 먼저 로컬 DB에 반영된다.
- 원격 반영이 필요한 작업은 sync queue에 적재된다.
- 가능한 경우 즉시 동기화를 시도하고, 실패하면 백그라운드 재시도를 예약한다.
- 원격 다운로드는 사용자 세션 기준으로 가져오며, 로컬 DB에 병합한다.

## 현재 계약

- 날짜 기준은 기기 로컬 타임존 자정이다.
- 삭제는 원격에서 soft delete로 표현된다.
- 메모 암호화가 비활성 상태면 메모는 평문 동기화 경로를 따른다.
- 메모 암호화가 활성 상태면 메모는 ciphertext 기준으로 동기화된다.
- 사용자가 온라인으로 복귀하면 pending 작업이 다시 업로드될 수 있다.

## 실패 시 기대 동작

- 동기화 실패가 있더라도 로컬 데이터는 즉시 사라지지 않는다.
- pending / failed 상태는 UI와 저장소 흐름에서 확인 가능해야 한다.
- 인증 토큰 만료가 발생하면 세션 갱신 후 재시도를 시도한다.

## 관련 코드

- `data/repository/VoidingRepositoryImpl.kt`
- `data/local/SyncQueueDao.kt`
- `data/local/VoidingEventDao.kt`
- `worker/SyncWorker.kt`
