package me.tayebyassine.homestead.listeners.borders;

import me.tayebyassine.homestead.borders.ChunkBorder;
import me.tayebyassine.homestead.borders.blocks.BorderBlockRenderer;
import me.tayebyassine.homestead.borders.blocks.FakeBorderRegistry;
import me.tayebyassine.homestead.borders.glow.ChunkGlowSpawner;
import me.tayebyassine.homestead.borders.glow.GlowEntityCleanup;
import me.tayebyassine.homestead.borders.glow.SelectedAreaGlowSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Protects the plugin's visual border blocks from being broken or dropped, and cleans up a player's
 * rendered borders when the block is removed or the player quits.
 * <p>
 * Also keeps glowing border markers private: they are hidden from players who join after the
 * markers spawned, and removed from the world when a chunk loads so markers persisted by older
 * plugin versions disappear as soon as the server touches the chunk holding them.
 */
public final class BorderBreakListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        FakeBorderRegistry.FakeBorderBlock fbb = FakeBorderRegistry.getByLocation(e.getBlock().getLocation());
        if (fbb == null) return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent e) {
        if (FakeBorderRegistry.getByLocation(e.getBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreakMonitor(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        FakeBorderRegistry.FakeBorderBlock fbb = FakeBorderRegistry.getByLocation(e.getBlock().getLocation());
        if (fbb == null) return;

        BorderBlockRenderer.removeRegion(fbb.regionId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        BorderBlockRenderer.removeAll(e.getPlayer());
        ChunkBorder.stop(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        ChunkGlowSpawner.hideForeignTo(e.getPlayer());
        SelectedAreaGlowSpawner.hideForeignTo(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e) {
        GlowEntityCleanup.removeTagged(e.getChunk().getEntities());
    }
}