param([string]$Workbook = 'app/build/outputs/finance-sample.xlsx', [switch]$FullMonth)
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
        fixture = $(if ($FullMonth) { 'FullMonthTest synthetic February 2028' } else { 'FinanceExcelTest synthetic data' })
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
    $BepExpected = if ($FullMonth) { @(8,5472000,6000001,22780) } else { @(6,72000,100000,476) }
    if ($BepResult.sheets -ne $BepExpected[0] -or $BepResult.charge -ne $BepExpected[1] -or $BepResult.paid -ne $BepExpected[2] -or $BepResult.subsidy -ne $BepExpected[3] -or $BepResult.groupColumns -ne 99 -or -not $BepResult.utf8NameRoundTrip -or -not $BepResult.inlineStringIsNotFormula) { throw 'Excel read-back did not match the synthetic fixture.' }
    if($FullMonth) {
        $BepChargeSum=0L; $BepPaidSum=0L; $BepSubsidySum=0.0
        foreach($BepIndex in 2..4) {
            $BepGroupSheet=$BepBook.Worksheets.Item($BepIndex)
            try {
                $BepLastRow=$BepGroupSheet.UsedRange.Rows.Count
                $BepChargeSum += [long]$BepGroupSheet.Cells.Item($BepLastRow,92).Value2
                $BepPaidSum += [long]$BepGroupSheet.Cells.Item($BepLastRow,93).Value2
                $BepSubsidySum += [double]$BepGroupSheet.Cells.Item($BepLastRow,99).Value2
            } finally { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($BepGroupSheet) | Out-Null }
        }
        $BepResult.groupChargeSum=$BepChargeSum; $BepResult.groupAllocationSum=$BepPaidSum
        $BepResult.roundedGroupSubsidy=[long][Math]::Round($BepSubsidySum,0,[MidpointRounding]::AwayFromZero)
        $BepResult.outstanding=[long]$BepSummary.Range('B8').Value2
        $BepResult.surplus=[long]$BepSummary.Range('B9').Value2
        $BepResult.receivedSubsidy=[long]$BepSummary.Range('B11').Value2
        $BepResult.cashOut=[long]$BepSummary.Range('B14').Value2
        $BepResult.cashBalance=[long]$BepSummary.Range('B15').Value2
        if($BepChargeSum -ne 5472000 -or $BepPaidSum -ne 6000001 -or $BepResult.roundedGroupSubsidy -ne 22780 -or $BepResult.outstanding -ne 30000 -or $BepResult.surplus -ne 362000 -or $BepResult.receivedSubsidy -ne 22780 -or $BepResult.cashOut -ne 85000 -or $BepResult.cashBalance -ne 6897380) { throw 'Full-month group and cash totals differ from Room golden fixture.' }
    }
    $BepEvidence = Join-Path $BepRoot 'docs/evidence/finance'
    New-Item -ItemType Directory -Path $BepEvidence -Force | Out-Null
    $BepEvidenceName = if ($FullMonth) { 'excel-full-month-check.json' } else { 'excel-check.json' }
    $BepResult | ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $BepEvidence $BepEvidenceName)
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
