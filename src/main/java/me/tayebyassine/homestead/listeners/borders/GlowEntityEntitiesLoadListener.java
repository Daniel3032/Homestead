package me.tayebyassine.homestead.listeners.borders;

import me.tayebyassine.homestead.borders.glow.GlowEntityCleanup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * Paper/Folia-only companion to {@link BorderBreakListener}: on these server softwares the
 * entities of a chunk are loaded asynchronously, after {@code ChunkLoadEvent} has already
 * fired, so leftover glowing border entities must be swept once the entity storage itself
 * becomes available.
 */
public final class GlowEntityEntitiesLoadListener implements Listener {

    /**
     * @return {@code true} if the {@code EntitiesLoadEvent} class is available on the server.
     */
    public static boolean isClassFound() {
        try {
            Class.forName("org.bukkit.event.world.EntitiesLoadEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        GlowEntityCleanup.removeTagged(e.getEntities());
    }
}
