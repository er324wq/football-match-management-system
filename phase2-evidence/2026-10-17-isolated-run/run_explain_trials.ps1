param(
 [Parameter(Mandatory=$true)][string]$SchemaName,
 [Parameter(Mandatory=$true)][string]$Label,
 [int]$Iterations=20,
 [int]$Warmups=3,
 [string[]]$OnlyQueries=@('match_page','top_scorers','standings')
)
if($SchemaName -notmatch '^football_admin_bench_[a-zA-Z0-9_]+$'){throw 'Refusing non-isolated schema name'}
$dir=$PSScriptRoot
if(-not $env:DB_PASSWORD){throw 'Set DB_PASSWORD in the current PowerShell process.'}
$env:MYSQL_PWD=$env:DB_PASSWORD
$queries=[ordered]@{}
$queries['match_page']=@"
SELECT m.*,ht.name AS home_team_name,at.name AS away_team_name,mr.home_score,mr.away_score,mr.referee
FROM ``match`` m LEFT JOIN team ht ON m.home_team_id=ht.id LEFT JOIN team at ON m.away_team_id=at.id
LEFT JOIN match_result mr ON m.id=mr.match_id WHERE m.deleted=0 ORDER BY m.match_time DESC LIMIT 0,10
"@.Trim()
$queries['match_status_page']=@"
SELECT m.*,ht.name AS home_team_name,at.name AS away_team_name,mr.home_score,mr.away_score,mr.referee
FROM ``match`` m LEFT JOIN team ht ON m.home_team_id=ht.id LEFT JOIN team at ON m.away_team_id=at.id
LEFT JOIN match_result mr ON m.id=mr.match_id WHERE m.deleted=0 AND m.status='finished' ORDER BY m.match_time DESC LIMIT 0,10
"@.Trim()
$queries['top_scorers']=@"
SELECT p.id AS player_id,p.name AS player_name,t.name AS team_name,COUNT(g.id) AS goal_count
FROM player p LEFT JOIN goal_record g ON p.id=g.player_id LEFT JOIN team t ON p.team_id=t.id
WHERE p.deleted=0 AND g.deleted=0 GROUP BY p.id,p.name,t.name ORDER BY goal_count DESC
"@.Trim()
$queries['standings']=@"
SELECT team_id,team_name,COUNT(*) AS played,
SUM(CASE WHEN is_home=1 AND home_score>away_score THEN 1 WHEN is_home=0 AND away_score>home_score THEN 1 ELSE 0 END) AS won,
SUM(CASE WHEN home_score=away_score THEN 1 ELSE 0 END) AS drawn,
SUM(CASE WHEN is_home=1 AND home_score<away_score THEN 1 WHEN is_home=0 AND away_score<home_score THEN 1 ELSE 0 END) AS lost,
SUM(CASE WHEN is_home=1 THEN home_score ELSE away_score END) AS goals_for,
SUM(CASE WHEN is_home=1 THEN away_score ELSE home_score END) AS goals_against,
(SUM(CASE WHEN is_home=1 THEN home_score ELSE away_score END)-SUM(CASE WHEN is_home=1 THEN away_score ELSE home_score END)) AS goal_difference,
(SUM(CASE WHEN is_home=1 AND home_score>away_score THEN 3 WHEN is_home=0 AND away_score>home_score THEN 3 ELSE 0 END)+SUM(CASE WHEN home_score=away_score THEN 1 ELSE 0 END)) AS points
FROM (
 SELECT m.home_team_id AS team_id,ht.name AS team_name,1 AS is_home,mr.home_score,mr.away_score FROM ``match`` m
 JOIN team ht ON m.home_team_id=ht.id JOIN match_result mr ON m.id=mr.match_id WHERE m.status IN ('finished','ended') AND m.deleted=0
 UNION ALL
 SELECT m.away_team_id AS team_id,at.name AS team_name,0 AS is_home,mr.home_score,mr.away_score FROM ``match`` m
 JOIN team at ON m.away_team_id=at.id JOIN match_result mr ON m.id=mr.match_id WHERE m.status IN ('finished','ended') AND m.deleted=0
) AS team_matches
GROUP BY team_id,team_name ORDER BY points DESC,goal_difference DESC,goals_for DESC
"@.Trim()
$mysql=(Get-Command mysql.exe -ErrorAction Stop).Source;$records=[System.Collections.Generic.List[object]]::new();$rawPath=Join-Path $dir "measure_${Label}_raw.txt";Set-Content $rawPath '' -Encoding UTF8
foreach($name in $OnlyQueries){if(!$queries.Contains($name)){throw "Unknown query: $name"};$query=$queries[$name];for($n=1;$n -le ($Warmups+$Iterations);$n++){$statement="USE ``$SchemaName``; EXPLAIN ANALYZE $query;";$out=& $mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u root --batch --raw --skip-column-names -e $statement 2>&1;if($LASTEXITCODE -ne 0){$out;throw "MySQL query failed: $name trial $n"};Add-Content -Path $rawPath -Value "### query=$name sample=$n warmup=$($n -le $Warmups)" -Encoding UTF8;Add-Content -Path $rawPath -Value $out -Encoding UTF8;$first=$out | Where-Object {$_ -match 'actual time='} | Select-Object -First 1;$m=[regex]::Match([string]$first,'actual time=([0-9.]+)\.\.([0-9.]+) rows=(\d+) loops=(\d+)');if(!$m.Success){throw "Could not parse EXPLAIN ANALYZE root timing for $name trial ${n}: $first"};if($n -gt $Warmups){$records.Add([pscustomobject]@{label=$Label;query=$name;sample=$n-$Warmups;server_first_row_ms=[double]::Parse($m.Groups[1].Value,[cultureinfo]::InvariantCulture);server_total_ms=[double]::Parse($m.Groups[2].Value,[cultureinfo]::InvariantCulture);root_rows=[int]$m.Groups[3].Value;root_loops=[int]$m.Groups[4].Value})}}}
$csv=Join-Path $dir "measure_${Label}_samples.csv";$records | Export-Csv -NoTypeInformation -Encoding UTF8 $csv
$summaryPath=Join-Path $dir "measure_${Label}_summary.csv";$summary=[System.Collections.Generic.List[object]]::new();foreach($g in ($records | Group-Object query)){$values=@($g.Group | ForEach-Object {$_.server_total_ms} | Sort-Object);$n=$values.Count;$median=if($n%2){$values[[int][Math]::Floor($n/2)]}else{($values[$n/2-1]+$values[$n/2])/2};$p95=$values[[Math]::Ceiling(.95*$n)-1];$summary.Add([pscustomobject]@{label=$Label;query=$g.Name;samples=$n;median_server_ms=[Math]::Round($median,4);p95_nearest_rank_server_ms=[Math]::Round($p95,4);min_server_ms=[Math]::Round($values[0],4);max_server_ms=[Math]::Round($values[-1],4)})};$summary | Export-Csv -NoTypeInformation -Encoding UTF8 $summaryPath;$summary | Format-Table -AutoSize;Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
