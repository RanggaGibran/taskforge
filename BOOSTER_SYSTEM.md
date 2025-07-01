# TaskForge Booster System

The booster system allows administrators to set multipliers for job rewards, both globally and for specific players.

## Commands

### Personal Boosters
Set boosters for specific players:
```
/taskforgeadmin booster player <multiplier> <playerName> <type>
```

Examples:
- `/taskforgeadmin booster player 2.0 Steve exp` - 2x experience for Steve
- `/taskforgeadmin booster player 3.0 Alex money` - 3x money for Alex
- `/taskforgeadmin booster player 1.5 Bob exp` - 1.5x experience for Bob

### Global Boosters
Set boosters that affect all players:
```
/taskforgeadmin booster global <multiplier> <type>
```

Examples:
- `/taskforgeadmin booster global 2.0 exp` - 2x experience for everyone
- `/taskforgeadmin booster global 1.5 money` - 1.5x money for everyone

### Remove Boosters
Remove specific boosters:
```
/taskforgeadmin booster remove player <playerName> <type>
/taskforgeadmin booster remove global <type>
```

Examples:
- `/taskforgeadmin booster remove player Steve exp`
- `/taskforgeadmin booster remove global money`

### List Boosters
View current boosters:
```
/taskforgeadmin booster list              # List global boosters
/taskforgeadmin booster list <playerName> # List specific player's boosters
```

### Clear Boosters
Clear all boosters:
```
/taskforgeadmin booster clear player <playerName> # Clear all boosters for a player
/taskforgeadmin booster clear global              # Clear all global boosters
```

## Booster Types

- **exp** - Experience multiplier
- **money** - Money multiplier

## How Boosters Work

1. **Stacking**: Personal and global boosters stack multiplicatively
   - If global exp booster is 2.0x and player has 1.5x personal exp booster
   - Total multiplier = 2.0 × 1.5 = 3.0x experience

2. **Default Values**: 1.0 = normal rewards (no boost)

3. **Salary Integration**: Money boosters work with both instant payments and salary system

## Examples

### Event Boost
Set a global 2x experience boost for an event:
```
/taskforgeadmin booster global 2.0 exp
```

### VIP Player Rewards
Give a VIP player permanent boosters:
```
/taskforgeadmin booster player 1.5 VIPPlayer exp
/taskforgeadmin booster player 1.3 VIPPlayer money
```

### Weekend Bonus
Weekend money boost for all players:
```
/taskforgeadmin booster global 1.5 money
```

When the weekend is over:
```
/taskforgeadmin booster remove global money
```

## Implementation Notes

- Boosters are stored in memory and reset on server restart
- Boosters apply to all job rewards (experience and money)
- Works with existing salary system - boosted money goes to pending salary
- Tab completion available for all commands and parameters
