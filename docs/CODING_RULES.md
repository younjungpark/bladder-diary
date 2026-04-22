# bladder-diary 코딩 규칙

이 문서는 bladder-diary의 Kotlin/Android 코딩 규칙을 저장소 내부 하네스에 고정하기 위한 기준 문서다. 코딩 에이전트와 사람은 전역 메모보다 이 문서를 우선 참조한다. 자동 포맷과 기본 스타일 검사는 `Spotless + ktlint`와 루트 `.editorconfig`를 기준으로 본다.

## 언제 읽는가

아래 작업에서는 `docs/CODING_RULES.md`를 먼저 연다.

- Kotlin 소스 수정
- 테스트 코드 수정
- 리팩터링
- 명명 규칙, import, KDoc, 포맷 정리
- Compose UI, Room 모델, ViewModel, WorkManager, Supabase 연동 변경

## 핵심 규칙

### 1. 명명 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 패키지 | lowercase | `presentation.main`, `data.remote.dto` |
| 클래스/object/interface/enum/sealed class | UpperCamelCase | `MainViewModel`, `VoidingEventEntity` |
| Composable 함수 | UpperCamelCase | `MainScreen`, `PdfExportDialog` |
| 일반 함수 | lowerCamelCase | `requestSync`, `consumeMessage` |
| 프로퍼티/파라미터/로컬 변수 | lowerCamelCase | `selectedDate`, `pendingSyncCount` |
| Boolean 프로퍼티 | `is`/`has`/`can`/`should` 접두사 | `isSyncing`, `hasIncontinence` |
| 상수 | `UPPER_SNAKE_CASE` | `DEFAULT_TIMEOUT_MS` |
| Room/DTO/유스케이스 접미사 | 역할을 드러내는 접미사 유지 | `VoidingEventEntity`, `UserE2eeKeyRemoteDto`, `AddVoidingEventUseCase` |
| 테스트 메서드 | 백틱으로 감싼 설명형 이름 권장 | ``fun `PIN 설정 후 검증 성공 시 잠금 해제 상태가 된다`()`` |

추가 규칙은 아래와 같다.

- 접두사 기반 헝가리언 노테이션은 사용하지 않는다.
- 메서드와 변수 이름은 축약보다 의미 전달을 우선한다.
- `PDF`, `PIN`, `E2EE`, `API`, `DTO`처럼 프로젝트에서 이미 굳은 약어는 기존 표기를 유지한다.
- Room entity, 원격 DTO, 도메인 모델은 이름만 비슷하게 두지 말고 역할이 드러나도록 구분한다.

### 2. import 규칙

- wildcard import는 사용하지 않는다.
- import는 Kotlin/Android Studio 기본 정렬을 따른다.
- alias import는 이름 충돌을 해소하거나 의미를 분명히 할 때만 사용한다.
- 사용하지 않는 import는 남기지 않는다.

### 3. 포맷 규칙

- 들여쓰기는 공백 4칸을 사용한다.
- 탭 문자는 사용하지 않는다.
- 한 줄 길이는 100자를 기준으로 보되, 가독성이 나빠지면 더 일찍 줄바꿈한다.
- 중괄호는 기본적으로 같은 줄에서 여는 스타일을 따른다.
- 파라미터나 인자 목록이 길어질 때는 의미 단위가 보이도록 줄바꿈한다.
- 체이닝 호출은 한 줄에 과도하게 몰아넣지 말고, 줄바꿈 시 호출 단위가 드러나게 정리한다.
- 저장소 전반에서 trailing comma를 기본 스타일로 쓰고 있지 않으므로 새 코드도 특별한 이유가 없으면 도입하지 않는다.
- 의미 없는 빈 줄, 과도한 정렬, 장식용 공백 맞춤은 피한다.

### 4. KDoc 및 주석 규칙

- KDoc과 코드 주석은 한국어로 작성한다.
- 문체는 `~한다`를 사용한다.
- 공개 API, 복잡한 도메인 규칙, 암호화/동기화처럼 맥락이 필요한 로직에만 설명을 추가한다.
- 코드만 읽어도 명확한 내용은 주석으로 반복하지 않는다.
- KDoc 태그를 쓸 때는 `@param`, `@return`, `@throws` 순서를 유지한다.

### 5. 계층 및 Android/Compose 규칙

- 패키지 책임은 `core`, `data`, `domain`, `export`, `presentation`, `ui`, `worker` 구조를 기준으로 유지한다.
- `presentation`은 화면 상태와 사용자 상호작용을 다루고, 저장소 구현 세부사항이나 SQL/HTTP 표현을 직접 노출하지 않는다.
- Room entity와 원격 DTO는 저장소 계층에서 도메인 모델로 변환하고, UI까지 직접 전달하지 않는다.
- 화면 상태와 비동기 작업의 진입점은 ViewModel에 두고, Composable은 상태 렌더링과 UI 이벤트 전달에 집중한다.
- `remember`와 `rememberSaveable`은 화면 내부의 일시적인 UI 상태에 한정해 사용한다.
- 공용 의존성을 새로 추가할 때는 `AppGraph`에서 초기화 위치와 생명주기를 함께 정리한다.
- 사용자에게 보이는 문구는 기존 앱 톤에 맞는 한국어 표현을 사용한다.

### 6. 테스트 코드 규칙

- 테스트 클래스 이름은 기본적으로 `*Test.kt`를 사용한다.
- 테스트 메서드 이름은 백틱을 사용한 한국어 문장형 이름을 권장한다.
- 테스트 본문은 가능하면 Given-When-Then 흐름이 읽히도록 정리한다.
- 코루틴 테스트는 `runTest`를 기본으로 사용하고, 메인 디스패처 개입이 있으면 `MainDispatcherRule` 같은 규칙 객체를 함께 사용한다.
- 테스트 더블은 `Fake`, `Stub`, `Test` 등 역할이 드러나는 이름을 사용한다.
- 성공/실패 케이스를 함께 보완해 회귀를 막는다.

## 확인 방법

코딩 규칙과 기본 동작을 함께 확인할 때는 아래 명령을 우선 사용한다.

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat spotlessApply
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

UI 또는 기기 의존 동작까지 확인해야 하면 아래 명령을 추가로 사용한다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```
