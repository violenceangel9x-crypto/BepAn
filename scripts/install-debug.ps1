param([Parameter(Mandatory = $true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$BepAdb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$BepRoot = Split-Path $PSScriptRoot -Parent
$BepApk = Join-Path $BepRoot 'app/build/outputs/apk/debug/app-debug.apk'
if (-not (Test-Path -LiteralPath $BepAdb)) { throw 'adb not found in the default Android SDK.' }
if (-not (Test-Path -LiteralPath $BepApk)) { throw 'Run scripts/build.ps1 before installing.' }
$BepMetadata = Get-Content -LiteralPath (Join-Path $BepRoot 'app/build/outputs/apk/debug/output-metadata.json') -Raw | ConvertFrom-Json
if ($BepMetadata.applicationId -ne 'vn.bepan.debug') { throw 'Only the Bep An debug package may be installed by this script.' }
$BepState = & $BepAdb -s $Serial get-state
if ($LASTEXITCODE -ne 0 -or $BepState -ne 'device') { throw 'Serial is not in device state. Check adb devices -l and USB authorization.' }
& $BepAdb -s $Serial shell getprop ro.product.model
& $BepAdb -s $Serial install -r $BepApk
if ($LASTEXITCODE -ne 0) { throw 'Debug install failed. The script does not uninstall the app or clear data.' }
& $BepAdb -s $Serial shell am start -n 'vn.bepan.debug/vn.bepan.MainActivity'
