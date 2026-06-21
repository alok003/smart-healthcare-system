#!/bin/bash
set -e

# ─────────────────────────────────────────
# Usage:
#   source bootstrap-args.env
#   ./bootstrap.sh $HOST $VM_USER $PEM $REPO $BRANCH \
#     $DB_USER $DB_PASS $SECRET_KEY $EXPIRATION $EMAIL_USER $EMAIL_PASS
# ─────────────────────────────────────────

HOST=$1
VM_USER=$2
PEM=$3
REPO=$4
BRANCH=$5
DB_USER=$6
DB_PASS=$7
SECRET_KEY=$8
EXPIRATION=$9
EMAIL_USER=${10}
EMAIL_PASS=${11}

APP_DIR="/app/smart-healthcare-system"
SSH="ssh -i $PEM -o StrictHostKeyChecking=no -T $VM_USER@$HOST"

if [ -z "$HOST" ] || [ -z "$VM_USER" ] || [ -z "$PEM" ] || [ -z "$REPO" ] || \
   [ -z "$BRANCH" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASS" ] || \
   [ -z "$SECRET_KEY" ] || [ -z "$EXPIRATION" ] || [ -z "$EMAIL_USER" ] || \
   [ -z "$EMAIL_PASS" ]; then
  echo "ERROR: Missing arguments."
  echo "Usage: ./bootstrap.sh <HOST> <VM_USER> <PEM_FILE> <REPO_URL> <BRANCH> <DB_USER> <DB_PASS> <SECRET_KEY> <EXPIRATION> <EMAIL_USER> <EMAIL_PASS>"
  exit 1
fi

echo "==> Connecting to $VM_USER@$HOST..."

$SSH << EOF
  set -e

  LOG_DIR="$APP_DIR/logs"
  FULL_LOG="\$LOG_DIR/bootstrap.log"
  INFO_LOG="\$LOG_DIR/bootstrap-info.log"

  # Create log dir early (may not exist yet)
  mkdir -p /tmp/bootstrap-logs

  FULL_LOG="/tmp/bootstrap-logs/bootstrap.log"
  INFO_LOG="/tmp/bootstrap-logs/bootstrap-info.log"

  # Helpers
  log_info() {
    TS=\$(date '+%Y-%m-%d %H:%M:%S')
    echo "[\$TS] [INFO]  \$1" | tee -a \$FULL_LOG \$INFO_LOG
  }

  log_detail() {
    TS=\$(date '+%Y-%m-%d %H:%M:%S')
    echo "[\$TS] [DETAIL] \$1" | tee -a \$FULL_LOG
  }

  log_error() {
    TS=\$(date '+%Y-%m-%d %H:%M:%S')
    echo "[\$TS] [ERROR] \$1" | tee -a \$FULL_LOG \$INFO_LOG
  }

  log_skip() {
    TS=\$(date '+%Y-%m-%d %H:%M:%S')
    echo "[\$TS] [SKIP]  \$1" | tee -a \$FULL_LOG \$INFO_LOG
  }

  log_done() {
    TS=\$(date '+%Y-%m-%d %H:%M:%S')
    echo "[\$TS] [DONE]  \$1" | tee -a \$FULL_LOG \$INFO_LOG
  }

  log_info "========================================"
  log_info "Bootstrap started"
  log_info "Host: $HOST | User: $VM_USER | Branch: $BRANCH"
  log_info "========================================"

  # ── 1. INFRA CHECKS ──────────────────────────────────────────

  log_info "--- SECTION 1: Infrastructure Checks ---"

  log_info "Checking Java..."
  if ! command -v java &> /dev/null; then
    log_info "Java not found — installing Java 17..."
    sudo apt-get update -qq >> \$FULL_LOG 2>&1
    sudo apt-get install -y -qq openjdk-17-jdk >> \$FULL_LOG 2>&1
    log_done "Java 17 installed: \$(java -version 2>&1 | head -1)"
  else
    log_skip "Java already installed: \$(java -version 2>&1 | head -1)"
  fi

  log_info "Checking Docker..."
  if ! command -v docker &> /dev/null; then
    log_info "Docker not found — installing Docker..."
    sudo apt-get update -qq >> \$FULL_LOG 2>&1
    sudo apt-get install -y -qq curl apt-transport-https ca-certificates gnupg >> \$FULL_LOG 2>&1
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg 2>> \$FULL_LOG
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu focal stable" | sudo tee /etc/apt/sources.list.d/docker.list >> \$FULL_LOG
    sudo apt-get update -qq >> \$FULL_LOG 2>&1
    sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin >> \$FULL_LOG 2>&1
    sudo usermod -aG docker $VM_USER
    sudo systemctl enable docker >> \$FULL_LOG 2>&1
    sudo systemctl start docker >> \$FULL_LOG 2>&1
    log_done "Docker installed: \$(docker --version)"
  else
    log_skip "Docker already installed: \$(docker --version)"
    sudo usermod -aG docker $VM_USER
  fi

  log_info "Checking Git..."
  if ! command -v git &> /dev/null; then
    log_info "Git not found — installing..."
    sudo apt-get install -y -qq git >> \$FULL_LOG 2>&1
    log_done "Git installed: \$(git --version)"
  else
    log_skip "Git already installed: \$(git --version)"
  fi

  log_info "Disk space check:"
  df -h / | tee -a \$FULL_LOG \$INFO_LOG

  # ── 2. REPO ───────────────────────────────────────────────────

  log_info "--- SECTION 2: Repository ---"

  sudo mkdir -p /app
  sudo chown $VM_USER:$VM_USER /app

  FRESH_CLONE=false
  CHANGED_FILES=""

  if [ ! -d "$APP_DIR/.git" ]; then
    log_info "No repo found — fresh clone from $REPO branch $BRANCH..."
    git clone -b $BRANCH $REPO $APP_DIR >> \$FULL_LOG 2>&1
    FRESH_CLONE=true
    log_done "Repo cloned successfully"
  else
    log_info "Repo exists — fetching latest from $BRANCH..."
    cd $APP_DIR
    git fetch origin $BRANCH >> \$FULL_LOG 2>&1
    CHANGED_FILES=\$(git diff --name-only HEAD origin/$BRANCH)
    if [ -z "\$CHANGED_FILES" ]; then
      log_info "No changes detected in remote"
    else
      log_info "Changed files detected:"
      echo "\$CHANGED_FILES" | while read f; do log_detail "  → \$f"; done
    fi
    git pull origin $BRANCH >> \$FULL_LOG 2>&1
    log_done "Repo updated"
  fi

  # Move logs to app dir now that it exists
  mkdir -p $APP_DIR/logs
  sudo chown -R $VM_USER:$VM_USER $APP_DIR/logs
  cp \$FULL_LOG $APP_DIR/logs/bootstrap.log 2>/dev/null || true
  cp \$INFO_LOG $APP_DIR/logs/bootstrap-info.log 2>/dev/null || true
  FULL_LOG="$APP_DIR/logs/bootstrap.log"
  INFO_LOG="$APP_DIR/logs/bootstrap-info.log"

  # ── 3. VAULT.ENV ─────────────────────────────────────────────

  log_info "--- SECTION 3: Vault.env ---"
  cat > $APP_DIR/Vault.env << VAULTEOF
DATABASE_USERNAME=$DB_USER
DATABASE_PASS=$DB_PASS
SECRET_KEY=$SECRET_KEY
EXPIRATION=$EXPIRATION
EMAIL_USERNAME=$EMAIL_USER
EMAIL_PASSWORD=$EMAIL_PASS
VAULTEOF
  log_done "Vault.env created"

  # ── 4. SYSTEMD ────────────────────────────────────────────────

  log_info "--- SECTION 4: Systemd ---"
  if [ ! -f /etc/systemd/system/smart-healthcare.service ]; then
    log_info "Creating systemd service..."
    sudo tee /etc/systemd/system/smart-healthcare.service > /dev/null << SERVICEEOF
[Unit]
Description=Smart Healthcare System
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=300
User=$VM_USER

[Install]
WantedBy=multi-user.target
SERVICEEOF
    sudo systemctl daemon-reload >> \$FULL_LOG 2>&1
    sudo systemctl enable smart-healthcare.service >> \$FULL_LOG 2>&1
    log_done "Systemd service created and enabled"
  else
    log_skip "Systemd service already exists"
  fi

  # ── 5. CLEANUP ────────────────────────────────────────────────

  log_info "--- SECTION 5: Docker Cleanup ---"
  log_info "Disk before cleanup:"
  df -h / | tee -a \$FULL_LOG

  STOPPED=\$(sudo docker container prune -f 2>&1)
  log_detail "Containers pruned: \$STOPPED"

  DANGLING=\$(sudo docker image prune -f 2>&1)
  log_detail "Dangling images pruned: \$DANGLING"

  log_info "Disk after cleanup:"
  df -h / | tee -a \$FULL_LOG

  # ── 6. BUILD ──────────────────────────────────────────────────

  log_info "--- SECTION 6: Build ---"

  chmod +x $APP_DIR/build-all.sh
  find $APP_DIR -name "mvnw" -exec chmod +x {} \;

  build_service() {
    SERVICE=\$1
    DIR=\$2
    log_info "Building \$SERVICE..."
    cd $APP_DIR/\$DIR
    if ./mvnw clean package -DskipTests >> \$FULL_LOG 2>&1; then
      log_done "\$SERVICE built successfully"
    else
      log_error "\$SERVICE build FAILED — run: tail -f \$FULL_LOG"
      exit 1
    fi
    cd $APP_DIR
  }

  if [ "\$FRESH_CLONE" = "true" ]; then
    log_info "Fresh clone — building all 9 services..."
    build_service "config-server"   "configServer"
    build_service "eureka-server"   "eurekaServer"
    build_service "gateway"         "gateway"
    build_service "user-service"    "userService"
    build_service "admin-service"   "adminService"
    build_service "doctor-service"  "doctorService"
    build_service "patient-service" "patientService"
    build_service "appointment-service" "appointmentService"
    build_service "notification-service" "notificationService"
    log_done "All 9 services built"
  else
    log_info "Checking which services changed..."
    BUILT=0

    if echo "\$CHANGED_FILES" | grep -q "^userService/";         then build_service "user-service"         "userService";        BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^adminService/";        then build_service "admin-service"        "adminService";       BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^doctorService/";       then build_service "doctor-service"       "doctorService";      BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^patientService/";      then build_service "patient-service"      "patientService";     BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^appointmentService/";  then build_service "appointment-service"  "appointmentService"; BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^notificationService/"; then build_service "notification-service" "notificationService";BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^gateway/";             then build_service "gateway"              "gateway";            BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^configServer/";        then build_service "config-server"        "configServer";       BUILT=1; fi
    if echo "\$CHANGED_FILES" | grep -q "^eurekaServer/";        then build_service "eureka-server"        "eurekaServer";       BUILT=1; fi

    if [ "\$BUILT" = "0" ]; then
      log_skip "No service code changed — skipping build"
    fi
  fi

  # ── 7. START ──────────────────────────────────────────────────

  log_info "--- SECTION 7: Starting Services ---"
  cd $APP_DIR
  sudo docker compose down >> \$FULL_LOG 2>&1 || true
  sudo docker compose up -d >> \$FULL_LOG 2>&1
  log_done "docker compose up -d complete"

  log_info "Container status:"
  sudo docker compose ps | tee -a \$FULL_LOG \$INFO_LOG

  log_info "========================================"
  log_done "Bootstrap complete!"
  log_info "Logs: \$INFO_LOG (info) | \$FULL_LOG (full)"
  log_info "========================================"
  log_info "API Gateway:   http://$HOST:8080/actuator/health"
  log_info "Eureka:        http://$HOST:8761"
  log_info "Kafka UI:      http://$HOST:9090"
  log_info "Config Server: http://$HOST:8888/actuator/health"
  log_info "Wait ~3-5 minutes for all services to register with Eureka"
EOF
