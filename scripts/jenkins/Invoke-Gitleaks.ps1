param([string]$Version = "8.29.0")

$ErrorActionPreference = "Stop"
$expectedSha256 = "262f6d6a41ba11892ece339ab866b21a02e4d029e16e8916a9b0b59292001fe7"
$toolDirectory = Join-Path $PSScriptRoot "..\..\build\tools\gitleaks-$Version"
$archive = Join-Path $toolDirectory "gitleaks.zip"
$executable = Join-Path $toolDirectory "gitleaks.exe"
$reportDirectory = Join-Path $PSScriptRoot "..\..\build\reports\gitleaks"
$report = Join-Path $reportDirectory "gitleaks.sarif"

New-Item -ItemType Directory -Path $toolDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null

if (-not (Test-Path -LiteralPath $executable)) {
    $downloadUrl = "https://github.com/gitleaks/gitleaks/releases/download/v$Version/gitleaks_${Version}_windows_x64.zip"
    Invoke-WebRequest -Uri $downloadUrl -OutFile $archive -TimeoutSec 120
    $actualSha256 = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualSha256 -ne $expectedSha256) {
        Remove-Item -LiteralPath $archive -Force
        throw "El checksum del binario de Gitleaks no coincide."
    }
    Expand-Archive -LiteralPath $archive -DestinationPath $toolDirectory -Force
    Remove-Item -LiteralPath $archive -Force
}

& $executable git --redact --report-format sarif --report-path $report .
if ($LASTEXITCODE -ne 0) {
    throw "Gitleaks encontro secretos o no pudo completar el escaneo."
}
