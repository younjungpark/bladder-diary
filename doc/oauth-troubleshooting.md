# OAuth 트러블슈팅 기록 (Google + Kakao)

작성일: 2026-02-15  
프로젝트: BladderDiary (Android + Supabase Auth)

## 목적
- OAuth 연동 중 발생했던 이슈를 재발 방지용으로 기록합니다.
- 공개용 `README.md`에는 요약만 두고, 상세 내역은 본 문서에 보관합니다.

## 공통 설정 기준
1. Supabase `Authentication > URL Configuration > Redirect URLs`
- `bladderdiary://auth/callback`
2. 앱 로컬 설정 (`local.properties`)
- `SUPABASE_URL=https://zakbgkytvziozyzqmzxr.supabase.co`
- `SUPABASE_ANON_KEY=...`
- `SUPABASE_REDIRECT_URI=bladderdiary://auth/callback`
3. Android 딥링크 인텐트 필터
- `bladderdiary://auth/callback` 수신 가능해야 함

## Google OAuth

### 주요 증상
1. 에뮬레이터에서 로그인 차단
- `This browser or app may not be secure`
2. 인증 후 앱 복귀 실패
- `:3000/?error=invalid_request` 형태로 이동
3. 앱 복귀 후 실패
- `OAuth state parameter missing`
- Supabase Auth 로그 `/callback | 400: OAuth state parameter missing`

### 원인
1. 에뮬레이터 브라우저/Play 서비스 환경 편차
2. Google Cloud와 Supabase Redirect URI 설정 불일치 가능성
3. `/auth/v1/authorize` 요청에 `response_type=token` 임의 추가로 state 처리 충돌
4. 앱 복귀 시 콜백 전달 타이밍 이슈

### 적용한 해결
1. Google Cloud OAuth 클라이언트(웹) Redirect URI를 아래로 고정
- `https://zakbgkytvziozyzqmzxr.supabase.co/auth/v1/callback`
2. Supabase Redirect URL은 앱 딥링크로 유지
- `bladderdiary://auth/callback`
3. authorize URL에서 `response_type=token` 제거
4. 앱 콜백 전달을 `Channel(BUFFERED)` 기반으로 유지
5. 최종 검증은 실기기 + Chrome Custom Tab 기준으로 진행

### 최종 확인
1. Google 로그인 성공
2. 앱 재실행 시 세션 유지 정상
3. 로그아웃 후 인증 화면 복귀 정상
4. 재로그인 정상

## Kakao OAuth (KOE205)

### 주요 증상
1. 카카오 계정 선택까지 진행되나 마지막에 `KOE205`
2. 상세 오류에 `설정하지 않은 동의 항목` 표시
- 예: `profile_image`, `profile_nickname`

### 원인
1. 요청한 scope와 카카오 동의항목 설정 불일치
2. 초기에는 `account_email`만 설정하고 `profile_nickname`/`profile_image`는 미설정

### 적용한 해결
1. 앱을 개인 개발자 비즈 앱으로 전환
2. 카카오 로그인 사용 설정 `ON` 확인
3. 카카오 로그인 > 동의항목에서 아래 항목 설정
- `account_email`: 필수 동의
- `profile_nickname`: 선택 동의
- `profile_image`: 선택 동의
4. Kakao 앱 아이콘 등록
- `128x128 PNG` 업로드

### 최종 확인
1. Kakao 로그인 성공
2. 동의 완료 후 앱 복귀 정상
3. 세션 생성 및 로그인 상태 유지 정상

## 재발 방지 체크리스트
1. Supabase Provider scope와 각 OAuth 콘솔 동의항목을 항상 1:1로 맞춘다.
2. Redirect URI는 Provider 콘솔과 Supabase 양쪽에서 정확히 일치시킨다.
3. OAuth authorize URL에 임의 파라미터를 추가하기 전에 state/redirect 영향도를 검토한다.
4. 최종 판정은 실기기 기준으로 수행하고, 앱 로그와 Supabase Auth 로그를 함께 확인한다.
