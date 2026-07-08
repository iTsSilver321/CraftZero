package com.craftzero.entity.ai;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.main.CombatRules;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobAttackTargetGoalTest {
    @Test
    @DisplayName("Melee attack goals should bite assigned living targets without a player")
    void meleeAttackUsesAssignedLivingTarget() {
        World world = new World(17001L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 70.0f, 0.5f);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(zombie, sheep));

            zombie.getAI().setTarget(sheep);
            zombie.getAI().setMoveTarget(sheep.getX(), sheep.getZ());
            world.updateEntities(1.0f / 20.0f);

            assertEquals(sheep.getMaxHealth() - CombatRules.EASY_ZOMBIE_DAMAGE,
                    sheep.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ranged attack goals should shoot assigned living targets without a player")
    void rangedAttackUsesAssignedLivingTarget() {
        World world = new World(17002L);
        try {
            makeFloor(world, -2, 8, -2, 2, 69);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 70.0f, 0.5f);
            Zombie zombie = new Zombie();
            zombie.setPosition(6.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(skeleton, zombie));

            skeleton.getAI().setTarget(zombie);
            skeleton.getAI().setMoveTarget(zombie.getX(), zombie.getZ());
            world.updateEntities(1.0f / 20.0f);
            world.updateEntities(0.0f);

            ArrowEntity arrow = world.getEntities().stream()
                    .filter(ArrowEntity.class::isInstance)
                    .map(ArrowEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(arrow.getMotionX() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    private static void makeFloor(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, y, z, BlockType.STONE, 0);
            }
        }
    }
}
