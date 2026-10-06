/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2026
 * Distributed under the GNU General Public License v3 or later.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** Rapunzel events are kept separate from the legacy Cleric spell system. */
public final class RapunzelTalents {
    private RapunzelTalents() {}

    public static String migrateTalent(Hero hero, String name) {
        if (hero.heroClass != HeroClass.CLERIC) return name;
        switch (name) {
            case "SANCTUARY_MEAL": return "SACRAMENT_VEIL";
            case "BLESSED_MEAL": return "BLESSED_SACRAMENT";
            case "HAND_OF_SALVATION": return "SAVING_HAND";
            case "SATIATED_SPELLS": return "SACRAMENT_VEIL";
            case "HOLY_INTUITION": return "IMPURE_CURIOSITY";
            case "SEARING_LIGHT": return "JAMMING_PULSE";
            case "SHIELD_OF_LIGHT": return "PILGRIMS_INTUITION";
            case "ENLIGHTENING_MEAL": return "BLESSED_SACRAMENT";
            case "RECALL_INSCRIPTION": return "ARTIFACT_RESONANCE";
            case "SUNRAY": return "JAMMER_AMPLIFICATION";
            case "DIVINE_SENSE": return "HERE_I_GO";
            case "BLESS": return "SAVING_HAND";
            case "CLEANSE": return "JAMMER_OPTIMIZATION";
            case "LIGHT_READING": return "OMINOUS_INTUITION";
            case "HOLY_LANCE": return "GRACE_VEIL";
            case "HALLOWED_GROUND": return "MERCIFUL_HAND";
            case "MNEMONIC_PRAYER": return "GRACE_COUNTERATTACK";
            case "LAY_ON_HANDS": return "VIBRATING_STAFF";
            case "AURA_OF_PROTECTION": return "SHIELD_RUSH";
            case "WALL_OF_LIGHT": return "RESONANT_OVERLOAD";
            default: return name;
        }
    }

    public static boolean isEquipment(Item item) {
        return item instanceof EquipableItem;
    }

    public static void giveBarrier(Char target, int amount) {
        if (amount > 0) Buff.affect(target, Barrier.class).incShield(amount);
    }

    public static void onFoodEaten(Hero hero) {
        int points = hero.pointsInTalent(Talent.SACRAMENT_VEIL);
        if (points > 0) giveBarrier(hero, 1 + 2 * points);
        points = hero.pointsInTalent(Talent.BLESSED_SACRAMENT);
        if (points > 0) Buff.affect(hero, BlessedSacrament.class).reset(1 + points);
    }

    public static void onHealingPotionDrunk(Hero hero) {
        int points = hero.pointsInTalent(Talent.SAVING_HAND);
        if (points > 0) giveBarrier(hero, 1 + 2 * points);
    }

    public static void onArtifactUsed(Hero hero) {
        int points = hero.pointsInTalent(Talent.ARTIFACT_RESONANCE);
        if (points > 0) Buff.affect(hero, ArtifactResonance.class).set(1 + 2 * points);
    }

    // Permanent markers live on the actual enemy, not on a transient visible-enemy list.
    public static class PilgrimAttacked extends Buff {}
    public static class PilgrimSeen extends Buff {}

    /** Called only after a hit enters attackProc, including hits reduced to zero by armor. */
    public static void beginAttackDamage(Hero hero, Char enemy) {
        hero.rapunzelFirstAttackTarget = -1;
        hero.rapunzelCounterDamage = 0;
        if (!hero.rapunzelAttackInProgress || enemy == null || !isNormalAttack(hero) || (enemy.alignment != Char.Alignment.ENEMY
                && !(enemy instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic))) return;
        if (hero.heroClass == HeroClass.CLERIC || hero.hasTalent(Talent.PILGRIMS_INTUITION)) {
            if (enemy.buff(PilgrimAttacked.class) == null) {
                Buff.affect(enemy, PilgrimAttacked.class);
                hero.rapunzelFirstAttackTarget = enemy.id();
            }
        }
        GraceCounter counter = hero.buff(GraceCounter.class);
        if (counter != null) {
            hero.rapunzelCounterDamage = counter.consume() * 2 * hero.pointsInTalent(Talent.GRACE_COUNTERATTACK);
        }
    }

