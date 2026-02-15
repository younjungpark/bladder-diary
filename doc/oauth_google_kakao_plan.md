# BladderDiary OAuth 구현 계획 (Google + Kakao)

## 1) 요약
이번 구현 범위는 Google, Kakao OAuth만 포함합니다.  
기존 이메일/비밀번호 로그인은 유지하고, 소셜 로그인 버튼을 병행 제공하는 방식으로 설계합니다.

## 2) 목표와 성공 기준
- 목표: 로그인 편의성을 높이면서 기존 인증 흐름을 깨지 않고 확장
- 성공 기준
1. 기존 이메일 로그인/회원가입이 그대로 동작합니다.
2. Google/Kakao OAuth 로그인 성공 시 세션이 저장되고 메인 화면으로 전환됩니다.
3. 로그아웃 시 세션이 제거되고 인증 화면으로 복귀합니다.

## 3) 구현 범위
### In Scope
1. Android 앱 Google/Kakao OAuth 버튼 추가
2. OAuth 콜백(딥링크) 처리 추가
3. Auth 계층 인터페이스 확장
4. Supabase Provider 설정 반영
5. README 업데이트

### Out of Scope
1. Naver 로그인
2. 별도 OAuth 브리지 백엔드
3. 계정 병합 기능

## 4) 아키텍처 결정
1. 인증만 Supabase Auth SDK 도입 (OAuth 안정성 확보)
2. 기존 데이터 API(Ktor + Supabase REST)는 유지
3. 로그인 정책은 병행 운영(이메일 + 소셜)

## 5) 변경 설계
### 수정 파일
1. app/build.gradle.kts
2. app/src/main/AndroidManifest.xml
3. app/src/main/java/com/bladderdiary/app/domain/model/AuthRepository.kt
4. app/src/main/java/com/bladderdiary/app/data/repository/AuthRepositoryImpl.kt
5. app/src/main/java/com/bladderdiary/app/presentation/auth/AuthViewModel.kt
6. app/src/main/java/com/bladderdiary/app/presentation/auth/AuthScreen.kt
7. app/src/main/java/com/bladderdiary/app/MainActivity.kt
8. README.md

### 추가 파일
1. app/src/main/java/com/bladderdiary/app/domain/model/SocialProvider.kt (GOOGLE, KAKAO)
2. app/src/main/java/com/bladderdiary/app/data/remote/SupabaseAuthClient.kt (OAuth 전용)

## 6) 인터페이스/타입 변경
### AuthRepository
1. suspend fun signInWithSocial(provider: SocialProvider): Result<Unit>
2. suspend fun handleOAuthCallback(callbackUrl: String): Result<AuthResult>

### AuthUiState
1. isOAuthLoading: Boolean
2. pendingProvider: SocialProvider?
3. oauthErrorMessage: String?

## 7) UI/플로우
1. 인증 화면에 "Google로 계속", "카카오로 계속" 버튼 추가
2. 버튼 클릭 시 외부 브라우저(Custom Tabs)로 OAuth 시작
3. bladderdiary://auth/callback 딥링크로 앱 복귀
4. 콜백 처리 성공 시 세션 저장 후 메인 진입
5. 실패 시 에러 메시지 노출

## 8) Android/Manifest
1. MainActivity에 VIEW intent-filter 추가
2. 스킴/호스트: bladderdiary://auth/callback
3. onNewIntent에서 콜백 URL을 ViewModel로 전달

## 9) Supabase 설정
1. Auth > Providers에서 Google 활성화 및 Client ID/Secret 등록
2. Auth > Providers에서 Kakao 활성화 및 키 등록
3. Redirect URL에 bladderdiary://auth/callback 등록
4. 보안 원칙 유지: service_role 키 앱 미포함, RLS 유지

## 10) 테스트 케이스
### 단위 테스트
1. 이메일 로그인 회귀 테스트
2. Google/Kakao OAuth 시작 시 로딩 상태 전이
3. 콜백 성공/실패 분기 처리

### 통합/수동 테스트
1. Google OAuth 성공 후 메인 화면 전환
2. Kakao OAuth 성공 후 세션 유지
3. 취소/오류/네트워크 장애 시 예외 처리
4. 로그아웃 후 인증 화면 복귀

## 11) 문서 업데이트
README.md에 아래 추가
1. Google/Kakao 설정 절차
2. Redirect URL/딥링크 설정
3. 자주 발생하는 설정 오류와 점검 항목

## 12) 가정 및 기본값
1. 기존 minSdk/targetSdk 구조는 유지
2. OAuth 제공자는 Google/Kakao로 고정
3. DB 스키마/RLS 정책 변경은 불필요
