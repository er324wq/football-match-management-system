$ErrorActionPreference = 'Stop'
$schema = 'football_admin_phase5_tx_20261017'
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$sourceSql = Join-Path $root 'backend\src\main\resources\db\init.sql'
$isolatedSql = Join-Path $PSScriptRoot 'isolated_schema_init.sql'
if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD in the current PowerShell process.' }
if (-not (Test-Path -LiteralPath $mysql)) { throw 'MySQL client unavailable.' }
$env:MYSQL_PWD = $env:DB_PASSWORD
$exists = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($LASTEXITCODE -ne 0 -or $exists -ne '0') { throw "Safety stop: $schema exists or existence check failed." }
$sql = Get-Content -LiteralPath $sourceSql -Raw
$sql = $sql.Replace('CREATE DATABASE IF NOT EXISTS football_admin ', "CREATE DATABASE IF NOT EXISTS $schema ").Replace('USE football_admin;', "USE $schema;")
if ($sql.Contains('USE football_admin;') -or -not $sql.Contains("USE $schema;")) { throw 'Schema rewrite verification failed.' }
[System.IO.File]::WriteAllText($isolatedSql, $sql, (New-Object System.Text.UTF8Encoding($false)))
& $mysql --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 --execute=$sql
if ($LASTEXITCODE -ne 0) { throw 'Isolated schema initialization failed.' }
$check = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$schema' AND engine='InnoDB'"
if ($LASTEXITCODE -ne 0 -or $check -ne '13') { throw "Expected 13 InnoDB tables; observed $check" }
Write-Output "Created isolated test schema $schema with $check InnoDB tables."
