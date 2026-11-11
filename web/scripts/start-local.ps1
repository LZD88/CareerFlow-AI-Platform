[CmdletBinding()]
param(
    [switch]$InitializeDatabase,
    [switch]$Build,
    [string]$JavaProject,
    [string]$PythonProject,
    [string]$RuntimeDir
)

$ErrorActionPreference = 'Stop'
$frontRoot = Split-Path -Parent $PSScriptRoot
$projectsRoot = Split-Path -Parent $frontRoot
if (-not $RuntimeDir) { $RuntimeDir = Join-Path $frontRoot '.local-run' }
$null = New-Item -ItemType Directory -Path $RuntimeDir -Force
$RuntimeDir = (Resolve-Path -LiteralPath $RuntimeDir).Path

function Find-Project([string]$Folder, [string]$Marker) {
    $container = Join-Path $projectsRoot $Folder
    if (Test-Path -LiteralPath (Join-Path $container $Marker)) { return $container }
    $matches = @(Get-ChildItem -LiteralPath $container -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName $Marker) })
    if ($matches.Count -ne 1) { throw "Cannot identify $Folder. Pass -JavaProject or -PythonProject explicitly." }
    return $matches[0].FullName
}
function Find-Tool([string]$Override, [string[]]$Candidates, [string]$Command) {
    if ($Override) {
        if (-not (Test-Path -LiteralPath $Override)) { throw "Tool not found: $Override" }
        return $Override
    }
    foreach ($candidate in $Candidates) { if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate } }
    $found = Get-Command $Command -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    throw "Missing tool: $Command. Set the corresponding LOCAL_*_BIN environment variable."
}
function Get-Listener([int]$Port) {
    return Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
}
function Wait-Http([string]$Url, [int]$Seconds = 60) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Service did not become ready: $Url. See logs in $RuntimeDir"
}
$started = [System.Collections.Generic.List[object]]::new()
$runStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
function Start-ServiceProcess([string]$Name, [string]$Executable, [string[]]$Arguments, [string]$Directory, [int]$Port, [string]$ExpectedCommand) {
    $listener = Get-Listener $Port
    if ($listener) {
        $running = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
        $parent = Get-CimInstance Win32_Process -Filter "ProcessId=$($running.ParentProcessId)" -ErrorAction SilentlyContinue
        if (($running.CommandLine -notlike "*$ExpectedCommand*") -and ($parent.CommandLine -notlike "*$ExpectedCommand*")) {
            throw "Port $Port is used by an unrelated process $($listener.OwningProcess)."
        }
        Write-Host "$Name already running (port $Port)."
        return
    }
    $launched = Start-Process -FilePath $Executable -ArgumentList $Arguments -WorkingDirectory $Directory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $RuntimeDir "$Name-$runStamp.out.log") `
        -RedirectStandardError (Join-Path $RuntimeDir "$Name-$runStamp.err.log")
    $started.Add([pscustomobject]@{ name = $Name; pid = $launched.Id; port = $Port; executable = $Executable; startedAt = (Get-Date).ToString('o') })
    $started | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $RuntimeDir "started-$runStamp.json") -Encoding UTF8
    Write-Host "Started $Name (launcher $($launched.Id), port $Port)."
}

if (-not $JavaProject) { $JavaProject = Find-Project 'admin' 'pom.xml' }
if (-not $PythonProject) { $PythonProject = Find-Project 'agent' 'pyproject.toml' }
$JavaProject = (Resolve-Path -LiteralPath $JavaProject).Path
$PythonProject = (Resolve-Path -LiteralPath $PythonProject).Path
$javaHomeCandidate = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { '' }
$javaBin = Find-Tool $env:LOCAL_JAVA_BIN @($javaHomeCandidate) 'java'
$nodeBin = (Get-Command node -ErrorAction Stop).Source
$redisBin = Find-Tool $env:LOCAL_REDIS_BIN @() 'redis-server'
$pythonBin = Find-Tool $env:LOCAL_PYTHON_BIN @((Join-Path $PythonProject '.venv/Scripts/python.exe')) 'python'
$viteEntry = Join-Path $frontRoot 'node_modules/vite/bin/vite.js'
if (-not (Test-Path -LiteralPath $viteEntry)) { throw 'Frontend dependencies missing. Run npm ci in the frontend project.' }
if (-not (Get-Listener 3306)) { throw 'Start local MySQL on port 3306 first.' }
$env:PYTHONUTF8 = '1'
if ($InitializeDatabase) {
    & $nodeBin (Join-Path $JavaProject 'scripts/init-local.cjs')
    if ($LASTEXITCODE -ne 0) { throw 'Local database initialization failed.' }
}
$jar = Join-Path $JavaProject 'target/YQ-AI-ADMIN-1.0-SNAPSHOT.jar'
if ($Build -or -not (Test-Path -LiteralPath $jar)) {
    if (Get-Listener 8081) { throw 'Stop Java on port 8081 before rebuilding its running JAR.' }
    $mavenBin = Find-Tool $env:LOCAL_MAVEN_BIN @() 'mvn'
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaBin)
    Push-Location -LiteralPath $JavaProject
    try { & $mavenBin package '-DskipTests'; if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' } }
    finally { Pop-Location }
}
$redisData = Join-Path $RuntimeDir 'redis-data'
$null = New-Item -ItemType Directory -Path $redisData -Force
$redisPassword = if ($env:LOCAL_REDIS_PASSWORD) { $env:LOCAL_REDIS_PASSWORD } else { 'training-local-redis' }
Start-ServiceProcess 'redis' $redisBin @('--bind','127.0.0.1','--port','6380','--appendonly','yes','--dir', ('"' + $redisData + '"'), '--requirepass', $redisPassword) $RuntimeDir 6380 'redis-server'
Start-ServiceProcess 'python' $pythonBin @('-m','uvicorn','app.main:app','--host','127.0.0.1','--port','9000') $PythonProject 9000 $PythonProject
Wait-Http 'http://127.0.0.1:9000/health'
Start-ServiceProcess 'java' $javaBin @('-Dfile.encoding=UTF-8','-jar',('"' + $jar + '"'),'--spring.profiles.active=local') $JavaProject 8081 'YQ-AI-ADMIN-1.0-SNAPSHOT.jar'
Wait-Http 'http://127.0.0.1:8081/yq-admin/v3/api-docs'
Start-ServiceProcess 'frontend' $nodeBin @(('"' + $viteEntry + '"'),'--host','127.0.0.1','--port','5173','--strictPort') $frontRoot 5173 $frontRoot
Wait-Http 'http://127.0.0.1:5173'
Write-Host "Ready: http://127.0.0.1:5173 (admin / password)"
Write-Host "Java: http://127.0.0.1:8081/yq-admin ; Python: http://127.0.0.1:9000"
Write-Host "Logs: $RuntimeDir"
