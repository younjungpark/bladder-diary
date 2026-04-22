# Security And Lock Flow

## 목적

이 문서는 로그인, 계정 전환 보호, PIN 잠금, E2EE 비밀문구 흐름을 설명한다.

## 핵심 구성요소

- `data/remote/SessionStore.kt`
- `data/remote/PinStore.kt`
- `data/repository/AuthRepositoryImpl.kt`
- `data/repository/LockRepositoryImpl.kt`
- `data/repository/E2eeRepositoryImpl.kt`
- `data/security/PinCrypto.kt`
- `data/security/MemoCrypto.kt`
- `presentation/auth/`
- `presentation/pin/`
- `presentation/e2ee/`

## 기본 흐름

1. 사용자는 Google 또는 Kakao로 로그인한다.
2. 세션 정보와 remembered account가 로컬에 저장된다.
3. 기존 계정이 남아 있으면 account switch guard가 다른 계정 로그인 전 명시적 허용을 요구한다.
4. PIN이 설정되어 있으면 앱 잠금 해제에 로컬 PIN 검증이 필요하다.
5. E2EE가 켜져 있으면 메모 평문은 업로드 전에 암호화되고, 읽을 때 로컬에서 복호화된다.

## 설계 메모

- PIN은 로컬 잠금 기능이며, 원격 인증 정보와 별개다.
- E2EE는 선택 기능이며 사용자가 직접 비밀문구를 관리한다.
- 비밀문구나 서명 정보는 저장소에 커밋하지 않는다.

## 검증 포인트

- 계정 전환 보호가 의도대로 동작하는지
- PIN 오입력 잠금과 해제 흐름이 정상인지
- E2EE 활성화 후 메모 재업로드와 복호화가 정상인지
