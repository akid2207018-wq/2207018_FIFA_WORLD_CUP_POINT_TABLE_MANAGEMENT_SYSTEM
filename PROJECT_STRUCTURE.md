# FIFA World Cup Match & Points Table Management System

A comprehensive JavaFX desktop application for managing FIFA World Cup tournaments with complete match scheduling, real-time standings, knockout bracket generation, and player statistics tracking.

## 🏆 Features Overview

### 1. **Team Registration & Management**
- Register teams with name, code (3-letter), and flag image
- Import/Export team data via JSON
- Store team information in SQLite database
- View all registered teams in a table

### 2. **Group Formation**
- Organize teams into 8 World Cup groups (A-H)
- Manual team assignment to groups
- Auto-assign functionality for quick setup
- Visual group display with drag-and-drop support

### 3. **Automatic Match Scheduling**
- Generate complete group-stage fixtures (round-robin)
- Each team plays every other team in its group once
- Export match schedules to JSON
- Filter matches by group

### 4. **Match Results Entry**
- Enter and update match scores
- Score validation (no negative values)
- Mark matches as completed
- Filter by pending/completed matches

### 5. **Dynamic Points Table**
- Real-time standings updates after each match
- Points allocation: Win = 3, Draw = 1, Loss = 0
- Automatic calculation of:
  - Played (P)
  - Won (W)
  - Drawn (D)
  - Lost (L)
  - Goals For (GF)
  - Goals Against (GA)
  - Goal Difference (GD)
  - Points (Pts)
- FIFA sorting rules: Points → GD → GF → Head-to-Head

### 6. **Group Qualifiers**
- Determine top 2 teams from each group
- Visual qualification indicators
- Auto-qualification logic based on standings

### 7. **Knockout Bracket Generator**
- Generate Round of 16 matches from group qualifiers
- Visual tournament bracket display
- Official World Cup pairing rules (e.g., 1A vs 2B)
- Bracket columns: R16 → Quarters → Semis → Final

### 8. **Knockout Advancement**
- Winners automatically advance to next round
- Quarter-Finals generation from R16 winners
- Semi-Finals from Quarter winners
- Final match and Champion determination

### 9. **Match History & Statistics**
- View all completed matches
- Filter by stage (GROUP, ROUND_16, QUARTER, SEMI, FINAL)
- Filter by specific team
- Export match history to JSON
- Display match dates and scores

### 10. **Team Statistics Dashboard**
- Comprehensive team stats (GF, GA, GD, Points)
- Win/Draw/Loss counts
- Real-time updates based on match results
- Export team statistics

### 11. **Player Statistics Tracking**
- Register players with team, jersey number, and position
- Track individual statistics:
  - Goals scored
  - Assists
  - Yellow/Red cards
  - Matches played
- Top Scorers leaderboard
- Top Assists leaderboard
- Filter players by team

---

## 📂 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── fifaworldcup/
│   │           ├── Main.java                      # Application entry point
│   │           ├── model/                         # Data models
│   │           │   ├── Team.java
│   │           │   ├── Match.java
│   │           │   ├── Player.java
│   │           │   └── KnockoutMatch.java
│   │           ├── database/                      # Database layer
│   │           │   └── DatabaseManager.java
│   │           ├── service/                       # Business logic
│   │           │   ├── TeamService.java
│   │           │   ├── MatchService.java
│   │           │   ├── PlayerService.java
│   │           │   ├── StandingsService.java
│   │           │   └── KnockoutService.java
│   │           ├── ui/                            # Controllers
│   │           │   ├── HomeController.java
│   │           │   ├── TeamRegistrationController.java
│   │           │   ├── GroupFormationController.java
│   │           │   ├── MatchSchedulingController.java
│   │           │   ├── MatchResultsController.java
│   │           │   ├── PointsTableController.java
│   │           │   ├── KnockoutBracketController.java
│   │           │   ├── MatchHistoryController.java
│   │           │   └── PlayerStatsController.java
│   │           └── util/                          # Utilities
│   │               └── NavigationUtil.java
│   └── resources/
│       ├── fxml/                                  # FXML scenes
│       │   ├── home.fxml
│       │   ├── team_registration.fxml
│       │   ├── group_formation.fxml
│       │   ├── match_scheduling.fxml
│       │   ├── match_results.fxml
│       │   ├── points_table.fxml
│       │   ├── knockout_bracket.fxml
│       │   ├── match_history.fxml
│       │   └── player_stats.fxml
│       ├── styles/
│       │   └── style.css                         # Application styles
│       └── images/                                # Team flags, icons
└── pom.xml                                        # Maven configuration
```

---

## 🛠️ Technology Stack

- **Java 11+**
- **JavaFX 17** - UI Framework
- **SQLite** - Database (sqlite-jdbc 3.44.0.0)
- **Gson 2.10.1** - JSON serialization
- **Maven** - Build tool

---



### Database Initialization
The SQLite database (`fifa_worldcup.db`) is automatically created on first run with the following tables:
- `teams` - Team information and statistics
- `matches` - Group stage matches
- `players` - Player details and stats
- `knockout_matches` - Knockout stage brackets

---

## 📊 Database Schema

### Teams Table
```sql
CREATE TABLE teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    code TEXT NOT NULL UNIQUE,
    group_name TEXT,
    flag_path TEXT,
    played INTEGER DEFAULT 0,
    won INTEGER DEFAULT 0,
    drawn INTEGER DEFAULT 0,
    lost INTEGER DEFAULT 0,
    goals_for INTEGER DEFAULT 0,
    goals_against INTEGER DEFAULT 0,
    goal_difference INTEGER DEFAULT 0,
    points INTEGER DEFAULT 0,
    qualified INTEGER DEFAULT 0
);
```

### Matches Table
```sql
CREATE TABLE matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team1_id INTEGER,
    team2_id INTEGER,
    team1_score INTEGER DEFAULT 0,
    team2_score INTEGER DEFAULT 0,
    stage TEXT,
    group_name TEXT,
    match_date TEXT,
    match_number INTEGER,
    completed INTEGER DEFAULT 0
);
```

### Players Table
```sql
CREATE TABLE players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    team_id INTEGER,
    jersey_number INTEGER,
    position TEXT,
    goals INTEGER DEFAULT 0,
    assists INTEGER DEFAULT 0,
    yellow_cards INTEGER DEFAULT 0,
    red_cards INTEGER DEFAULT 0,
    matches_played INTEGER DEFAULT 0
);
```

### Knockout Matches Table
```sql
CREATE TABLE knockout_matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_number INTEGER,
    round TEXT,
    team1_id INTEGER,
    team2_id INTEGER,
    team1_score INTEGER DEFAULT 0,
    team2_score INTEGER DEFAULT 0,
    winner_id INTEGER,
    bracket_position TEXT,
    completed INTEGER DEFAULT 0
);
