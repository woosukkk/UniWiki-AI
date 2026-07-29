$envFile = Join-Path $PSScriptRoot '.env'

if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) {
            continue
        }

        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }

        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1)
        Set-Item -Path "Env:$name" -Value $value
    }
}

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
}
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '..\.gradle'
}

& (Join-Path $PSScriptRoot 'gradlew.bat') bootRun --no-daemon
