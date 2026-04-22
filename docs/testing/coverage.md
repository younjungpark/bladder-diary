# Coverage

## 현재 운영 방향

- 현재 저장소는 JaCoCo 같은 전용 coverage 리포트를 기본 하네스로 두고 있지 않다.
- 대신 변경한 동작에 맞는 단위 테스트와 수동 smoke 검증을 함께 요구한다.
- coverage 숫자보다 변경 위험을 줄이는 테스트 배치에 우선순위를 둔다.

## 현재 테스트가 다루는 영역

- `data/remote/`
  provider 해석과 세션 주변 로직
- `data/repository/`
  계정 전환 보호, PIN 저장소 동작
- `data/security/`
  암호화 유틸
- `export/`
  PDF 보고서 생성
- `presentation/`
  Auth, PIN, E2EE, Main 화면 ViewModel과 시간 상태 계산

## 테스트 추가 기준

- 저장소 계층 동작이 바뀌면 repository 또는 usecase 테스트를 추가한다.
- ViewModel 상태 전이, 에러 메시지, 이벤트 소비 방식이 바뀌면 presentation 테스트를 추가한다.
- 암호화, 날짜 계산, PDF 집계 같은 계산 로직이 바뀌면 순수 함수 테스트를 우선 추가한다.
- UI 세부 배치만 바뀌고 로직 변화가 없으면 수동 smoke와 스크린샷 확인으로 충분할 수 있다.

## 기본 명령

```powershell
.\gradlew.bat :app:testDebugUnitTest
```
