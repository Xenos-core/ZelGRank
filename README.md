# ZelGRank

A lightweight Minecraft plugin that simplifies LuckPerms rank management with visual notifications, temporary rank support, and an intuitive GUI for viewing player permissions.

## Features

- **Simplified Rank Commands** - Easy-to-use commands for granting and removing ranks
- **Temporary Ranks** - Support for time-based rank assignments (seconds, minutes, hours, days, weeks, months, years)
- **Visual Notifications** - Beautiful broadcast messages 
- **Sound Effects** - sound plays for all players on rank changes
- **Permission GUI** - View player groups in an interactive inventory interface
- **Configurable Messages** - Customize all messages, sounds, and rank prices via config.yml
- **LuckPerms Integration** - Seamlessly works with LuckPerms as a hard dependency.

## Commands

| Command | Description | Permission | Usage |
|---------|-------------|------------|-------|
| `/giverank` | Grant a rank to a player | `zelgrank.giverank` | `/giverank <player> <rank> [time]` |
| `/demote` | Remove a rank from a player | `zelgrank.demote` | `/demote <player> <rank>` |
| `/ginfo` | View player's groups in GUI | `zelgrank.info` | `/ginfo <player>` |


### Time Format Examples
- `30s` - 30 seconds
- `5m` - 5 minutes
- `1h` - 1 hour
- `7d` - 7 days
- `2w` - 2 weeks
- `1M` - 1 month
- `1y` - 1 year

### Command Examples
```
/giverank CreepySxS VIP 30d
/giverank Steve MVP
/demote CreepySxS VIP
/ginfo CreepySxS
```

## Ensure **LuckPerms** is installed (required dependency).

## Developer API

ZelGRank now exposes `ZelGRankApi` through Bukkit services:

```java
RegisteredServiceProvider<ZelGRankApi> provider =
    Bukkit.getServicesManager().getRegistration(ZelGRankApi.class);

ZelGRankApi api = provider.getProvider();
```

Available API methods:

- `giveRank(uuid, name, rank, duration, actor)`
- `removeRank(uuid, name, rank, actor)`
- `getRanks(uuid)`
- `hasRank(uuid, rank)`
- `lookupUniqueId(playerName)`
- `rankExists(rank)`

Plugins can also listen for:

- `ZelGRankGivenEvent`
- `ZelGRankRemovedEvent`
