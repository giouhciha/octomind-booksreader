param(
    [string]$Serial = "emulator-5554",
    [string]$ApplicationId = "com.octomind.booksreader",
    [string]$Activity = ".MainActivity"
)

$ErrorActionPreference = "Stop"
$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
$apk = Join-Path $PSScriptRoot "..\..\app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path -LiteralPath $apk)) {
    throw "No se encontro el APK producido por connectedDebugAndroidTest."
}

& $adb -s $Serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    throw "No fue posible instalar el APK en $Serial."
}

$launchOutput = (& $adb -s $Serial shell am start -W -n "$ApplicationId/$Activity") -join "`n"
if ($LASTEXITCODE -ne 0 -or $launchOutput -notmatch "Status:\s+ok") {
    throw "Android no confirmo la apertura de $ApplicationId. Salida: $launchOutput"
}

$resumedActivity = (& $adb -s $Serial shell dumpsys activity activities) -join "`n"
if ($resumedActivity -notmatch [regex]::Escape($ApplicationId)) {
    throw "La aplicacion se instalo, pero no quedo visible en primer plano."
}

Write-Output "APP_INSTALL=SUCCESS"
Write-Output "APP_LAUNCH=SUCCESS"
