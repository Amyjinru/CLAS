#!/usr/bin/env bash
# One-off: align MySQL root password in an existing data volume with clas-secrets.
set -euo pipefail
export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"
NAMESPACE="${NAMESPACE:-clas}"

kubectl -n "$NAMESPACE" scale deployment/mysql --replicas=0
kubectl -n "$NAMESPACE" wait --for=delete pod -l app=mysql --timeout=120s || true
kubectl -n "$NAMESPACE" delete pod mysql-reset-root --ignore-not-found --wait=true

cat <<'EOF' | kubectl -n "$NAMESPACE" apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: mysql-reset-root
spec:
  restartPolicy: Never
  containers:
    - name: reset
      image: mysql:8.4.4
      command:
        - bash
        - -ec
        - |
          mysqld --skip-grant-tables --skip-networking &
          for i in $(seq 1 60); do
            mysql -uroot -Nse "SELECT 1" >/dev/null 2>&1 && break
            sleep 1
          done
          mysql -uroot <<SQL
          FLUSH PRIVILEGES;
          ALTER USER 'root'@'localhost' IDENTIFIED BY '${TARGET_PASS}';
          CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${TARGET_PASS}';
          ALTER USER 'root'@'%' IDENTIFIED BY '${TARGET_PASS}';
          GRANT ALL ON *.* TO 'root'@'%' WITH GRANT OPTION;
          FLUSH PRIVILEGES;
          SQL
          mysqladmin -uroot -p"${TARGET_PASS}" shutdown
      env:
        - name: TARGET_PASS
          valueFrom:
            secretKeyRef:
              name: clas-secrets
              key: MYSQL_PASSWORD
      volumeMounts:
        - name: data
          mountPath: /var/lib/mysql
  volumes:
    - name: data
      persistentVolumeClaim:
        claimName: mysql-data
EOF

kubectl -n "$NAMESPACE" wait --for=jsonpath='{.status.phase}'=Succeeded pod/mysql-reset-root --timeout=300s
kubectl -n "$NAMESPACE" delete pod mysql-reset-root --ignore-not-found
kubectl -n "$NAMESPACE" scale deployment/mysql --replicas=1
kubectl -n "$NAMESPACE" rollout status deployment/mysql --timeout=300s
kubectl -n "$NAMESPACE" exec deploy/mysql -- sh -c 'mysqladmin ping -h127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent'
kubectl -n "$NAMESPACE" exec deploy/mysql -- sh -c 'mysqladmin ping -hmysql -uroot -p"$MYSQL_ROOT_PASSWORD" --silent'
echo mysql_password_synced
