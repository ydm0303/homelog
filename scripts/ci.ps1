<#
.SYNOPSIS
    家记 HomeLog · 本地 CI 脚本（编译 + Lint + 构建）

.DESCRIPTION
    在本地一次性验证前后端是否可构建，等价于最小化的流水线检查。
    不需要任何云端 CI，手动按需执行即可。

    执行内容：
      后端  1) mvn clean package        —— 编译 + 打包（默认跳过测试）
      前端  2) npm ci（仅当 node_modules 缺失时）
            3) eslint 静态检查
            4) vue-tsc 类型检查 + vite 生产构建

.PARAMETER Target
    all（默认） | backend | frontend

.PARAMETER SkipLint
    跳过前端 ESLint 检查

.PARAMETER StrictLint
    ESLint 报错即判定失败（默认只告警，不阻断）

.PARAMETER RunTests
    后端执行单元测试（当前仓库尚无测试）

.PARAMETER Clean
    后端构建前先清理 target（等价 mvn clean package）。
    默认不清理，便于在离线 / 受限网络环境下复用已下载的 Maven 插件。

.PARAMETER TypeCheck
    前端执行 vue-tsc 类型检查。
    注意：当前 lock 锁定的 vue-tsc 1.8.27 与 TypeScript 5.9 不兼容，
    需先把 typescript 降到 5.4.x 或把 vue-tsc 升到 2.x 后才能启用。

.PARAMETER CleanInstall
    强制重新执行 npm ci（清空并重装 node_modules）

.EXAMPLE
    ./scripts/ci.ps1
    ./scripts/ci.ps1 -Target frontend -SkipLint
    ./scripts/ci.ps1 -Clean -StrictLint -RunTests
#>
[CmdletBinding()]
param(
    [ValidateSet('all', 'backend', 'frontend')]
    [string]$Target = 'all',

    [switch]$SkipLint,
    [switch]$StrictLint,
    [switch]$RunTests,
    [switch]$Clean,
    [switch]$TypeCheck,
    [switch]$CleanInstall
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# 路径与全局状态
# ---------------------------------------------------------------------------
$ScriptDir    = $PSScriptRoot
$RepoRoot     = Split-Path -Parent $ScriptDir
$BackendDir   = Join-Path $RepoRoot 'homelog-backend'
$FrontendDir  = Join-Path $RepoRoot 'homelog-frontend'

$script:Results = New-Object System.Collections.ArrayList
$script:Failed  = $false

# ---------------------------------------------------------------------------
# 输出辅助
# ---------------------------------------------------------------------------
function Write-Banner {
    param([string]$Text)
    Write-Host ''
    Write-Host ('=' * 68) -ForegroundColor DarkGray
    Write-Host " $Text" -ForegroundColor White
    Write-Host ('=' * 68) -ForegroundColor DarkGray
}

function Write-Section {
    param([string]$Text)
    Write-Host ''
    Write-Host ">> $Text" -ForegroundColor Cyan
}

# 执行一个步骤并记录耗时/结果
function Invoke-Step {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$WorkDir,
        [Parameter(Mandatory)][scriptblock]$Action,
        [switch]$NonBlocking
    )

    Write-Host "   -> $Name" -ForegroundColor Gray

    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    $status = 'PASS'
    $errorText = ''

    Push-Location $WorkDir
    try {
        $global:LASTEXITCODE = 0
        & $Action
        if ($LASTEXITCODE -ne 0) {
            throw "命令退出码 $LASTEXITCODE"
        }
    }
    catch {
        $errorText = $_.Exception.Message
        if ($NonBlocking) {
            $status = 'WARN'
        }
        else {
            $status = 'FAIL'
            $script:Failed = $true
        }
    }
    finally {
        $timer.Stop()
        Pop-Location
    }

    $seconds = [math]::Round($timer.Elapsed.TotalSeconds, 1)

    switch ($status) {
        'PASS' { Write-Host "   OK   $Name (${seconds}s)" -ForegroundColor Green }
        'WARN' { Write-Host "   WARN $Name (${seconds}s) - $errorText" -ForegroundColor Yellow }
        'FAIL' { Write-Host "   FAIL $Name (${seconds}s) - $errorText" -ForegroundColor Red }
    }

    [void]$script:Results.Add([pscustomobject]@{
        Step    = $Name
        Status  = $status
        Seconds = $seconds
    })

    return ($status -ne 'FAIL')
}

