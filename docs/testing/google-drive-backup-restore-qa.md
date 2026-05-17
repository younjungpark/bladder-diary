# Google Drive 백업/복원 QA

## 목적

이 문서는 BLA-13 기준 Google Drive 암호화 백업/복원 기능의 문서화된 QA 기록이다. BLA-4와 BLA-8에서 구현한 엔진, UX, 자동 백업, 수동 `.bdbackup` fallback이 Supabase 동기화와 분리되어 동작한다는 점을 릴리즈 전 확인 항목으로 남긴다.

## 전제 조건

- Google Cloud Console에서 Drive API가 사용 설정되어 있어야 한다.
- Google 인증 플랫폼에 Android OAuth 클라이언트가 등록되어 있어야 한다.
- 디버그 APK와 릴리즈 APK를 모두 실기기에서 검증하려면 각각의 서명 인증서 SHA-1을 Android OAuth 클라이언트로 등록한다.
- OAuth 앱 게시 상태가 Testing이면 테스트에 사용할 Google 계정을 Audience 테스트 사용자에 추가한다.
- 앱은 `https://www.googleapis.com/auth/drive.appdata` scope만 요청한다.
- Google Drive 앱 설치는 필수가 아니지만 Google Play services, Google 계정, 네트워크 연결은 필요할 수 있다.

## 수동 QA 기록

2026-05-17 실기기 릴리즈 APK 기준으로 아래 흐름을 확인했다.

| 항목 | 결과 |
| --- | --- |
| Google Drive 권한 | 계정 선택과 `drive.appdata` 권한 동의 후 백업 진행 가능 |
| 디버그 OAuth 등록 | 디버그 SHA-1 Android OAuth 클라이언트 등록 후 권한 차단 해소 |
| 릴리즈 OAuth 등록 | 릴리즈 SHA-1 Android OAuth 클라이언트 등록 후 릴리즈 APK에서 백업 성공 |
| 테스트 사용자 | OAuth 앱 Testing 상태에서 테스트 Google 계정 등록 후 접근 가능 |
| 지금 Google Drive 백업 | `백업 및 복원` 화면의 마지막 성공 시각 갱신 확인 |
| Google Drive에서 복원 | 백업 비밀번호 입력 후 복원 미리보기와 확정 흐름 확인 |
| 병합/교체 복원 | 기본 `백업과 합치기`와 `백업으로 교체`의 사용자-facing 차이 확인 |
| 자동 백업 | 자동 백업 켜기/끄기와 30분 지연 예약 정책 확인 |
| 수동 파일 fallback | `.bdbackup` 파일 내보내기/가져오기 동작 확인 |
| Supabase 동기화 분리 | 클라우드 동기화 ON/OFF와 Google Drive 백업/복원 기능이 별도 흐름임을 확인 |
| 상단 상태 아이콘 | Supabase 동기화와 Drive 자동 백업 아이콘이 활성 상태 위주로 표시되는지 확인 |

## 실패 경로와 문서화 기준

- Google 계정이 테스트 사용자에 없으면 OAuth 앱 Testing 상태에서 접근이 차단될 수 있다.
- `UNREGISTERED_ON_API_CONSOLE`은 현재 설치된 APK의 패키지명과 SHA-1 조합이 Android OAuth 클라이언트에 등록되지 않은 상태로 본다.
- 권한 거부, Google Play services 사용 불가, 네트워크 실패는 앱 메시지와 마지막 백업 오류 상태로 노출한다.
- 백업 비밀번호 오류, 손상 파일, 잘못된 백업 포맷은 복원 실패로 처리하고 로컬 DB를 변경하지 않아야 한다.
- Google Drive를 사용할 수 없거나 권한을 거부한 사용자는 같은 암호화 envelope를 쓰는 `.bdbackup` 수동 내보내기/가져오기 경로를 사용한다.

## 자동 테스트 커버리지

- `BackupEnvelopeFactoryTest`
  백업 envelope 왕복 복호화, 민감 기록 평문 노출 방지, 잘못된 비밀번호/손상 파일 실패를 검증한다.
- `BackupEngineTest`
  fake Drive client 기반 업로드/복원, 저장된 로컬 DEK 누락 시 자동 백업 실패를 검증한다.
- `BackupRestorePlannerTest`
  병합/교체 복원, tombstone 유지, 중복 백업 기록 정리를 검증한다.
- `MainViewModelTest`
  Drive 백업 성공 메시지, 수동 백업 파일 생성, 복원 미리보기와 확정 메시지를 검증한다.
- `MainScreenSharedTest`
  Supabase 동기화와 Google Drive 자동 백업 상태 아이콘 표시 정책을 검증한다.

## 릴리즈 전 확인 명령

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
```

실기기 릴리즈 APK 검증이 필요하면 기존 설치 앱의 서명과 맞지 않을 수 있으므로 기존 앱 제거 후 설치한다.

```powershell
adb uninstall com.chausoft.bladderdiary
adb install app\build\outputs\apk\release\app-release.apk
```

## 개인정보 고지 체크

- Google Drive 백업은 Supabase 동기화와 다른 선택 기능이다.
- 백업 파일은 앱에서 백업 전용 비밀번호로 암호화된 뒤 저장된다.
- 백업 파일명과 암호화 전 manifest에는 기록 수, 날짜 범위, 메모, 배뇨량, 절박감, 요실금 여부 같은 민감 건강정보를 넣지 않는다.
- 백업 파일에는 Supabase token, PIN, WorkManager 상태, `sync_queue`를 포함하지 않는다.
- 회원탈퇴는 로컬 백업 설정과 자동 백업 키를 초기화하지만, 이미 Google Drive `appDataFolder`에 저장된 암호화 백업 파일이나 사용자가 내보낸 `.bdbackup` 파일은 별도 저장소에 남을 수 있다.
