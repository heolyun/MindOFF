[CmdletBinding()]
param(
    [string]$Region = 'ap-northeast-2',
    [string]$EnvironmentName = 'prod',
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$CognitoDomainPrefix,
    [string]$FrontendUrl = 'https://mindoff-project-preview.vercel.app',
    [string]$ImageTag = (Get-Date -Format 'yyyyMMddHHmmss'),
    [string]$AwsProfile = ''
)

$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path -Parent $PSScriptRoot
$foundationStack = "mindoff-$EnvironmentName-foundation"
$backendStack = "mindoff-$EnvironmentName-backend"

function Invoke-Aws {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $profileArguments = if ($AwsProfile) { @('--profile', $AwsProfile) } else { @() }
    & aws @profileArguments @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI command failed: aws $($Arguments -join ' ')"
    }
}

function Get-StackOutputs {
    param([string]$StackName)

    $json = Invoke-Aws cloudformation describe-stacks `
        --region $Region `
        --stack-name $StackName `
        --query 'Stacks[0].Outputs' `
        --output json
    $items = $json | ConvertFrom-Json
    $result = @{}
    foreach ($item in $items) {
        $result[$item.OutputKey] = $item.OutputValue
    }
    return $result
}

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw 'AWS CLI is not installed or this terminal must be restarted after installation.'
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not installed.'
}

Invoke-Aws sts get-caller-identity --region $Region --output table

$callbackUrls = "mindoff://auth,$FrontendUrl"
Invoke-Aws cloudformation deploy `
    --region $Region `
    --stack-name $foundationStack `
    --template-file (Join-Path $PSScriptRoot 'foundation.yml') `
    --capabilities CAPABILITY_IAM `
    --parameter-overrides `
        "EnvironmentName=$EnvironmentName" `
        "CognitoDomainPrefix=$CognitoDomainPrefix" `
        "CallbackUrls=$callbackUrls" `
        "LogoutUrls=$callbackUrls" `
    --no-fail-on-empty-changeset

$foundation = Get-StackOutputs -StackName $foundationStack
$repositoryUri = $foundation.EcrRepositoryUri
$registry = $repositoryUri.Split('/')[0]

$ecrPassword = Invoke-Aws ecr get-login-password --region $Region
$ecrPassword | docker login --username AWS --password-stdin $registry
if ($LASTEXITCODE -ne 0) { throw 'ECR login failed.' }

$localImage = "mindoff-api:$ImageTag"
$remoteImage = "${repositoryUri}:$ImageTag"
docker build --platform linux/amd64 --tag $localImage (Join-Path $workspacePath 'backend')
if ($LASTEXITCODE -ne 0) { throw 'Docker image build failed.' }
docker tag $localImage $remoteImage
docker push $remoteImage
if ($LASTEXITCODE -ne 0) { throw 'Docker image push failed.' }

$originVerifyHeader = [Convert]::ToHexString(
    [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
).ToLowerInvariant()

Invoke-Aws cloudformation deploy `
    --region $Region `
    --stack-name $backendStack `
    --template-file (Join-Path $PSScriptRoot 'backend.yml') `
    --capabilities CAPABILITY_IAM `
    --parameter-overrides `
        "EnvironmentName=$EnvironmentName" `
        "EcrRepositoryUri=$repositoryUri" `
        "ImageTag=$ImageTag" `
        "TaskRoleArn=$($foundation.TaskRoleArn)" `
        "ReceiptBucketName=$($foundation.ReceiptBucketName)" `
        "CognitoIssuerUri=$($foundation.CognitoIssuerUri)" `
        "CorsAllowedOrigins=$FrontendUrl" `
        "OriginVerifyHeader=$originVerifyHeader" `
    --no-fail-on-empty-changeset

$backend = Get-StackOutputs -StackName $backendStack
Write-Host "MindOFF API: $($backend.ApiUrl)"
Write-Host "Health: $($backend.ApiUrl)/actuator/health"
