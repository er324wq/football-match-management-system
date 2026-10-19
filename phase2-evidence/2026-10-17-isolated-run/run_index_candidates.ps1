param([string]$SchemaName='football_admin_bench_20261017_a1')
if($SchemaName -notmatch '^football_admin_bench_[a-zA-Z0-9_]+$'){throw 'Refusing non-isolated schema'}
if(-not $env:DB_PASSWORD){throw 'Set DB_PASSWORD in the current PowerShell process.'}
$env:MYSQL_PWD=$env:DB_PASSWORD
$mysql=(Get-Command mysql.exe -ErrorAction Stop).Source;$runner=Join-Path $PSScriptRoot 'run_explain_trials.ps1';$planPath=Join-Path $PSScriptRoot 'candidate_plans.txt';Set-Content $planPath '' -Encoding UTF8
function Invoke-Db([string]$Sql,[string]$Label){$o=& $mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u root --batch --raw --table -e "USE ``$SchemaName``; $Sql" 2>&1;if($LASTEXITCODE -ne 0){$o;throw "DB command failed: $Label"};Add-Content -Path $planPath -Value "### $Label" -Encoding UTF8;Add-Content -Path $planPath -Value $o -Encoding UTF8;$o}
$queries=[ordered]@{}
$queries['match_page']="SELECT m.*,ht.name AS home_team_name,at.name AS away_team_name,mr.home_score,mr.away_score,mr.referee FROM ``match`` m LEFT JOIN team ht ON m.home_team_id=ht.id LEFT JOIN team at ON m.away_team_id=at.id LEFT JOIN match_result mr ON m.id=mr.match_id WHERE m.deleted=0 ORDER BY m.match_time DESC LIMIT 0,10"
$queries['match_status_page']="SELECT m.*,ht.name AS home_team_name,at.name AS away_team_name,mr.home_score,mr.away_score,mr.referee FROM ``match`` m LEFT JOIN team ht ON m.home_team_id=ht.id LEFT JOIN team at ON m.away_team_id=at.id LEFT JOIN match_result mr ON m.id=mr.match_id WHERE m.deleted=0 AND m.status='finished' ORDER BY m.match_time DESC LIMIT 0,10"
$queries['top_scorers']="SELECT p.id AS player_id,p.name AS player_name,t.name AS team_name,COUNT(g.id) AS goal_count FROM player p LEFT JOIN goal_record g ON p.id=g.player_id LEFT JOIN team t ON p.team_id=t.id WHERE p.deleted=0 AND g.deleted=0 GROUP BY p.id,p.name,t.name ORDER BY goal_count DESC"
$queries['standings']="SELECT team_id,team_name,COUNT(*) AS played,SUM(CASE WHEN is_home=1 AND home_score>away_score THEN 1 WHEN is_home=0 AND away_score>home_score THEN 1 ELSE 0 END) AS won,SUM(CASE WHEN home_score=away_score THEN 1 ELSE 0 END) AS drawn,SUM(CASE WHEN is_home=1 AND home_score<away_score THEN 1 WHEN is_home=0 AND away_score<home_score THEN 1 ELSE 0 END) AS lost,SUM(CASE WHEN is_home=1 THEN home_score ELSE away_score END) AS goals_for,SUM(CASE WHEN is_home=1 THEN away_score ELSE home_score END) AS goals_against,(SUM(CASE WHEN is_home=1 THEN home_score ELSE away_score END)-SUM(CASE WHEN is_home=1 THEN away_score ELSE home_score END)) AS goal_difference,(SUM(CASE WHEN is_home=1 AND home_score>away_score THEN 3 WHEN is_home=0 AND away_score>home_score THEN 3 ELSE 0 END)+SUM(CASE WHEN home_score=away_score THEN 1 ELSE 0 END)) AS points FROM (SELECT m.home_team_id AS team_id,ht.name AS team_name,1 AS is_home,mr.home_score,mr.away_score FROM ``match`` m JOIN team ht ON m.home_team_id=ht.id JOIN match_result mr ON m.id=mr.match_id WHERE m.status IN ('finished','ended') AND m.deleted=0 UNION ALL SELECT m.away_team_id AS team_id,at.name AS team_name,0 AS is_home,mr.home_score,mr.away_score FROM ``match`` m JOIN team at ON m.away_team_id=at.id JOIN match_result mr ON m.id=mr.match_id WHERE m.status IN ('finished','ended') AND m.deleted=0) AS team_matches GROUP BY team_id,team_name ORDER BY points DESC,goal_difference DESC,goals_for DESC"
try {
 Invoke-Db "EXPLAIN FORMAT=TRADITIONAL $($queries['match_status_page']);" 'status_page_baseline'
 & powershell -NoProfile -ExecutionPolicy Bypass -File $runner -SchemaName $SchemaName -Label baseline_status -Iterations 20 -Warmups 3 -OnlyQueries match_status_page
 if($LASTEXITCODE -ne 0){throw 'Baseline status measurement failed'}
 $cases=@(
  @{Table='`match`';Index='bench_deleted_match_time';Definition='(deleted,match_time)';Query='match_page';Label='idx_match_deleted_time'},
  @{Table='`match`';Index='bench_deleted_status_time';Definition='(deleted,status,match_time)';Query='match_status_page';Label='idx_match_deleted_status_time'},
  @{Table='goal_record';Index='bench_deleted_player';Definition='(deleted,player_id)';Query='top_scorers';Label='idx_goal_deleted_player'},
  @{Table='`match`';Index='bench_status_deleted';Definition='(status,deleted)';Query='standings';Label='idx_match_status_deleted'}
 )
 foreach($c in $cases){
  Invoke-Db "ALTER TABLE $($c.Table) ADD INDEX $($c.Index) $($c.Definition); ANALYZE TABLE $($c.Table); EXPLAIN FORMAT=TRADITIONAL $($queries[$c.Query]);" "before_measure_$($c.Label)"
  try { & powershell -NoProfile -ExecutionPolicy Bypass -File $runner -SchemaName $SchemaName -Label $c.Label -Iterations 20 -Warmups 3 -OnlyQueries $c.Query; if($LASTEXITCODE -ne 0){throw "Measurement failed for $($c.Label)"} }
  finally { Invoke-Db "ALTER TABLE $($c.Table) DROP INDEX $($c.Index); ANALYZE TABLE $($c.Table);" "restore_baseline_$($c.Label)" | Out-Null }
 }
} finally {Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue}