    private static boolean isNormalAttack(Hero hero) {
        return hero.belongings.abilityWeapon == null
                && hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.MonkAbility.UnarmedAbilityTracker.class) == null
                && hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.SmiteTracker.class) == null;
    }

    public static int onAttackProc(Hero hero, Char enemy, int damage) {
        if (!isNormalAttack(hero) || enemy.alignment != Char.Alignment.ENEMY) return damage;
        // Snapshot jamming BEFORE rolling the new pulse: a newly jammed target gets no same-hit bonus.
        boolean wasJammed = enemy.buff(Jamming.class) != null;
        if (wasJammed) damage = Math.round(damage * (1f + 0.1f * hero.pointsInTalent(Talent.JAMMER_AMPLIFICATION)));
        if (hero.rapunzelFirstAttackTarget == enemy.id()) damage += 2 * hero.pointsInTalent(Talent.PILGRIMS_INTUITION);
        damage += hero.rapunzelCounterDamage;
        // One attackProc per attack; also safe against enchantments invoking a secondary proc.
        hero.rapunzelFirstAttackTarget = -1;
        hero.rapunzelCounterDamage = 0;
        int pulse = hero.pointsInTalent(Talent.JAMMING_PULSE);
        if (pulse > 0 && Random.Float() < pulseChance(hero)) {
            Buff.prolong(enemy, Jamming.class, pulseDuration(hero));
        }
        return damage;
    }

    public static float pulseChance(Hero hero) {
        return 0.1f + 0.1f * hero.pointsInTalent(Talent.JAMMING_PULSE)
                + (hero.pointsInTalent(Talent.JAMMER_OPTIMIZATION) >= 3 ? 0.1f : 0f);
    }

    public static int pulseDuration(Hero hero) {
        return 1 + hero.pointsInTalent(Talent.JAMMING_PULSE)
                + (hero.pointsInTalent(Talent.JAMMER_OPTIMIZATION) >= 1 ? 1 : 0);
    }

    public static void onEnemySeen(Hero hero, Char enemy) {
        if (enemy.alignment != Char.Alignment.ENEMY
                || (hero.heroClass != HeroClass.CLERIC && !hero.hasTalent(Talent.HERE_I_GO))
                || enemy.buff(PilgrimSeen.class) != null) return;
        // Mark even at rank zero or during the shared cooldown, never queue a later trigger.
        Buff.affect(enemy, PilgrimSeen.class);
        int points = hero.pointsInTalent(Talent.HERE_I_GO);
        if (points > 0 && hero.buff(PilgrimCooldown.class) == null) {
            Buff.prolong(hero, PilgrimHaste.class, 1 + points);
            Buff.prolong(hero, PilgrimCooldown.class, 15);
        }
    }

    public static void onItemCollected(Hero hero, Item item) {
        int points = hero.pointsInTalent(Talent.OMINOUS_INTUITION);
        if (!isEquipment(item) || item.isIdentified()) return;
        // Dropping and picking up again must not reroll the 50% acquisition check.
        if (!item.rapunzelCurseChecked) {
            item.rapunzelCurseChecked = true;
            if (points >= 2 || (points == 1 && Random.Int(2) == 0)) item.cursedKnown = true;
        } else if (points >= 2) {
            item.cursedKnown = true;
        }
        if (points >= 3 && item.cursed) item.identify();
    }

    /** Receives the actual, clamped HP change; zero and overhealing never fire effects. */
    public static void onActualHealing(Char recipient, int actual, boolean transferred) {
        if (actual <= 0) return;
        Hero hero = Dungeon.hero;
        if (hero == null || hero.subClass != HeroSubClass.PURE_GRACE) return;
        if (recipient == hero) {
            addCounterStack(hero);
            int points = hero.pointsInTalent(Talent.GRACE_VEIL);
            if (points > 0) giveBarrier(hero, 1 + 3 * points);
        }
        // A transferred heal still grants local shields/stacks, but cannot initiate another transfer.
        if (transferred || Dungeon.level == null) return;
        int mercy = hero.pointsInTalent(Talent.MERCIFUL_HAND);
        if (mercy == 0) return;
        if (recipient == hero) {
            float amount = actual * (mercy == 1 ? 0.25f : 0.5f);
            for (Char ally : new java.util.ArrayList<Char>(Dungeon.level.mobs)) {
                if (nearbyAlly(hero, ally)) healTransferred(ally, amount);
            }
        } else if (mercy >= 3 && nearbyAlly(hero, recipient)) {
            healTransferred(hero, actual * 0.5f);
        }
    }

    private static boolean nearbyAlly(Hero hero, Char ally) {
        return ally != hero && ally.isAlive() && ally.alignment == Char.Alignment.ALLY
                && Dungeon.level.distance(hero.pos, ally.pos) <= 2;
    }

    private static void healTransferred(Char recipient, float amount) {
        if (!recipient.isAlive() || recipient.HP >= recipient.HT || amount <= 0) return;
        MercyRemainder remainder = Buff.affect(recipient, MercyRemainder.class);
        int whole = remainder.accumulate(amount);
        int actual = Math.max(0, Math.min(whole, recipient.HT - recipient.HP));
        if (!recipient.isAlive() || actual == 0) return;
        recipient.HP += actual;
        if (recipient.sprite != null) recipient.sprite.showStatusWithIcon(
                com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE, Integer.toString(actual),
                com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
        onActualHealing(recipient, actual, true);
    }

    public static void onShieldGained(Char recipient, int amount) {
        if (amount > 0 && recipient instanceof Hero
                && ((Hero) recipient).subClass == HeroSubClass.PURE_GRACE) addCounterStack((Hero) recipient);
    }

    private static void addCounterStack(Hero hero) {
        if (hero.hasTalent(Talent.GRACE_COUNTERATTACK)) Buff.affect(hero, GraceCounter.class).addStack();
    }

    /** Only the two Papess actives call this. No pulse rolls or physical attack procs. */
    public static int resonanceDamage(Hero hero, Char enemy, int damage) {
        Jamming jam = enemy.buff(Jamming.class);
        int points = hero.pointsInTalent(Talent.RESONANT_OVERLOAD);
        if (jam != null && points > 0) {
            damage = Math.round(damage * (1 + 0.1f * points));
            if (points >= 3) jam.extend(1);
        }
        return damage;
    }

    public static class MercyRemainder extends Buff {
        private float fraction;
        public int accumulate(float amount) {
            fraction += amount;
            int whole = (int) fraction;
            fraction -= whole;
            return whole;
        }
        @Override public void storeInBundle(Bundle bundle) { super.storeInBundle(bundle); bundle.put("fraction", fraction); }
        @Override public void restoreFromBundle(Bundle bundle) { super.restoreFromBundle(bundle); fraction = bundle.getFloat("fraction"); }
    }

    public static class Jamming extends FlavourBuff {
        { type = buffType.NEGATIVE; }
        public float accuracyMultiplier() {
            return Dungeon.hero != null && Dungeon.hero.pointsInTalent(Talent.JAMMER_OPTIMIZATION) >= 2 ? 0.75f : 0.8f;
        }
        public void extend(float turns) { spend(turns); }
        @Override public int icon() { return BuffIndicator.HEX; }
        @Override public void tintIcon(Image icon) { icon.hardlight(0.75f, 0.3f, 1f); }
    }

    public static class PilgrimHaste extends FlavourBuff {
        { type = buffType.POSITIVE; }
        @Override public int icon() { return BuffIndicator.HASTE; }
    }
    public static class PilgrimCooldown extends FlavourBuff {
        @Override public int icon() { return BuffIndicator.TIME; }
    }
    public static class ArtifactResonance extends ArtifactRecharge {}

    public static class BlessedSacrament extends Buff {
        private int turns;
        { type = buffType.POSITIVE; actPriority = HERO_PRIO - 1; }
        public void reset(int turns) { this.turns = turns; postpone(TICK); }
        @Override public boolean act() {
            if (turns <= 0) { detach(); return true; }
            target.heal(1);
            if (--turns == 0) detach(); else spend(TICK);
            return true;
        }
        @Override public int icon() { return BuffIndicator.HEALING; }
        @Override public String iconTextDisplay() { return Integer.toString(turns); }
        @Override public String desc() { return Messages.get(this, "desc", turns); }
        @Override public void storeInBundle(Bundle bundle) { super.storeInBundle(bundle); bundle.put("turns", turns); }
        @Override public void restoreFromBundle(Bundle bundle) { super.restoreFromBundle(bundle); turns = bundle.getInt("turns"); }
    }

    public static class GraceCounter extends Buff {
        private int stacks;
        { type = buffType.POSITIVE; }
        public void addStack() { stacks = Math.min(2, stacks + 1); }
        public int stacks() { return stacks; }
        public int consume() { int result = stacks; detach(); return result; }
        @Override public int icon() { return BuffIndicator.WEAPON; }
        @Override public String iconTextDisplay() { return Integer.toString(stacks); }
        @Override public void storeInBundle(Bundle bundle) { super.storeInBundle(bundle); bundle.put("stacks", stacks); }
        @Override public void restoreFromBundle(Bundle bundle) { super.restoreFromBundle(bundle); stacks = Math.min(2, bundle.getInt("stacks")); }
    }
}
