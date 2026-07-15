# Event Ledger

## Overview

Two Spring Boot microservices.

- Event Gateway
- Account Service

---

## Architecture

Browser

↓

Gateway

↓

Account Service

---

## Technologies

Java 21

Spring Boot

OpenFeign

Resilience4j

H2

Docker

OpenTelemetry

Micrometer

---

## Run

mvn clean install

docker compose up

---

## Endpoints

Gateway

POST /events

GET /events/{id}

GET /events?account=

Account

POST /accounts/{id}/transactions

GET /accounts/{id}

GET /accounts/{id}/balance

---

## Testing

mvn test