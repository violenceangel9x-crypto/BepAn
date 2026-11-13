param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [ValidateSet('core','presentation','recovery','regression','talkback','configuration')][string]$Suite='core',
    [switch]$SkipBuild
)
$ErrorActionPreference='Stop'
$BepRoot=Split-Path $PSScriptRoot -Parent
$BepAdb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if ($Serial -notmatch '^[A-Za-z0-9_.:-]+$') { throw 'Invalid device serial.' }
Push-Location $BepRoot
try {
    if(-not $SkipBuild) { & "$PSScriptRoot/build.ps1" -Tasks @(':app:assembleDebug',':app:assembleDebugAndroidTest'); if($LASTEXITCODE -ne 0) { throw 'Build failed.' } }
    foreach($BepApk in @('app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $BepAdb -s $Serial install -r $BepApk
        if($LASTEXITCODE -ne 0) { throw 'Update install failed; do not uninstall or clear app data.' }
    }
    $BepClasses=switch($Suite) {
        'core' { @('vn.bepan.FinanceNativeTest') }
        'presentation' { @('vn.bepan.FinancePresentationTest') }
        'recovery' { @('vn.bepan.FinanceRecoveryTest#prepareBeforeProcessDeath','vn.bepan.FinanceRecoveryTest#restoreAndCommitAfterProcessDeath','vn.bepan.FinanceRecoveryTest#savedReceiptSurvivesSecondProcessDeath') }
        'regression' { @('vn.bepan.NativeFlowTest') }
        'talkback' { @('vn.bepan.TalkBackFinanceTest') }
        'configuration' { @('vn.bepan.ConfigurationNativeTest') }
    }
    $BepRuns=@()
    $BepRawDir=Join-Path $BepRoot ".tools/native-finance/$Serial"
    New-Item -ItemType Directory -Path $BepRawDir -Force | Out-Null
    foreach($BepClass in $BepClasses) {
        if($Suite -eq 'recovery') { & $BepAdb -s $Serial shell am force-stop vn.bepan.debug }
        # Windows PowerShell wraps harmless emulator stderr as NativeCommandError.
        $ErrorActionPreference='Continue'
        try { $BepOutput=(& $BepAdb -s $Serial shell am instrument -w -r -e class $BepClass vn.bepan.debug.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | Out-String) }
        finally { $ErrorActionPreference='Stop' }
        $BepOutput | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $BepRawDir (($BepClass -replace '[^A-Za-z0-9_.-]','_')+'.txt'))
        $BepPassed=$BepOutput -match 'OK \((\d+) tests?\)'
        $BepCount=if($BepPassed) { [int]$Matches[1] } else { 0 }
        $BepSkipped=[regex]::Matches($BepOutput,'INSTRUMENTATION_STATUS_CODE: -(3|4)').Count
        $BepRuns+=@{class=$BepClass;passed=$BepPassed;tests=$BepCount;skipped=$BepSkipped}
        Write-Output "$BepClass : passed=$BepPassed tests=$BepCount skipped=$BepSkipped"
        if(-not $BepPassed) { Write-Output $BepOutput; throw 'Native acceptance failed. Raw test output is in .tools/native-finance.' }
    }
    $BepEvidence=Join-Path $BepRoot 'docs/evidence/finance'
    New-Item -ItemType Directory -Path $BepEvidence -Force | Out-Null
    $BepResult=[ordered]@{
        device=$Serial; sdk=((& $BepAdb -s $Serial shell getprop ro.build.version.sdk) | Out-String).Trim()
        fontScale=((& $BepAdb -s $Serial shell settings get system font_scale) | Out-String).Trim()
        timestampUtc=[DateTime]::UtcNow.ToString('o'); suite=$Suite; runs=$BepRuns
        apkSha256=(Get-FileHash 'app/build/outputs/apk/debug/app-debug.apk' -Algorithm SHA256).Hash
        testApkSha256=(Get-FileHash 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk' -Algorithm SHA256).Hash
    }
    $BepResult | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $BepEvidence "$Serial-$Suite.json")
} finally { Pop-Location }
