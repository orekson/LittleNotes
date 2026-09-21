param([string[]]$Tasks = @('testDebugUnitTest','lintDebug','assembleDebug'))
$ErrorActionPreference = 'Stop'
if ($Tasks.Count -eq 1 -and $Tasks[0] -match ',') { $Tasks = $Tasks[0] -split ',' | ForEach-Object { $_.Trim() } }
$projectRoot = $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$toolsRoot = Join-Path $workspaceRoot '.tools'
$env:JAVA_HOME = (Get-ChildItem -LiteralPath (Join-Path $toolsRoot 'jdk') -Directory | Select-Object -First 1).FullName
$env:ANDROID_HOME = Join-Path $toolsRoot 'sdk'
$env:ANDROID_USER_HOME = Join-Path $toolsRoot 'play-android-user'
$env:GRADLE_USER_HOME = Join-Path $toolsRoot 'gradle-user'
$projectCache = Join-Path $toolsRoot 'play-project-cache'
$buildOutput = Join-Path $toolsRoot 'play-build-output'
$kotlinHome = Join-Path $toolsRoot 'play-kotlin'
$initScript = Join-Path $toolsRoot 'play-build.init.gradle'
Push-Location $projectRoot
try {
    & (Join-Path $toolsRoot 'gradle-8.13\bin\gradle.bat') `
        --project-cache-dir $projectCache `
        --init-script $initScript `
        "-DplayBuildDir=$buildOutput" `
        "-Pkotlin.project.persistent.dir=$kotlinHome" `
        "-Pkotlin.user.home=$kotlinHome" `
        --no-daemon @Tasks
    exit $LASTEXITCODE
} finally { Pop-Location }
