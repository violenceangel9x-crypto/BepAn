param([string[]]$Tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (-not $env:JAVA_HOME) {
        $BepJdk = Get-ChildItem -LiteralPath '.tools/jdk' -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($BepJdk) { $env:JAVA_HOME = $BepJdk.FullName }
    }
    if (-not (Test-Path 'local.properties')) {
        $BepSdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
        if (Test-Path $BepSdk) { 'sdk.dir=' + $BepSdk.Replace('\','/').Replace(':','\:') | Set-Content -Encoding ASCII local.properties }
    }
    & .\gradlew.bat @Tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
} finally { Pop-Location }
