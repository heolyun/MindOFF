# MindOFF

> 내가 기억하지 않아도 되는 생활

MindOFF는 영수증·냉장고·생활용품·구독처럼 잊기 쉬운 생활 정보를 대신 기억하고, 확인할 가치가 있는 순간에만 다음 행동을 제안하는 생활형 개인비서입니다.

## 현재 구현 범위

현재 제품은 로컬 API와 Vercel Preview Data 모드에서 핵심 흐름을 검증하는 MVP 초안입니다.

- Expo TypeScript 앱, Spring Boot API, PostgreSQL, Docker Compose
- 냉장고·생활용품·Need List·구독 기본 흐름
- Household 구성원 조회와 초대 링크 생성·공유·수락
- 영수증 촬영·이미지 접수, OCR 전체 필드 검토·확정, 품목 자동 반영
- Need 구매 완료 후 냉장고·생활용품 재등록
- 사용주기 기록, 최근 기록 가중 예측, 구매 완료 후 새 주기 시작
- 생활용품 예상 소진일 보정과 구독 월·연 결제·Household 공유
- 유통기한·사용주기·무료체험 Attention과 홈 요약
- Cognito JWT·토큰 갱신·로그아웃 코드
- S3·Textract 어댑터와 ECS/RDS 배포 템플릿

Cognito, S3, Textract, ECS, RDS 등 AWS 자원은 아직 생성되지 않았습니다. 로컬 기본 설정은 샘플 OCR을 사용합니다. 상세 상태는 [MVP 구현 상태](docs/MVP_STATUS.md)를 기준으로 관리합니다.

## 구성

```text
mobile (Expo / React Native)
        │ REST
backend (Spring Boot)
        │ JPA + Flyway
PostgreSQL
```

백엔드는 Household 공유 데이터와 개인 구독 데이터를 분리합니다. 모바일은 앱 시작 시 개발용 사용자와 Household를 한 번 부트스트랩하고 이후 실제 API 데이터를 사용합니다.

## 로컬 실행

### 1. PostgreSQL

```powershell
docker compose up -d postgres
```

### 2. Spring Boot API

```powershell
cd backend
.\gradlew.bat bootRun
```

API 상태는 `http://localhost:8080/actuator/health`에서 확인할 수 있습니다.

### 3. Expo 앱

```powershell
cd mobile
pnpm install
pnpm start
```

- iOS 시뮬레이터와 웹은 기본적으로 `http://localhost:8080`을 사용합니다.
- Android 에뮬레이터는 기본적으로 `http://10.0.2.2:8080`을 사용합니다.
- 실제 기기에서는 `mobile/.env`에 `EXPO_PUBLIC_API_URL=http://<개발-PC-IP>:8080`을 설정합니다.

## 검증

```powershell
cd backend
.\gradlew.bat test

cd ..\mobile
pnpm typecheck
```

백엔드 통합 테스트는 사용자·Household 부트스트랩, 냉장고 임박 항목, 생활용품 사용 완료, Need List 자동 추가, 구독 금액과 홈 요약 계산을 확인합니다.

`.github/workflows/ci.yml`은 push와 pull request마다 백엔드 테스트, 모바일 타입 검사, Vercel용 웹 빌드를 실행합니다.

## Vercel 미리보기

배포 주소: https://mindoff-project-preview.vercel.app

Vercel 배포는 백엔드 없이도 화면과 상호작용을 확인할 수 있도록 브라우저 저장소 기반 Preview Data 모드로 빌드됩니다. 냉장고·생활용품·Need List·구독 변경 내용은 같은 브라우저에 저장됩니다.

```powershell
cd mobile
pnpm build:web:preview
```

실제 Spring Boot API를 사용하는 모바일 개발 모드는 기존과 동일하며 `EXPO_PUBLIC_DEMO_MODE`를 설정하지 않습니다. `vercel.json`은 pnpm 버전을 고정하고 Preview Data 모드 빌드와 `mobile/dist` 정적 배포를 구성합니다.

## 주요 API

| 영역 | 엔드포인트 |
| --- | --- |
| 개발 로그인 | `POST /api/dev/bootstrap` |
| Household | `GET /api/households/{id}`, `GET/POST /api/households/{id}/invitations`, `POST /api/household-invitations/{token}/accept` |
| 냉장고 | `GET/POST /api/households/{id}/fridge`, `PATCH /api/fridge/{id}/finish` |
| 생활용품 | `GET/POST /api/households/{id}/items`, `PATCH /api/household-items/{id}/finish` |
| Need List | `GET/POST /api/households/{id}/needs`, `PATCH /api/needs/{id}/complete` |
| 구독 | `GET/POST /api/users/{id}/subscriptions` |
| 영수증 | `GET /api/households/{id}/receipts`, `POST /api/households/{id}/receipts/intake`, `PATCH /api/receipts/{id}/confirm` |
| 확인 목록 | `GET /api/attention` |
| 홈 | `GET /api/home` |

## 다음 개발 순서

1. 실제 Cognito 가입·로그인과 Household 초대 전달
2. S3·Textract 실제 영수증 검증
3. ECS Fargate·RDS 운영 배포와 Vercel API 연결
4. EventBridge 기반 시간 확인과 모바일 Push
5. Home 행동 연결과 Widget

AWS 서비스는 기능 요구가 생기는 단계에서만 추가합니다. 초기 처리량이 작은 동안 SQS는 사용하지 않습니다.

## 서비스화 준비 코드

- Cognito JWT 검증과 인증 사용자 세션
- Expo AuthSession PKCE 로그인과 토큰 보관
- 비공개 S3 저장 및 Textract `AnalyzeExpense` OCR
- 개발·미리보기·운영 환경 분리
- 비루트 사용자로 실행되는 운영 컨테이너
- Cognito·S3·ECR·ECS IAM CloudFormation 기반 스택

운영 변수는 `.env.production.example`, AWS 스택은 `infra/foundation.yml`과 `infra/backend.yml`을 참고합니다. 이 항목들은 배포 완료가 아니라 준비된 코드입니다.
