package io.github.seamo.oceanIsland;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomWorldPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CustomWorldPlugin has been enabled!");
        // 월드가 로드될 때마다 커스텀 월드 생성 처리
        getServer().getScheduler().runTask(this, this::generateCustomWorld);
    }

    private void generateCustomWorld() {
        World world = getServer().getWorlds().get(0);  // 첫 번째 월드를 가져옵니다.
        if (world == null) return;

        int chunkSize = 16;  // 각 청크의 크기는 16x16 블록
        int range = 15;  // 청크 범위 (-10000 ~ 10000)

        // 일정 범위씩 처리하는 작업 분할
        for (int chunkX = -range; chunkX < range; chunkX += 5) { // 한 번에 10청크씩 처리
            for (int chunkZ = -range; chunkZ < range; chunkZ += 5) {
                final int finalChunkX = chunkX;  // final로 변경
                final int finalChunkZ = chunkZ;  // final로 변경

                // 비동기 작업으로 청크를 처리합니다.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 청크 순회
                        for (int cx = finalChunkX; cx < finalChunkX + 5 && cx < range; cx++) {
                            for (int cz = finalChunkZ; cz < finalChunkZ + 5 && cz < range; cz++) {

                                // 가운데 16청크(4x4 청크) 영역을 제외하고 물로 변환
                                if (Math.abs(cx) <= 1 && Math.abs(cz) <= 1) {
                                    continue;  // 가운데 16청크는 건드리지 않음
                                }

                                // 나머지 청크는 물로 변환
                                for (int x = cx * chunkSize; x < (cx + 1) * chunkSize; x++) {
                                    for (int z = cz * chunkSize; z < (cz + 1) * chunkSize; z++) {
                                        for (int y = -64; y <= 63; y++) {
                                            Block block = world.getBlockAt(x, y, z);

                                            if (y < 0) {
                                                block.setType(Material.BEDROCK);  // -64 이하 부분은 베드락
                                            } else if (y <= 63) {
                                                block.setType(Material.WATER);  // 0~63까지 물로 설정
                                            }
                                        }

                                        // y=64 이상은 공기로 설정
                                        if (world.getBlockAt(x, 64, z).getType() != Material.WATER) {
                                            for (int y = 64; y < 256; y++) {
                                                Block block = world.getBlockAt(x, y, z);
                                                block.setType(Material.AIR);  // 물 위는 공기로 설정
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        getLogger().info("Processed chunk range: " + finalChunkX + " to " + (finalChunkX + 100) + " / " + finalChunkZ + " to " + (finalChunkZ + 100));
                    }
                }.runTask(this);
            }
        }

        getLogger().info("Custom World generation started!");
    }
}
