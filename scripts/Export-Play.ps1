[CmdletBinding()]
param(
    [string]$OutputPath = 'output/LittleNotes-Play',
    [string]$PublicRepoPath,
    [switch]$AllowUncommitted,
    [switch]$Apply,
    [switch]$Push,
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$resolvedOutput = if ([System.IO.Path]::IsPathRooted($OutputPath)) {
    [System.IO.Path]::GetFullPath($OutputPath)
} else {
    [System.IO.Path]::GetFullPath((Join-Path $repoRoot $OutputPath))
}
if ($Push -and -not $Apply) {
    throw '-Push requires -Apply so the destination is explicit.'
}
if ($Apply -and [string]::IsNullOrWhiteSpace($PublicRepoPath)) {
    throw '-Apply requires -PublicRepoPath pointing to a local LittleNotes-Play clone.'
}
if ($PublicRepoPath -and ([System.IO.Path]::GetFullPath($PublicRepoPath) -eq $repoRoot)) {
    throw 'The public destination cannot be the private source repository.'
}

function Invoke-Git([string[]]$Arguments) {
    & git -C $repoRoot @Arguments
    if ($LASTEXITCODE -ne 0) { throw "git $($Arguments -join ' ') failed with exit code $LASTEXITCODE" }
}

if (-not $AllowUncommitted) {
    & git -C $repoRoot diff --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Working tree has unstaged changes. Commit them or pass -AllowUncommitted for a local preview.' }
    & git -C $repoRoot diff --cached --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Index has staged changes. Commit them or pass -AllowUncommitted for a local preview.' }
}

if (Test-Path $resolvedOutput) {
    $outputParent = [System.IO.Path]::GetFullPath((Split-Path $resolvedOutput -Parent))
    $allowedParent = [System.IO.Path]::GetFullPath((Join-Path $repoRoot 'output'))
    if (-not $outputParent.StartsWith($allowedParent, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove an export directory outside $allowedParent"
    }
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}
New-Item -ItemType Directory -Force $resolvedOutput | Out-Null

if ($AllowUncommitted) {
    $excluded = @('.git', '.gradle', '.kotlin', '.tools', 'output')
    foreach ($item in Get-ChildItem -LiteralPath $repoRoot -Force) {
        if ($excluded -notcontains $item.Name) {
            Copy-Item -LiteralPath $item.FullName -Destination (Join-Path $resolvedOutput $item.Name) -Recurse -Force
        }
    }
} else {
    $archive = Join-Path $env:TEMP ('littlenotes-' + [guid]::NewGuid().ToString('N') + '.tar')
    try {
        & git -C $repoRoot archive --format=tar --output=$archive HEAD
        if ($LASTEXITCODE -ne 0) { throw 'Unable to create a source archive.' }
        & tar -xf $archive -C $resolvedOutput
        if ($LASTEXITCODE -ne 0) { throw 'Unable to extract the source archive.' }
    } finally {
        Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue
    }
}

foreach ($relative in @(
    'app/src/personal',
    'app_TO_googleplay',
    '.github/workflows/android-google-play.yml',
    'app_TO_googleplay/.github',
    '.gradle',
    '.kotlin',
    '.tools',
    'output',
    'app/build',
    'scripts',
    'CHANGELOG.md',
    'UPDATING.md',
    'verification.md',
    'local.properties'
)) {
    $path = Join-Path $resolvedOutput $relative
    if (Test-Path $path) { Remove-Item -LiteralPath $path -Recurse -Force }
}

# Only the explicitly reviewed public documentation is exported.
$docsPath = Join-Path $resolvedOutput 'docs'
if (Test-Path $docsPath) { Remove-Item -LiteralPath $docsPath -Recurse -Force }
New-Item -ItemType Directory -Force (Join-Path $docsPath 'third_party') | Out-Null
$publicDocs = @(
    'ASSET_SOURCES_PLAY.md',
    'PLAY_CONSOLE_DRAFT.md',
    'PRIVACY_POLICY.md',
    'PLAY_RELEASE.md',
    'PLAY_CHANGELOG.md'
)
foreach ($name in $publicDocs) {
    $source = Join-Path $repoRoot (Join-Path 'docs' $name)
    if (-not (Test-Path $source)) { throw "Missing reviewed public document: $source" }
    Copy-Item -LiteralPath $source -Destination (Join-Path $docsPath $name)
}
Copy-Item -LiteralPath (Join-Path $repoRoot 'docs/third_party/TWEMOJI_LICENSE_GRAPHICS.txt') -Destination (Join-Path $docsPath 'third_party/TWEMOJI_LICENSE_GRAPHICS.txt')
Copy-Item -LiteralPath (Join-Path $repoRoot 'docs/PLAY_README.md') -Destination (Join-Path $resolvedOutput 'README.md')
Copy-Item -LiteralPath (Join-Path $repoRoot 'docs/PLAY_CHANGELOG.md') -Destination (Join-Path $resolvedOutput 'CHANGELOG.md')

# The public repository builds only the safe flavor and never contains a personal source set.
$buildFile = Join-Path $resolvedOutput 'app/build.gradle.kts'
$buildText = Get-Content -LiteralPath $buildFile -Raw
$buildText = [regex]::Replace($buildText, '(?ms)\r?\n\s*create\("personal"\)\s*\{\s*dimension = "distribution"\s*versionNameSuffix = "-personal"\s*\}', '')
Set-Content -LiteralPath $buildFile -Value $buildText -NoNewline

$forbidden = @('src[/\\]personal', '(?i)hololive', '(?i)walfie', '(?i)sigstick', '(?i)holo[_-]')
$files = Get-ChildItem -LiteralPath $resolvedOutput -Recurse -File -Force
foreach ($file in $files) {
    $relative = $file.FullName.Substring($resolvedOutput.Length + 1)
    foreach ($pattern in $forbidden) {
        if ($relative -match $pattern) { throw "Forbidden private path in export: $relative" }
    }
    if ($file.Extension -in @('.kt','.kts','.java','.xml','.md','.txt','.gradle','.ps1','.yml','.yaml','.json','.properties')) {
        $content = Get-Content -LiteralPath $file.FullName -Raw
        foreach ($pattern in $forbidden) {
            if ($content -match $pattern) { throw "Forbidden private text in export: $relative" }
        }
    }
}

if (-not $SkipBuild) {
    Push-Location $resolvedOutput
    try {
        & .\gradlew.bat testPlayDebugUnitTest lintPlayDebug assemblePlayDebug --no-daemon
        if ($LASTEXITCODE -ne 0) { throw 'Play flavor validation failed in the export staging directory.' }
    } finally {
        Pop-Location
    }
}

# Build outputs are verification artifacts, never repository source.
$stagingBuild = Join-Path $resolvedOutput 'app/build'
if (Test-Path $stagingBuild) { Remove-Item -LiteralPath $stagingBuild -Recurse -Force }

$manifest = Get-ChildItem -LiteralPath $resolvedOutput -Recurse -File -Force |
    ForEach-Object { [pscustomobject]@{ Path = $_.FullName.Substring($resolvedOutput.Length + 1); SHA256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash } }
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $resolvedOutput 'PLAY_EXPORT_MANIFEST.json') -Encoding UTF8

if ($Apply) {
    $publicRoot = (Resolve-Path $PublicRepoPath).Path
    if (Test-Path (Join-Path $publicRoot '.git')) {
        & git -C $publicRoot diff --quiet
        if ($LASTEXITCODE -ne 0) { throw 'Public repository has uncommitted changes.' }
    }
    foreach ($item in Get-ChildItem -LiteralPath $publicRoot -Force) {
        if ($item.Name -ne '.git') { Remove-Item -LiteralPath $item.FullName -Recurse -Force }
    }
    foreach ($item in Get-ChildItem -LiteralPath $resolvedOutput -Force) {
        Copy-Item -LiteralPath $item.FullName -Destination (Join-Path $publicRoot $item.Name) -Recurse -Force
    }
    if ($Push) {
        & git -C $publicRoot add -A
        & git -C $publicRoot commit -m 'Sync safe Google Play flavor'
        & git -C $publicRoot push
    }
}

Write-Output "Play export ready: $resolvedOutput"
