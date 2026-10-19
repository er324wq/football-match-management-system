$ErrorActionPreference = 'Stop'
$schema = 'football_admin_phase4_tx_20261017'
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD in the current PowerShell process.' }
$env:MYSQL_PWD = $env:DB_PASSWORD
$check = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($LASTEXITCODE -ne 0 -or $check -ne '1') { throw 'Safety stop: exact temporary schema is not present.' }
& $mysql --host=127.0.0.1 --port=3306 --user=root --execute="DROP DATABASE $schema"
if ($LASTEXITCODE -ne 0) { throw 'Temporary schema cleanup failed.' }
Write-Output "Dropped disposable schema $schema."