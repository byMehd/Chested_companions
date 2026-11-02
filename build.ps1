param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments = @(':1.21.11-fabric:build', ':1.21.11-neoforge:build', ':1.21.11-forge:build'))
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $portableJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($portableJdk) { $env:JAVA21_HOME = $portableJdk.FullName }
    # Windows packaged applications can prevent Java's internal Unix-domain wakeup socket.
    # An invalid Unix-domain path makes Java fall back to its supported TCP loopback pipe.
    $env:JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS + ' -Djdk.net.unixdomain.tmpdir=C:\invalid|path').Trim())
    & .\gradlew.bat @GradleArguments '-Porg.gradle.java.installations.fromEnv=JAVA21_HOME,JAVA_HOME,JAVA_HOME_21_X64,JAVA_HOME_25_X64' --console=plain
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
