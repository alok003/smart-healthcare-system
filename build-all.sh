#!/bin/bash
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

echo "Building All Services..."
echo

services=(
  "configServer:Config Server"
  "eurekaServer:Eureka Server"
  "gateway:Gateway"
  "userService:User Service"
  "adminService:Admin Service"
  "doctorService:Doctor Service"
  "patientService:Patient Service"
  "appointmentService:Appointment Service"
  "notificationService:Notification Service"
)

total=${#services[@]}
count=1

for entry in "${services[@]}"; do
  dir="${entry%%:*}"
  name="${entry##*:}"
  echo "[$count/$total] Building $name..."
  cd "$APP_DIR/$dir"
  ./mvnw clean package -DskipTests -q
  cd "$APP_DIR"
  ((count++))
done

echo
echo "========================================"
echo "All services built successfully!"
echo "========================================"
