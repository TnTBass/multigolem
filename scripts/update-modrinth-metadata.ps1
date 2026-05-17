param(
    [string] $Slug = "multigolem",
    [string] $VersionId = "",
    [string] $Version = "",
    [string] $ChangelogPath = "",
    [switch] $SyncDescription,
    [switch] $SyncVersionChangelog
)

$ErrorActionPreference = "Stop"

$token = $env:MODRINTH_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "MODRINTH_TOKEN is not set."
}

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Version)) {
    $gradleProperties = Get-Content -LiteralPath (Join-Path $root "gradle.properties")
    $Version = ($gradleProperties | Where-Object { $_ -like "mod_version=*" } | Select-Object -First 1) -replace "^mod_version=", ""
}

if ([string]::IsNullOrWhiteSpace($Version) -and $SyncVersionChangelog) {
    throw "Version was not provided and could not be read from gradle.properties."
}

$headers = @{
    "Authorization" = $token
    "User-Agent" = "TnTBass/multigolem Modrinth metadata update"
    "Accept" = "application/json"
}

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

function Get-ListingBody {
    $listingDoc = Join-Path $root "docs/modrinth-listing.md"
    if (!(Test-Path -LiteralPath $listingDoc)) {
        throw "docs/modrinth-listing.md not found."
    }

    $listingContent = (Get-Content -Raw -LiteralPath $listingDoc) -replace "`r`n", "`n"
    $descMatch = [regex]::Match($listingContent, '## Project Description\n+````markdown\n([\s\S]*?)````')
    if (!$descMatch.Success) {
        throw "Could not find '## Project Description' block in docs/modrinth-listing.md."
    }

    return $descMatch.Groups[1].Value.TrimEnd()
}

$result = [ordered]@{
    ProjectSlug = $Slug
}

if ($SyncDescription) {
    $descBody = Get-ListingBody
    $descPayload = [System.Text.Encoding]::UTF8.GetBytes((@{ body = $descBody } | ConvertTo-Json -Depth 2))
    Invoke-RestMethod `
        -Uri "https://api.modrinth.com/v2/project/$Slug" `
        -Method PATCH `
        -Headers $headers `
        -ContentType "application/json" `
        -Body $descPayload | Out-Null

    $project = Invoke-RestMethod `
        -Uri "https://api.modrinth.com/v2/project/$Slug" `
        -Headers $headers
    $result.ProjectBodyHasConfig = $project.body.Contains("config/multigolem.json")
    $result.ProjectBodyStartsWith = $project.body.Substring(0, [Math]::Min(120, $project.body.Length))
}

if ($SyncVersionChangelog) {
    if ([string]::IsNullOrWhiteSpace($VersionId)) {
        throw "VersionId is required when syncing a version changelog."
    }

    $changelog = Get-Changelog -Version $Version -Path $ChangelogPath
    $versionPayload = [System.Text.Encoding]::UTF8.GetBytes((@{ changelog = $changelog } | ConvertTo-Json -Depth 2))
    Invoke-RestMethod `
        -Uri "https://api.modrinth.com/v2/version/$VersionId" `
        -Method PATCH `
        -Headers $headers `
        -ContentType "application/json" `
        -Body $versionPayload | Out-Null

    $versionReadback = Invoke-RestMethod `
        -Uri "https://api.modrinth.com/v2/version/$VersionId" `
        -Headers $headers
    $result.VersionId = $VersionId
    $result.VersionNumber = $versionReadback.version_number
    $result.VersionChangelogHasHealthFix = $versionReadback.changelog.Contains("losing their extra health")
    $result.VersionChangelogStartsWith = $versionReadback.changelog.Substring(0, [Math]::Min(120, $versionReadback.changelog.Length))
}

$result | ConvertTo-Json -Depth 4
