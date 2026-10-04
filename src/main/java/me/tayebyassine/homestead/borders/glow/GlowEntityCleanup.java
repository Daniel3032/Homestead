package me.tayebyassine.homestead.borders.glow;

import me.tayebyassine.homestead.Homestead;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Lifecycle helper for the glowing border display entities spawned by
 * {@link ChunkGlowSpawner} and {@link SelectedAreaGlowSpawner}.
 * <p>
 * Every marker carries two persistent data tags: {@code homestead_border_glow}, which marks it
 * as plugin-owned, and {@code homestead_border_viewer}, the UUID of the player the border is
 * for. Because markers are real world entities, they must be explicitly hidden from every
 * player other than their viewer, otherwise one player's {@code /hs borders} would render for
 * the whole server. The same tags let the plugin sweep away leftovers persisted by older
 * plugin versions, remove a single player's markers on demand, and drop stale scoreboard
 * glow-team memberships.
 *
 * @see ChunkGlowSpawner
 * @see SelectedAreaGlowSpawner
 */
public final class GlowEntityCleanup {

    private static final String ENTITY_TAG = "homestead_border_glow";
    private static final String VIEWER_TAG = "homestead_border_viewer";
    private static final String TEAM_PREFIX = "homestead_glow_";

    private static NamespacedKey cachedKey;
    private static NamespacedKey cachedViewerKey;

    private GlowEntityCleanup() {
        throw new AssertionError("Uninstantiable class");
    }

    /**
     * Returns the marker tag key, resolved once on first use so repeated sweeps do not
     * rebuild it for every entity.
     *
     * @return the {@link NamespacedKey} identifying border glow entities
     */
    private static NamespacedKey key() {
        if (cachedKey == null) {
            cachedKey = new NamespacedKey(Homestead.getInstance(), ENTITY_TAG);
        }

        return cachedKey;
    }

    /**
     * Returns the viewer tag key, resolved once on first use.
     *
     * @return the {@link NamespacedKey} holding a marker's viewer UUID
     */
    private static NamespacedKey viewerKey() {
        if (cachedViewerKey == null) {
            cachedViewerKey = new NamespacedKey(Homestead.getInstance(), VIEWER_TAG);
        }

        return cachedViewerKey;
    }

    /**
     * Checks whether an entity is one of the plugin's glowing border markers.
     *
     * @param entity the entity to check; null-safe
     * @return {@code true} if the entity carries the border glow tag
     */
    public static boolean isGlowEntity(Entity entity) {
        if (entity == null || Homestead.getInstance() == null) return false;

        return entity.getPersistentDataContainer().has(key(), PersistentDataType.BYTE);
    }

    /**
     * Records which player a marker was spawned for.
     *
     * @param entity   the marker to tag
     * @param viewerId the UUID of the player the border belongs to
     */
    public static void tagViewer(Entity entity, UUID viewerId) {
        if (entity == null || viewerId == null || Homestead.getInstance() == null) return;

        entity.getPersistentDataContainer().set(viewerKey(), PersistentDataType.STRING, viewerId.toString());
    }

    /**
     * Reads the viewer UUID recorded on a marker.
     *
     * @param entity the marker to inspect
     * @return the viewer's UUID, or {@code null} if the marker carries no viewer tag
     */
    public static UUID viewerOf(Entity entity) {
        if (entity == null || Homestead.getInstance() == null) return null;

        String value = entity.getPersistentDataContainer().get(viewerKey(), PersistentDataType.STRING);
        if (value == null) return null;

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Hides a marker from every online player except its viewer.
     * <p>
     * Marker entities exist in the world for all clients by default; the per-player
     * visibility API is what keeps one player's border private to that player.
     *
     * @param entity   the marker to hide
     * @param viewerId the UUID of the only player who should see it
     */
    public static void hideFromEveryoneBut(Entity entity, UUID viewerId) {
        if (entity == null || viewerId == null || Homestead.getInstance() == null) return;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewerId)) continue;

            try {
                online.hideEntity(Homestead.getInstance(), entity);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Removes every tagged border glow entity from the given collection.
     * <p>
     * Also drops the entity from any {@code homestead_glow_*} scoreboard team so that
     * stale team entries are not persisted. Failures on individual entities are ignored.
     *
     * @param entities the entities to filter and remove
     * @return the number of entities actually removed
     */
    public static int removeTagged(Collection<? extends Entity> entities) {
        return removeMatching(entities, entity -> true);
    }

    /**
     * Removes every tagged border glow entity from the given array.
     *
     * @param entities the entities to filter and remove
     * @return the number of entities actually removed
     * @see #removeTagged(Collection)
     */
    public static int removeTagged(Entity[] entities) {
        if (entities == null || entities.length == 0) return 0;

        return removeTagged(Arrays.asList(entities));
    }

    /**
     * Sweeps every currently loaded world and removes all leftover border glow entities,
     * regardless of which player they belong to.
     * <p>
     * Invoked once on startup, before any border task can be running, so it only ever
     * touches entities persisted by a previous server session.
     *
     * @return the number of entities removed
     */
    public static int sweepLoadedWorlds() {
        int removed = 0;

        for (World world : Bukkit.getWorlds()) {
            removed += removeTagged(world.getEntities());
        }

        return removed;
    }

    /**
     * Sweeps every currently loaded world and removes only the border glow entities
     * belonging to the given player.
     * <p>
     *
     * @param viewerId the UUID of the player whose markers should be removed
     * @return the number of entities removed
     */
    public static int removeOwnedBy(UUID viewerId) {
        if (viewerId == null || Homestead.getInstance() == null) return 0;

        int removed = 0;

        for (World world : Bukkit.getWorlds()) {
            removed += removeMatching(world.getEntities(), entity -> viewerId.equals(viewerOf(entity)));
        }

        return removed;
    }

    /**
     * Removes tagged entities that satisfy the given predicate.
     *
     * @param entities  the entities to filter
     * @param predicate which tagged entities to remove
     * @return the number of entities actually removed
     */
    private static int removeMatching(Collection<? extends Entity> entities, Predicate<Entity> predicate) {
        if (entities == null || entities.isEmpty()) return 0;

        int removed = 0;

        for (Entity entity : entities) {
            try {
                if (!isGlowEntity(entity) || !predicate.test(entity)) continue;

                forgetTeamEntry(entity);
                entity.remove();
                removed++;
            } catch (Exception ignored) {
            }
        }

        return removed;
    }

    /**
     * Drops the entity's unique ID from every glow team on the main scoreboard.
     *
     * @param entity the entity whose team membership should be forgotten
     */
    private static void forgetTeamEntry(Entity entity) {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String entry = entity.getUniqueId().toString();

            for (Team team : scoreboard.getTeams()) {
                if (team.getName().startsWith(TEAM_PREFIX) && team.hasEntry(entry)) {
                    team.removeEntry(entry);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
