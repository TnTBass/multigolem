param(
    [string] $ProjectId = "",
    [Parameter(Mandatory = $true)]
    [string] $FileId,
    [string] $Version = "",
    [string] $ChangelogPath = ""
)

$ErrorActionPreference = "Stop"

$token = $env:CURSEFORGE_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "CURSEFORGE_TOKEN is not set."
}

if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    $ProjectId = $env:CURSEFORGE_PROJECT_ID
}

if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    throw "CURSEFORGE_PROJECT_ID is not set and -ProjectId was not provided."
}

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Version)) {
    $gradleProperties = Get-Content -LiteralPath (Join-Path $root "gradle.properties")
    $Version = ($gradleProperties | Where-Object { $_ -like "mod_version=*" } | Select-Object -First 1) -replace "^mod_version=", ""
}

if ([string]::IsNullOrWhiteSpace($Version)) {
    throw "Version was not provided and could not be read from gradle.properties."
}

$uploadDir = Join-Path $root "build/curseforge-upload"
$metadataPath = Join-Path $uploadDir "curseforge-update-file-metadata.json"
New-Item -ItemType Directory -Force -Path $uploadDir | Out-Null
$curl = if ($IsWindows) { "curl.exe" } else { "curl" }

function Get-Changelog {
    param(
        [string] $Version,
        [string] $Path
    )

    if (![string]::IsNullOrWhiteSpace($Path)) {
        $resolvedPath = Join-Path $root $Path
        if (Test-Path -LiteralPath $resolvedPath) {
            $content = ((Get-Content -Raw -LiteralPath $resolvedPath) ?? "").Trim()
            if (![string]::IsNullOrWhiteSpace($content)) {
                return $content
            }
        }
    }

    $lines = Get-Content -LiteralPath (Join-Path $root "CHANGELOG.md")
    $capturing = $false
    $captured = New-Object System.Collections.Generic.List[string]
    $escapedVersion = [regex]::Escape($Version)
    $sectionPattern = "^##\s+$escapedVersion(\s+[-–—]\s+.*)?$"
    foreach ($line in $lines) {
        if ($line -match $sectionPattern) {
            $capturing = $true
            continue
        }

        if ($capturing -and $line -like "## *") {
            break
        }

        if ($capturing) {
            $captured.Add($line)
        }
    }

    $changelog = ($captured -join "`n").Trim()
    if ([string]::IsNullOrWhiteSpace($changelog)) {
        throw "Could not find non-empty changelog section for version '$Version'."
    }

    return $changelog
}

$metadata = @{
    fileID = [int]$FileId
    changelog = Get-Changelog -Version $Version -Path $ChangelogPath
    changelogType = "markdown"
} | ConvertTo-Json -Depth 10
$metadata | Set-Content -LiteralPath $metadataPath -Encoding UTF8

$updateResponse = & $curl -sS `
    -X POST "https://minecraft.curseforge.com/api/projects/$ProjectId/update-file" `
    -H "X-Api-Token: $token" `
    -H "Accept: application/json" `
    -F "metadata=<$metadataPath;type=application/json"

if ($LASTEXITCODE -ne 0) {
    throw "curl failed while updating CurseForge file changelog."
}

$curseForgeFile = $updateResponse | ConvertFrom-Json
if ($curseForgeFile.error -or $curseForgeFile.errors) {
    throw "CurseForge file changelog update failed: $updateResponse"
}

[pscustomobject]@{
    ProjectId = $ProjectId
    FileId = [int]$FileId
    VersionNumber = $Version
    ChangelogUpdated = $true
} | ConvertTo-Json -Depth 4
