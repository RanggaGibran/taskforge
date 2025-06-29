# TaskForge - Comprehensive Jobs Plugin for Minecraft

TaskForge is a highly configurable and balanced Jobs plugin for Minecraft 1.20.6 that focuses on long-term progression and balanced gameplay.

## Features

### Core Features
- **6 Comprehensive Jobs**: Miner, Lumberjack, Farmer, Hunter, Fisherman, and Builder
- **Interactive GUI System**: Multi-layered GUI with job details, objectives, and rewards
- **Balanced Progression**: Exponential leveling system with realistic chance-based rewards
- **Extensive Configuration**: Fully customizable through YAML files
- **Multiple Action Types**: Break, Place, Kill, Breed, Tame, Fish, Craft, Smelt, Enchant, Brew, and Brush
- **Level Rewards**: Money, command execution, particle effects, titles, and items
- **Professional Interface**: Beautiful job browser with detailed job management
- **Economy Integration**: Vault economy support with balanced money distribution
- **PlaceholderAPI Support**: Comprehensive placeholders for job data
- **SQLite Database**: Persistent player data storage

### Jobs Available

#### 🔨 Miner
- Focus on mining ores and stones
- Actions: Breaking stone, ores, and earth materials
- Special rewards for rare ores like diamond and emerald

#### 🪓 Lumberjack  
- Specialized in cutting trees
- Actions: Breaking all wood types including nether stems
- Comprehensive coverage of all wood variants

#### 🌾 Farmer
- Agriculture and animal husbandry
- Actions: Breaking crops, breeding/taming animals
- Covers all farmable crops and livestock

#### ⚔️ Hunter
- Combat and monster hunting
- Actions: Killing hostile and passive mobs
- Balanced rewards for different mob types

#### 🎣 Fisherman
- Fishing and aquatic resources
- Actions: Catching fish, treasure, and junk
- Special rewards for rare catches

#### 🏗️ Builder
- Construction and building
- Actions: Placing blocks and building materials
- Encourages creative building projects

## Installation

1. Download the latest `taskforge-1.0-SNAPSHOT.jar` from the target folder
2. Place it in your server's `plugins` folder
3. Install Vault (required dependency)
4. Optionally install PlaceholderAPI for placeholder support
5. Start/restart your server
6. Configure `config.yml` and `jobs.yml` as needed

## Commands

### Player Commands
- `/jobs` or `/jobs browse` - Open the jobs GUI
- `/jobs join <job>` - Join a specific job
- `/jobs leave <job>` - Leave a specific job  
- `/jobs stats` - View your job statistics
- `/jobs top [job]` - View leaderboards
- `/jobs toggle <feature>` - Toggle cosmetic features

### Admin Commands
- `/taskforgeadmin reload` - Reload plugin configuration
- `/taskforgeadmin setlevel <player> <job> <level>` - Set player's job level
- `/taskforgeadmin addexp <player> <job> <amount>` - Add experience to player
- `/taskforgeadmin resetjob <player> <job>` - Reset player's job progress
- `/taskforgeadmin info <player>` - View player's job information

## Permissions

### Player Permissions
- `taskforge.use` - Basic job commands (default: true)

### Admin Permissions  
- `taskforge.admin` - Admin commands (default: op)

### Job Permissions
- `taskforge.job.all` - Access to all jobs (default: true)

## Configuration

### Main Config (config.yml)

```yaml
settings:
  prefix: "&8[&bTaskForge&8] &r"
  max_jobs: 3
  
leveling:
  base_exp: 100
  multiplier: 1.2
  max_level: 100
  
database:
  type: "SQLITE"
  sqlite_file: "taskforge.db"
```

### Jobs Config (jobs.yml)

Each job is configured with:
- Display name and description
- Player head texture or item icon
- Objectives with experience, money, and chance values
- Level rewards at specific milestones

## Placeholders (PlaceholderAPI)

- `%taskforge_<job>_level%` - Player's level in job
- `%taskforge_<job>_exp%` - Player's experience in job
- `%taskforge_<job>_exp_required%` - Experience required for next level
- `%taskforge_<job>_progress%` - Progress percentage to next level
- `%taskforge_<job>_progress_bar%` - Visual progress bar
- `%taskforge_<job>_name%` - Job display name

## Balancing Philosophy

TaskForge implements several balancing mechanisms:

1. **Low Money & EXP Values**: Prevents instant wealth and encourages long-term play
2. **Chance System**: Not every action gives rewards (configurable 50-90% chances)
3. **Exponential Leveling**: Higher levels require significantly more experience
4. **Material-Based Rewards**: More valuable/rare materials give better rewards
5. **Max Jobs Limit**: Players can only join a limited number of jobs

## Database Schema

The plugin creates three main tables:
- `player_jobs` - Job levels and experience
- `player_stats` - Cumulative statistics  
- `player_settings` - Player preferences

## Development

### Building from Source

1. Clone the repository
2. Install Maven 3.9.9+
3. Run `mvn clean package`
4. Find the JAR in the `target` folder

### Dependencies

- Spigot API 1.20.6
- Vault (required)
- PlaceholderAPI (optional)
- SQLite JDBC driver

## Support

For issues, suggestions, or support:
- Create an issue on the project repository
- Join our Discord community
- Check the documentation wiki

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Changelog

### Version 1.0-SNAPSHOT
- Initial release
- 6 complete jobs with extensive objectives
- GUI-based job browser
- Level rewards system
- PlaceholderAPI integration
- SQLite database support
- Comprehensive configuration options
