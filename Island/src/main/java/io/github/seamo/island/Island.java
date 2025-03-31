package io.github.seamo.island;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BossBar;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Random;
import java.util.HashMap;
import java.util.UUID;

public class Island extends JavaPlugin implements Listener {
    private static Island instance;
    private final HashMap<UUID, BukkitRunnable> waterDamageTasks = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public static Island getInstance() {
        return instance;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();  // getPlayer() -> getEntity()로 수정
        event.isCancelled();
        event.setCancelled(true);
        event.getKeepInventory();
        event.setKeepLevel(true);
        event.setKeepInventory(true);
        event.getKeepLevel();
        long oneHourInMillis = 3 * 60 * 1000; //앞쪽 10이 분임 ex{ 10 = 10분을 밀리초로 계산
        Date banExpiry = new Date(System.currentTimeMillis() + oneHourInMillis);  // 현재 시간에 1시간을 더함
        player.ban("§4§l접속이 끊겼습니다!", banExpiry, null);
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlock().getType() == Material.WATER) {
            if (!waterDamageTasks.containsKey(player.getUniqueId())) {
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isDead() || player.getLocation().getBlock().getType() != Material.WATER) {
                            this.cancel();
                            waterDamageTasks.remove(player.getUniqueId());
                            return;
                        }
                        player.damage(1.0);
                    }
                };
                task.runTaskTimer(this, 0, 10);
                waterDamageTasks.put(player.getUniqueId(), task);
            }
        }
    }

    private final Random random = new Random();

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (random.nextInt(4) == 0) { // 10% 확률
                Player player = event.getPlayer();
                World world = player.getWorld();
                Location spawnLocation = event.getHook().getLocation();

                // 블레이즈 또는 엔더맨을 랜덤으로 선택
                Class<? extends Entity> entityClass = random.nextBoolean() ? Blaze.class : Enderman.class;
                Entity entity = world.spawn(spawnLocation, entityClass);

                // 플레이어 방향으로 속도 설정
                Vector velocity = player.getLocation().toVector().subtract(spawnLocation.toVector()).normalize();
                velocity.multiply(1.5); // 속도 조정 (1.5배)
                entity.setVelocity(velocity);
            }
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && event.getVehicle().getType().name().contains("BOAT")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (event.getBlockPlaced().getType() == Material.DRAGON_EGG) {
            World world = player.getWorld();
            Block block = event.getBlock();
            Location loc = block.getLocation();

            // 포탈 파티클 생성 (10회 반복)
            for (int i = 0; i < 20; i++) {
                int delay = i * 5; // 각 반복마다 5틱(0.25초)씩 딜레이 증가
                Bukkit.getScheduler().runTaskLater(Island.getInstance(), () -> {
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 100, 0.5, 0.5, 0.5);
                }, delay);
            }

            // 최종적으로 50틱(2.5초) 후 텔레포트
            Bukkit.getScheduler().runTaskLater(Island.getInstance(), () -> {
                Location targetLocation = new Location(world, -451, 104, 393);
                player.teleport(targetLocation);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 100, 0.5, 0.5, 0.5);
            }, 100);
        }
    }
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEndermanDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Enderman) {
            event.getDrops().add(new ItemStack(Material.END_PORTAL_FRAME, 1));
        }
    }
}