# Test Catalog Guide

## 목적

이 문서는 현재 테스트 파일 구성을 빠르게 훑기 위한 안내 문서다.

## 현재 패키지 기준 읽는 법

- `app/src/test/java/com/bladderdiary/app/data/remote/`
  원격 계층의 작은 규칙과 helper를 확인할 때 본다.
- `app/src/test/java/com/bladderdiary/app/data/repository/`
  저장소 계층 상태 전이와 보호 로직을 확인할 때 본다.
- `app/src/test/java/com/bladderdiary/app/data/security/`
  암호화/해시 같은 순수 로직을 확인할 때 본다.
- `app/src/test/java/com/bladderdiary/app/data/backup/`
  Google Drive 및 수동 백업 envelope, 복원 planner, backup engine fake 연동을 확인할 때 본다.
- `app/src/test/java/com/bladderdiary/app/export/`
  PDF 집계와 보고서 구성을 확인할 때 본다.
- `app/src/test/java/com/bladderdiary/app/presentation/`
  ViewModel, 화면 상태, 이벤트 흐름을 확인할 때 본다.

## 빠른 탐색 팁

```powershell
Get-ChildItem -Recurse -File app\src\test
```

특정 기능을 바꿀 때는 먼저 같은 패키지의 테스트가 있는지 확인하고,
없다면 가장 가까운 계층에 새 테스트를 추가한다.
