# API Examples

Practical snippets for the most common API tasks. All examples use the published API coordinates from [Developer API](API.md).

!!! warning "Model ID Change"

    As of release 5.2.0.0, all model IDs use [Twitter Snowflake IDs](https://en.wikipedia.org/wiki/Snowflake_ID) instead of UUID v4.

---

## Creating a Region

Create a new region owned by a player:

```java
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.tayebyassine.homestead.managers.RegionManager;
import me.tayebyassine.homestead.models.Region;

Player player = Bukkit.getPlayer("TFA_Gaming");
Region region = RegionManager.createRegion("ExampleRegion", player);
```

---

## Fetching a Region

Look up a region by name (case-insensitive) or by its numeric ID:

```java
import me.tayebyassine.homestead.managers.RegionManager;
import me.tayebyassine.homestead.models.Region;

// By name (case-insensitive)
Region region = RegionManager.findRegion("ExampleRegion");

// By ID
Region region = RegionManager.findRegion(309031393541763072L);
```

---

## Deleting a Region

Delete a region by ID after confirming it exists:

```java
import me.tayebyassine.homestead.managers.RegionManager;
import me.tayebyassine.homestead.models.Region;

long regionId = 309031393541763072L;

Region region = RegionManager.findRegion(regionId);
if (region != null) {
    RegionManager.deleteRegion(regionId);
}
```

---

## Claiming a Chunk

Claim a chunk for a region. The return value is a `ChunkManager.Error`. `null` means success:

```java
import org.bukkit.Chunk;
import me.tayebyassine.homestead.managers.ChunkManager;
import me.tayebyassine.homestead.models.Region;

Region region = ...;
Chunk chunk = ...;

ChunkManager.Error error = ChunkManager.claimChunk(region.getUniqueId(), chunk);

if (error == null) {
    // Success
} else {
    // Handle error (e.g. error == ChunkManager.Error.CHUNK_NOT_ADJACENT_TO_REGION)
}
```

Possible errors: `REGION_NOT_FOUND`, `CHUNK_NOT_FOUND`, `CHUNK_IN_DISABLED_WORLD`, `CHUNK_NOT_ADJACENT_TO_REGION`, `CHUNK_WOULD_SPLIT_REGION`.

---

## Unclaiming a Chunk

Remove a chunk from a region:

```java
import org.bukkit.Chunk;
import me.tayebyassine.homestead.managers.ChunkManager;
import me.tayebyassine.homestead.models.Region;

Region region = ...;
Chunk chunk = ...;

ChunkManager.Error error = ChunkManager.unclaimChunk(region.getUniqueId(), chunk);

if (error == null) {
    // Success
}
```

---

## Managing Members

Invite players, revoke invites, and check invitation status:

```java
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.tayebyassine.homestead.managers.InviteManager;
import me.tayebyassine.homestead.models.Region;

Player target = Bukkit.getPlayer("test_account");
Region region = ...;

// Invite a player
InviteManager.invitePlayer(region, target);

// Delete invites of a player
InviteManager.deleteInvitesOfPlayer(target);

// Delete invites of a region
InviteManager.deleteInvitesOfRegion(region);

// Get invites of a player
InviteManager.getInvitesOfPlayer(target);

// Get invites of a region
InviteManager.getInvitesOfRegion(region);

// Check if invited
boolean invited = InviteManager.isInvited(region, target);
```

---

## Working with Flags

Read and modify bitwise flag values on a region:

```java
import java.util.List;
import me.tayebyassine.homestead.flags.FlagCalculator;
import me.tayebyassine.homestead.flags.PlayerFlag;
import me.tayebyassine.homestead.models.Region;

Region region = ...;

// Get flag values
long playerFlags = region.getPlayerFlags();
long worldFlags = region.getWorldFlags();

// Get all player flag names as they appear in the config
List<String> flagNames = PlayerFlag.getFlags();

// Resolve a config name to its bitmask value
long flag = PlayerFlag.parse("break-blocks");

// Modify flags — PlayerFlag is an enum, so OR in the bit you want
long newFlags = playerFlags | PlayerFlag.BREAK_BLOCKS.getBitmask();
// → Non-members CAN break blocks

newFlags = FlagCalculator.removeFlag(newFlags, PlayerFlag.CONTAINERS);
// → Non-members CANNOT open containers

// Check if a flag is set
if (FlagCalculator.isFlagSet(playerFlags, PlayerFlag.PVP)) {
    // PvP is allowed
}

// Apply changes
region.setPlayerFlags(newFlags);
```

!!! warning "Don't Mix Flag Types"

    Each flag class (`PlayerFlag`, `WorldFlag`, `ControlFlag`) has its own values. Don't use `WorldFlag` values for player flags.

!!! note "The flags are enums, not constants classes"

    `PlayerFlag.valueOf("break-blocks")` throws `IllegalArgumentException` — `valueOf` matches the Java constant name (`BREAK_BLOCKS`), not the config name. Use `PlayerFlag.parse("break-blocks")` to go from config name to bitmask, or `PlayerFlag.BREAK_BLOCKS.getBitmask()` for the constant.

---

## Listening to Events

Register a listener to react to Homestead events:

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.tayebyassine.homestead.api.events.chunk.ChunkClaimEvent;
import me.tayebyassine.homestead.api.events.communication.RegionChatEvent;
import me.tayebyassine.homestead.api.events.economy.BankDepositEvent;
import me.tayebyassine.homestead.api.events.player.PlayerJoinRegionEvent;
import me.tayebyassine.homestead.api.events.region.RegionCreateEvent;

public class HomesteadListener implements Listener {

    @EventHandler
    public void onChunkClaim(ChunkClaimEvent event) {
        event.getRegion();  // The region
        event.getChunk();   // The chunk being claimed
        event.setCancelled(true);  // ChunkClaimEvent is cancellable
    }

    @EventHandler
    public void onRegionCreate(RegionCreateEvent event) {
        event.getRegion();
        event.getPlayer();  // OfflinePlayer
    }

    @EventHandler
    public void onPlayerJoinRegion(PlayerJoinRegionEvent event) {
        event.getPlayer();  // OfflinePlayer
        event.getRegion();
    }

    @EventHandler
    public void onBankDeposit(BankDepositEvent event) {
        event.getRegion();
        event.getAmount();
    }

    @EventHandler
    public void onRegionChat(RegionChatEvent event) {
        event.getPlayer();  // OfflinePlayer
        event.getMessage();
        event.getRegion();
    }
}
```

!!! warning "Events live in sub-packages"

    `me.tayebyassine.homestead.api.events.*` only reaches `APIEvent` — every concrete event sits in a sub-package such as `api.events.chunk` or `api.events.region`. Import them individually, as above. See [Developer API](API.md#api-events) for the full mapping.

### Register Your Listener

```java
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new HomesteadListener(), this);
    }
}
```

!!! note "Depend on Homestead at runtime too"

    Add `depend: [Homestead]` (or `softdepend: [Homestead]` if optional) to your `plugin.yml` so Bukkit loads Homestead before your plugin — otherwise the event classes will not be on the server when you register.

