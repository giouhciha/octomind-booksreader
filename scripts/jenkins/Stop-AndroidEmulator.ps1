param([int]$Port = 5554)

$ErrorActionPreference = "Stop"
$marker = Join-Path $PSScriptRoot "..\..\build\jenkins-emulator-$Port.started"

if (-not (Test-Path -LiteralPath $marker)) {
    Write-Output "Jenkins no inicio el emulador $Port; no se cerrara ningun dispositivo."
    exit 0
}

if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    if (Test-Path -LiteralPath $adb) {
        & $adb -s "emulator-$Port" emu kill | Out-Null
    }
}

Remove-Item -LiteralPath $marker -Force -ErrorAction SilentlyContinue
Write-Output "Emulador $Port cerrado."
