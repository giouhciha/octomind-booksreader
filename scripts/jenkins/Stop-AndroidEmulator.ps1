param([int]$Port = 5554)

$ErrorActionPreference = "Stop"
$marker = Join-Path $PSScriptRoot "..\..\build\jenkins-emulator-$Port.started"

if (-not (Test-Path -LiteralPath $marker)) {
    Write-Output "Jenkins no inicio el emulador $Port; no se cerrara ningun dispositivo."
    exit 0
}

$emulatorProcessId = 0
[int]::TryParse((Get-Content -LiteralPath $marker -Raw -ErrorAction SilentlyContinue).Trim(), [ref]$emulatorProcessId) | Out-Null

if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    if (Test-Path -LiteralPath $adb) {
        $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
        $processInfo.FileName = $adb
        $processInfo.Arguments = "-s emulator-$Port emu kill"
        $processInfo.UseShellExecute = $false
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.CreateNoWindow = $true

        $adbProcess = [System.Diagnostics.Process]::Start($processInfo)
        if (-not $adbProcess.WaitForExit(10000)) {
            $adbProcess.Kill()
            $adbProcess.WaitForExit()
        }
    }
}

if ($emulatorProcessId -gt 0) {
    $emulatorProcess = Get-Process -Id $emulatorProcessId -ErrorAction SilentlyContinue
    if ($null -ne $emulatorProcess) {
        $expectedDirectory = (Resolve-Path (Join-Path $env:ANDROID_HOME "emulator")).Path
        if ($emulatorProcess.Path.StartsWith($expectedDirectory, [StringComparison]::OrdinalIgnoreCase)) {
            $taskKill = Join-Path $env:SystemRoot "System32\taskkill.exe"
            $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
            $processInfo.FileName = $taskKill
            $processInfo.Arguments = "/PID $emulatorProcessId /T /F"
            $processInfo.UseShellExecute = $false
            $processInfo.RedirectStandardOutput = $true
            $processInfo.RedirectStandardError = $true
            $processInfo.CreateNoWindow = $true

            $taskKillProcess = [System.Diagnostics.Process]::Start($processInfo)
            if (-not $taskKillProcess.WaitForExit(10000)) {
                $taskKillProcess.Kill()
                $taskKillProcess.WaitForExit()
            }
        }
    }
}

Remove-Item -LiteralPath $marker -Force -ErrorAction SilentlyContinue
Write-Output "Emulador $Port cerrado."
