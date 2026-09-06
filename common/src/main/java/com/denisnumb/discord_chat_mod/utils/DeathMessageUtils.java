package com.denisnumb.discord_chat_mod.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraft.world.damagesource.FallLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class DeathMessageUtils {
    private static final Style INTENTIONAL_GAME_DESIGN_STYLE = Style.EMPTY
            .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://bugs.mojang.com/browse/MCPE-28723")))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("MCPE-28723")));

    public record DeathMessageComponents(
            Component diedEntity,
            String deathCauseLocaleKey,
            @Nullable Component killerEntity,
            @Nullable Component item
    ) {}

    public static DeathMessageComponents getDeathMessageComponents(List<CombatEntry> entries, LivingEntity diedEntity) {
        if (entries.isEmpty())
            return new DeathMessageComponents(getEntityDisplayName(diedEntity), "death.attack.generic", null, null);

        DamageSource damageSource = entries.getLast().source();
        CombatEntry mostSignificantFall = getMostSignificantFall(entries);
        DeathMessageType deathMessageType = damageSource.type().deathMessageType();

        if (deathMessageType == DeathMessageType.FALL_VARIANTS && mostSignificantFall != null)
            return getFallMessage(mostSignificantFall, damageSource.getEntity(), diedEntity);
        else if (deathMessageType == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
            String base = "death.attack." + damageSource.getMsgId();
            Component intentionalGameDesign
                    = ComponentUtils.wrapInSquareBrackets(Component.translatable(base + ".link")).withStyle(INTENTIONAL_GAME_DESIGN_STYLE);
            return new DeathMessageComponents(getEntityDisplayName(diedEntity), base + "message", intentionalGameDesign, null);
        }

        return getLocalizedDeathMessage(damageSource, diedEntity);
    }

    private static DeathMessageComponents getFallMessage(CombatEntry combatEntry, @Nullable Entity killerEntity, LivingEntity diedEntity) {
        DamageSource damageSource = combatEntry.source();
        if (!damageSource.is(DamageTypeTags.IS_FALL) && !damageSource.is(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL)) {
            Component killerEntityDisplayName = getEntityDisplayName(killerEntity);
            Entity fallCauseEntity = damageSource.getEntity();
            Component fallCauseEntityDisplayName = getEntityDisplayName(fallCauseEntity);
            if (fallCauseEntityDisplayName != null && !fallCauseEntityDisplayName.equals(killerEntityDisplayName))
                return getMessageForAssistedFall(fallCauseEntity, fallCauseEntityDisplayName, "death.fell.assist.item", "death.fell.assist", diedEntity);

            return killerEntityDisplayName != null
                    ? getMessageForAssistedFall(killerEntity, killerEntityDisplayName, "death.fell.finish.item", "death.fell.finish", diedEntity)
                    : new DeathMessageComponents(getEntityDisplayName(diedEntity), "death.fell.killer", null, null);
        }

        FallLocation fallLocation = Objects.requireNonNullElse(combatEntry.fallLocation(), FallLocation.GENERIC);
        return new DeathMessageComponents(getEntityDisplayName(diedEntity), fallLocation.languageKey(), null, null);
    }

    private static DeathMessageComponents getLocalizedDeathMessage(DamageSource source, LivingEntity diedEntity) {
        Component diedEntityName = getEntityDisplayName(diedEntity);
        String attackBase = "death.attack." + source.type().msgId();

        if (source.getEntity() == null && source.getDirectEntity() == null) {
            LivingEntity playerKiller = diedEntity.getKillCredit();
            String byPlayer = attackBase + ".player";

            return playerKiller != null
                    ? new DeathMessageComponents(diedEntityName, byPlayer, getEntityDisplayName(playerKiller), null)
                    : new DeathMessageComponents(diedEntityName, attackBase, null, null);
        } else {
            Component killerEntityName = source.getEntity() == null
                    ? getEntityDisplayName(source.getDirectEntity())
                    : getEntityDisplayName(source.getEntity());

            Entity entity = source.getEntity();
            ItemStack item = (entity instanceof LivingEntity)
                    ? ((LivingEntity) entity).getMainHandItem()
                    : ItemStack.EMPTY;

            return !item.isEmpty() && item.has(DataComponents.CUSTOM_NAME)
                    ? new DeathMessageComponents(diedEntityName, attackBase + ".item", killerEntityName, item.getDisplayName())
                    : new DeathMessageComponents(diedEntityName, attackBase, killerEntityName, null);
        }
    }

    private static DeathMessageComponents getMessageForAssistedFall(Entity killer, Component killerDisplayName, String withItem, String withoutItem, LivingEntity diedEntity) {
        ItemStack itemStack = (killer instanceof LivingEntity livingEntity)
                ? livingEntity.getMainHandItem()
                : ItemStack.EMPTY;

        return !itemStack.isEmpty() && itemStack.has(DataComponents.CUSTOM_NAME)
                ? new DeathMessageComponents(getEntityDisplayName(diedEntity), withItem, killerDisplayName, itemStack.getDisplayName())
                : new DeathMessageComponents(getEntityDisplayName(diedEntity), withoutItem, killerDisplayName, null);
    }

    @Nullable
    private static Component getEntityDisplayName(@Nullable Entity entity) {
        return switch (entity) {
            case null -> null;
            case Player player -> player.getDisplayName();
            case TamableAnimal animal when animal.hasCustomName() -> animal.getDisplayName();
            default -> Component.translatable(entity.getType().getDescriptionId());
        };
    }

    @Nullable
    private static CombatEntry getMostSignificantFall(List<CombatEntry> entries) {
        CombatEntry combatEntry = null;
        CombatEntry combatEntry2 = null;
        float f = 0.0F;
        float g = 0.0F;

        for (int i = 0; i < entries.size(); ++i) {
            CombatEntry combatEntry3 = entries.get(i);
            CombatEntry combatEntry4 = i > 0 ? entries.get(i - 1) : null;
            DamageSource damageSource = combatEntry3.source();
            boolean bl = damageSource.is(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL);
            float h = bl ? Float.MAX_VALUE : combatEntry3.fallDistance();
            if ((damageSource.is(DamageTypeTags.IS_FALL) || bl) && h > 0.0F && (combatEntry == null || h > g)) {
                if (i > 0) {
                    combatEntry = combatEntry4;
                } else {
                    combatEntry = combatEntry3;
                }

                g = h;
            }

            if (combatEntry3.fallLocation() != null && (combatEntry2 == null || combatEntry3.damage() > f)) {
                combatEntry2 = combatEntry3;
                f = combatEntry3.damage();
            }
        }

        if (g > 5.0F && combatEntry != null) {
            return combatEntry;
        } else if (f > 5.0F && combatEntry2 != null) {
            return combatEntry2;
        } else {
            return null;
        }
    }
}
