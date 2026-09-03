#!/usr/bin/env bash
set -euo pipefail
LOG=/tmp/clas-compose-deploy.log
exec > >(tee -a "$LOG") 2>&1

echo "=== compose deploy $(date -Is) ==="
sudo mkdir -p /opt/clas
if [[ ! -d /opt/clas/.git ]]; then
  sudo git clone https://github.com/Amyjinru/CLAS.git /opt/clas
fi
cd /opt/clas
sudo git fetch origin main
sudo git checkout -f main

sudo tee .env >/dev/null <<'EOF'
MYSQL_DATABASE=clas
MYSQL_USER=clas
MYSQL_PASSWORD=trouble314
MYSQL_ROOT_PASSWORD=trouble314-root
JWT_SECRET=clas-local-integration-secret-2026-hint314!
RIDER_IDENTITY_ENCRYPTION_KEY=local-compose-test-aes-key-32b!!
CLAS_DEMO_ACCOUNTS_ENABLED=true
CLAS_DEMO_ACCESS_PASSWORD=Abc123!
CORS_ALLOWED_ORIGINS=http://81.70.59.38,http://localhost,http://127.0.0.1
EOF

sudo sed -i 's/"8088:80"/"80:80"/' docker-compose.yml

echo ">>> docker compose build"
sudo docker compose --env-file .env build --pull=false

echo ">>> docker compose up"
sudo docker compose --env-file .env up -d

echo ">>> wait health"
for i in $(seq 1 60); do
  if curl -fsS http://127.0.0.1/api/health >/tmp/h.json 2>/dev/null; then
    cat /tmp/h.json
    echo
    echo "=== compose deploy OK $(date -Is) ==="
    exit 0
  fi
  sleep 5
done
echo "health check timeout" >&2
sudo docker compose ps
exit 1
