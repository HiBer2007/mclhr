param(
    [string]$OutDir = "build"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$out = Join-Path $root $OutDir
$classes = Join-Path $out "classes"

if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
New-Item -ItemType Directory -Force $classes | Out-Null

$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\javac.exe" } else { "javac.exe" }
$jar = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\jar.exe" } else { "jar.exe" }

$sources = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter *.java |
    ForEach-Object { $_.FullName }

& $javac --release 17 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

& $jar --create --file (Join-Path $out "mclhr.jar") --main-class com.hibernet.mclhr.MclhrCli -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Write-Host "built: $(Join-Path $out 'mclhr.jar')"