# 执行外部命令并实时输出（保留退出码）
function Invoke-CommandLine {
    param(
        [Parameter(Mandatory)][string]$File,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    # 原生命令往 stderr 写内容时，若 ErrorActionPreference 为 Stop 会被当成终止错误，
    # 这里临时放开，退出码由下面统一判断
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $File @Arguments
        $code = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previous
    }

    if ($code -ne 0) {
        throw "$File 执行失败（退出码 $code）"
    }
}

# 检查命令是否存在
function Test-CommandAvailable {
    param([Parameter(Mandatory)][string]$Name)
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

# 定位 java：优先 PATH，其次 JAVA_HOME（很多环境 Maven 能用但 java 不在 PATH）
function Resolve-JavaCommand {
    if (Test-CommandAvailable 'java') { return 'java' }
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $candidate) { return $candidate }
    }
    return $null
}

# ---------------------------------------------------------------------------
# 前置检查
# ---------------------------------------------------------------------------
Write-Banner '家记 HomeLog · 本地 CI'

Write-Host ''
Write-Host "仓库根目录 : $RepoRoot"
Write-Host "执行范围   : $Target"
Write-Host "跳过 Lint  : $([bool]$SkipLint)"
Write-Host "严格 Lint  : $([bool]$StrictLint)"
Write-Host "运行测试   : $([bool]$RunTests)"

$needBackend  = ($Target -eq 'all' -or $Target -eq 'backend')
$needFrontend = ($Target -eq 'all' -or $Target -eq 'frontend')

if ($needBackend) {
    if (-not (Test-CommandAvailable 'mvn')) { throw '未找到 mvn，请安装 Maven 3.6+ 并加入 PATH' }
    $javaCmd = Resolve-JavaCommand
    if ($null -eq $javaCmd) { throw '未找到 java，请安装 JDK/JRE 8+ 并加入 PATH 或设置 JAVA_HOME' }
}

if ($needFrontend) {
    if (-not (Test-CommandAvailable 'npm')) { throw '未找到 npm，请安装 Node.js 18+ 并加入 PATH' }
}

Write-Host ''
Write-Host '工具版本：' -ForegroundColor DarkGray
if ($needBackend) {
    # 用 mvn -v 输出判断 JDK（避免直接跑 java -version 往 stderr 写内容产生噪音）
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $mvnInfo = (& mvn -v 2>&1) | Out-String
    }
    finally {
        $ErrorActionPreference = $previous
    }
    ($mvnInfo -split "\r?\n") |
        Where-Object { $_ -match '^(Apache Maven|Java version|Maven home)' } |
        ForEach-Object { Write-Host "  $($_.Trim())" }
}
if ($needFrontend) {
    Write-Host "  node $(node -v) / npm $(npm -v)"
}

# ---------------------------------------------------------------------------
# 后端
# ---------------------------------------------------------------------------
if ($needBackend) {
    Write-Banner '后端 · Spring Boot'

    $mvnArgs = @('-B', '-ntp')
    $mvnStage = 'package'
    if ($Clean) {
        $mvnArgs += 'clean'
        $mvnStage = 'clean package'
    }
    $mvnArgs += 'package'
    if (-not $RunTests) {
        $mvnArgs += '-DskipTests'
    }

    $ok = Invoke-Step -Name "mvn $mvnStage" -WorkDir $BackendDir -Action {
        Invoke-CommandLine -File 'mvn' -Arguments $mvnArgs
    }

    if ($ok) {
        $jar = Get-ChildItem -Path (Join-Path $BackendDir 'target') -Filter '*.jar' -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-javadoc.jar' } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

        if ($null -ne $jar) {
            Write-Host ("   产物: {0} ({1:N1} MB)" -f $jar.Name, ($jar.Length / 1MB)) -ForegroundColor DarkGray
        }
        else {
            Write-Host '   未找到 jar 产物' -ForegroundColor Yellow
        }
    }
}

