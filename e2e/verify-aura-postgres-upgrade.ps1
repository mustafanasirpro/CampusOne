param()

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pgBin = "C:\Program Files\PostgreSQL\17\bin"
$pgData = Join-Path $env:TEMP "campusone-aura-v35-pg"
$pgPort = 55439
$database = "campusone_aura_v35_upgrade"
$serverPort = 18084
$app = $null
$postgres = $null
$postgresStartedHere = $false

function Wait-For-Health([System.Diagnostics.Process]$Process) {
    $deadline = (Get-Date).AddSeconds(60)
    do {
        Start-Sleep -Milliseconds 500
        if ($Process.HasExited) { throw "The upgrade verification backend stopped during startup." }
        try {
            $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 `
                "http://127.0.0.1:$serverPort/api/v1/health"
        } catch { $response = $null }
    } while (($null -eq $response -or $response.StatusCode -ne 200) -and (Get-Date) -lt $deadline)
    if ($null -eq $response -or $response.StatusCode -ne 200) {
        throw "The upgrade verification backend did not become healthy."
    }
}

function Stop-Backend([System.Diagnostics.Process]$Process) {
    if ($null -ne $Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force
        $Process.WaitForExit()
    }
}

try {
    if (-not (Test-Path (Join-Path $pgData "PG_VERSION"))) {
        & (Join-Path $pgBin "initdb.exe") -D $pgData -U postgres -A trust `
            --no-locale --encoding=UTF8 | Out-Null
    }
    & (Join-Path $pgBin "pg_ctl.exe") -D $pgData status *> $null
    if ($LASTEXITCODE -ne 0) {
        $postgres = Start-Process -FilePath (Join-Path $pgBin "postgres.exe") `
            -ArgumentList @("-D", "`"$pgData`"", "-p", "$pgPort") `
            -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput "test-results/postgres-upgrade.log" `
            -RedirectStandardError "test-results/postgres-upgrade-error.log"
        $postgresStartedHere = $true
        $deadline = (Get-Date).AddSeconds(20)
        do {
            Start-Sleep -Milliseconds 250
            if ($postgres.HasExited) { throw "The upgrade PostgreSQL cluster stopped during startup." }
            & (Join-Path $pgBin "pg_isready.exe") -h 127.0.0.1 -p $pgPort *> $null
            $postgresReady = $LASTEXITCODE -eq 0
        } while (-not $postgresReady -and (Get-Date) -lt $deadline)
        if (-not $postgresReady) { throw "The upgrade PostgreSQL cluster did not become ready." }
    }

    & (Join-Path $pgBin "dropdb.exe") -h 127.0.0.1 -p $pgPort `
        -U postgres --if-exists $database
    & (Join-Path $pgBin "createdb.exe") -h 127.0.0.1 -p $pgPort `
        -U postgres $database

    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
    $env:DB_URL = "jdbc:postgresql://127.0.0.1:$pgPort/$database"
    $env:DB_USERNAME = "postgres"
    $env:DB_PASSWORD = ""
    $env:SERVER_PORT = "$serverPort"
    $env:AUTH_COOKIE_SECURE = "false"
    $env:AUTH_COOKIE_SAME_SITE = "Lax"
    $env:APP_CORS_ALLOWED_ORIGINS = "http://127.0.0.1:5174"
    $env:MAIL_PROVIDER = "disabled"

    $jar = Get-ChildItem (Join-Path $root "backend\target\campusone-backend-*.jar") |
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1
    if ($null -eq $jar) { throw "Package the backend before running upgrade verification." }

    $app = Start-Process -FilePath "java" `
        -ArgumentList @("-jar", $jar.FullName, "--spring.flyway.target=34") `
        -WorkingDirectory (Join-Path $root "backend") -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "test-results/backend-upgrade-v34.log" `
        -RedirectStandardError "test-results/backend-upgrade-v34-error.log"
    Wait-For-Health $app
    $versionBefore = & (Join-Path $pgBin "psql.exe") -h 127.0.0.1 `
        -p $pgPort -U postgres -d $database -tAc `
        "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"
    Stop-Backend $app
    $app = $null

    $app = Start-Process -FilePath "java" -ArgumentList @("-jar", $jar.FullName) `
        -WorkingDirectory (Join-Path $root "backend") -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "test-results/backend-upgrade-v35.log" `
        -RedirectStandardError "test-results/backend-upgrade-v35-error.log"
    Wait-For-Health $app
    $versionAfter = & (Join-Path $pgBin "psql.exe") -h 127.0.0.1 `
        -p $pgPort -U postgres -d $database -tAc `
        "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"
    if ($versionBefore.Trim() -ne "34" -or $versionAfter.Trim() -ne "35") {
        throw "Expected a V34 to V35 upgrade."
    }
    Write-Output "UPGRADE_FROM=34 UPGRADE_TO=35 HEALTH=PASS"
} finally {
    Stop-Backend $app
    if ($postgresStartedHere) {
        & (Join-Path $pgBin "pg_ctl.exe") -D $pgData -m fast -w stop *> $null
    }
}
