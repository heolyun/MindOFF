# MindOFF AWS 배포

기본 리전은 서울(`ap-northeast-2`)입니다.

- `foundation.yml`: Cognito, 비공개 영수증 S3, ECR, ECS 작업 역할
- `backend.yml`: VPC, PostgreSQL RDS, ECS Fargate, ALB, CloudFront HTTPS
- `deploy-backend.ps1`: 스택 생성, 이미지 빌드·업로드, 서비스 배포

배포 전후 확인 순서는 [AWS 배포 체크리스트](../docs/AWS_DEPLOYMENT_CHECKLIST.md)에 정리되어 있습니다.

## 배포

AWS CLI 로그인과 Docker Desktop 실행 후 프로젝트 루트에서 실행합니다.

```powershell
.\infra\deploy-backend.ps1 `
  -CognitoDomainPrefix mindoff-prod-<unique-suffix>
```

초기 구성은 Fargate 작업 1개와 `db.t4g.micro` PostgreSQL 1개입니다. RDS는 외부에 공개하지 않고, 데이터베이스 비밀번호는 Secrets Manager에서 생성해 ECS 작업에 전달합니다. CloudFront 기본 도메인이 HTTPS API 주소가 됩니다.

실제 생성 시 RDS, Fargate, ALB, CloudFront, 로그 및 저장소 사용료가 발생합니다.

## AWS 로그인 전에 가능한 검증

```powershell
uvx cfn-lint infra/foundation.yml infra/backend.yml
docker build --platform linux/amd64 --tag mindoff-api:preflight backend
docker compose up -d --build
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
docker compose down
```

GitHub Actions도 CloudFormation 문법, 배포 스크립트 문법, 백엔드 테스트·컨테이너 빌드와 모바일 웹 빌드를 매번 확인합니다.