# ---------------------------------------------------------------------------
# 前端
# ---------------------------------------------------------------------------
if ($needFrontend) {
    Write-Banner '前端 · Vue 3'

    $nodeModules = Join-Path $FrontendDir 'node_modules'

    if ($CleanInstall -or -not (Test-Path $nodeModules)) {
        Invoke-Step -Name 'npm ci' -WorkDir $FrontendDir -Action {
            Invoke-CommandLine -File 'npm' -Arguments @('ci')
        } | Out-Null
    }
    else {
        Write-Host '   -> 跳过 npm ci（node_modules 已存在，需要重装请加 -CleanInstall）' -ForegroundColor DarkGray
    }

    $eslintBin = Join-Path $FrontendDir 'node_modules\eslint\bin\eslint.js'
    $eslintConfigs = @('.eslintrc.cjs', '.eslintrc.js', '.eslintrc.json', '.eslintrc.yml', 'eslint.config.js')
    $hasEslintConfig = $false
    foreach ($cfg in $eslintConfigs) {
        if (Test-Path (Join-Path $FrontendDir $cfg)) { $hasEslintConfig = $true; break }
    }

    if ($SkipLint) {
        Write-Host '   -> 跳过 eslint（-SkipLint）' -ForegroundColor DarkGray
    }
    elseif (-not (Test-Path $eslintBin)) {
        Write-Host '   -> 跳过 eslint（node_modules 中未安装 eslint，请先 npm install）' -ForegroundColor Yellow
    }
    elseif (-not $hasEslintConfig) {
        Write-Host '   -> 跳过 eslint（缺少 ESLint 配置文件）' -ForegroundColor Yellow
    }
    else {
        # 直接调用本地 eslint，避免 npx 在受限执行策略下无法启动
        $lintArgs = @($eslintBin, '.', '--ext', '.vue,.js,.ts,.tsx')
        if ($StrictLint) {
            $lintArgs += '--max-warnings=0'
        }

        Invoke-Step -Name 'eslint' -WorkDir $FrontendDir -Action {
            Invoke-CommandLine -File 'node' -Arguments $lintArgs
        } -NonBlocking:(-not $StrictLint) | Out-Null
    }

    if ($TypeCheck) {
        Invoke-Step -Name 'vue-tsc --noEmit' -WorkDir $FrontendDir -Action {
            Invoke-CommandLine -File 'npm' -Arguments @('run', 'type-check')
        } | Out-Null
    }

    Invoke-Step -Name 'vite build' -WorkDir $FrontendDir -Action {
        Invoke-CommandLine -File 'npm' -Arguments @('run', 'build')
    } | Out-Null

    $distDir = Join-Path $FrontendDir 'dist'
    if (Test-Path $distDir) {
        $distSize = (Get-ChildItem -Path $distDir -Recurse -File | Measure-Object -Property Length -Sum).Sum
        Write-Host ("   产物: dist\ ({0:N1} MB)" -f ($distSize / 1MB)) -ForegroundColor DarkGray
    }
}

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
Write-Banner '汇总'

$script:Results | Format-Table -AutoSize -Property @(
    'Step',
    'Status',
    @{ Name = 'Seconds'; Expression = { $_.Seconds } }
) | Out-String | Write-Host

if ($script:Failed) {
    Write-Host '结果: 失败，请查看上方 FAIL 项' -ForegroundColor Red
    exit 1
}

$warnCount = @($script:Results | Where-Object { $_.Status -eq 'WARN' }).Count
if ($warnCount -gt 0) {
    Write-Host "结果: 通过（$warnCount 项告警）" -ForegroundColor Yellow
    exit 0
}

Write-Host '结果: 全部通过' -ForegroundColor Green
exit 0
