package me.yourname.burzhuy.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler adapter that works with both Paper and Folia
 * Automatically detects the server type and uses appropriate scheduler
 */
public class SchedulerAdapter {
    
    private final Plugin plugin;
    private final boolean isFolia;
    private Object globalRegionScheduler;
    private Object asyncScheduler;
    
    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = detectFolia();
        
        if (isFolia) {
            try {
                // Get Folia schedulers
                Class<?> serverClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                Method getGlobalRegionScheduler = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
                Method getAsyncScheduler = Bukkit.getServer().getClass().getMethod("getAsyncScheduler");
                
                globalRegionScheduler = getGlobalRegionScheduler.invoke(Bukkit.getServer());
                asyncScheduler = getAsyncScheduler.invoke(Bukkit.getServer());
                
                plugin.getLogger().info("Running on Folia - using region-based scheduling");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize Folia schedulers!");
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().info("Running on Paper/Spigot - using standard Bukkit scheduler");
        }
    }
    
    /**
     * Detect if running on Folia
     */
    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Run a task (sync on Paper, global region on Folia)
     */
    public void runTask(Runnable task) {
        if (isFolia) {
            runGlobalTask(task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run a delayed task (sync on Paper, global region on Folia)
     */
    public void runTaskLater(Runnable task, long delayTicks) {
        if (isFolia) {
            runGlobalTaskLater(task, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
    
    /**
     * Run a repeating task (sync on Paper, global region on Folia)
     */
    public TaskWrapper runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            return runGlobalTaskTimer(task, delayTicks, periodTicks);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delayTicks, periodTicks);
            return new BukkitTaskWrapper(taskId);
        }
    }
    
    /**
     * Run async task
     */
    public void runTaskAsynchronously(Runnable task) {
        if (isFolia) {
            try {
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Method runMethod = asyncScheduler.getClass().getMethod("runNow", Plugin.class, consumerClass);
                
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(
                    consumerClass.getClassLoader(),
                    new Class[] { consumerClass },
                    (proxy, method, args) -> {
                        task.run();
                        return null;
                    }
                );
                
                runMethod.invoke(asyncScheduler, plugin, consumer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    /**
     * Run task at entity location (Folia-specific)
     */
    public void runAtEntity(Entity entity, Runnable task) {
        if (isFolia) {
            try {
                Method getScheduler = entity.getClass().getMethod("getScheduler");
                Object entityScheduler = getScheduler.invoke(entity);
                
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Method runMethod = entityScheduler.getClass().getMethod("run", Plugin.class, consumerClass, Runnable.class);
                
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(
                    consumerClass.getClassLoader(),
                    new Class[] { consumerClass },
                    (proxy, method, args) -> {
                        task.run();
                        return null;
                    }
                );
                
                runMethod.invoke(entityScheduler, plugin, consumer, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run task at location (Folia-specific)
     */
    public void runAtLocation(Location location, Runnable task) {
        if (isFolia) {
            try {
                Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
                Method getRegionScheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler");
                Object regionScheduler = getRegionScheduler.invoke(Bukkit.getServer());
                
                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Method runMethod = regionSchedulerClass.getMethod("run", Plugin.class, Location.class, consumerClass);
                
                Object consumer = java.lang.reflect.Proxy.newProxyInstance(
                    consumerClass.getClassLoader(),
                    new Class[] { consumerClass },
                    (proxy, method, args) -> {
                        task.run();
                        return null;
                    }
                );
                
                runMethod.invoke(regionScheduler, plugin, location, consumer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    // Folia-specific helper methods
    private void runGlobalTask(Runnable task) {
        try {
            // Folia uses Consumer<ScheduledTask> instead of Runnable
            Class<?> consumerClass = Class.forName("java.util.function.Consumer");
            Method runMethod = globalRegionScheduler.getClass().getMethod("run", Plugin.class, consumerClass);
            
            // Create a lambda that matches Consumer<ScheduledTask>
            Object consumer = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class[] { consumerClass },
                (proxy, method, args) -> {
                    task.run();
                    return null;
                }
            );
            
            runMethod.invoke(globalRegionScheduler, plugin, consumer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void runGlobalTaskLater(Runnable task, long delayTicks) {
        try {
            Class<?> consumerClass = Class.forName("java.util.function.Consumer");
            Method runDelayedMethod = globalRegionScheduler.getClass().getMethod(
                "runDelayed", Plugin.class, consumerClass, long.class
            );
            
            Object consumer = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class[] { consumerClass },
                (proxy, method, args) -> {
                    task.run();
                    return null;
                }
            );
            
            runDelayedMethod.invoke(globalRegionScheduler, plugin, consumer, delayTicks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private TaskWrapper runGlobalTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        try {
            Class<?> consumerClass = Class.forName("java.util.function.Consumer");
            Method runAtFixedRateMethod = globalRegionScheduler.getClass().getMethod(
                "runAtFixedRate", Plugin.class, consumerClass, long.class, long.class
            );
            
            Object consumer = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class[] { consumerClass },
                (proxy, method, args) -> {
                    task.run();
                    return null;
                }
            );
            
            Object scheduledTask = runAtFixedRateMethod.invoke(
                globalRegionScheduler, plugin, consumer, delayTicks, periodTicks
            );
            return new FoliaTaskWrapper(scheduledTask);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean isFolia() {
        return isFolia;
    }
    
    /**
     * Interface for task cancellation that works on both Paper and Folia
     */
    public interface TaskWrapper {
        void cancel();
    }
    
    /**
     * Bukkit task wrapper
     */
    private static class BukkitTaskWrapper implements TaskWrapper {
        private final int taskId;
        
        public BukkitTaskWrapper(int taskId) {
            this.taskId = taskId;
        }
        
        @Override
        public void cancel() {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    
    /**
     * Folia task wrapper
     */
    private static class FoliaTaskWrapper implements TaskWrapper {
        private final Object scheduledTask;
        
        public FoliaTaskWrapper(Object scheduledTask) {
            this.scheduledTask = scheduledTask;
        }
        
        @Override
        public void cancel() {
            try {
                Method cancelMethod = scheduledTask.getClass().getMethod("cancel");
                cancelMethod.setAccessible(true); // Allow access to public methods in package-private classes
                cancelMethod.invoke(scheduledTask);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
