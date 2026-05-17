# Design Docs Index

이 디렉터리는 bladder-diary 설계 보강 문서를 모아 두는 영역이다.
루트 `DESIGN.md`는 얇은 진입 문서로 두고, 실제 설명은 여기서 관리한다.

## 구성

- `offline-sync-flow.md`
  로컬 저장, 큐 적재, 백그라운드 동기화, 원격 병합 흐름
- `google-drive-backup-restore.md`
  Google Drive `appDataFolder` 기반 암호화 백업/복원 설계
- `security-and-lock-flow.md`
  로그인, 계정 전환, PIN, E2EE 흐름
- `data-model-er.md`
  로컬 Room과 원격 Supabase 테이블의 ER 다이어그램

## 함께 보는 문서

- `../DESIGN.md`
- `../SECURITY.md`
- `../RELIABILITY.md`
- `../product-specs/index.md`
