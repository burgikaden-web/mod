$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$ProjectDir = $PSScriptRoot
$ToolsDir = Join-Path $ProjectDir '.tools'
$GradleVersion = '8.6'
$GradleDir = Join-Path $ToolsDir "gradle-$GradleVersion"
$GradleExe = Join-Path $GradleDir 'bin\gradle.bat'

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null

# Gradle 8.6 is downloaded into this project the first time this script runs.
if (-not (Test-Path $GradleExe)) {
    Write-Step "Downloading Gradle $GradleVersion automatically"
    $GradleZip = Join-Path $ToolsDir "gradle-$GradleVersion-bin.zip"
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $GradleZip
    Expand-Archive -LiteralPath $GradleZip -DestinationPath $ToolsDir -Force
    Remove-Item $GradleZip -Force
}

# Use an installed JDK 17-21 if available. Otherwise download a portable JDK 17.
$UseBundledJdk = $true
$JavaCommand = Get-Command java -ErrorAction SilentlyContinue
$JavacCommand = Get-Command javac -ErrorAction SilentlyContinue
if ($JavaCommand -and $JavacCommand) {
    $JavaVersionText = (& java -version 2>&1 | Out-String)
    if ($JavaVersionText -match '(?:version\s+"|openjdk\s+)(\d+)') {
        $MajorVersion = [int]$Matches[1]
        if ($MajorVersion -ge 17 -and $MajorVersion -le 21) {
            $UseBundledJdk = $false
            Write-Step "Using installed Java $MajorVersion"
        }
    }
}

if ($UseBundledJdk) {
    $JdkDir = Get-ChildItem -Path $ToolsDir -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-17*' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $JdkDir -or -not (Test-Path (Join-Path $JdkDir.FullName 'bin\javac.exe'))) {
        Write-Step 'Downloading a portable Java 17 JDK automatically'
        $JdkZip = Join-Path $ToolsDir 'temurin-jdk17.zip'
        $JdkUrl = 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse'
        Invoke-WebRequest -Uri $JdkUrl -OutFile $JdkZip
        Expand-Archive -LiteralPath $JdkZip -DestinationPath $ToolsDir -Force
        Remove-Item $JdkZip -Force
        $JdkDir = Get-ChildItem -Path $ToolsDir -Directory |
            Where-Object { $_.Name -like 'jdk-17*' } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    }

    if (-not $JdkDir) {
        throw 'Could not find the extracted Java 17 JDK.'
    }
    $env:JAVA_HOME = $JdkDir.FullName
    $env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
    Write-Step "Using portable Java 17 from $env:JAVA_HOME"
}

Write-Step 'Building the mod (first build may download Minecraft/Fabric libraries)'
Push-Location $ProjectDir
try {
    & $GradleExe --no-daemon build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$JarPath = Join-Path $ProjectDir 'build\libs\one-for-all-1.0.0.jar'
if (Test-Path $JarPath) {
    Write-Host "`nSUCCESS: $JarPath" -ForegroundColor Green
    Write-Host 'Copy this JAR into your Minecraft 1.20.1 Fabric instance mods folder.'
} else {
    throw "Build completed, but the expected JAR was not found at $JarPath"
}
