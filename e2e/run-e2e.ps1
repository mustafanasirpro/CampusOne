param()

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backend = Join-Path $root "backend"
$artifacts = Join-Path $root "test-results"
$pgBin = "C:\Program Files\PostgreSQL\17\bin"
$pgData = Join-Path $env:TEMP "campusone-aura-e2e-pg17"
$pgLog = Join-Path $artifacts "postgres-e2e.log"
$pgErrorLog = Join-Path $artifacts "postgres-e2e-error.log"
$pgPort = 55440
$database = "campusone_aura_e2e"
$backendPort = 18083
$frontendPort = 5174
$backendProcess = $null
$frontendProcess = $null
$postgresProcess = $null
$postgresStartedHere = $false
$exitCode = 1

function Stop-LocalProcessTree([System.Diagnostics.Process]$Process) {
    if ($null -eq $Process -or $Process.HasExited) { return }
    $children = Get-CimInstance Win32_Process |
        Where-Object { $_.ParentProcessId -eq $Process.Id }
    foreach ($child in $children) {
        $childProcess = Get-Process -Id $child.ProcessId -ErrorAction SilentlyContinue
        if ($null -ne $childProcess) { Stop-LocalProcessTree $childProcess }
    }
    Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
}

try {
    Write-Host "[AURA E2E] Preparing isolated PostgreSQL"
    New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
    if (-not (Test-Path (Join-Path $pgBin "initdb.exe"))) {
        throw "PostgreSQL 17 is required for the AURA browser tests."
    }
    if (-not $pgData.StartsWith($env:TEMP, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "The disposable PostgreSQL data directory is outside the temporary directory."
    }
    if (-not (Test-Path (Join-Path $pgData "PG_VERSION"))) {
        & (Join-Path $pgBin "initdb.exe") -D $pgData -U postgres -A trust --no-locale --encoding=UTF8 | Out-Null
    }
    & (Join-Path $pgBin "pg_ctl.exe") -D $pgData status *> $null
    if ($LASTEXITCODE -ne 0) {
        $postgresProcess = Start-Process -FilePath (Join-Path $pgBin "postgres.exe") `
            -ArgumentList @("-D", "`"$pgData`"", "-p", "$pgPort") `
            -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $pgLog -RedirectStandardError $pgErrorLog
        $postgresStartedHere = $true
        $deadline = (Get-Date).AddSeconds(20)
        do {
            Start-Sleep -Milliseconds 250
            if ($postgresProcess.HasExited) {
                throw "The disposable PostgreSQL cluster stopped during startup."
            }
            & (Join-Path $pgBin "pg_isready.exe") -h 127.0.0.1 -p $pgPort *> $null
            $postgresReady = $LASTEXITCODE -eq 0
        } while (-not $postgresReady -and (Get-Date) -lt $deadline)
        if (-not $postgresReady) { throw "The disposable PostgreSQL cluster did not start." }
    }
    & (Join-Path $pgBin "dropdb.exe") -h 127.0.0.1 -p $pgPort -U postgres --if-exists $database
    if ($LASTEXITCODE -ne 0) { throw "The previous AURA E2E database could not be removed." }
    & (Join-Path $pgBin "createdb.exe") -h 127.0.0.1 -p $pgPort -U postgres $database
    if ($LASTEXITCODE -ne 0) { throw "The AURA E2E database could not be created." }

    Write-Host "[AURA E2E] Packaging backend"
    Push-Location $backend
    try { & mvn -q -DskipTests package } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw "The backend package build failed." }

    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
    $env:DB_URL = "jdbc:postgresql://127.0.0.1:$pgPort/$database"
    $env:DB_USERNAME = "postgres"
    $env:DB_PASSWORD = ""
    $env:SERVER_PORT = "$backendPort"
    $env:AUTH_COOKIE_SECURE = "false"
    $env:AUTH_COOKIE_SAME_SITE = "Lax"
    $env:APP_CORS_ALLOWED_ORIGINS = "http://127.0.0.1:$frontendPort"
    $env:MAIL_PROVIDER = "disabled"
    $env:PLAYWRIGHT_BACKEND_URL = "http://127.0.0.1:$backendPort/api/v1"
    $env:PLAYWRIGHT_PSQL = Join-Path $pgBin "psql.exe"
    $env:PLAYWRIGHT_DATABASE = $database
    $env:PLAYWRIGHT_POSTGRES_PORT = "$pgPort"
    $env:VITE_API_BASE_URL = $env:PLAYWRIGHT_BACKEND_URL

    $jar = Get-ChildItem (Join-Path $backend "target\campusone-backend-*.jar") |
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1
    Write-Host "[AURA E2E] Starting backend"
    $backendProcess = Start-Process -FilePath "java" -ArgumentList @("-jar", $jar.FullName) `
        -WorkingDirectory $backend -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $artifacts "backend-e2e.log") `
        -RedirectStandardError (Join-Path $artifacts "backend-e2e-error.log")
    $deadline = (Get-Date).AddSeconds(60)
    do {
        Start-Sleep -Milliseconds 500
        if ($backendProcess.HasExited) { throw "The E2E backend stopped during startup." }
        try {
            $health = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 `
                "http://127.0.0.1:$backendPort/api/v1/health"
        } catch { $health = $null }
    } while (($null -eq $health -or $health.StatusCode -ne 200) -and (Get-Date) -lt $deadline)
    if ($null -eq $health -or $health.StatusCode -ne 200) { throw "The E2E backend did not become healthy." }

    Write-Host "[AURA E2E] Starting frontend"
    $frontendProcess = Start-Process -FilePath "npm.cmd" `
        -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1", "--port", "$frontendPort") `
        -WorkingDirectory $root -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $artifacts "frontend-e2e.log") `
        -RedirectStandardError (Join-Path $artifacts "frontend-e2e-error.log")
    $deadline = (Get-Date).AddSeconds(45)
    do {
        Start-Sleep -Milliseconds 300
        if ($frontendProcess.HasExited) { throw "The E2E frontend stopped during startup." }
        try {
            $frontend = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 "http://127.0.0.1:$frontendPort"
        } catch { $frontend = $null }
    } while (($null -eq $frontend -or $frontend.StatusCode -ne 200) -and (Get-Date) -lt $deadline)
    if ($null -eq $frontend -or $frontend.StatusCode -ne 200) { throw "The E2E frontend did not become healthy." }

    Push-Location $root
    try {
        Write-Host "[AURA E2E] Running Playwright"
        $playwrightArguments = @("playwright", "test", "--config=playwright.config.ts")
        if (-not [string]::IsNullOrWhiteSpace($env:PLAYWRIGHT_GREP)) {
            $playwrightArguments += @("--grep", $env:PLAYWRIGHT_GREP)
        }
        & npx.cmd @playwrightArguments
        $exitCode = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    Stop-LocalProcessTree $frontendProcess
    Stop-LocalProcessTree $backendProcess
    if ($postgresStartedHere) {
        & (Join-Path $pgBin "pg_ctl.exe") -D $pgData -m fast -w stop *> $null
    }
}

exit $exitCode
