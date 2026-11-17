$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
# Do not set JAVA_TOOL_OPTIONS: Java may print an informational stderr line.
Set-Location $PSScriptRoot
$log = Join-Path $PSScriptRoot 'build-log.txt'
'VYBE build started: ' + (Get-Date) | Set-Content -Path $log -Encoding UTF8
Write-Host 'VYBE - ONE CLICK APK BUILDER' -ForegroundColor Cyan
function Find-Jdk {
  $candidates = New-Object System.Collections.Generic.List[string]
  if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }
  foreach ($cmd in @(Get-Command javac.exe -All -ErrorAction SilentlyContinue)) {
    if ($cmd.Source) { $candidates.Add((Split-Path (Split-Path $cmd.Source -Parent) -Parent)) }
  }
  foreach ($key in @('HKLM:\SOFTWARE\JavaSoft\JDK','HKLM:\SOFTWARE\JavaSoft\Java Development Kit','HKLM:\SOFTWARE\Eclipse Adoptium\JDK','HKLM:\SOFTWARE\Microsoft\JDK')) {
    if (Test-Path $key) {
      foreach ($child in @(Get-ChildItem $key -ErrorAction SilentlyContinue)) {
        foreach ($prop in @('JavaHome','Path')) {
          $value = (Get-ItemProperty $child.PSPath -ErrorAction SilentlyContinue).$prop
          if ($value) { $candidates.Add($value) }
        }
      }
    }
  }
  foreach ($base in @('C:\Program Files\Java','C:\Program Files\Eclipse Adoptium','C:\Program Files\Microsoft','C:\Program Files\Amazon Corretto','C:\Program Files\Zulu','C:\Program Files\BellSoft','C:\Program Files\Android\Android Studio\jbr')) {
    if (Test-Path $base) {
      $candidates.Add($base)
      foreach ($dir in @(Get-ChildItem $base -Directory -ErrorAction SilentlyContinue)) { $candidates.Add($dir.FullName) }
    }
  }
  foreach ($candidate in $candidates) {
    if ((Test-Path (Join-Path $candidate 'bin\java.exe')) -and (Test-Path (Join-Path $candidate 'bin\javac.exe'))) {
      $versionOutput = (& (Join-Path $candidate 'bin\javac.exe') -version 2>&1 | Out-String)
      if ($LASTEXITCODE -eq 0 -and $versionOutput -match 'javac\s+17(\.|\s|$)') { return $candidate }
    }
  }
  throw 'Could not find a complete JDK 17 or newer. Install JDK 17 from adoptium.net.'
}
try {
  $env:JAVA_HOME = Find-Jdk
  $env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
  Write-Host "Java JDK: $env:JAVA_HOME" -ForegroundColor Green
  & (Join-Path $env:JAVA_HOME 'bin\java.exe') -version
  $sdkCandidates = @($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,(Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
  $sdk = $sdkCandidates | Where-Object { $_ -and (Test-Path (Join-Path $_ 'platforms\android-35\android.jar')) } | Select-Object -First 1
  if (-not $sdk) { throw 'Android SDK Platform 35 missing. Install it in Android Studio > SDK Manager.' }
  $env:ANDROID_HOME = $sdk
  $env:ANDROID_SDK_ROOT = $sdk
  Write-Host "Android SDK: $sdk" -ForegroundColor Green
  $gradleHome = Join-Path $PSScriptRoot '.tools\gradle-8.9'
  $gradleBat = Join-Path $gradleHome 'bin\gradle.bat'
  if (-not (Test-Path $gradleBat)) {
    Write-Host 'Downloading Gradle 8.9 (first run only)...' -ForegroundColor Yellow
    $toolsDir = Join-Path $PSScriptRoot '.tools'
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $archive = Join-Path $toolsDir 'gradle-8.9-bin.zip'
    Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $archive
    Expand-Archive $archive -DestinationPath $toolsDir -Force
    Remove-Item $archive -Force
  }
  Write-Host 'Building VYBE APK. This can take several minutes...' -ForegroundColor Cyan
  # Start-Process keeps ordinary Java/Gradle stderr from becoming terminating PowerShell errors.
  # Save both output streams to build-log.txt even when Gradle fails.
  function Run-Gradle([string[]]$gradleArgs, [string]$label) {
    $stdout = Join-Path $PSScriptRoot '.vybe-gradle-stdout.tmp'
    $stderr = Join-Path $PSScriptRoot '.vybe-gradle-stderr.tmp'
    Remove-Item $stdout,$stderr -Force -ErrorAction SilentlyContinue
    Add-Content -Path $log -Value "`n=== $label ===" -Encoding UTF8
    $proc = Start-Process -FilePath $gradleBat -ArgumentList $gradleArgs -WorkingDirectory $PSScriptRoot -Wait -PassThru -NoNewWindow -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    foreach ($file in @($stdout,$stderr)) {
      if (Test-Path $file) {
        $lines = Get-Content $file -ErrorAction SilentlyContinue
        if ($lines) { $lines | Tee-Object -FilePath $log -Append }
        Remove-Item $file -Force -ErrorAction SilentlyContinue
      }
    }
    return $proc.ExitCode
  }
  $flags = @('--daemon','--build-cache','assembleDebug','--stacktrace','--console=plain')
  $code = Run-Gradle (@('--offline') + $flags) 'Offline attempt'
  if ($code -ne 0) {
    Write-Host 'Offline attempt failed or dependencies missing. Retrying online...' -ForegroundColor Yellow
    $code = Run-Gradle $flags 'Online attempt'
  }
  if ($code -ne 0) { throw "Gradle failed with exit code $code. Open build-log.txt for the full error." }
  $apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
  if (-not (Test-Path $apk)) { throw 'Build finished but APK was not found. See build-log.txt.' }
  Write-Host "SUCCESS! APK: $apk" -ForegroundColor Green
  Start-Process explorer.exe -ArgumentList ('"' + (Split-Path $apk -Parent) + '"')
} catch {
  Write-Host "BUILD FAILED: $($_.Exception.Message)" -ForegroundColor Red
  Add-Content -Path $log -Value ('BUILD FAILED: ' + $_.Exception.ToString()) -Encoding UTF8
  Write-Host 'If the issue persists, send build-log.txt for diagnosis.' -ForegroundColor Yellow
  exit 1
}
