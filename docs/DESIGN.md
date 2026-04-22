# DESIGN

## 목적

이 문서는 bladder-diary 설계 문서를 읽는 출발점이다.
세부 설계는 한 파일에 몰아넣지 않고, 주제별 문서와 계획 문서로 나눠 관리한다.

## 기본 진입 순서

1. `../README.md`
2. `design-docs/index.md`
3. 현재 작업과 직접 관련된 설계 문서 1~2개

`design-docs/` 전체를 한 번에 읽지 않는다.
필요한 주제 문서만 선택해서 연다.

## 선택적으로 볼 문서

- `SECURITY.md`
  인증, PIN, E2EE, 계정 전환 흐름이 중요할 때만 연다.
- `RELIABILITY.md`
  동기화, WorkManager, 검증 기준이 중요할 때만 연다.
- `product-specs/index.md`
  설정값이나 사용자 관점 계약을 함께 봐야 할 때만 연다.
- `exec-plans/completed/`
  과거 결정이나 마이그레이션 기록을 추적할 때만 연다.

## 설계 문서 작성 규칙

- 하나의 문서가 너무 커지면 주제별로 분리한다.
- 설계 문서는 배경, 현재 구조, 비목표, 영향 범위, 검증 기준을 포함한다.
- reference와 completed 계획은 기본 진입 문서로 삼지 않는다.
- 구현 전에 만든 실행 계획은 필요할 때만 `exec-plans/active/`에 둔다.
- 구현 후에도 장기 참조 가치가 있는 기록만 `exec-plans/completed/`에 남긴다.
