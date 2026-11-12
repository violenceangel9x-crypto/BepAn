param([string]$Workbook = 'app/build/outputs/finance-sample.xlsx')
$ErrorActionPreference = 'Stop'
$BepRoot = Split-Path $PSScriptRoot -Parent
$BepWorkbookPath = [IO.Path]::GetFullPath((Join-Path $BepRoot $Workbook))
if (-not (Test-Path -LiteralPath $BepWorkbookPath)) { throw 'Run FinanceExcelTest before this check.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$BepZip = [IO.Compression.ZipFile]::OpenRead($BepWorkbookPath)
try {
    $BepReader = New-Object IO.StreamReader($BepZip.GetEntry('xl/worksheets/sheet2.xml').Open())
    try { [xml]$BepSheetXml = $BepReader.ReadToEnd() } finally { $BepReader.Dispose() }
    $BepExpectedName = $BepSheetXml.SelectSingleNode("//*[local-name()='c' and @r='A4']").InnerText
} finally { $BepZip.Dispose() }
$BepExcel = $null
$BepBook = $null
$BepSummary = $null
$BepGroup = $null
try {
    $BepExcel = New-Object -ComObject Excel.Application
    $BepExcel.Visible = $false
    $BepExcel.DisplayAlerts = $false
    $BepExcel.AskToUpdateLinks = $false
    $BepExcel.AutomationSecurity = 3
    $BepBook = $BepExcel.Workbooks.Open($BepWorkbookPath, 0, $true)
    $BepSummary = $BepBook.Worksheets.Item(1)
    $BepGroup = $BepBook.Worksheets.Item(2)
    $BepResult = [ordered]@{
        fixture = 'FinanceExcelTest synthetic data'
        excelVersion = $BepExcel.Version
        readOnly = [bool]$BepBook.ReadOnly
        sheets = [int]$BepBook.Worksheets.Count
        charge = [long]$BepSummary.Range('B6').Value2
        paid = [long]$BepSummary.Range('B7').Value2
        subsidy = [long]$BepSummary.Range('B10').Value2
        groupColumns = [int]$BepGroup.UsedRange.Columns.Count
        utf8NameRoundTrip = ($BepGroup.Range('A4').Value2 -ceq $BepExpectedName)
        inlineStringIsNotFormula = (-not [bool]$BepGroup.Range('A4').HasFormula)
    }
    if ($BepResult.sheets -ne 6 -or $BepResult.charge -ne 72000 -or $BepResult.paid -ne 100000 -or $BepResult.subsidy -ne 476 -or $BepResult.groupColumns -ne 99 -or -not $BepResult.utf8NameRoundTrip -or -not $BepResult.inlineStringIsNotFormula) { throw 'Excel read-back did not match the synthetic fixture.' }
    $BepEvidence = Join-Path $BepRoot 'docs/evidence/finance'
    New-Item -ItemType Directory -Path $BepEvidence -Force | Out-Null
    $BepResult | ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $BepEvidence 'excel-check.json')
    $BepResult | ConvertTo-Json -Compress
} finally {
    if ($BepBook) { $BepBook.Close($false) }
    if ($BepExcel) { $BepExcel.Quit() }
    foreach ($BepCom in @($BepGroup, $BepSummary, $BepBook, $BepExcel)) {
        if ($BepCom) { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($BepCom) | Out-Null }
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
