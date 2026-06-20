#!/bin/bash
set -e

# ─────────────────────────────────────────
# Usage:
#   ./bootstrap.sh <HOST> <VM_USER> <PEM_FILE> <REPO_URL> <BRANCH> \
#     <DB_USER> <DB_PASS> <SECRET_KEY> <EXPIRATION> \
#     <EMAIL_USER> <EMAIL_PASS>
#
# Example:
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

  echo "==> Updating system..."
  sudo apt-get update -qq

  echo "==> Installing Java 17..."
  if ! command -v java &> /dev/null; then
    sudo apt-get install -y -qq openjdk-17-jdk
  else
    echo "Java already installed, skipping..."
  fi

  echo "==> Installing Docker..."
  if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $VM_USER
    sudo systemctl enable docker
    sudo systemctl start docker
  else
    echo "Docker already installed, skipping..."
    sudo usermod -aG docker $VM_USER
  fi

  echo "==> Installing Git..."
  sudo apt-get install -y -qq git

  echo "==> Cloning repo..."
  sudo mkdir -p /app
  sudo chown $VM_USER:$VM_USER /app
  git clone -b $BRANCH $REPO $APP_DIR

  echo "==> Creating Vault.env..."
  cat > $APP_DIR/Vault.env << VAULTEOF
DATABASE_USERNAME=$DB_USER
DATABASE_PASS=$DB_PASS
SECRET_KEY=$SECRET_KEY
EXPIRATION=$EXPIRATION
EMAIL_USERNAME=$EMAIL_USER
EMAIL_PASSWORD=$EMAIL_PASS
VAULTEOF

  echo "==> Setting permissions..."
  chmod +x $APP_DIR/build-all.sh
  find $APP_DIR -name "mvnw" -exec chmod +x {} \;

  echo "==> Building all services (this takes 5-10 minutes)..."
  cd $APP_DIR
  ./build-all.sh

  echo "==> Starting services..."
  cd $APP_DIR
  sudo docker compose up -d

  echo "==> Setting up systemd service..."
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

  sudo systemctl daemon-reload
  sudo systemctl enable smart-healthcare.service

  echo
  echo "========================================"
  echo "Bootstrap complete!"
  echo "========================================"
  echo "API Gateway:   http://$HOST:8080/actuator/health"
  echo "Eureka:        http://$HOST:8761"
  echo "Kafka UI:      http://$HOST:9090"
  echo "Config Server: http://$HOST:8888/actuator/health"
  echo "========================================"
  echo "Wait ~3-5 minutes for all services to register with Eureka"
EOF
