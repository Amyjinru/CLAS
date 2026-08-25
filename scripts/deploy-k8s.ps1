param(
    [Parameter(Mandatory = $true)][string]$ImageRegistry,
    [Parameter(Mandatory = $true)][string]$ImageTag,
    [string]$Namespace = 'clas'
)

$ErrorActionPreference = 'Stop'

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl create configmap clas-db-init --namespace $Namespace --from-file=00-schema.sql=database/schema.sql --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/mysql.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/frontend.yaml
kubectl -n $Namespace set image deployment/clas-backend backend="$ImageRegistry/clas-backend`:$ImageTag"
kubectl -n $Namespace set image deployment/clas-frontend frontend="$ImageRegistry/clas-frontend`:$ImageTag"
kubectl -n $Namespace rollout status deployment/clas-mysql --timeout=180s
kubectl -n $Namespace rollout status deployment/clas-redis --timeout=180s
kubectl -n $Namespace rollout status deployment/clas-backend --timeout=180s
kubectl -n $Namespace rollout status deployment/clas-frontend --timeout=180s
