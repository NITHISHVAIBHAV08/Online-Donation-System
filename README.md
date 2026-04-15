# Online Donation System

A Spring Boot REST API for processing and recording donations, with JUnit testing,
GitHub Actions CI/CD, Docker containerization, and Kubernetes deployment.

---

## Project Structure

```
donation-system/
├── src/
│   ├── main/java/com/donation/
│   │   ├── DonationApplication.java        ← Spring Boot entry point
│   │   ├── model/Donation.java             ← Entity
│   │   ├── repository/DonationRepository.java
│   │   ├── service/DonationService.java    ← Business logic
│   │   └── controller/DonationController.java
│   └── test/java/com/donation/service/
│       └── DonationServiceTest.java        ← JUnit 5 tests (25 tests)
├── .github/workflows/ci-cd.yml             ← GitHub Actions pipeline
├── Dockerfile                              ← Multi-stage Docker build
├── k8s/
│   ├── deployment.yaml                     ← Kubernetes Deployment
│   └── service.yaml                        ← Kubernetes Service + Namespaces
└── pom.xml
```

---

## Prerequisites

| Tool        | Version  |
|-------------|----------|
| Java JDK    | 17+      |
| Maven       | 3.8+     |
| Docker      | 24+      |
| kubectl     | 1.28+    |
| Git         | 2.x      |

---

## ▶ HOW TO RUN

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/online-donation-system.git
cd donation-system
```

---

### 2. Run the Application Locally

```bash
# Compile
mvn clean compile

# Run Spring Boot app (H2 in-memory DB — no external DB needed)
mvn spring-boot:run
```

App runs at: **http://localhost:8080**
H2 Console: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:donationdb`
- Username: `sa`, Password: *(blank)*

---

### 3. Run JUnit Tests

```bash
# Run all tests
mvn test

# Run tests + view report
mvn test surefire-report:report
# Report at: target/site/surefire-report.html

# Run a specific test class
mvn test -Dtest=DonationServiceTest

# Run a specific test method
mvn test -Dtest=DonationServiceTest#testProcessDonation_Success
```

---

### 4. Test the REST API (cURL examples)

**Make a donation:**
```bash
curl -X POST http://localhost:8080/api/donations \
  -H "Content-Type: application/json" \
  -d '{
    "donorName": "Alice Kumar",
    "donorEmail": "alice@example.com",
    "amount": 500.00,
    "paymentMethod": "UPI",
    "cause": "EDUCATION"
  }'
```

**Get all donations:**
```bash
curl http://localhost:8080/api/donations
```

**Get donation by ID:**
```bash
curl http://localhost:8080/api/donations/1
```

**Get donations by email:**
```bash
curl http://localhost:8080/api/donations/email/alice@example.com
```

**Get total amount collected:**
```bash
curl http://localhost:8080/api/donations/total
```

**Refund a donation:**
```bash
curl -X PUT http://localhost:8080/api/donations/1/refund
```

**Simulate failed payment** (use amount ending in .99):
```bash
curl -X POST http://localhost:8080/api/donations \
  -H "Content-Type: application/json" \
  -d '{
    "donorName": "Bob Test",
    "donorEmail": "bob@example.com",
    "amount": 99.99,
    "paymentMethod": "CREDIT_CARD",
    "cause": "HEALTH"
  }'
```

---

### 5. Docker

**Build the image:**
```bash
docker build -t online-donation-system:latest .
```

**Run the container:**
```bash
docker run -d \
  --name donation-app \
  -p 8080:8080 \
  online-donation-system:latest
```

**Push to Docker Hub:**
```bash
docker login
docker tag online-donation-system:latest YOUR_DOCKERHUB_USERNAME/online-donation-system:latest
docker push YOUR_DOCKERHUB_USERNAME/online-donation-system:latest
```

**Stop the container:**
```bash
docker stop donation-app && docker rm donation-app
```

---

### 6. Kubernetes Deployment

```bash
# Create namespaces
kubectl apply -f k8s/service.yaml

# Update image name in deployment.yaml first, then:
kubectl apply -f k8s/deployment.yaml

# Check deployment status
kubectl get deployments -n production
kubectl get pods -n production
kubectl get services -n production

# View application logs
kubectl logs -l app=donation-system -n production

# Scale up/down
kubectl scale deployment donation-system --replicas=5 -n production

# Rolling restart
kubectl rollout restart deployment/donation-system -n production

# Delete deployment
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/service.yaml
```

---

### 7. GitHub Actions CI/CD Setup

1. Push code to GitHub:
```bash
git init
git remote add origin https://github.com/YOUR_USERNAME/online-donation-system.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

2. Add these **GitHub Secrets** (Settings → Secrets → Actions):

| Secret Name          | Value                              |
|---------------------|------------------------------------|
| `DOCKER_USERNAME`   | Your Docker Hub username           |
| `DOCKER_PASSWORD`   | Your Docker Hub password/token     |
| `KUBE_CONFIG_STAGING` | Base64-encoded kubeconfig for staging |
| `KUBE_CONFIG_PROD`  | Base64-encoded kubeconfig for prod |

3. Generate base64 kubeconfig:
```bash
cat ~/.kube/config | base64
```

4. The pipeline triggers automatically on every `git push` to `main`.

---

## CI/CD Pipeline Stages

```
Push to main
     │
     ▼
[1] Build & Test  ──► JUnit Tests ──► Test Report
     │
     ▼
[2] Code Quality  ──► mvn verify
     │
     ▼
[3] Docker Build & Push ──► Docker Hub
     │
     ▼
[4] Deploy to Staging  ──► kubectl (auto)
     │
     ▼
[5] Deploy to Production ──► kubectl (manual approval)
```

---

## Payment Methods Supported

- `CREDIT_CARD`
- `DEBIT_CARD`
- `UPI`
- `NET_BANKING`

## Donation Limits

- Minimum: **$1.00**
- Maximum: **$100,000.00**

## Test Hook

To simulate a payment failure (for testing), use any amount ending in `.99`
(e.g., `99.99`, `199.99`). The system will mark the donation as `FAILED`.
