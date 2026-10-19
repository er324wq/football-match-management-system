$ErrorActionPreference = 'Stop'
$schema = 'football_admin_phase5_tx_20261017'
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD in the current PowerShell process.' }
if (-not (Test-Path -LiteralPath $mysql)) { throw 'MySQL client unavailable.' }
$env:MYSQL_PWD = $env:DB_PASSWORD
$exists = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($LASTEXITCODE -ne 0 -or $exists -ne '1') { throw "Safety stop: expected exact disposable schema $schema; observed $exists." }
$sql = 'DROP DATABASE ' + $schema
& $mysql --host=127.0.0.1 --port=3306 --user=root --execute=$sql
if ($LASTEXITCODE -ne 0) { throw 'Disposable schema cleanup failed.' }
$remaining = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($LASTEXITCODE -ne 0 -or $remaining -ne '0') { throw 'Disposable schema still exists after cleanup.' }
Write-Output "Removed only $schema; verified it no longer exists."
