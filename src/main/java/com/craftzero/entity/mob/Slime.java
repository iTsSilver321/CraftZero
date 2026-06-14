package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;

/**
 * Release-style slime with size-based health/damage and split behavior.
 */
public class Slime extends Mob {
    private final int size;
    private int jumpDelay;

    public Slime() {
        this(4);
    }

    public Slime(int size) {
        super(0.6f * normalizeSize(size), 0.6f * normalizeSize(size), normalizeSize(size) * normalizeSize(size));
        this.size = normalizeSize(size);
        this.definition = MobDefinition.SLIME;
        this.hostile = true;
        this.burnsInSunlight = false;
        this.moveSpeed = 0.16f + this.size * 0.015f;
        this.experienceValue = this.size;
        this.jumpDelay = 10 + random.nextInt(20);
    }

    private static int normalizeSize(int size) {
        if (size <= 1) {
            return 1;
        }
        if (size <= 2) {
            return 2;
        }
        return 4;
    }

    @Override
    public void tick() {
        if (dead) {
            super.tick();
            return;
        }
        Player player = world != null ? world.getPlayer() : null;
        if (player != null && !player.isCreative() && player.getDifficulty().allowsHostileSpawns()) {
            lookAt(player.getPosition().x, player.getPosition().y + 1.0f, player.getPosition().z);
            float dx = player.getPosition().x - x;
            float dz = player.getPosition().z - z;
            float distSq = dx * dx + dz * dz;
            if (distSq < 16.0f * 16.0f && onGround && jumpDelay <= 0) {
                targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                forwardSpeed = 1.0f;
                motionY = 0.42f;
                jumpDelay = 6 + random.nextInt(14);
            }
            if (distSq <= 0.6f * size * 0.6f * size && size > 1 && canAttack()) {
                performAttack();
                player.hurt(size, DamageSource.entity(DamageSource.Type.MOB_MELEE, this,
                        CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                        CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK));
            }
        }
        if (onGround && jumpDelay-- <= 0) {
            targetYaw = random.nextFloat() * 360.0f;
            forwardSpeed = 0.8f;
            motionY = 0.42f;
            jumpDelay = 10 + random.nextInt(20);
        }
        updateAnimation();
        super.tick();
    }

    @Override
    protected void onDeath() {
        if (world != null && size > 1) {
            int children = 2 + random.nextInt(3);
            int childSize = size / 2;
            for (int i = 0; i < children; i++) {
                Slime child = createChild(childSize);
                float ox = ((i % 2) - 0.5f) * size * 0.25f;
                float oz = ((i / 2) - 0.5f) * size * 0.25f;
                child.setPosition(x + ox, y + 0.2f, z + oz);
                child.setYaw(random.nextFloat() * 360.0f);
                world.spawnEntity(child);
            }
        }
        super.onDeath();
    }

    protected Slime createChild(int childSize) {
        return new Slime(childSize);
    }

    @Override
    public void dropLoot() {
        if (size == 1) {
            dropItems(ItemType.SLIMEBALL, 0, 2);
        }
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/slime.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SLIME;
    }

    public int getSize() {
        return size;
    }
}
