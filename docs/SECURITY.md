# SECURITY

## 보안의 핵심 축

- Google / Kakao 소셜 로그인 세션 관리
- 계정 전환 가드와 remembered account 처리
- 사용자별 4자리 PIN 잠금
- 메모 선택형 E2EE 비밀문구와 로컬 복호화 키 보관
- 민감 건강정보 및 Supabase 클라우드 저장에 대한 앱 내 고지
- `local.properties` 기반 비밀값 관리
- 개인정보 및 의료 고지 문서와의 정합성 유지

## 먼저 볼 문서

1. `../README.md`
2. `design-docs/security-and-lock-flow.md`
3. `product-specs/configuration-reference.md`
4. `product-specs/oauth-configuration.md`
5. `../PRIVACY_POLICY.md`
6. `../MEDICAL_DISCLAIMER.md`

## 현재 저장소 안의 관련 코드

- `app/src/main/java/com/bladderdiary/app/data/remote/SessionStore.kt`
- `app/src/main/java/com/bladderdiary/app/data/remote/PinStore.kt`
- `app/src/main/java/com/bladderdiary/app/data/remote/SupabaseAuthClient.kt`
- `app/src/main/java/com/bladderdiary/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/main/java/com/bladderdiary/app/data/repository/LockRepositoryImpl.kt`
- `app/src/main/java/com/bladderdiary/app/data/repository/E2eeRepositoryImpl.kt`
- `app/src/main/java/com/bladderdiary/app/data/security/`
- `app/src/main/java/com/bladderdiary/app/presentation/auth/`
- `app/src/main/java/com/bladderdiary/app/presentation/pin/`
- `app/src/main/java/com/bladderdiary/app/presentation/e2ee/`

## 문서 갱신 규칙

- 로그인 제공자, 콜백 URI, 세션 저장 방식이 바뀌면 `product-specs/oauth-configuration.md`와 이 문서를 함께 갱신한다.
- PIN 정책, 잠금 해제 흐름, 계정 전환 보호 로직이 바뀌면 설계 문서와 테스트 범위를 함께 갱신한다.
- E2EE 비밀문구, 원격 키 저장, 로컬 키 보관 방식이 바뀌면 관련 설계 문서와 사용자-facing 고지 문구를 함께 점검한다.
- 비밀값 저장 위치나 릴리즈 서명 방식이 바뀌면 `product-specs/configuration-reference.md`도 함께 갱신한다.

## 현재 방향

- 소셜 로그인은 Supabase Auth Providers를 통해 Google/Kakao를 연동한다.
- 로그인 전에는 배뇨 기록의 민감정보 성격과 Supabase 클라우드 저장 사실을 안내하고, 기존 로그인 사용자는 안내 버전별 1회 확인을 거친다.
- PIN은 로컬 잠금 기능이며, 계정 비밀번호를 대체하지 않는다.
- 메모 암호화는 선택 기능이며, 비밀문구를 잊어버리면 복구가 불가능하다.
- 회원탈퇴는 클라이언트에 `service_role` 키를 두지 않고, 운영자 확인용 요청 기록을 남긴 뒤 앱 데이터만 사용자 권한으로 삭제한다.
- `local.properties`와 개인 SDK/서명 정보는 버전 관리에 포함하지 않는다.
