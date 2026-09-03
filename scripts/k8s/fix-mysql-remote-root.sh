#!/usr/bin/env bash
# Ensure root can connect from other pods (migrate job) on existing MySQL data volumes.
set -euo pipefail
export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"
NAMESPACE="${NAMESPACE:-clas}"

kubectl -n "$NAMESPACE" exec deploy/mysql -- sh -c '
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "SELECT user,host FROM mysql.user WHERE user=\"root\";"
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
    CREATE USER IF NOT EXISTS \"root\"@\"%\" IDENTIFIED BY \"${MYSQL_ROOT_PASSWORD}\";
    ALTER USER \"root\"@\"%\" IDENTIFIED BY \"${MYSQL_ROOT_PASSWORD}\";
    GRANT ALL PRIVILEGES ON *.* TO \"root\"@\"%\" WITH GRANT OPTION;
    FLUSH PRIVILEGES;
  "
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -h mysql -Nse "SELECT 1" >/dev/null
  echo mysql_remote_root_ok
'
