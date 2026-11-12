param([ValidateSet('start','test')][string]$Mode = 'start')
$ErrorActionPreference = 'Stop'
$BepRoot = Split-Path $PSScriptRoot -Parent
$BepNodeCommand = Get-Command node -ErrorAction SilentlyContinue
$BepNode = if ($BepNodeCommand) { $BepNodeCommand.Source } else {
    Join-Path $env:ProgramFiles 'Microsoft Visual Studio/2022/Community/MSBuild/Microsoft/VisualStudio/NodeJs/node.exe'
}
if (-not (Test-Path -LiteralPath $BepNode)) { throw 'Node.js 22+ is required in PATH to run the gateway.' }
Push-Location $BepRoot
try {
    if ($Mode -eq 'test') { & $BepNode --test gateway/server.test.mjs }
    else { & $BepNode gateway/start.mjs }
    if ($LASTEXITCODE -ne 0) { throw "Gateway failed: $LASTEXITCODE" }
} finally { Pop-Location }
