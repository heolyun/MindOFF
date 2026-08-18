# MindOFF AWS 배포

기본 리전은 서울(`ap-northeast-2`)입니다.

- `foundation.yml`: Cognito, 비공개 영수증 S3, ECR, ECS 작업 역할
- `backend.yml`: VPC, PostgreSQL RDS, ECS Fargate, ALB, CloudFront HTTPS
- `deploy-backend.ps1`: 스택 생성, 이미지 빌드·업로드, 서비스 배포

## 배포

AWS CLI 로그인과 Docker Desktop 실행 후 프로젝트 루트에서 실행합니다.

```powershell
.\infra\deploy-backend.ps1 `
  -CognitoDomainPrefix mindoff-prod-<unique-suffix>
```

초기 구성은 Fargate 작업 1개와 `db.t4g.micro` PostgreSQL 1개입니다. RDS는 외부에 공개하지 않고, 데이터베이스 비밀번호는 Secrets Manager에서 생성해 ECS 작업에 전달합니다. CloudFront 기본 도메인이 HTTPS API 주소가 됩니다.

실제 생성 시 RDS, Fargate, ALB, CloudFront, 로그 및 저장소 사용료가 발생합니다.
