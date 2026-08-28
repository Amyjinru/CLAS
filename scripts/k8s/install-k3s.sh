#!/usr/bin/env bash
set -euo pipefail

if ! command -v curl >/dev/null; then
  echo 'curl is required to install k3s' >&2
  exit 1
fi

curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC='server --write-kubeconfig-mode 600' sh -
sudo k3s kubectl get nodes
sudo k3s kubectl get ingressclass,pvc -A
echo 'k3s installation completed; ensure cloud firewall permits TCP 80 before deployment.'
