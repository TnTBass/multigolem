$ErrorActionPreference = 'Stop'

# Exercise upload metadata without network requests, credentials, or real uploads.
$root = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path $root ('build/curseforge-upload-test/' + [guid]::NewGuid())
New-Item -ItemType Directory -Force (Join-Path $testRoot 'scripts') | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'upload-curseforge.ps1') -Destination (Join-Path $testRoot 'scripts')
Set-Content -LiteralPath (Join-Path $testRoot 'test.jar') -Value 'fixture'

function Invoke-RestMethod {
    param($Uri, $Headers)
    if ($Uri -ne 'https://minecraft.curseforge.com/api/game/versions') {
        throw "Unexpected API request: $Uri"
    }
    @(
        @{name='26.3'; id=1}, @{name='Fabric'; id=2}, @{name='NeoForge'; id=3},
        @{name='Client'; id=4}, @{name='Server'; id=5}
    )
}

function Invoke-TestCurl {
    $metadata = Get-Content -Raw -LiteralPath (Join-Path $testRoot 'build/curseforge-upload/curseforge-metadata.json') | ConvertFrom-Json
    $expectedLoaderId = if ($testLoader -eq 'fabric') { 2 } else { 3 }
    if (($metadata.gameVersions -join ',') -ne "1,$expectedLoaderId,4,5") {
        throw "Incorrect Minecraft, loader, or environment tags: $($metadata.gameVersions)"
    }
    if ($metadata.changelog -ne $expectedNotes) { throw 'Upload lost the loader-specific changelog.' }
    if ($testLoader -eq 'fabric') {
        if ($metadata.relations.projects.Count -ne 1 -or
            $metadata.relations.projects[0].slug -ne 'fabric-api' -or
            $metadata.relations.projects[0].type -ne 'requiredDependency') {
            throw 'Fabric must retain its required Fabric API dependency.'
        }
    } elseif ($metadata.PSObject.Properties.Name -contains 'relations') {
        throw 'NeoForge must not submit empty relations.'
    }
    $global:LASTEXITCODE = 0
    return $testResponse
}

Set-Alias -Name curl -Value Invoke-TestCurl
Set-Alias -Name curl.exe -Value Invoke-TestCurl
$previousToken = $env:CURSEFORGE_TOKEN
try {
    $env:CURSEFORGE_TOKEN = 'test-placeholder'
    foreach ($testLoader in @('fabric', 'neoforge')) {
        $loaderName = if ($testLoader -eq 'fabric') { 'Fabric' } else { 'NeoForge' }
        $expectedNotes = "- Updated compatibility to Minecraft 26.3 for $loaderName."
        Set-Content -LiteralPath (Join-Path $testRoot 'notes.md') -Value $expectedNotes
        $testResponse = '{"id":12345}'
        $uploadArgs = @{
            ProjectId='test'; Loader=$testLoader; Version='0.8.2+mc26.3'
            JarPath='test.jar'; ChangelogPath='notes.md'
        }
        $result = & (Join-Path $testRoot 'scripts/upload-curseforge.ps1') @uploadArgs | ConvertFrom-Json
        if ($result.FileId -ne 12345) { throw 'Successful upload did not return its file ID.' }
        foreach ($testResponse in @('{"errorCode":100,"errorMessage":"Missing environment"}', '{"id":""}')) {
            $rejected = $false
            try { & (Join-Path $testRoot 'scripts/upload-curseforge.ps1') @uploadArgs | Out-Null }
            catch {
                if ($_.Exception.Message -notmatch 'CurseForge file upload (failed|did not return)') { throw }
                $rejected = $true
            }
            if (-not $rejected) { throw "Upload incorrectly accepted response: $testResponse" }
        }
        Write-Host "$loaderName metadata and response checks passed."
    }
} finally {
    $env:CURSEFORGE_TOKEN = $previousToken
}
