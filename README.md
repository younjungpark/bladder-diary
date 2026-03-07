# BladderDiary

Supabase와 Room을 기반으로 구축된 BladderDiary 안드로이드 프로젝트입니다.

## 핵심 기능
- 소셜 회원가입/로그인 (Google, Kakao 지원)
- PIN 번호 설정 및 앱 실행 시 보안 잠금 유지
- 단 1번의 터치로 배뇨 기록 저장 (현재 시각 기준 저장)
- 날짜별 배뇨 기록 상세 조회, 일일 총 횟수 확인 및 각 기록별 메모 작성/수정
- 메모에 민감한 개인 정보가 포함될 경우를 대비한 선택형 종단간 암호화(E2EE) 비밀문구 설정/변경 지원
- 배뇨 기록 삭제 기능 (소프트 삭제 처리로 안전하게 보관 및 원격 반영)
- 오프라인 환경 로컬 저장 (Room) 및 온라인 연결 시 자동 전체 동기화 (WorkManager)
- 최신 안드로이드 표준에 맞춘 깔끔한 UI/UX (스플래시 화면, Adaptive Icon 적용 등)

## 개발 환경
- Android Studio (Giraffe 이상 권장)
- JDK 17
- Android SDK 34

## Supabase 설정
1. Supabase 프로젝트 생성
2. SQL Editor에서 `supabase/sql/001_init.sql` 실행
3. Authentication > Providers에서 Google, Kakao provider 활성화 및 앱 키 등록
4. Authentication > URL Configuration에서 Redirect URL에 `bladderdiary://auth/callback` 추가
5. 프로젝트 루트 `local.properties`에 아래 앱 연동 필수 키값 추가

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
SUPABASE_REDIRECT_URI=bladderdiary://auth/callback
```

## 실행
1. Android Studio로 프로젝트 열기
2. Gradle Sync
3. 에뮬레이터/실기기에서 `app` 실행

## 참고
- 일별 기준은 **기기 로컬 타임존 자정**입니다.
- 메모 암호화는 선택 기능이며, 비밀문구를 설정하면 메모를 종단간 암호화(E2EE)하여 서버에 저장할 수 있습니다.
- 비밀문구를 설정하지 않은 메모는 평문으로 동기화됩니다.
- 삭제는 소프트 삭제(`deleted_at`)로 원격 반영됩니다.

## 라이선스
- 이 프로젝트는 `Apache License 2.0`으로 공개됩니다.
- 자세한 내용은 [LICENSE](/c:/work/BladderDiary/LICENSE) 파일을 참고하세요.
- 단, `BladderDiary` 이름, 로고, 아이콘 등 브랜드 자산은 오픈소스 라이선스에 포함되지 않습니다.
- 브랜드 사용 기준은 [TRADEMARK_POLICY.md](/c:/work/BladderDiary/TRADEMARK_POLICY.md)를 참고하세요.

## 배포 문서
- 개인정보 처리방침: [PRIVACY_POLICY.md](/c:/work/BladderDiary/PRIVACY_POLICY.md)
- 의료 비고지 안내: [MEDICAL_DISCLAIMER.md](/c:/work/BladderDiary/MEDICAL_DISCLAIMER.md)
