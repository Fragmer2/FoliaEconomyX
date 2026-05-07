# BurzhuyPlugin v2.0.0

A comprehensive Minecraft economy plugin with a buyer system, quest mechanics, and multiplier progression. Fully compatible with both Paper and Folia servers.

## Features

- **Dynamic Buyer System**: Players can sell items to an NPC with randomized lots that refresh automatically
- **Slot Upgrades**: Expand your trading capacity by purchasing additional slots
- **Multiplier Quests**: Complete quests to increase your selling multiplier
- **Auto-Refresh**: Lots automatically shuffle at configurable intervals
- **Multi-Language Support**: All messages stored in `messages.yml` for easy translation
- **Folia Compatible**: Works seamlessly on both Paper and Folia servers with region-based threading

## Requirements

- Minecraft Server 1.21+
- Paper or Folia
- Vault plugin
- Any economy plugin supported by Vault (e.g., EssentialsX)

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Ensure Vault and an economy plugin are installed
4. Start/restart your server
5. Configure `config.yml` and `messages.yml` to your liking
6. Reload the plugin with `/reload confirm` or restart

## Configuration

### Main Config (`config.yml`)

```yaml
buyer_reset:
  interval_seconds: 21600 # How often lots refresh (6 hours default)
  
multiplier:
  base: 1.0              # Starting multiplier
  per_level: 0.4         # Multiplier increase per level
  max_level: 5           # Maximum multiplier level

upgrade_slot_cost:
  base: 80000            # Base cost for first slot upgrade
  per_slot: 10000        # Additional cost per slot owned
```

### Messages (`messages.yml`)

All player-facing messages are in this file. You can translate them to any language by editing the values.

### Quest Configuration

Quests are configured in `config.yml` under `interface.quests`. Each quest has:
- `name`: Display name
- `type`: Quest type (MOB_KILL, ITEM_DELIVER, BLOCK_BREAK, etc.)
- `target`: Target entity/material/dimension
- `amount`: Number required to complete

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/buyer` | Open buyer menu | `burzhuy.buyer` |
| `/upgradebuyer` | Open slot upgrade menu | `burzhuy.slotupgrademenu` |
| `/multiplierquest` | Open quest menu | `burzhuy.multiplierquest` |
| `/refreshlots <player>` | Refresh player's lots (admin) | `burzhuy.admin` |

## Permissions

### Slot Limits
- `burzhuy.maxslots.12` - 12 slots (default)
- `burzhuy.maxslots.14` - 14 slots
- `burzhuy.maxslots.16` - 16 slots
- `burzhuy.maxslots.18` - 18 slots
- `burzhuy.maxslots.20` - 20 slots
- `burzhuy.maxslots.22` - 22 slots

### Menu Access
- `burzhuy.buyer` - Access buyer menu (default: true)
- `burzhuy.slotupgrademenu` - Access upgrade menu (default: true)
- `burzhuy.multiplierquest` - Access quest menu (default: true)

### Admin
- `burzhuy.admin` - Admin commands (default: op)

## Quest Types

1. **MOB_KILL**: Kill specific entities
2. **ITEM_DELIVER**: Deliver items to complete
3. **BLOCK_BREAK**: Mine specific blocks
4. **ITEM_CRAFT**: Craft specific items
5. **GET_ITEM**: Obtain specific items
6. **TRAVEL**: Visit dimensions (Nether/End)
7. **WALK_DISTANCE**: Walk a certain distance
8. **VISIT_OCEAN**: Visit ocean biomes
9. **JUMP**: Jump a number of times
10. **USE_TOTEM**: Use totems of undying
11. **DRINK_POTION**: Consume specific potions

## Folia Compatibility

This plugin is designed to work on both Paper and Folia:

- **Paper**: Uses standard Bukkit scheduler
- **Folia**: Automatically detects Folia and uses region-based scheduling
- No configuration needed - the plugin adapts automatically

The scheduler adapter ensures:
- Entity-specific tasks run in the entity's region
- Global tasks use the global region scheduler
- Async tasks use the async scheduler
- No thread safety issues

## API Usage

### Getting Started

```java
BurzhuyPlugin plugin = (BurzhuyPlugin) Bukkit.getPluginManager().getPlugin("BurzhuyPlugin");
BuyerDataManager dataManager = plugin.getBuyerDataManager();
```

### Accessing Player Data

```java
Player player = // ... get player
BuyerData data = dataManager.get(player);
int slots = data.getSlots();
int multiplierLevel = data.getMultiplierLevel();
```

## Development

### Building from Source

```bash
git clone <repository>
cd BurzhuyPlugin
mvn clean package
```

The compiled JAR will be in `target/BurzhuyPlugin-2.0.0.jar`

### Project Structure

```
src/main/java/me/yourname/burzhuy/
├── BurzhuyPlugin.java          # Main plugin class
├── scheduler/
│   └── SchedulerAdapter.java   # Folia compatibility layer
├── utils/
│   └── MessageManager.java     # Message handling
├── data/
│   ├── BuyerData.java          # Player data model
│   └── BuyerDataManager.java   # Data persistence
├── economy/
│   └── EconomyManager.java     # Vault integration
├── items/
│   ├── ItemPrice.java          # Item price model
│   └── ItemPriceManager.java   # Price management
├── menu/
│   ├── BuyerMenu.java          # Main buyer interface
│   ├── SlotUpgradeMenu.java    # Upgrade interface
│   └── MultiplierQuestMenu.java # Quest interface
└── quest/
    ├── QuestType.java           # Quest types enum
    ├── MultiplierQuest.java     # Quest model
    ├── MultiplierQuestPool.java # Quest loading
    ├── MultiplierQuestUtil.java # Quest utilities
    └── MultiplierQuestListener.java # Quest event handling
```

## Migration from v1.x

1. Backup your `playerdata.yml`
2. Install v2.0.0
3. Old player data will be automatically migrated
4. Update any custom configurations
5. Translate `messages.yml` if needed

## Support

For issues, suggestions, or contributions:
- Create an issue on GitHub
- Join our Discord server
- Contact the development team

## License

This plugin is released under the MIT License. See LICENSE file for details.

## Credits

- **Author**: BohdanStepantsov
- **Version**: 2.0.0
- **API**: 1.21
- **Compatible with**: Paper & Folia

## Changelog

### v2.0.0 (Current)
- Full Folia support with automatic detection
- Complete English localization
- Message system with `messages.yml`
- Updated to Minecraft 1.21 API
- Improved error handling
- Code refactoring and optimization
- Better documentation

### v1.0.0
- Initial release
- Basic buyer system
- Quest mechanics
- Russian language only
