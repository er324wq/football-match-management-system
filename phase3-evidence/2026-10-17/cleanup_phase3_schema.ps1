$ErrorActionPreference = 'Stop'

$schema = 'football_admin_phase3_it_20261017'
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
if (-not $env:DB_PASSWORD) {
    throw 'Set DB_PASSWORD in the current PowerShell process before running this script.'
}
$env:MYSQL_PWD = $env:DB_PASSWORD
$exists = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names `
    --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($LASTEXITCODE -ne 0 -or $exists -ne '1') {
    throw "Safety stop: expected exactly one $schema schema; refusing to drop anything."
}
& $mysql --host=127.0.0.1 --port=3306 --user=root --execute='DROP DATABASE `football_admin_phase3_it_20261017`'
if ($LASTEXITCODE -ne 0) { throw 'Could not drop the exact disposable test schema.' }
$remaining = & $mysql --host=127.0.0.1 --port=3306 --user=root --batch --skip-column-names `
    --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$schema'"
if ($remaining -ne '0') { throw 'Cleanup could not be verified.' }
Write-Output "Removed disposable schema $schema; verified absent."
