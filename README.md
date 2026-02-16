# BladderDiary

Supabase + Room 기반 BladderDiary 안드로이드 MVP입니다.

## 핵심 기능
- 이메일 회원가입/로그인
- 1탭 배뇨 기록 저장(현재 시각)
- 날짜별 기록 목록/총 횟수 확인
- 기록 삭제
- 오프라인 로컬 저장 후 온라인 자동 동기화(WorkManager)

## 개발 환경
- Android Studio (Giraffe 이상 권장)
- JDK 17
- Android SDK 34

## Supabase 설정
1. Supabase 프로젝트 생성
2. SQL Editor에서 `supabase/sql/001_init.sql` 실행
3. Authentication에서 Email provider 활성화
4. Authentication > Providers에서 Google, Kakao provider 활성화 및 앱 키 등록
5. Authentication > URL Configuration에서 Redirect URL에 `bladderdiary://auth/callback` 추가
6. 프로젝트 루트 `local.properties`에 아래 값 추가

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
- 삭제는 소프트 삭제(`deleted_at`)로 원격 반영됩니다.
