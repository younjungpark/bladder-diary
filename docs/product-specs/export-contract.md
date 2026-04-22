# Export Contract

## 목적

이 문서는 PDF 내보내기 기능의 입력과 결과 계약을 설명한다.

## 입력 조건

- 사용자는 시작일과 종료일을 선택한다.
- 필요하면 메모 포함 여부를 선택한다.
- 시작일은 종료일보다 늦을 수 없다.

## 결과 계약

- 선택 기간에 기록이 있으면 PDF 공유용 파일을 생성한다.
- 생성된 파일은 Android 공유 시트로 전달된다.
- 기록이 없으면 공유 파일을 만들지 않고 안내 메시지를 보여준다.
- 내보내기 실패 시 오류 메시지를 보여준다.

## 주의점

- 메모 포함 선택 시 메모 평문 또는 복호화 결과가 보고서에 포함될 수 있다.
- PDF는 개인 참고용 기록이며 의료적 진단을 대체하지 않는다.

## 관련 코드

- `presentation/main/MainViewModel.kt`
- `presentation/main/MainScreen.kt`
- `export/VoidingPdfExporter.kt`
- `export/AndroidVoidingPdfExporter.kt`
- `export/VoidingPdfReportBuilder.kt`
