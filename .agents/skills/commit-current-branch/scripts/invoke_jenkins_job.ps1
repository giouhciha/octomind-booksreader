param(
    [string]$JenkinsUrl = "http://localhost:8080",
    [string]$JobName = "octomind-booksreader",
    [int]$TimeoutSeconds = 900,
    [int]$PollSeconds = 3
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:JENKINS_USER) -or
    [string]::IsNullOrWhiteSpace($env:JENKINS_API_TOKEN)) {
    throw "Configura JENKINS_USER y JENKINS_API_TOKEN antes de ejecutar Jenkins."
}

$encodedCredentials = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes("$($env:JENKINS_USER):$($env:JENKINS_API_TOKEN)")
)
$headers = @{ Authorization = "Basic $encodedCredentials" }
$baseUrl = $JenkinsUrl.TrimEnd("/")
$escapedJobName = [Uri]::EscapeDataString($JobName)
$jobUrl = "$baseUrl/job/$escapedJobName"

try {
    $crumb = Invoke-RestMethod `
        -Uri "$baseUrl/crumbIssuer/api/json" `
        -Headers $headers `
        -Method Get `
        -TimeoutSec 15
    $headers[$crumb.crumbRequestField] = $crumb.crumb
} catch {
    Write-Host "Jenkins no entrego un crumb CSRF; se intentara con el token de API."
}

$triggerResponse = Invoke-WebRequest `
    -Uri "$jobUrl/build?delay=0sec" `
    -Headers $headers `
    -Method Post `
    -TimeoutSec 30

$queueUrl = $triggerResponse.Headers.Location
if ([string]::IsNullOrWhiteSpace($queueUrl)) {
    throw "Jenkins acepto la solicitud, pero no devolvio la ubicacion de la cola."
}
if (-not [Uri]::IsWellFormedUriString($queueUrl, [UriKind]::Absolute)) {
    $queueUrl = ([Uri]::new([Uri]$baseUrl, $queueUrl)).AbsoluteUri
}

$deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
$buildNumber = $null

while ([DateTimeOffset]::UtcNow -lt $deadline) {
    $queueItem = Invoke-RestMethod `
        -Uri "$($queueUrl.TrimEnd('/'))/api/json" `
        -Headers $headers `
        -Method Get `
        -TimeoutSec 15

    if ($queueItem.cancelled) {
        throw "La ejecucion de Jenkins fue cancelada mientras estaba en cola."
    }
    if ($null -ne $queueItem.executable.number) {
        $buildNumber = [int]$queueItem.executable.number
        break
    }
    Start-Sleep -Seconds $PollSeconds
}

if ($null -eq $buildNumber) {
    throw "Jenkins no inicio el build antes del limite de $TimeoutSeconds segundos."
}

$buildUrl = "$jobUrl/$buildNumber/"
while ([DateTimeOffset]::UtcNow -lt $deadline) {
    $build = Invoke-RestMethod `
        -Uri "$buildUrl/api/json" `
        -Headers $headers `
        -Method Get `
        -TimeoutSec 15

    if (-not $build.building) {
        Write-Output "JENKINS_BUILD_NUMBER=$buildNumber"
        Write-Output "JENKINS_BUILD_URL=$buildUrl"
        Write-Output "JENKINS_BUILD_RESULT=$($build.result)"
        if ($build.result -ne "SUCCESS") {
            exit 1
        }
        exit 0
    }
    Start-Sleep -Seconds $PollSeconds
}

throw "Jenkins no termino el build $buildNumber antes del limite de $TimeoutSeconds segundos."
