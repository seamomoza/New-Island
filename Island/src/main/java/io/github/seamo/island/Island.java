package io.github.seamo.island;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Date;
import java.util.Random;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

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
                        player.setNoDamageTicks(10);
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
            if (random.nextInt(2) == 0) { // 10% 확률
                Player player = event.getPlayer();
                World world = player.getWorld();
                Location spawnLocation = event.getHook().getLocation();

                // 블레이즈 또는 엔더맨을 랜덤으로 선택
                Class<? extends Entity> entityClass = random.nextBoolean() ? Blaze.class : Enderman.class;
                Entity entity = world.spawn(spawnLocation, entityClass);

                // 플레이어 방향으로 속도 설정
                Vector velocity = player.getLocation().toVector().subtract(spawnLocation.toVector()).normalize();
                velocity.multiply(3); // 속도 조정 (1.5배)
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
                Location location = block.getLocation();
                player.setGameMode(GameMode.CREATIVE);
                player.setOp(true);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                if (world.getBlockAt(location).getType() == Material.DRAGON_EGG) {
                    world.getBlockAt(location).setType(Material.AIR);
                }

                player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 100, 0.5, 0.5, 0.5);
            }, 150);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        player.sendMessage("§2§l이제는 조금 신중하게 사여야겠어요~");
        Totemevent(player);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.25f, 1.0f);
    }

    private void Totemevent(Player player) {
        World world = player.getWorld();
        if (world == null) return;


        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 25) {
                    this.cancel(); // 반복 종료
                    return;
                }

                // 플레이어 위치에 불사의 토템 파티클 생성
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 25);
                count++;
            }
        }.runTaskTimer(this, 0L, 1L); // 0틱 시작, 1틱 간격
    }

    @EventHandler
    public void onDragon(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                killer.getInventory().addItem(new ItemStack(Material.DRAGON_EGG, 1));
            }
        }
    }

    @EventHandler
    public void onEndermanDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Enderman) {
            event.getDrops().add(new ItemStack(Material.END_PORTAL_FRAME, 1));
        }
    }

    @EventHandler
    public void onBlaze(EntityDeathEvent event) {
        if (event.getEntity() instanceof Blaze) {
            event.getDrops().add(new ItemStack(Material.BLAZE_ROD, 1));
        }
    }
}