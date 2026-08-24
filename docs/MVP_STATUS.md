# MindOFF MVP 구현 상태

상태 기준:

- **Implemented**: 로컬 또는 Preview Data 모드에서 검증 가능한 기능
- **In Progress**: 코드가 일부 준비됐지만 실제 서비스 흐름이 닫히지 않은 기능
- **Planned**: 아직 구현되지 않은 기능

## 제품 기능

| 영역 | 상태 | 현재 범위 | 다음 완료 조건 |
| --- | --- | --- | --- |
| Account | In Progress | MindOFF 자체 가입·이메일 인증·로그인·재설정 화면, Cognito JWT, 토큰 갱신·로그아웃, 실제 가입·로그인·로그아웃 확인 | 토큰 갱신 운영 검증 |
| Household | In Progress | 자동 생성, 구성원 조회, 초대 링크 생성·공유·수락·상태 확인 | 초대 이메일 전달 |
| Receipt | In Progress | 촬영·실제 S3 업로드, OCR 초안, 전체 필드 검토·분류·확정 | Textract 계정 활성화 후 검증 |
| Refrigerator | Implemented | 등록, 구매일, 유통기한, 임박 표시, 다 먹음, Need 재구매 | `아직 있어요` 상태 보정 |
| Household Items | Implemented | 등록, 다 씀, 사용기간, 가중평균 예측, 예상일, `아직 있어요` 보정 | 예측 정확도 데이터 축적 |
| Subscription | Implemented | 월·연 결제, 월 환산 금액, 체험 종료, 관리 URL, Household 공유, 수정·삭제 | 알림 연동 |
| Need List | Implemented | 자동/수동 추가, 구매 링크, 상세값 편집, 냉장고·생활용품 재구매 | 구매처 추천 연동 |
| Home | Implemented | 확인 목록, Need 수, 고정비, 이번 달 영수증 금액, 화면 행동 연결 | 구매 품목 요약 확장 |
| Push | Planned | 없음 | 임박·예상 소진·체험 종료 알림 |
| Widget | Planned | 없음 | 모바일 위젯 |

## AWS

| 영역 | 상태 | 비고 |
| --- | --- | --- |
| Cognito | In Progress | User Pool과 직접 인증 API 배포, 실제 이메일 인증·로그인·로그아웃 확인 | 토큰 갱신 운영 검증 |
| S3 | Implemented | 실제 비공개·암호화 영수증 업로드 확인 | 보존 정책 결정 |
| Textract | In Progress | `AnalyzeExpense` 호출 확인, AWS Free 계정 플랜에서 서비스 활성화 제한 | Paid 플랜 전환 여부 결정 후 재검증 |
| ECR/ECS/RDS | Implemented | 서울 리전 운영 스택 배포, DB 마이그레이션, ECS steady state, CloudFront HTTPS readiness 검증 완료 |
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
- [x] Household 초대 링크 진입·수락·만료 처리
- [x] 영수증 촬영과 OCR 전체 필드 검토
- [x] Need 구매 완료 후 냉장고·생활용품 재등록
- [x] 생활용품 예상 소진일과 `아직 있어요` 보정
- [x] 구독 월·연 결제와 Household 공유
- [x] 홈 지표·확인 항목 행동 연결
- [x] CloudFormation·배포 스크립트·운영 컨테이너 사전 검증
- [x] Vercel 운영 빌드와 AWS API·Cognito 연결
- [x] 실제 Cognito 가입·로그인 검증
- [x] 실제 Cognito 로그아웃 검증
- [ ] 실제 S3·Textract 영수증 검증
- [x] 실제 S3 영수증 업로드 검증
- [ ] Household 초대 이메일/딥링크
- [ ] EventBridge 기반 확인 작업
- [ ] 모바일 Push
- [x] ECS/RDS 운영 배포

마지막 업데이트: 2026-08-24
