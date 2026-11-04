param(
    [string]$OutDir = "build",
    [switch]$RunTests
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$out = Join-Path $root $OutDir
$classes = Join-Path $out "classes"
$testClasses = Join-Path $out "test-classes"

if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
New-Item -ItemType Directory -Force $classes | Out-Null

$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\javac.exe" } else { "javac.exe" }
$java  = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java.exe" }
$jar   = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\jar.exe" } else { "jar.exe" }

function New-SourceList([string]$listFile, [string]$sourceRoot) {
    $lines = Get-ChildItem $sourceRoot -Recurse -Filter *.java |
        ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }
    Set-Content -Path $listFile -Value $lines -Encoding ASCII
}

# 用 response file + cmd，避免 PowerShell 对 javac 参数的二次解析
New-SourceList (Join-Path $out "main-sources.txt") (Join-Path $root "src\main\java")
cmd /c "`"$javac`" --release 17 -encoding UTF-8 -d `"$classes`" @`"$out\main-sources.txt`""
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

if ($RunTests) {
    if (Test-Path $testClasses) { Remove-Item $testClasses -Recurse -Force }
    New-Item -ItemType Directory -Force $testClasses | Out-Null
    New-SourceList (Join-Path $out "test-sources.txt") (Join-Path $root "src\test\java")
    cmd /c "`"$javac`" --release 17 -encoding UTF-8 -cp `"$classes`" -d `"$testClasses`" @`"$out\test-sources.txt`""
    if ($LASTEXITCODE -ne 0) { throw "test javac failed" }
    & $java -cp "$classes;$testClasses" com.hibernet.mclhr.SelfTest
    if ($LASTEXITCODE -ne 0) { throw "self-test failed" }
    & $java -cp "$classes;$testClasses" com.hibernet.mclhr.LoaderSelfTest
    if ($LASTEXITCODE -ne 0) { throw "loader self-test failed" }
}

& $jar --create --file (Join-Path $out "mclhr.jar") --main-class com.hibernet.mclhr.MclhrCli -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Write-Host "built: $(Join-Path $out 'mclhr.jar')"
