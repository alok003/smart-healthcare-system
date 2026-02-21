@echo off
echo Building All Services...
echo.

echo [1/9] Building Config Server...
cd configServer
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [2/9] Building Eureka Server...
cd eurekaServer
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [3/9] Building Gateway...
cd gateway
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [4/9] Building User Service...
cd userService
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [5/9] Building Admin Service...
cd adminService
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [6/9] Building Doctor Service...
cd doctorService
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [7/9] Building Patient Service...
cd patientService
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [8/9] Building Appointment Service...
cd appointmentService
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo [9/9] Building Notification Service...
cd notificationService
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 exit /b %errorlevel%
cd ..

echo.
echo ========================================
echo All services built successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Ensure MySQL is running on localhost:3306
echo 2. Ensure MongoDB is running on localhost:27017
echo 3. Stop existing containers: docker compose down
echo 4. Start services: docker compose up -d --build
echo.
