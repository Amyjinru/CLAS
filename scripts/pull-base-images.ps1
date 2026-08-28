param(
    [string]$Mirror = 'docker.1ms.run/library'
)

$ErrorActionPreference = 'Stop'

$images = @(
    'maven:3.9.9-eclipse-temurin-17',
    'eclipse-temurin:17-jre-alpine',
    'node:22.14-alpine',
    'nginx:1.27-alpine',
    'mysql:8.4.4',
    'redis:7.4.2-alpine'
)

foreach ($image in $images) {
    $mirrorImage = "$Mirror/$image"
    Write-Host "Pulling $mirrorImage ..."
    docker pull $mirrorImage
    docker tag $mirrorImage $image
    Write-Host "Tagged $image"
}

Write-Host ''
Write-Host 'Base images ready. Next:'
Write-Host '  docker compose --env-file .env build --pull=false'
Write-Host '  docker compose --env-file .env up -d'
Write-Host '  .\scripts\compose.ps1 smoke'
