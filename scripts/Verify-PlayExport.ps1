[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path $Path).Path
$forbidden = @('src[/\\]personal', '(?i)hololive', '(?i)walfie', '(?i)sigstick', '(?i)holo[_-]')

foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -File -Force) {
    $relative = $file.FullName.Substring($root.Length + 1)
    foreach ($pattern in $forbidden) {
        if ($relative -match $pattern) { throw "Forbidden private path: $relative" }
    }
    if ($file.Extension -in @('.kt','.kts','.java','.xml','.md','.txt','.gradle','.ps1','.yml','.yaml','.json','.properties')) {
        $content = Get-Content -LiteralPath $file.FullName -Raw
        foreach ($pattern in $forbidden) {
            if ($content -match $pattern) { throw "Forbidden private text: $relative" }
        }
    }
}

if (-not (Test-Path (Join-Path $root 'app/src/play/assets/stickers'))) { throw 'Missing Play sticker assets.' }
if (Test-Path (Join-Path $root 'app/src/personal')) { throw 'Personal source set must not be exported.' }
if (Test-Path (Join-Path $root 'app/build')) { throw 'Generated app/build output must not be exported.' }
Write-Output "Play export is clean: $root"
