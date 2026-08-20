# MindOFF MVP 구현 상태

상태 기준:

- **Implemented**: 로컬 또는 Preview Data 모드에서 검증 가능한 기능
- **In Progress**: 코드가 일부 준비됐지만 실제 서비스 흐름이 닫히지 않은 기능
- **Planned**: 아직 구현되지 않은 기능

## 제품 기능

| 영역 | 상태 | 현재 범위 | 다음 완료 조건 |
| --- | --- | --- | --- |
| Account | In Progress | Cognito PKCE 로그인, JWT 검증, 토큰 보관·갱신·로그아웃 코드 | 실제 Cognito 연결 및 가입·로그아웃 검증 |
| Household | In Progress | 자동 생성, 구성원 조회, 초대 링크 생성·공유·상태 확인, 수락 모델 | 초대 이메일과 링크 자동 수락 |
| Receipt | In Progress | 이미지 업로드, OCR 초안, 검토·분류·확정 | 카메라 촬영, 전체 필드 수정, 실제 Textract 검증 |
| Refrigerator | Implemented | 등록, 구매일, 유통기한, 임박 표시, 다 먹음 | `아직 있어요`와 구매목록 선택 UI |
| Household Items | In Progress | 등록, 다 씀, 사용기간, 가중평균 예측 | 상태 보정 응답과 예측일 UI |
| Subscription | In Progress | 기본 등록, 월 금액, 체험 종료일, 관리 URL | 연/월 선택과 Household 공유 |
| Need List | In Progress | 자동/수동 추가, 구매 링크, 구매 완료 | 이전 구매 상세값 편집 및 냉장고 재구매 흐름 |
| Home | Implemented | 확인 목록, Need 수, 기록된 고정비, 이번 달 영수증 금액 | 구매 품목 요약과 행동 연결 |
| Push | Planned | 없음 | 임박·예상 소진·체험 종료 알림 |
| Widget | Planned | 없음 | 모바일 위젯 |

## AWS

| 영역 | 상태 | 비고 |
| --- | --- | --- |
| Cognito | In Progress | CloudFormation과 앱 연동 코드만 준비 |
| S3 | In Progress | 비공개 버킷 정의와 업로드 어댑터만 준비 |
| Textract | In Progress | `AnalyzeExpense` 어댑터만 준비, 실계정 미검증 |
| ECR/ECS/RDS | In Progress | 배포 스택과 컨테이너 준비, 실제 자원 없음 |
| Lambda | Planned | OCR 처리 방식 확정 후 구현 |
| EventBridge Scheduler | Planned | 시간 기반 확인 작업 |
| Push/SNS | Planned | 모바일 Push 전달 방식 확정 필요 |
| SQS | Planned | 처리량·Retry·DLQ 필요 시에만 도입 |

## MVP 체크리스트

- [x] 로컬 PostgreSQL과 Spring Boot API
- [x] Vercel Preview Data 모드
- [x] 영수증 검토 후 냉장고/생활용품 반영
- [x] 생활용품 사용주기 기록과 단순 예측
- [x] 구매 완료 후 생활용품 새 주기 시작
- [x] 이번 달 MindOFF 기록 금액 집계
- [x] Cognito 토큰 갱신과 로컬 로그아웃
- [x] Household 초대 생성·수락 데이터 모델
- [x] Household 구성원·초대 링크 관리 화면
- [ ] 실제 Cognito 가입·로그인 검증
- [ ] 실제 S3·Textract 영수증 검증
- [ ] Household 초대 이메일/딥링크
- [ ] EventBridge 기반 확인 작업
- [ ] 모바일 Push
- [ ] ECS/RDS 운영 배포

마지막 업데이트: 2026-08-20
