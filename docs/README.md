# bladder-diary 문서 하네스

이 디렉터리는 bladder-diary의 저장소 내부 지식 베이스이다.
에이전트와 사람이 같은 문서 진입점을 공유할 수 있도록 역할별로 나눠 관리한다.

## 구조

- `design-docs/`
  설계 원칙, 데이터 흐름, 보안/잠금 흐름 같은 설계 보강 문서
- `exec-plans/`
  진행 중 계획, 완료 기록, 기술 부채 추적
- `product-specs/`
  설정, 동기화, 내보내기 같은 사용자 관점 계약 문서
- `testing/`
  테스트와 검증 관련 상세 문서
- `references/`
  보조 참고 자료와 비기준 문서

## 루트 문서

- `CODING_RULES.md`
- `DESIGN.md`
- `PLANS.md`
- `SECURITY.md`
- `RELIABILITY.md`
- `SMOKE_TEST.md`
- `TEST_COVERAGE.md`
- `product-specs/index.md`
- `testing/README.md`

## 운영 원칙

- 저장소 내부 문서를 우선 기준으로 삼는다.
- 루트 문서는 얇은 안내판 역할에 집중하고, 상세 설명은 하위 디렉터리 문서로 분리한다.
- 구현 동작이 바뀌면 코드, 테스트, 관련 문서를 함께 갱신한다.
- 참고 자료와 완료 계획은 기본 진입 문서로 삼지 않는다.
