param(
    [string]$SchemaName = 'football_admin_bench_20261017_a1',
    [string]$OutputSql = (Join-Path $PSScriptRoot 'setup_and_synthetic_data.sql')
)
if ($SchemaName -notmatch '^football_admin_bench_[a-zA-Z0-9_]+$') { throw 'Schema name must use the isolated football_admin_bench_ prefix.' }
$writer = [System.IO.StreamWriter]::new($OutputSql, $false, [System.Text.UTF8Encoding]::new($false))
function Write-Batches([string]$Table, [string]$Columns, [int]$Count, [scriptblock]$Row, [int]$BatchSize = 250) {
    for ($start = 1; $start -le $Count; $start += $BatchSize) {
        $end = [Math]::Min($Count, $start + $BatchSize - 1)
        $values = [System.Collections.Generic.List[string]]::new()
        for ($i = $start; $i -le $end; $i++) { $values.Add((& $Row $i)) }
        $writer.WriteLine("INSERT INTO $Table ($Columns) VALUES")
        $writer.WriteLine(($values -join ",`n") + ';')
    }
}
try {
    $writer.WriteLine("CREATE DATABASE ``$SchemaName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
    $writer.WriteLine("USE ``$SchemaName``;")
    $writer.WriteLine(@"
CREATE TABLE team (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, coach VARCHAR(50), city VARCHAR(50), description TEXT,
 status TINYINT DEFAULT 1, deleted TINYINT DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_name(name), INDEX idx_city(city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE player (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50) NOT NULL, age INT, position VARCHAR(20), jersey_number INT, team_id BIGINT,
 status TINYINT DEFAULT 1, deleted TINYINT DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_name(name), INDEX idx_team_id(team_id), CONSTRAINT fk_player_team FOREIGN KEY(team_id) REFERENCES team(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE ``match`` (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, home_team_id BIGINT NOT NULL, away_team_id BIGINT NOT NULL, match_time DATETIME NOT NULL,
 stadium VARCHAR(100), status VARCHAR(20) DEFAULT 'upcoming', tag VARCHAR(50), deleted TINYINT DEFAULT 0,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_match_time(match_time), INDEX idx_home_team(home_team_id), INDEX idx_away_team(away_team_id), INDEX idx_status(status),
 CONSTRAINT fk_match_home FOREIGN KEY(home_team_id) REFERENCES team(id) ON DELETE CASCADE,
 CONSTRAINT fk_match_away FOREIGN KEY(away_team_id) REFERENCES team(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE match_result (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, match_id BIGINT NOT NULL, home_score INT DEFAULT 0, away_score INT DEFAULT 0,
 referee VARCHAR(50), deleted TINYINT DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_match_id(match_id), UNIQUE KEY uk_match_id(match_id),
 CONSTRAINT fk_result_match FOREIGN KEY(match_id) REFERENCES ``match``(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE goal_record (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, match_id BIGINT NOT NULL, player_id BIGINT NOT NULL, goal_time INT NOT NULL,
 deleted TINYINT DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_match_id(match_id), INDEX idx_player_id(player_id),
 CONSTRAINT fk_goal_match FOREIGN KEY(match_id) REFERENCES ``match``(id) ON DELETE CASCADE,
 CONSTRAINT fk_goal_player FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@)
    Write-Batches 'team' 'name,coach,city,description,status,deleted' 128 { param($i) "('BenchTeam_$('{0:D4}' -f $i)','Synthetic Coach','Bench City','synthetic benchmark row',1,0)" }
    Write-Batches 'player' 'name,age,position,jersey_number,team_id,status,deleted' 12800 { param($i) "('BenchPlayer_$('{0:D5}' -f $i)',25,'Midfielder',$($i % 99 + 1),$((($i - 1) % 128) + 1),1,$([int]($i % 20 -eq 0)))" }
    Write-Batches '`match`' 'home_team_id,away_team_id,match_time,stadium,status,tag,deleted' 20000 { param($i) $status=if($i -le 12000){'finished'}elseif($i -le 13000){'ended'}elseif($i -le 16000){'in_progress'}else{'upcoming'}; $deleted=[int]($i % 20 -eq 0); "($((($i - 1) % 128) + 1),$((($i + 62) % 128) + 1),DATE_ADD('2024-01-01 00:00:00',INTERVAL $i MINUTE),'Bench Stadium','$status','synthetic',$deleted)" }
    Write-Batches 'match_result' 'match_id,home_score,away_score,referee,deleted' 13000 { param($i) "($i,$($i % 5),$((($i + 2) % 5)),'Bench Referee',$([int]($i % 20 -eq 0)))" }
    Write-Batches 'goal_record' 'match_id,player_id,goal_time,deleted' 100000 { param($i) "($((($i - 1) % 20000) + 1),$((($i * 7919 - 1) % 12800) + 1),$((($i - 1) % 90) + 1),$([int]($i % 20 -eq 0)))" }
}
finally { $writer.Dispose() }
