Write-Host "서버 구동을 시작합니다..."
$env:JAVA_HOME = "$env:USERPROFILE\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew bootRun
