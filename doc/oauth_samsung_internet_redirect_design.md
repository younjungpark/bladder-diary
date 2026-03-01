# 삼성 인터넷 OAuth 리다이렉트 안정화 및 확인창 최소화 설계안

작성일: 2026-03-01  
프로젝트: BladderDiary (Android + Supabase Auth)

## 1) 요약
삼성 인터넷에서 OAuth 복귀 실패를 안정화하기 위해, 현재는 도메인 없이 가능한 커스텀 스킴 + `singleTask` 방식을 적용합니다.

핵심 결정사항:
1. 목표: 삼성 인터넷에서 OAuth 리다이렉트 성공률 확보
2. 적용 해법: `MainActivity` `launchMode=singleTask` + 커스텀 스킴 유지
3. 중기 해법: 도메인 확보 후 App Links(`https` + `autoVerify` + `assetlinks.json`) 전환 검토

## 2) 범위
### In Scope
1. 삼성 인터넷 OAuth 복귀 실패 재현/원인 경로 명시
2. `singleTask` 기반 리다이렉트 안정화 설계
3. OAuth 브라우저/콜백 폴백 전략 설계
4. App Links 전환 사전 점검 항목 설계
5. `doc` 폴더 설계 문서 작성

### Out of Scope
1. Supabase/Auth Provider 자체 변경 작업
2. iOS 대응
3. OAuth 제공자 추가(Naver 등)

## 3) 구현 설계 (결정 완료)

### 3.1 1차 안정화: 리다이렉트 성공률 확보
대상 파일:
1. `app/src/main/AndroidManifest.xml`
2. `app/src/main/java/com/bladderdiary/app/MainActivity.kt`
3. `app/src/main/java/com/bladderdiary/app/data/repository/AuthRepositoryImpl.kt`
4. `app/src/main/java/com/bladderdiary/app/presentation/auth/AuthViewModel.kt` (필요 시 상태/에러 메시지 보강)

핵심 설계:
1. `MainActivity`에 `android:launchMode="singleTask"` 적용
2. 딥링크 인텐트 필터는 기존 커스텀 스킴(`bladderdiary://auth/callback`) 유지
3. `onNewIntent`/`onCreate` 콜백 중복 처리 방지
   - 최근 처리한 callback URL 또는 `state` 기준 dedupe(짧은 TTL)
4. OAuth 시작 시 브라우저 실행 실패/미지원 케이스 명시적 예외 처리
5. 사용자 메시지 개선
   - "브라우저에서 인증 후 앱으로 돌아오세요"
   - 실패 시 "기본 브라우저 변경/재시도" 안내

### 3.2 2차 개선: 확인창 최소화(App Links)
사전 점검 항목:
1. 운영 가능한 HTTPS 도메인 보유 여부
2. `/.well-known/assetlinks.json` 배포 가능 여부
3. 릴리즈 keystore SHA-256 확정 여부
4. Supabase `redirect_to`를 HTTPS App Link로 전환 가능한지 검증

점검 통과 시 설계:
1. App Link 인텐트 필터 추가(`https`, host/path, `android:autoVerify="true"`)
2. `assetlinks.json` 배포
3. OAuth `redirect_to`를 App Link URL로 변경
4. 커스텀 스킴은 호환성 fallback으로 일정 기간 병행 유지

점검 미통과 시 기본값:
1. 1차 안정화안(singleTask + UX 가이드) 유지
2. 확인창은 브라우저 보안 정책으로 수용
3. App Links는 후속 인프라 과제로 분리

## 4) 인터페이스/동작 계약 변경
외부 API 변경은 없고, 앱 내부 동작 계약을 강화합니다.

1. `MainActivity` 딥링크 수신 계약 강화
   - 동일 콜백 중복 처리 방지 규칙 추가
2. `AuthRepository.signInWithSocial()` 에러 표준화
   - 브라우저 실행 실패/지원 불가를 구분 가능한 메시지로 표준화

## 5) 테스트 케이스 및 시나리오

### 5.1 기능 검증
1. 삼성 인터넷 기본 브라우저 환경에서 Google OAuth 복귀 성공
2. 삼성 인터넷 기본 브라우저 환경에서 Kakao OAuth 복귀 성공
3. Chrome 기본 브라우저 환경 회귀(기존 성공 경로 유지)
4. OAuth 취소 시 로딩 상태 해제 및 오류 메시지 정상 노출
5. 동일 콜백 중복 전달 시 세션 저장 1회만 수행

### 5.2 회귀 검증
1. 이메일/비밀번호 로그인 정상
2. 로그아웃 후 인증 화면 복귀 정상
3. 앱 백그라운드/포그라운드 전환 시 PIN/네비게이션 흐름 정상

### 5.3 확인창 최소화 검증(App Links 적용 시)
1. `adb shell am start -a android.intent.action.VIEW -d "<https app link>"`로 앱 직접 매칭 확인
2. `autoVerify` 성공 여부(`adb shell dumpsys package domain-preferred-apps`) 확인
3. 주요 브라우저(Chrome, Samsung Internet)에서 확인창 발생 빈도 비교

## 6) 산출 문서
본 문서: `doc/oauth_samsung_internet_redirect_design.md`

## 7) 가정 및 기본값
1. 현재 도메인/`assetlinks.json` 배포 가능 여부는 미확정
2. 기본 실행안은 1차 안정화(singleTask 중심)
3. App Links 전환은 사전 점검 결과가 "가능"일 때만 2차로 진행
4. 앱 실행 확인창은 단기적으로 일부 단말에서 허용

## 8) 이번 반영 항목 (코드 기준)
1. `MainActivity`에 `launchMode="singleTask"` 적용
2. 커스텀 스킴(`bladderdiary://auth/callback`) 유지
3. `MainActivity` 콜백 수신 로직에 중복 콜백 dedupe 추가
4. `local.properties.example`을 커스텀 스킴 기준으로 정리

## 9) App Links 전환 체크리스트 (향후)
1. 운영 도메인에 `https://<APP_LINK_HOST>/.well-known/assetlinks.json` 배포
2. `assetlinks.json`에 릴리즈 SHA-256 지문 반영
3. Supabase Redirect URL을 `https://<APP_LINK_HOST><APP_LINK_PATH_PREFIX>`로 등록
4. 실제 기기에서 `autoVerify` 성공 여부 확인

`assetlinks.json` 예시:
```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.bladderdiary.app",
      "sha256_cert_fingerprints": [
        "RELEASE_CERT_SHA256_FINGERPRINT"
      ]
    }
  }
]
```
