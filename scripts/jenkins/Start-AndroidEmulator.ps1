param(
    [string]$AvdName = "Pixel_10_Pro_XL",
    [int]$Port = 5554,
    [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    throw "ANDROID_HOME no esta configurado."
}

$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
$emulator = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"
$serial = "emulator-$Port"
$marker = Join-Path $PSScriptRoot "..\..\build\jenkins-emulator-$Port.started"

if (-not (Test-Path -LiteralPath $adb) -or -not (Test-Path -LiteralPath $emulator)) {
    throw "No se encontraron adb y emulator dentro de ANDROID_HOME."
}

$existingState = & $adb -s $serial get-state 2>$null
if ($LASTEXITCODE -eq 0 -and $existingState -eq "device") {
    throw "El puerto $Port ya esta ocupado por otro emulador; no se modificara ese dispositivo."
}

Remove-Item -LiteralPath $marker -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path (Split-Path -Parent $marker) -Force | Out-Null
$arguments = @(
    "-avd", $AvdName,
    "-port", $Port,
    "-no-window",
    "-no-audio",
    "-no-boot-anim",
    "-no-snapshot-save",
    "-gpu", "swiftshader_indirect"
)
Start-Process -FilePath $emulator -ArgumentList $arguments -WindowStyle Hidden
New-Item -ItemType File -Path $marker -Force | Out-Null

$deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
while ([DateTimeOffset]::UtcNow -lt $deadline) {
    $state = & $adb -s $serial get-state 2>$null
    if ($LASTEXITCODE -eq 0 -and $state -eq "device") {
        $bootCompleted = (& $adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
        if ($bootCompleted -eq "1") {
            & $adb -s $serial shell input keyevent 82 | Out-Null
            Write-Output "ANDROID_EMULATOR_SERIAL=$serial"
            exit 0
        }
    }
    Start-Sleep -Seconds 3
}

throw "El emulador $AvdName no termino de iniciar en $TimeoutSeconds segundos."
