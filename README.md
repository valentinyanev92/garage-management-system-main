# Garage Management System (GMS)
**Final Project – Spring Advanced (October 2025)**  
SoftUni Java Web Development Track

<!-- =======================
       TECHNOLOGY BADGES
======================== -->
<!-- Backend Stack -->
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring MVC](https://img.shields.io/badge/Spring_MVC-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Feign](https://img.shields.io/badge/Feign_Client-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Ollama](https://img.shields.io/badge/Ollama_AI-black?style=for-the-badge)

<!-- Infrastructure -->
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-000000?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GreenAPI](https://img.shields.io/badge/GreenAPI-1A8C3A?style=for-the-badge&logo=whatsapp&logoColor=white)

---

## 1. Project Overview

The **Garage Management System (GMS)** is a full-stack, multi-module platform for managing vehicles, repair orders, parts, and invoices within an auto-repair environment.

This project strictly follows all requirements of the **SoftUni Spring Advanced – Individual Project Assignment**, including:

- One **Main Spring Boot Application** (domain, business logic, UI)
- One **independent REST Microservice** (invoices, history, WhatsApp integration)
- Communication via **Feign Client**
- Asynchronous event processing via **Apache Kafka**
- **MongoDB** for the microservice
- **Redis caching**
- **Spring AI + Ollama local LLM** integration
- Fully **Dockerized infrastructure**

---

# 2. SoftUni Requirements Compliance

---

## 2.1 Technology Stack

### Backend
- Java 17
- Spring Boot 3.4.0
- Spring MVC / REST
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Scheduling
- Spring AI + Ollama
- Feign Client
- Redis Cache
- Kafka Producer / Consumer
- MongoDB (microservice)
- iText PDF generation

### Frontend
- Thymeleaf
- Bootstrap 5
- Custom CSS/JS

### DevOps
- Docker Compose
- Git + GitHub (public repo)

---

## 2.2 Project Architecture (Two Applications)

### **Main Application**
- Full domain model, services, repositories, controllers
- Thymeleaf UI
- Role-based dashboards: **USER**, **MECHANIC**, **ADMIN**
- Manages cars, parts, repair orders
- Triggers invoice generation via Feign
- Publishes repair-status events via Spring Events + Kafka

### **REST Microservice**
Runs independently under the context path: /api/v1

Handles:
- Invoice generation (PDF)
- Repair history lookups
- WhatsApp notifications (GreenAPI)
- MongoDB storage

---

## 2.3 Domain Entities

### Main Application (5 entities)
- User
- Car
- RepairOrder
- Part
- UsedPart

### Microservice (2 entities)
- InvoiceRecord
- MessageLog

All entities:
- Use **UUID** IDs
- Have repository + service layers

---

## 2.4 Web Pages (10+ required, 15+ implemented)

Dynamic pages include:
- Home
- Login / Register
- User dashboard
- Car pages (create, edit, delete, restore)
- Repair order pages (create, view status, complete)
- Mechanic dashboard
- Admin panel
- AI Mechanic Advisor
- Invoice list page

Fully covers UI requirement.

---

## 2.5 REST Microservice Functionality

Context path: /api/v1


### **POST Endpoints**
| Endpoint                        | Description                                 |
|---------------------------------|---------------------------------------------|
| **POST /api/v1/invoices**       | Generate invoice (PDF) and store in MongoDB |
| **POST /api/v1/complete-order** | Send WhatsApp notification via GreenAPI     |

### **GET Endpoints**
| Endpoint                                     | Description                       |
|----------------------------------------------|-----------------------------------|
| **GET /api/v1/history**                      | Get all invoice/notification logs |
| **GET /api/v1/pdf/repair/{repairId}/latest** | Get logs for specific repair      |

→ Fully exceeds the requirement (2 modifying + 1 GET).  
→ Invoked by **Feign Client** from the main application.

---

## 2.6 Functionalities

### Main Application (12+ valid functionalities)
- Create / edit / delete / restore car
- Create repair order
- Accept repair order
- Add used parts
- Complete repair order
- Cancel repair order
- Trigger invoice creation
- View invoices
- Admin: manage all repair orders
- Mechanic: manage assigned orders
- AI mechanic suggestions
- View repair history

### Microservice (3+ functionalities)
- Generate invoice (PDF)
- Store invoice log in MongoDB
- Send WhatsApp notification via GreenAPI
- Query repair history

---

## 2.7 Security & Roles

Roles implemented:
- **USER**
- **MECHANIC**
- **ADMIN**

Features:
- Authentication + Authorization
- CSRF enabled
- Role-based access
- Admin: user management
- Users can edit their own profiles

---

## 2.8 Database Requirements

- Main app → MySQL
- Microservice → MongoDB
- All IDs → UUID
- Passwords → BCrypt
- Multiple entity relationships
- Spring Data JPA everywhere

---

## 2.9 Validation & Error Handling

### Main App:
- DTO validation
- Entity validation
- Service validation
- Global exception handler
- Controller exception handlers
- Built-in + custom exceptions (3 custom)
- No white-label errors

### Microservice:
- DTO validation
- Built-in validation handlers
- Custom exception support

---

## 2.10 Scheduling & Caching

- Cron-based scheduled job
- Fixed-rate scheduled job
- Redis caching for parts (get, evict, refresh)

---

## 2.11 Testing

- Unit tests
- Integration tests
- API tests (MockMvc)
- ~80%+ line coverage

---

## 2.12 Logging

- Logs in every main functionality
- Logs in microservice
- AOP logging advice implemented

---

## 2.13 Code Quality & Style

- No dead code or unused imports
- Thin controllers
- Clear layered architecture
- SOLID principles applied
- Java naming conventions followed
- Feature-based organization

---

# 3. Bonus Features

| Bonus Feature                        | Points | Status |
|--------------------------------------|--------|--------|
| Apache Kafka                         | +4     | ✔      |
| MongoDB                              | +4     | ✔      |
| Redis caching                        | +5     | ✔      |
| Spring Events                        | +1     | ✔      |
| AOP Advice                           | +2     | ✔      |
| LLM integration (Spring AI + Ollama) | +4     | ✔      |
| PDF export                           | +2     | ✔      |
| Dockerized setup                     | +1     | ✔      |
| GreenAPI / 3rd-Party REST API        | +2     | ✔      |

Total potential bonus: **25+ points**  
(SoftUni counts max +15)

---
# 4. How to Run the Project

### 4.1 Clone both repositories
```bash
  git clone https://github.com/valentinyanev92/garage-management-system-main
  git clone https://github.com/valentinyanev92/garage-management-system-api
```
### 4.2 Start Docker infrastructure

From the main application directory:
```bash
  docker compose up -d
```

### This starts:
- MySQL
- Redis
- Kafka
- Zookeeper
- MongoDB (used by microservice)
- Ollama

### 4.3 Start Main Application
```bash
  mvn spring-boot:run
```

### 4.4 Start Microservice
```bash
    mvn spring-boot:run
```

---

### 5. Author
Valentin Rumenov Yanev

SoftUni Java Developer Track (2024–2025)

