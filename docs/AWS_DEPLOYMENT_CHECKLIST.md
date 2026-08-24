# MindOFF AWS 배포 체크리스트

실제 AWS 자원을 만들기 전부터 배포 후 서비스 연결까지 같은 순서로 확인하기 위한 운영 문서입니다.

## 1. 계정 복구 전에도 가능한 점검

- [x] 백엔드 테스트 통과: `backend\gradlew.bat test`
- [x] CloudFormation 검사 통과: `uvx cfn-lint infra/foundation.yml infra/backend.yml`
- [x] PowerShell 배포 스크립트 문법 검사 통과
- [x] `linux/amd64` 운영 컨테이너 빌드 통과
- [x] Docker Compose의 PostgreSQL·API 기동 및 readiness 확인
- [x] GitHub Actions에 인프라 검증 추가

로컬 전체 구동 확인:

```powershell
docker compose up -d --build
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
docker compose down
```

## 2. 배포 직전

- [x] AWS 루트 계정 MFA 복구 완료
- [x] AWS CLI에서 배포 계정 확인: `499989195406`
- [x] 기본 리전 `ap-northeast-2` 확인
- [x] 월 `$10` Billing 예산과 50%·80%·100% 알림 설정
- [x] Free 플랜 RDS 자동 백업 보관 기간 1일 적용
- [x] 고유한 Cognito 도메인 접두사 적용
- [x] Vercel 운영 주소와 CORS 허용 주소 일치 확인
- [x] 저장소와 GitHub Actions에 AWS 비밀키·DB 비밀번호가 없는지 확인

비용이 계속 발생하는 핵심 자원은 RDS, Fargate, ALB입니다. 학습·포트폴리오 확인이 끝나면 유지할 자원과 삭제할 자원을 바로 결정합니다.

## 3. 백엔드 배포

```powershell
.\infra\deploy-backend.ps1 `
  -CognitoDomainPrefix mindoff-prod-<unique-suffix>
```

스크립트가 다음 순서로 처리합니다.

1. Cognito·S3·ECR·ECS 역할 기반 스택 생성
2. 백엔드 이미지 빌드와 ECR 업로드
3. VPC·RDS·ECS Fargate·ALB·CloudFront 스택 생성
4. HTTPS API 주소 출력

## 4. 배포 직후 확인

- [x] `https://d1fq2tsi4ud0ut.cloudfront.net/actuator/health/readiness`가 `UP`
- [x] Vercel 운영 번들에서 AWS API·Cognito 설정과 CORS 확인
- [x] Cognito 관리형 화면 없이 MindOFF 가입·인증·로그인·재설정 화면 연결
- [ ] Cognito 회원가입·로그인·토큰 갱신·로그아웃
- [ ] Household 생성, 초대 링크 수락, 구성원 공유 데이터 확인
- [ ] JPG/PNG 영수증 업로드, S3 저장, Tesseract 초안, 최종 확정
- [ ] 냉장고·생활용품·Need List·공유 구독의 저장과 재조회
- [x] ECS 로그에서 Spring Boot 기동, PostgreSQL 연결, Flyway V1~V5 적용 확인

Vercel Production 환경에 다음 값을 반영했습니다.

```text
EXPO_PUBLIC_API_URL=https://<api-domain>
EXPO_PUBLIC_AUTH_MODE=cognito
EXPO_PUBLIC_COGNITO_ISSUER_URI=https://cognito-idp.ap-northeast-2.amazonaws.com/<user-pool-id>
EXPO_PUBLIC_COGNITO_CLIENT_ID=<public-app-client-id>
```

## 5. 실패·중단 시

- ECS 배포 회로 차단기가 실패한 작업 정의를 자동 롤백하는지 확인합니다.
- CloudFormation 이벤트에서 처음 실패한 자원을 기준으로 원인을 찾습니다.
- DB와 Cognito는 삭제 보호가 켜져 있고, S3·ECR은 스택 삭제 후에도 유지되도록 정의되어 있습니다.
- 완전 삭제가 필요하면 보존할 데이터와 스냅샷을 먼저 결정한 뒤 삭제 보호를 직접 해제합니다.
- 비용 확인 없이 실패한 스택이나 고정 비용 자원을 방치하지 않습니다.

현재 상태: AWS 운영 백엔드와 Vercel 운영 화면의 연결을 완료했습니다. CloudFront HTTPS, CORS, Cognito 직접 인증 API가 정상입니다. 실제 사용자 이메일 인증과 영수증 OCR 검증이 다음 작업입니다.
