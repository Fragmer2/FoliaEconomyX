# FoliaEconomyX v2.0 - Advanced Multi-Currency Economy

## Features Overview

###  Implemented (in this archive)
1. **Multi-Currency System** - Regular & Donate currencies
2. **Triple Confirmation** - For donate currency transfers  
3. **Currency Manager** - Full currency management system
4. **Confirmation Manager** - Tracks pending transfers
5. **Enhanced Configuration** - Complete config.yml with all options
6. **Enhanced Messages** - Full messages.yml with localization support

###  Structure Created (you need to implement)

```
src/main/java/com/yourname/foliaeconomyx/
├── FoliaEconomyX.java            TODO: Main plugin class
├── api/
│   ├── FoliaEconomyAPI.java      TODO: Public API for developers
│   └── events/                    TODO: Custom events
├── currency/
│   ├── Currency.java              DONE
│   ├── CurrencyType.java          DONE
│   └── CurrencyManager.java       DONE
├── storage/
│   ├── BalanceStorage.java        TODO: Storage interface
│   ├── mysql/
│   │   └── MySQLStorage.java      TODO: MySQL implementation
│   └── yaml/
│       └── YamlStorage.java       TODO: YAML implementation
├── commands/
│   ├── MoneyCommand.java          TODO: /money command
│   ├── PayCommand.java            TODO: /pay with confirmations
│   ├── EcoCommand.java            TODO: /eco admin
│   ├── BaltopCommand.java         TODO: /baltop command
│   ├── CurrencyCommand.java       TODO: /currency command
│   ├── MigrateCommand.java        TODO: /ecomigrate
│   └── HistoryCommand.java        TODO: /ecohistory
├── confirmation/
│   ├── ConfirmationManager.java   DONE
│   └── PendingTransfer.java       DONE
├── listeners/
│   └── PlayerListener.java        TODO: Event handlers
├── placeholders/
│   └── EconomyExpansion.java      TODO: PlaceholderAPI
├── migration/
│   ├── MigrationManager.java      TODO: Migration handler
│   └── migrators/                 TODO: Plugin-specific migrators
├── webhook/
│   └── DiscordWebhook.java        TODO: Discord notifications
└── utils/
    ├── ConfigManager.java         TODO: Enhanced config handler
    ├── MessageManager.java        TODO: Multi-language support
    ├── NumberFormatter.java       TODO: Format large numbers
    └── UpdateChecker.java         TODO: Check for updates
```

## Implementation Priority

### Phase 1: Core Functionality (Essential)
1. **FoliaEconomyX.java** - Main plugin initialization
2. **BalanceStorage interface** - Define storage contract
3. **MySQLStorage** - Database implementation with HikariCP
4. **YamlStorage** - File-based fallback
5. **EconomyManager** - Multi-currency balance management
6. **VaultEconomyProvider** - Vault integration (default currency only)

### Phase 2: Commands (Required)
1. **MoneyCommand** - View balances for all currencies
2. **PayCommand** - Transfer with confirmation system
3. **EcoCommand** - Admin commands for all currencies
4. **BaltopCommand** - Leaderboards per currency

### Phase 3: Advanced Features
1. **PlaceholderAPI** - %foliaeconomyx_balance_<currency>%
2. **TransactionLogger** - Log to database
3. **HistoryCommand** - View transaction history
4. **DiscordWebhook** - Notify admins of donate transfers

### Phase 4: Enhancements
1. **MigrationManager** - Import from other plugins
2. **LuckPerms integration** - Group-based limits
3. **bStats** - Usage statistics
4. **UpdateChecker** - Auto update notifications
5. **Localization** - Multiple language files

## Key Implementation Notes

### MySQL Storage Structure

```sql
CREATE TABLE balances (
    uuid VARCHAR(36) NOT NULL,
    currency VARCHAR(32) NOT NULL,
    balance DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid, currency),
    INDEX idx_currency (currency),
    INDEX idx_balance (balance)
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    from_uuid VARCHAR(36),
    to_uuid VARCHAR(36),
    currency VARCHAR(32) NOT NULL,
    amount DOUBLE NOT NULL,
    balance_after DOUBLE,
    INDEX idx_from (from_uuid),
    INDEX idx_to (to_uuid),
    INDEX idx_currency (currency),
    INDEX idx_timestamp (timestamp)
);
```

### Confirmation System Flow

```java
// In PayCommand.java
Currency currency = currencyManager.getCurrency(currencyId);

if (currency.needsConfirmation(amount)) {
    if (!player.hasPermission("foliaeconomyx.pay.bypass")) {
        int currentLevel = confirmationManager.getCurrentLevel(player.getUniqueId());
        
        if (currentLevel < currency.getRequireConfirmation()) {
            confirmationManager.addConfirmation(player, target, currencyId, amount, 
                currency.getRequireConfirmation());
            
            // Send confirmation message based on level
            sendConfirmationMessage(player, currentLevel + 1, currency, amount, target);
            return;
        }
    }
}

// All confirmations complete, process transfer
economyManager.transfer(player.getUniqueId(), targetUuid, currencyId, amount);
```

### PlaceholderAPI Example

```java
// %foliaeconomyx_balance_dollars%
// %foliaeconomyx_balance_gems%
// %foliaeconomyx_baltop_1_name_dollars%
// %foliaeconomyx_baltop_1_balance_gems%
```

### API Usage Example

```java
// For other plugins to use
FoliaEconomyAPI api = FoliaEconomyX.getAPI();

// Get balance
double gems = api.getBalance(uuid, "gems");

// Deposit
api.deposit(uuid, "gems", 100);

// Transfer
api.transfer(fromUuid, toUuid, "dollars", 500);

// Listen to events
@EventHandler
public void onBalanceChange(BalanceChangeEvent event) {
    Player player = event.getPlayer();
    String currency = event.getCurrency();
    double oldBalance = event.getOldBalance();
    double newBalance = event.getNewBalance();
}
```

## Configuration Examples

### Enable MySQL
```yaml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: foliaeconomyx
    username: minecraft
    password: securepassword
```

### Add Custom Currency
```yaml
currencies:
  regular:
    tokens:
      enabled: true
      symbol: "🎫"
      name-singular: "Token"
      name-plural: "Tokens"
      starting-balance: 0
      allow-transfer: true
```

### Discord Webhook
```yaml
donate-protection:
  discord-webhook:
    enabled: true
    url: "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
    minimum-amount: 100
```

## Build Instructions

1. Implement missing classes (marked with  TODO)
2. Run `mvn clean package`
3. Find JAR in `target/FoliaEconomyX-2.0.0.jar`

## Dependencies Included

- HikariCP (connection pooling)
- MySQL Connector
- PostgreSQL Driver
- bStats (metrics)
- SLF4J (logging)
- Gson (JSON)

All shaded and relocated to avoid conflicts.

## Testing Checklist

- [ ] Regular currency transfer works
- [ ] Donate currency requires 3 confirmations
- [ ] Confirmation expires after 30 seconds
- [ ] Cooldown prevents spam
- [ ] MySQL saves/loads correctly
- [ ] baltop updates and caches
- [ ] PlaceholderAPI works
- [ ] Vault compatibility maintained
- [ ] Migration imports data correctly
- [ ] Discord webhook sends notifications

## Support

This is a comprehensive foundation. You have:
-  Multi-currency system
-  Confirmation system
-  Full configuration structure
-  All dependencies configured
-  Need to implement storage, commands, and integrations

