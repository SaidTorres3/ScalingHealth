/*
 * Scaling Health
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.scalinghealth.event;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.utils.EquipmentTierMap;
import net.silentchaos512.scalinghealth.utils.MobPotionMap;

@Deprecated
public class DifficultyHandler {
    public static final String NBT_ENTITY_DIFFICULTY = ScalingHealth.RESOURCE_PREFIX + "difficulty";
    public static DifficultyHandler INSTANCE = new DifficultyHandler();

    private static int POTION_APPLY_TIME = 10 * 1200;
    private static final String[] POTION_DEFAULTS = {
            "minecraft:strength,30,1",
            "minecraft:speed,10,1",
            "minecraft:speed,50,2",
            "minecraft:fire_resistance,10,1",
            "minecraft:invisibility,25,1",
            "minecraft:resistance,30,1"
    };

    public MobPotionMap potionMap = new MobPotionMap();

    public void initPotionMap() {
        /*
        potionMap.clear();

        String[] lines = Config.INSTANCE.getConfiguration().getStringList("Mob Potions",
                Config.CAT_MOB_POTION, POTION_DEFAULTS,
                "The potion effects that mobs can spawn with. You can add effects from other mods if you"
                        + " want to, or remove existing ones. Each line has 3 values separated by commas: the"
                        + " potion ID, the minimum difficulty (higher = less common), and the level (1 = level I,"
                        + " 2 = level II, etc).");

        for (String line : lines) {
            String[] params = line.split(",");
            if (params.length >= 3) {
                // Ignore extra parameters
                if (params.length > 3) {
                    ScalingHealth.logHelper.warn("Mob potion effects: extra parameters in line: " + line
                            + ". Ignoring extra parameters and processing the first 3.");
                }

                // Parse parameters.
                int index = -1;
                String id = "null";
                Potion potion;
                int minDiff, level;
                try {
                    id = params[++index];
                    potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
                    if (potion == null)
                        throw new NullPointerException();
                    minDiff = Integer.parseInt(params[++index]);
                    level = Integer.parseInt(params[++index]);
                } catch (NumberFormatException ex) {
                    ScalingHealth.logHelper.warn("Mob potion effects: could not parse parameter " + index
                            + " as integer. Ignoring entire line: " + line);
                    continue;
                } catch (NullPointerException ex) {
                    ScalingHealth.logHelper.warn("Mob potion effects: potion \"" + id + "\" does not exist.");
                    continue;
                }

                // Put it in the map if nothing goes wrong!
                potionMap.put(potion, minDiff, level - 1);
            } else {
                ScalingHealth.logHelper.warn("Mob potion effects: malformed line (need 3 comma-separated values): "
                        + line + "Ignoring entire line.");
            }
        }
        */
    }

    /*
    @SubscribeEvent
    public void onMobSpawn(LivingUpdateEvent event) {
        if (!(Config.Difficulty.maxValue <= 0) && !event.getEntity().world.isRemote && event.getEntity() instanceof EntityLiving) {
            EntityLiving entityLiving = (EntityLiving) event.getEntity();

            if (canIncreaseEntityHealth(entityLiving) && !entityBlacklistedFromHealthIncrease(entityLiving)) {
//                ScalingHealth.logHelper.debug("{}, {}", entityLiving.getEntityData().getShort(NBT_ENTITY_DIFFICULTY), entityLiving);

                boolean makeBlight = increaseEntityHealth(entityLiving);
                if (makeBlight && !BlightHandler.isBlight(entityLiving))
                    makeEntityBlight(entityLiving, ScalingHealth.random);
            }
        }
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        EntityLivingBase killed = event.getEntityLiving();
        DamageSource source = event.getSource();

        // Killed by player?
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityDifficultyChangeList.DifficultyChanges changes = Config.Difficulty.DIFFICULTY_PER_KILL_BY_MOB.get(killed);
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            SHPlayerDataHandler.PlayerData data = SHPlayerDataHandler.get(player);
            if (data != null) {
                boolean isBlight = BlightHandler.isBlight(killed);
                float amount = isBlight ? changes.onBlightKill : changes.onStandardKill;
                if (Config.Debug.debugMode) {
                    ScalingHealth.logHelper.info("Killed " + (isBlight ? "Blight " : "") + killed.getName()
                            + ": difficulty " + (amount > 0 ? "+" : "") + amount);
                }
                data.incrementDifficulty(amount, true);
            }
        }
    }

    private boolean increaseEntityHealth(EntityLivingBase entityLiving) {
        if (Config.Difficulty.maxValue <= 0) return false;

        World world = entityLiving.world;
        float difficulty = (float) Config.Difficulty.AREA_DIFFICULTY_MODE.getAreaDifficulty(world, entityLiving.getPosition());
        float originalDifficulty = difficulty;
        float originalMaxHealth = entityLiving.getMaxHealth();
        Random rand = ScalingHealth.random;
        boolean makeBlight = false;
        boolean isHostile = entityLiving instanceof IMob;

        // Lunar phase multipliers?
        if (Config.Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS_ENABLED && world.getWorldTime() % 24000 > 12000) {
            int moonPhase = world.provider.getMoonPhase(world.getWorldTime()) % 8;
            float multi = Config.Difficulty.DIFFICULTY_LUNAR_MULTIPLIERS[moonPhase];
            difficulty *= multi;
        }

        // Make blight?
        if (!entityBlacklistedFromBecomingBlight(entityLiving)) {
            float chance = difficulty / Config.Difficulty.maxValue * Config.Mob.Blight.chanceMultiplier;
            if ((Config.Mob.Blight.blightAlways && Config.Mob.Blight.blightAllList.matches(entityLiving)) || rand.nextFloat() < chance) {
                makeBlight = true;
                difficulty *= Config.Mob.Blight.difficultyMultiplier;
            }
        }

        entityLiving.getEntityData().setShort(NBT_ENTITY_DIFFICULTY, (short) difficulty);

        float totalDifficulty = difficulty;

        float genAddedHealth = difficulty;
        float genAddedDamage = 0;
        float baseMaxHealth = (float) entityLiving
                .getAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue();
        float healthMultiplier = isHostile ? Config.Mob.Health.hostileHealthMultiplier
                : Config.Mob.Health.peacefulHealthMultiplier;

        genAddedHealth *= healthMultiplier;

        if (Config.Difficulty.statsConsumeDifficulty) difficulty -= genAddedHealth;

        if (difficulty > 0) {
            float diffIncrease = 2 * healthMultiplier * difficulty * rand.nextFloat();
            if (Config.Difficulty.statsConsumeDifficulty) difficulty -= diffIncrease;
            genAddedHealth += diffIncrease;
        }

        // Increase attack damage.
        if (difficulty > 0) {
            float diffIncrease = difficulty * rand.nextFloat();
            genAddedDamage = diffIncrease * Config.Mob.damageMultiplier;
            // Clamp the value so it doesn't go over the maximum config.
            if (Config.Mob.maxDamageBoost > 0f)
                genAddedDamage = MathHelper.clamp(genAddedDamage, 0f, Config.Mob.maxDamageBoost);

            // Decrease difficulty based on the damage actually added, instead of diffIncrease.
            if (Config.Difficulty.statsConsumeDifficulty)
                difficulty -= genAddedDamage / Config.Mob.damageMultiplier;
        }

        // Random potion effect
        float potionChance = isHostile ? Config.Mob.hostilePotionChance
                : Config.Mob.passivePotionChance;
        if (difficulty > 0 && rand.nextFloat() < potionChance) {
            MobPotionMap.PotionEntry pot = potionMap.getRandom(rand, (int) difficulty);
            if (pot != null) {
                entityLiving.addPotionEffect(new PotionEffect(pot.potion, POTION_APPLY_TIME));
            }
        }

        // Apply extra health and damage.
        float healthMulti;
        float healthScaleDiff = Math.max(0, baseMaxHealth - 20f);
        switch (Config.Mob.Health.healthScalingMode) {
            case ADD:
                ModifierHandler.setMaxHealth(entityLiving, genAddedHealth + baseMaxHealth, 0);
                break;
            case MULTI:
                healthMulti = genAddedHealth / 20f;
                ModifierHandler.setMaxHealth(entityLiving, healthMulti + baseMaxHealth, 1);
                break;
            case MULTI_HALF:
                healthMulti = genAddedHealth / (20f + healthScaleDiff * 0.5f);
                ModifierHandler.setMaxHealth(entityLiving, healthMulti + baseMaxHealth, 1);
                break;
            case MULTI_QUARTER:
                healthMulti = genAddedHealth / (20f + healthScaleDiff * 0.75f);
                ModifierHandler.setMaxHealth(entityLiving, healthMulti + baseMaxHealth, 1);
                break;
            default:
                ScalingHealth.logHelper.fatal("Unknown mob health scaling mode: "
                        + Config.Mob.Health.healthScalingMode.name());
                break;
        }
        ModifierHandler.addAttackDamage(entityLiving, genAddedDamage, 0);

        // Heal.
        if (entityLiving.getMaxHealth() != originalMaxHealth) {
            entityLiving.setHealth(entityLiving.getMaxHealth());
        }

        if (Config.Debug.debugMode && Config.Debug.logSpawns && originalDifficulty > 0f) {
            BlockPos pos = entityLiving.getPosition();
            String line = "Spawn debug: %s (%d, %d, %d): Difficulty=%.2f, Health +%.2f, Damage +%.2f";
            line = String.format(line, entityLiving.getName(), pos.getX(), pos.getY(), pos.getZ(),
                    totalDifficulty, genAddedHealth, genAddedDamage);
            ScalingHealth.LOGGER.info(line);
        }

        return makeBlight;
    }

    private void makeEntityBlight(EntityLiving entityLiving, Random rand) {
        BlightSpawnEvent event = new BlightSpawnEvent.Pre(entityLiving, entityLiving.world,
                (float) entityLiving.posX, (float) entityLiving.posY, (float) entityLiving.posZ);
        // Someone canceled the "blightification"?
        if (MinecraftForge.EVENT_BUS.post(event)) return;

        BlightHandler.markBlight(entityLiving);
        BlightHandler.spawnBlightFire(entityLiving);

        // ==============
        // Potion Effects
        // ==============

        BlightHandler.applyBlightPotionEffects(entityLiving);

        // ================
        // Random Equipment
        // ================

        // Select a tier (0 to 4)
        final int highestTier = 4;
        final int commonTier = Config.BLIGHT_EQUIPMENT_HIGHEST_COMMON_TIER;
        int tier = rand.nextInt(1 + commonTier);
        for (int j = 0; j < highestTier - commonTier; ++j) {
            if (rand.nextFloat() < Config.BLIGHT_EQUIPMENT_TIER_UP_CHANCE) {
                ++tier;
            }
        }
        tier = MathHelper.clamp(tier, 0, highestTier);

        float pieceChance = Config.BLIGHT_EQUIPMENT_ARMOR_PIECE_CHANCE;

        // Armor slots
        for (EntityEquipmentSlot slot : ORDERED_SLOTS) {
            ItemStack oldEquipment = entityLiving.getItemStackFromSlot(slot);

            if (slot != EntityEquipmentSlot.HEAD && rand.nextFloat() > pieceChance)
                break;

            if (oldEquipment.isEmpty()) {
                ItemStack newEquipment = selectEquipmentForSlot(slot, tier);
                if (!newEquipment.isEmpty()) {
                    entityLiving.setItemStackToSlot(slot, newEquipment);
                }
            }
        }

        // Hand slots
        pieceChance = Config.BLIGHT_EQUIPMENT_HAND_PIECE_CHANCE;
        if (rand.nextFloat() > pieceChance) {
            // Main hand
            ItemStack oldEquipment = entityLiving.getHeldItemMainhand();
            if (oldEquipment.isEmpty()) {
                ItemStack newEquipment = selectEquipmentForSlot(EntityEquipmentSlot.MAINHAND, tier);
                if (!newEquipment.isEmpty()) {
                    entityLiving.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, newEquipment);
                }
            }

            // Off hand (only if we tried to do main hand)
            if (rand.nextFloat() > pieceChance) {
                oldEquipment = entityLiving.getHeldItemOffhand();
                if (oldEquipment.isEmpty()) {
                    ItemStack newEquipment = selectEquipmentForSlot(EntityEquipmentSlot.OFFHAND, tier);
                    if (!newEquipment.isEmpty()) {
                        entityLiving.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, newEquipment);
                    }
                }
            }
        }

        // Add random enchantments
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = entityLiving.getItemStackFromSlot(slot);
            if (!stack.isEmpty() && !stack.isEnchanted())
                EnchantmentHelper.addRandomEnchantment(rand, stack, 30, false);
        }

        // ===============
        // Special Effects
        // ===============

        if (Config.Mob.Blight.superchargeCreepers && entityLiving instanceof EntityCreeper) {
            entityLiving.onStruckByLightning(new EntityLightningBolt(entityLiving.world,
                    entityLiving.posX, entityLiving.posY, entityLiving.posZ, true));
            entityLiving.extinguish();
        }

        // Notify clients
        MessageMarkBlight message = new MessageMarkBlight(entityLiving);
        NetworkHandler.INSTANCE.sendToAllAround(message, new TargetPoint(entityLiving.dimension,
                entityLiving.posX, entityLiving.posY, entityLiving.posZ, 128));

        MinecraftForge.EVENT_BUS.post(new BlightSpawnEvent.Post(entityLiving, entityLiving.world,
                (float) entityLiving.posX, (float) entityLiving.posY, (float) entityLiving.posZ));
    }

    private boolean entityBlacklistedFromHealthIncrease(EntityLivingBase entityLiving) {
        if (entityLiving == null) return true;

        boolean isBoss = !entityLiving.isNonBoss();
        boolean isHostile = entityLiving instanceof IMob;
        boolean isPassive = !isHostile;

        if ((isHostile && (Config.Mob.Health.hostileHealthMultiplier == 0 || !Config.Mob.Health.allowHostile))
                || (isPassive && (Config.Mob.Health.peacefulHealthMultiplier == 0 || !Config.Mob.Health.allowPeaceful))
                || (isBoss && (Config.Mob.Health.hostileHealthMultiplier == 0 || !Config.Mob.Health.allowBoss)))
            return true;

        EntityMatchList blacklist = Config.Mob.Health.mobBlacklist;
        List<Integer> dimBlacklist = Config.Mob.Health.dimensionBlacklist;

        if (blacklist == null || dimBlacklist == null) return false;

        return blacklist.contains(entityLiving) || dimBlacklist.contains(entityLiving.dimension);
    }

    private boolean canIncreaseEntityHealth(EntityLivingBase entityLiving) {
        if (entityLiving == null || !entityLiving.world.getGameRules().getBoolean(ScalingHealth.GAME_RULE_DIFFICULTY))
            return false;

        AttributeModifier modifier = entityLiving.getAttribute(SharedMonsterAttributes.MAX_HEALTH)
                .getModifier(ModifierHandler.MODIFIER_ID_HEALTH);
        // The tickExisted > 1 kinda helps with Lycanites, but checking for a modifier amount of 0 should catch issues with
        // some mobs not receiving health increases.
        // ScalingHealth.logHelper.debug(modifier != null ? modifier.getAmount() : null);
        return entityLiving.ticksExisted > 1 && entityLiving.getEntityData().getShort(NBT_ENTITY_DIFFICULTY) < 1
                && (modifier == null || modifier.getAmount() == 0.0 || Double.isNaN(modifier.getAmount()));
    }

    private boolean entityBlacklistedFromBecomingBlight(EntityLivingBase entityLiving) {
        if (entityLiving == null) return true;

        DimensionConfig config = Config.get(entityLiving);
        // TODO

        EntityMatchList blacklist = Config.Mob.Blight.blacklist;
        boolean isBoss = !entityLiving.isNonBoss();
        boolean isHostile = entityLiving instanceof IMob;
        boolean isPassive = !isHostile;

        return blacklist != null && (blacklist.contains(entityLiving)
                || (isHostile && Config.Mob.Blight.blacklistHostiles)
                || (isPassive && Config.Mob.Blight.blacklistPassives)
                || (isBoss && Config.Mob.Blight.blacklistBosses));
    }

    @SubscribeEvent
    public void onWorldTick(WorldTickEvent event) {
        if (event.world.getGameTime() % 20 == 0) {
            ScalingHealthSavedData data = ScalingHealthSavedData.get(event.world);
            data.difficulty += Config.Difficulty.perSecond;
            data.markDirty();
        }
    }
    */

    // **************************************************************************
    // Equipment
    // **************************************************************************

    private static final EntityEquipmentSlot[] ORDERED_SLOTS = {
            EntityEquipmentSlot.HEAD,
            EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS,
            EntityEquipmentSlot.FEET
    };

    public EquipmentTierMap mapHelmets = new EquipmentTierMap(5, EntityEquipmentSlot.HEAD);
    public EquipmentTierMap mapChestplates = new EquipmentTierMap(5, EntityEquipmentSlot.CHEST);
    public EquipmentTierMap mapLeggings = new EquipmentTierMap(5, EntityEquipmentSlot.LEGS);
    public EquipmentTierMap mapBoots = new EquipmentTierMap(5, EntityEquipmentSlot.FEET);
    public EquipmentTierMap mapMainhands = new EquipmentTierMap(5, EntityEquipmentSlot.MAINHAND);
    public EquipmentTierMap mapOffhands = new EquipmentTierMap(5, EntityEquipmentSlot.OFFHAND);

    public void initDefaultEquipment() {
        /*
        mapHelmets.put(new ItemStack(Items.LEATHER_HELMET), 0);
        mapHelmets.put(new ItemStack(Items.GOLDEN_HELMET), 1);
        mapHelmets.put(new ItemStack(Items.CHAINMAIL_HELMET), 2);
        mapHelmets.put(new ItemStack(Items.IRON_HELMET), 3);
        mapHelmets.put(new ItemStack(Items.DIAMOND_HELMET), 4);

        mapChestplates.put(new ItemStack(Items.LEATHER_CHESTPLATE), 0);
        mapChestplates.put(new ItemStack(Items.GOLDEN_CHESTPLATE), 1);
        mapChestplates.put(new ItemStack(Items.CHAINMAIL_CHESTPLATE), 2);
        mapChestplates.put(new ItemStack(Items.IRON_CHESTPLATE), 3);
        mapChestplates.put(new ItemStack(Items.DIAMOND_CHESTPLATE), 4);

        mapLeggings.put(new ItemStack(Items.LEATHER_LEGGINGS), 0);
        mapLeggings.put(new ItemStack(Items.GOLDEN_LEGGINGS), 1);
        mapLeggings.put(new ItemStack(Items.CHAINMAIL_LEGGINGS), 2);
        mapLeggings.put(new ItemStack(Items.IRON_LEGGINGS), 3);
        mapLeggings.put(new ItemStack(Items.DIAMOND_LEGGINGS), 4);

        mapBoots.put(new ItemStack(Items.LEATHER_BOOTS), 0);
        mapBoots.put(new ItemStack(Items.GOLDEN_BOOTS), 1);
        mapBoots.put(new ItemStack(Items.CHAINMAIL_BOOTS), 2);
        mapBoots.put(new ItemStack(Items.IRON_BOOTS), 3);
        mapBoots.put(new ItemStack(Items.DIAMOND_BOOTS), 4);
        */
    }

    private ItemStack selectEquipmentForSlot(EntityEquipmentSlot slot, int tier) {
        tier = MathHelper.clamp(tier, 0, 4);
        switch (slot) {
            case CHEST:
                return mapChestplates.getRandom(tier);
            case FEET:
                return mapBoots.getRandom(tier);
            case HEAD:
                return mapHelmets.getRandom(tier);
            case LEGS:
                return mapLeggings.getRandom(tier);
            case MAINHAND:
                return mapMainhands.getRandom(tier);
            case OFFHAND:
                return mapOffhands.getRandom(tier);
            default:
                return ItemStack.EMPTY;
        }
    }
}
