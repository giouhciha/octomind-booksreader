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
$reportDirectory = Join-Path $PSScriptRoot "..\..\build\reports\emulator"
$standardOutputLog = Join-Path $reportDirectory "emulator-$Port.stdout.log"
$standardErrorLog = Join-Path $reportDirectory "emulator-$Port.stderr.log"

if (-not (Test-Path -LiteralPath $adb) -or -not (Test-Path -LiteralPath $emulator)) {
    throw "No se encontraron adb y emulator dentro de ANDROID_HOME."
}

function Invoke-AdbQuietly {
    param([string[]]$Arguments)

    $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = $adb
    $processInfo.Arguments = ($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join " "
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $processInfo.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::Start($processInfo)
    $standardOutput = $process.StandardOutput.ReadToEnd().Trim()
    $process.StandardError.ReadToEnd() | Out-Null
    $process.WaitForExit()

    return [PSCustomObject]@{
        ExitCode = $process.ExitCode
        Output = $standardOutput
    }
}

$existingState = Invoke-AdbQuietly -Arguments @("-s", $serial, "get-state")
if ($existingState.ExitCode -eq 0 -and $existingState.Output -eq "device") {
    throw "El puerto $Port ya esta ocupado por otro emulador; no se modificara ese dispositivo."
}

Remove-Item -LiteralPath $marker -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path (Split-Path -Parent $marker) -Force | Out-Null
New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
Remove-Item -LiteralPath $standardOutputLog, $standardErrorLog -Force -ErrorAction SilentlyContinue
$arguments = @(
    "-avd", $AvdName,
    "-port", $Port,
    "-no-window",
    "-no-audio",
    "-no-boot-anim",
    "-no-snapshot",
    "-no-snapshot-save",
    "-no-metrics",
    "-gpu", "swiftshader_indirect"
)
$emulatorProcess = Start-Process `
    -FilePath $emulator `
    -ArgumentList $arguments `
    -WindowStyle Hidden `
    -RedirectStandardOutput $standardOutputLog `
    -RedirectStandardError $standardErrorLog `
    -PassThru
$emulatorProcess.Id | Set-Content -LiteralPath $marker -Encoding ascii

$deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
while ([DateTimeOffset]::UtcNow -lt $deadline) {
    $emulatorProcess.Refresh()
    if ($emulatorProcess.HasExited) {
        throw "El proceso del emulador termino con codigo $($emulatorProcess.ExitCode). Revisa $standardOutputLog y $standardErrorLog."
    }

    $state = Invoke-AdbQuietly -Arguments @("-s", $serial, "get-state")
    if ($state.ExitCode -eq 0 -and $state.Output -eq "device") {
        $bootCompleted = Invoke-AdbQuietly -Arguments @("-s", $serial, "shell", "getprop", "sys.boot_completed")
        if ($bootCompleted.ExitCode -eq 0 -and $bootCompleted.Output -eq "1") {
            Invoke-AdbQuietly -Arguments @("-s", $serial, "shell", "input", "keyevent", "82") | Out-Null
            Write-Output "ANDROID_EMULATOR_SERIAL=$serial"
            exit 0
        }
    }
    Start-Sleep -Seconds 3
}

throw "El emulador $AvdName no termino de iniciar en $TimeoutSeconds segundos. Revisa $standardOutputLog y $standardErrorLog."
