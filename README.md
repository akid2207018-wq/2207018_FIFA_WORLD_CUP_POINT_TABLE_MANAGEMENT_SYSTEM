# FIFA World Cup - Point Table Management System

A comprehensive JavaFX desktop application for managing FIFA World Cup tournaments with features for team registration, match scheduling, real-time points tracking, and knockout bracket generation.

## Features

### 1. **Group Formation & Team Registration**
- Register all participating teams with basic details (name, code, flag)
- Automatically group teams into predefined World Cup groups (A–H)
- Store team data in SQLite with JSON import/export support

### 2. **Automatic Group-Stage Match Scheduling**
- Generate all group-stage fixtures based on registered teams
- Round-robin schedule ensuring each team plays every other team once
- Export/import match schedules to/from JSON

### 3. **Match Result Entry System**
- User-friendly UI for entering match scores
- Score editing with validation (no negative values)
- Instant database updates after submission

### 4. **Dynamic Points Table Updater**
- Automatic points allocation (Win = 3, Draw = 1, Loss = 0)
- Real-time calculation of goals scored, conceded, and goal difference
- Live table updates in the UI

### 5. **Real-Time Standings & Group Sorting**
- Display updated group standings instantly after each match
- FIFA-compliant sorting rules:
  - Points → Goal Difference → Goals Scored → Head-to-Head
- Dynamic UI refresh in JavaFX

### 6. **Group Ranking & Qualification Logic**
- Automatically identify top 2 teams from each group
- Store qualified teams for knockout stage
- Visual indicators for advanced teams

### 7. **Knockout Bracket Generator**
- Automatic pairing of qualified teams (Round of 16)
- Visual bracket display using JavaFX
- Store bracket layout in SQLite + JSON

### 8. **Automatic Advancement for Knockout Winners**
- Winners automatically advance through rounds:
  - Quarter-Finals → Semi-Finals → Finals → Champion
- Dynamic bracket updates with locked previous rounds

### 9. **Match History & Statistics Storage**
- Store all historical match data (teams, scores, dates, stages)
- View history by team, group, or tournament stage
- Backup and sharing via JSON export

### 10. **Team Statistics Calculation**
- Real-time computation of:
  - **GF** (Goals For)
  - **GA** (Goals Against)
  - **GD** (Goal Difference)
  - **Points** & Win/Draw/Loss Count

## Project Structure

```
fifa-world-cup-management/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fifaworldcup/
│   │   │       ├── Main.java
│   │   │       ├── ui/
│   │   │       ├── models/
│   │   │       ├── services/
│   │   │       └── database/
│   │   └── resources/
│   │       ├── styles/
│   │       │   └── style.css
│   │       └── images/
│   └── test/
├── pom.xml
└── README.md
```

## Technologies Used

- **GUI Framework**: JavaFX 17.0.1
- **Database**: SQLite3
- **Data Format**: JSON (Gson)
- **Build Tool**: Maven
- **Java Version**: 11+

## System Requirements

- Java 11 or higher
- 4GB RAM minimum
- 100MB disk space

## Installation & Setup

### Prerequisites
- Maven 3.6+
- Java Development Kit (JDK) 11+

### Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/akid2207018-wq/2207018_FIFA_WORLD_CUP_POINT_TABLE_MANAGEMENT_SYSTEM.git
   cd fifa-world-cup-management
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn javafx:run
   ```

   Or run directly:
   ```bash
   java -jar target/fifa-world-cup-management-1.0.0-jar-with-dependencies.jar
   ```

## Usage Guide

1. **Start the Application**: Launch the home screen
2. **Register Teams**: Add participating teams and assign them to groups
3. **Schedule Matches**: Auto-generate group-stage fixtures
4. **Enter Results**: Input match scores as games are played
5. **Track Standings**: View real-time points table and group rankings
6. **Generate Brackets**: Create knockout stage pairings
7. **Track Progress**: Monitor advancement through tournament stages
8. **Export Data**: Save tournament data as JSON for backup/sharing

## Database Schema

### Tables
- **teams**: Team information (id, name, code, group)
- **matches**: Match records (id, home_team, away_team, score, stage, date)
- **standings**: Group standings (team_id, group, points, goals_for, goals_against, wins, draws, losses)
- **knockout**: Knockout bracket structure (match_id, round, bracket_position)

## API/Service Layer

### Core Services
- `TeamService`: Team management and group assignment
- `MatchService`: Match scheduling and result entry
- `StandingsService`: Real-time points table calculations
- `KnockoutService`: Bracket generation and advancement logic
- `DataExportService`: JSON export/import functionality

## Configuration

Edit configuration settings in `src/main/resources/config/app.properties`:
- Database path
- Default tournament year
- Group configurations
- Match rules

## Known Limitations

- Currently supports single tournament instance
- Manual tie-breaking for equal points (future enhancement)
- Penalty shootout not implemented (uses head-to-head only)

## Future Enhancements

- [ ] Multi-tournament support
- [ ] Penalty shootout logic
- [ ] Match commentary/live updates
- [ ] Team performance analytics & charts
- [ ] User authentication for admin controls
- [ ] Web dashboard integration
- [ ] Tournament scheduling with timezones
- [ ] Mobile app companion

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

**Akid** (2207018)
- Email: [your-email@example.com]
- GitHub: [@akid2207018-wq](https://github.com/akid2207018-wq)

## Support & Issues

For bug reports or feature requests, please open an issue on the GitHub repository.

---

**Version**: 1.0.0  
**Last Updated**: December 2025
