# Sentinel Issue Processor — AWS Lambda Function

This AWS Lambda function automatically processes screenshot proofs and images uploaded when users raise issues or report service downtimes in Uptime Sentinel.

## Architecture

1. **Upload**: User uploads an issue screenshot on the Sentinel frontend (`client-uptime`).
2. **S3 Storage**: `monitoring-service` stores the image into the configured AWS S3 bucket (`AWS_S3_BUCKET_NAME`) under `issues/{userId}/{uuid}-{filename}`.
3. **Lambda Invocation**: `monitoring-service` uses AWS SDK v2 (`software.amazon.awssdk:lambda`) to invoke this Lambda function (`sentinel-issue-processor`).
4. **Validation & Auditing**: The Lambda verifies the S3 object integrity, extracts metadata, evaluates priority level, and returns the execution report.
5. **Persistence**: The execution ARN/result is stored with the `IssueTicket` in PostgreSQL.

---

## Deployment Guide

### Option 1: AWS Console (Quickest)
1. Go to **AWS Lambda Console** -> **Create function**.
2. Name: `sentinel-issue-processor`
3. Runtime: **Node.js 20.x** (or 18.x)
4. Architecture: `x86_64` or `arm64`
5. Copy the code from `index.js` into the Lambda code editor.
6. In **Configuration** -> **Permissions**, ensure the execution role has:
   - `s3:GetObject` and `s3:HeadObject` on your S3 bucket ARN (e.g., `arn:aws:s3:::sentinel-issue-proofs/*`).
   - `AWSLambdaBasicExecutionRole` (CloudWatch Logs).
7. Click **Deploy**.

### Option 2: AWS CLI
```bash
cd aws-lambda/issue-processor
npm install
zip -r function.zip index.js package.json node_modules

aws lambda create-function \
  --function-name sentinel-issue-processor \
  --runtime nodejs20.x \
  --role arn:aws:iam::<YOUR_ACCOUNT_ID>:role/<YOUR_LAMBDA_ROLE> \
  --handler index.handler \
  --zip-file fileb://function.zip
```

---

## Environment Variables
- `AWS_REGION`: e.g. `us-east-1`
- `AWS_S3_BUCKET_NAME`: e.g. `sentinel-issue-proofs`
