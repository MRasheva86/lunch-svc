📋 Overview
lunch-svc is the dedicated REST microservice responsible for:
Meal and lunch data persistence
Lunch order processing
Daily automatic status updates
Stateless REST API consumed by the Main Application
Validation and error handling
Scheduled tasks
Independent database
This microservice is triggered through OpenFeign calls from the main application _School-Lunch.

🛠 Technology Stack
Java 17
Spring Boot 3.4
Spring Web / REST
Spring Data JPA
MySQL 
Validation
Scheduling
JUnit + MockMvc + Integration tests

📦 Domain Model
All entities use UUID as primary key.
1. Lunch
2. Meal
3. OrderStatus
   
📡 REST API Endpoints
POST /api/lunch
Create new lunch order.
PUT /api/lunch/{id}/status
Update lunch order status.
GET /api/meals
Return list of meals.
GET /api/lunch/today

Fetch today's lunch information.
All modifying operations are invoked via Feign Client from the main application.

🔄 Functionalities (Valid Microservice Functionalities)
1. Persist lunch orders and meals
Main app triggers creation → microservice stores Lunch + Meal relations.
2. Update lunch order status
Change order state through PUT/POST/DELETE operations.
3. Scheduled status completion (13:00 daily)
Cron job processes all today's orders → marks them as COMPLETED.
4. Trigger-based verification job
5 minutes after the cron job → checks if order statuses were updated successfully.

⏱ Scheduled Jobs
1. Cron Job — Every day at 13:00
0 0 13 * * *
Checks all orders for today
If any exist → sets status to COMPLETED
2. Follow-up Job — 5 minutes later
Triggered only after first job completes.
Used for consistency verification.

⚠️ Validation & Error Handling
DTO validation using annotations
Custom exception classes
Global exception handler
JSON error responses
No white-label errors

🧪 Testing
Microservice includes:
Unit tests
Integration tests
REST API tests

Ensures stable and predictable behavior for inter-service communication.

🗃 Database
Independent database (separate from main app)
Spring Data JPA
UUID identifiers
Meal–Lunch relationship

🚀 Run Instructions
mvn spring-boot:run

Ensure that:
Database properties are configured
Runs on a different port from _School-Lunch

📚 Commit Guidelines
Using Conventional Commits, such as:
feat: add lunch status scheduler
fix: correct meal price validation
test: add API test for lunch creation

🏁 Conclusion
lunch-svc is a clean, domain-specific REST microservice handling all lunch-related data and scheduled processing. It integrates seamlessly with the main application.
