$ErrorActionPreference = 'Stop'

$schema = 'football_admin_phase3_it_20261017'
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$sourceSql = Join-Path $root 'backend\src\main\resources\db\init.sql'
$isolatedSql = Join-Path $PSScriptRoot 'isolated_schema_init.sql'

if (-not $env:DB_PASSWORD) {
    throw 'Set DB_PASSWORD in the current PowerShell process before running this script.'
}
if (-not (Test-Path -LiteralPath $mysql)) {
    throw 'MySQL client was not found on PATH.'
}

$env:MYSQL_PWD = $env:DB_PASSWORD
$exists = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names `
    --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($LASTEXITCODE -ne 0) { throw 'Could not verify target schema state.' }
if ($exists -ne '0') { throw "Safety stop: $schema already exists; refusing to reuse or overwrite it." }

$sql = Get-Content -LiteralPath $sourceSql -Raw
$sql = $sql.Replace('CREATE DATABASE IF NOT EXISTS football_admin ', "CREATE DATABASE IF NOT EXISTS $schema ")
$sql = $sql.Replace('USE football_admin;', "USE $schema;")
if ($sql.Contains('USE football_admin;') -or -not $sql.Contains("USE $schema;")) {
    throw 'Schema rewrite verification failed; no SQL was executed.'
}
[System.IO.File]::WriteAllText($isolatedSql, $sql, (New-Object System.Text.UTF8Encoding($false)))

& $mysql --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 --execute=$sql
if ($LASTEXITCODE -ne 0) { throw 'Isolated schema initialization failed; inspect MySQL output before cleanup.' }
Write-Output "Initialized disposable schema $schema from init.sql."
