# Script de validation des variables d'environnement
# Vérifie que toutes les variables requises sont définies

$requiredEnvVars = @(
    "REDIS_TTL",
    "JWT_SECRET",
    "TWILIO_ACCOUNT_SID",
    "TWILIO_AUTH_TOKEN",
    "TWILIO_FROM_NUMBER",
    "SPRING_PROFILES_ACTIVE",
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD",
    "GOOGLE_CLIENT_ID",
    "GOOGLE_CLIENT_SECRET"
)

$missingVars = @()

foreach ($var in $requiredEnvVars) {
    if (-not (Test-Path "env:$var")) {
        $missingVars += $var
    }
}

if ($missingVars.Count -gt 0) {
    Write-Error "Les variables d'environnement suivantes sont manquantes :"
    foreach ($var in $missingVars) {
        Write-Error "- $var"
    }
    exit 1
} else {
    Write-Host "Toutes les variables d'environnement requises sont définies" -ForegroundColor Green
}
