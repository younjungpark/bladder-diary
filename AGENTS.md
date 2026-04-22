# bladder-diary 에이전트 작업 가이드

## 목적

이 저장소의 `AGENTS.md`는 항상 짧은 진입 지도만 제공한다.
기본 컨텍스트는 얇게 유지하고, 필요한 문서와 파일만 조건부로 연다.

## 기본 진입 문서

작업 시작 시 기본으로 읽을 문서는 아래 두 개만 둔다.

1. `README.md`
2. `docs/README.md`

작은 버그 수정이나 단일 파일 작업은 위 두 문서와 관련 코드만 먼저 확인한다.

## 조건부 문서 로딩 규칙

- Kotlin 소스나 테스트를 수정하거나 명명 규칙, import, KDoc, 포맷에 영향이 있을 때는 `docs/CODING_RULES.md`를 연다.
- 앱 구조, 계층 책임, 패키지 분리, 의존성 초기화 흐름을 확인할 때는 `docs/DESIGN.md`, `docs/design-docs/index.md`, `app/src/main/java/com/bladderdiary/app/core/AppGraph.kt`를 먼저 본다.
- 인증, PIN, E2EE, 계정 전환, 비밀값 처리에 영향이 있을 때는 `docs/SECURITY.md`와 `docs/product-specs/configuration-reference.md`를 연다.
- 동기화, WorkManager, 오프라인 처리, 타임존 기준, PDF 내보내기, 테스트 신뢰성에 영향이 있을 때는 `docs/RELIABILITY.md`와 `docs/testing/README.md`를 연다.
- 릴리즈 APK, 서명, 배포 전 점검이 필요할 때만 `docs/product-specs/configuration-reference.md`, `doc/store_release_checklist.md`, `app/build.gradle.kts`를 연다.
- 개인정보, 의료 고지, 상표 정책에 영향이 있을 때만 `PRIVACY_POLICY.md`, `MEDICAL_DISCLAIMER.md`, `TRADEMARK_POLICY.md`를 연다.
- 작업이 여러 패키지나 여러 계층에 걸치면 `docs/PLANS.md`와 `docs/exec-plans/active/`의 관련 문서 1개만 연다.
- 작업이 여러 패키지나 여러 계층에 걸쳐도 저장소 전체를 한 번에 훑지 않고 관련 디렉터리만 골라서 본다.

## 기본 탐색에서 제외할 경로

- `.gradle/`
  빌드 캐시와 내부 메타데이터는 기본 진입 경로가 아니다.
- `app/build/`
  생성 산출물은 문제 재현이나 결과 확인이 필요할 때만 본다.
- `supabase/`
  스키마, 인증, 동기화 변경이 아닐 때는 기본 탐색에서 제외한다.
- `doc/store_release_checklist.md`
  릴리즈 작업이 아닐 때는 기본 진입 문서가 아니다.
- `docs/exec-plans/completed/`
  완료 계획은 기본 진입 문서가 아니다. 과거 결정 추적이 필요할 때만 본다.
- `docs/references/`
  참고 자료는 기준본이 아니다. 출처 추적이나 비교가 필요할 때만 본다.

## 문서 운영 원칙

- Markdown 문서는 하드 개행을 최소화한다. 제목, 목록, 표, 코드 블록, 인용, 의미 있는 문단 구분이 아닌데도 문장을 여러 줄로 인위적으로 쪼개지 않는다.
- Kotlin/Android 코딩 규칙은 전역 메모보다 저장소 내부의 `docs/CODING_RULES.md`를 우선 기준으로 삼는다.
- `docs/README.md`는 문서 하네스의 루트 인덱스로 유지한다.
- `AGENTS.md`는 계속 짧게 유지하고, 깊은 설명은 `docs/`로 보낸다.
- 구현 동작이 바뀌면 코드와 테스트만 바꾸지 말고 `README.md`나 관련 정책 문서도 함께 갱신한다.
- 작업 시작 시 저장소 외부 경로를 읽기 진입점으로 삼지 않는다.
- 커밋 메시지는 제목과 본문을 모두 한글로 작성한다.
