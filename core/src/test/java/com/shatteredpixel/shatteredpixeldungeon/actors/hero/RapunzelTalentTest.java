/* Distributed under the GNU General Public License v3 or later. */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldRush;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.VibratingStaff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Random;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/** Run with ./gradlew :core:rapunzelTest. Assertions throw even without Java's -ea switch. */
public class RapunzelTalentTest {
    private static int cases;
    private static int assertions;
    private static TestHero hero;
    private static TestLevel level;

    public static void main(String[] args) {
        setupFiles();
        run("class slots and unlock thresholds", RapunzelTalentTest::talentLayout);
        run("earth guardian creation is not healing", RapunzelTalentTest::guardianCreation);
        run("food shields and exact healing ticks", RapunzelTalentTest::food);
        run("healing potion event only", RapunzelTalentTest::potion);
        run("curiosity speed and instant equip identification", RapunzelTalentTest::curiosity);
        run("instant wand identification on actual use", RapunzelTalentTest::wand);
        run("first normal attack, repeated targets, and misses", RapunzelTalentTest::firstAttack);
        run("pre-existing jamming versus same-hit pulse", RapunzelTalentTest::amplification);
        run("jamming probability, duration and accuracy", RapunzelTalentTest::jamming);
        run("artifact recharge duration and amount", RapunzelTalentTest::artifacts);
        run("first sight, shared cooldown, no deferred trigger", RapunzelTalentTest::sight);
        run("curse acquisition, no reroll, rank 3 full ID", RapunzelTalentTest::curses);
        run("actual healing, overheal and healing shield stacks", RapunzelTalentTest::grace);
        run("all counter stacks consumed only by normal attacks", RapunzelTalentTest::counter);
        run("ally transfer percentages, distance and no recursion", RapunzelTalentTest::mercy);
        run("fractional healing persistence", RapunzelTalentTest::fractions);
        run("Papess-only actives and cooldown gates", RapunzelTalentTest::activeGates);
        run("magic shockwave excludes allies and blocked targets", RapunzelTalentTest::shockwave);
        run("rush collision, immovable targets and overload", RapunzelTalentTest::rush);
        run("save markers, cooldowns, stacks and item acquisition", RapunzelTalentTest::persistence);
        run("legacy Cleric talent and subclass migration", RapunzelTalentTest::migration);
        run("preview tiers do not change gameplay unlocks", RapunzelTalentTest::preview);
        run("English and Korean descriptions resolve", RapunzelTalentTest::messages);
        System.out.println("PASS: " + cases + " cases, " + assertions + " assertions");
    }

    private static void run(String name, Runnable test) {
        Actor.clear();
        Random.resetGenerators();
        Random.pushGenerator(1701);
        hero = new TestHero();
        hero.heroClass = HeroClass.CLERIC;
        hero.pos = 55;
        Dungeon.hero = hero;
        level = new TestLevel();
        level.mobs = new java.util.HashSet<>();
        level.setSize(12, 12);
        Arrays.fill(level.map, Terrain.EMPTY);
        Arrays.fill(level.passable, true);
        Arrays.fill(level.openSpace, true);
        hero.fieldOfView = new boolean[level.length()];
        Arrays.fill(hero.fieldOfView, true);
        Dungeon.level = level;
        Dungeon.challenges = 0;
        Talent.initClassTalents(hero);
        test.run();
        cases++;
        System.out.println("PASS: " + name);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void eq(float expected, float actual, String message) {
        check(Math.abs(expected - actual) < 0.001f, message + ": expected " + expected + ", got " + actual);
    }
    private static void rank(Talent talent, int rank) {
        for (java.util.LinkedHashMap<Talent, Integer> tier : hero.talents) {
            if (tier.containsKey(talent)) { tier.put(talent, rank); return; }
        }
        throw new AssertionError("Unavailable talent " + talent);
    }
    private static void subclass(HeroSubClass sub) {
        hero.subClass = sub;
        Talent.initSubclassTalents(hero);
    }
    private static TestMob enemy(int pos) {
        TestMob mob = new TestMob(); mob.pos = pos; level.mobs.add(mob); return mob;
    }
    private static TestMob ally(int pos) {
        TestMob mob = enemy(pos); mob.alignment = Char.Alignment.ALLY; mob.HP = 10; return mob;
    }

    private static void talentLayout() {
        check(Arrays.equals(HeroClass.CLERIC.subClasses(), new HeroSubClass[]{HeroSubClass.PURE_GRACE, HeroSubClass.RAPUNZEL_PAPESS}), "Rapunzel subclasses");
        eq(4, hero.talents.get(0).size(), "T1 count"); eq(5, hero.talents.get(1).size(), "T2 count");
        eq(2, hero.talents.get(2).size(), "common T3 count");
        check(!hero.talents.get(0).containsKey(Talent.SATIATED_SPELLS), "legacy talent removed from selection");
        check(Arrays.equals(Talent.tierLevelThresholds, new int[]{0,2,7,13,21,31}), "unchanged gameplay levels");
    }
    private static void food() {
        for (int r = 1; r <= 2; r++) {
            Buff.detach(hero, Barrier.class);
            rank(Talent.SACRAMENT_VEIL, r); rank(Talent.BLESSED_SACRAMENT, r);
            hero.HP = 5;
            Talent.onFoodEaten(hero, 100, null);
            eq(1 + 2*r, hero.shielding(), "food shield");
            RapunzelTalents.BlessedSacrament buff = hero.buff(RapunzelTalents.BlessedSacrament.class);
            for (int turn = 0; turn < 1+r; turn++) buff.act();
            eq(6 + r, hero.HP, "1 HP for exactly 2/3 turns");
            check(hero.buff(RapunzelTalents.BlessedSacrament.class) == null, "meal healing expires");
        }
    }
    private static void potion() {
        for (int r = 1; r <= 2; r++) {
            Buff.detach(hero, Barrier.class); rank(Talent.SAVING_HAND, r);
            hero.HP = 5; hero.heal(3); eq(0, hero.shielding(), "ordinary healing is not drinking a potion");
            RapunzelTalents.onHealingPotionDrunk(hero); eq(1 + 2*r, hero.shielding(), "potion shield");
        }
    }
    private static void guardianCreation() {
        subclass(HeroSubClass.PURE_GRACE);
        rank(Talent.MERCIFUL_HAND, 3);
        hero.HP = 5;
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian guardian =
                new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian();
        guardian.pos = 56;
        guardian.setInfo(hero, 0, 8);
        eq(8, guardian.HP, "initial guardian HP");
        eq(5, hero.HP, "creation does not reverse heal");
        guardian.setInfo(hero, 0, 4);
        eq(12, guardian.HP, "existing guardian recovery");
        eq(7, hero.HP, "guardian actual recovery reverse transfers");
    }
    private static void curiosity() {
        rank(Talent.IMPURE_CURIOSITY, 1);
        eq(3, Talent.itemIDSpeedFactor(hero, new ClothArmor()), "armor speed");
        eq(3, Talent.itemIDSpeedFactor(hero, new Cudgel()), "weapon speed");
        eq(3, Talent.itemIDSpeedFactor(hero, new Ring()), "ring speed");
        eq(3, Talent.itemIDSpeedFactor(hero, new WandOfMagicMissile()), "wand speed");
        ClothArmor armor = new ClothArmor(); Talent.onItemEquipped(hero, armor);
        check(!armor.isIdentified(), "rank 1 is not instant");
        rank(Talent.IMPURE_CURIOSITY, 2); Talent.onItemEquipped(hero, armor);
        check(armor.isIdentified(), "rank 2 identifies on equip");
    }
    private static void wand() {
        rank(Talent.IMPURE_CURIOSITY, 2);
        TestWand wand = new TestWand(); wand.setUser(hero);
        check(!wand.isIdentified(), "unidentified before use");
        wand.wandUsed(); check(wand.isIdentified(), "wand immediately identifies on use");
    }
    private static void firstAttack() {
        rank(Talent.PILGRIMS_INTUITION, 2);
        TestMob enemy = enemy(56);
        hero.attack(enemy); eq(14, enemy.lastDamage, "first hit adds four");
        hero.attack(enemy); eq(10, enemy.lastDamage, "same enemy no repeat");
        TestMob missed = enemy(57); missed.evasion = Char.INFINITE_EVASION;
        check(!hero.attack(missed), "first attack misses");
        missed.evasion = 0; hero.attack(missed); eq(10, missed.lastDamage, "miss already consumed opportunity");
    }
    private static void amplification() {
        rank(Talent.JAMMING_PULSE, 2); rank(Talent.JAMMER_AMPLIFICATION, 2);
        TestMob fresh = enemy(56);
        // Find a deterministic successful pulse, with every attempt being against a fresh enemy.
        boolean applied = false;
        for (int i = 0; i < 100; i++) {
            Buff.detach(fresh, RapunzelTalents.Jamming.class);
            int damage = Talent.onAttackProc(hero, fresh, 100);
            eq(100, damage, "fresh pulse cannot amplify the same hit");
            if (fresh.buff(RapunzelTalents.Jamming.class) != null) { applied = true; break; }
        }
        check(applied, "pulse was actually exercised");
        eq(120, Talent.onAttackProc(hero, fresh, 100), "pre-existing jam amplified");
        rank(Talent.JAMMER_AMPLIFICATION, 1); eq(110, Talent.onAttackProc(hero, fresh, 100), "rank 1 amplified");
        hero.belongings.abilityWeapon = new Cudgel();
        eq(100, Talent.onAttackProc(hero, fresh, 100), "weapon ability excluded");
    }
    private static void jamming() {
        rank(Talent.JAMMING_PULSE, 1); eq(.2f, RapunzelTalents.pulseChance(hero), "base chance"); eq(2, RapunzelTalents.pulseDuration(hero), "base duration");
        rank(Talent.JAMMING_PULSE, 2); eq(.3f, RapunzelTalents.pulseChance(hero), "rank 2 chance"); eq(3, RapunzelTalents.pulseDuration(hero), "rank 2 duration");
        TestMob mob = enemy(56); RapunzelTalents.Jamming jam = Buff.prolong(mob, RapunzelTalents.Jamming.class, 3);
        eq(.8f, jam.accuracyMultiplier(), "20% penalty");
        rank(Talent.JAMMER_OPTIMIZATION, 1); eq(4, RapunzelTalents.pulseDuration(hero), "extra turn");
        rank(Talent.JAMMER_OPTIMIZATION, 2); eq(.75f, jam.accuracyMultiplier(), "25% penalty");
        rank(Talent.JAMMER_OPTIMIZATION, 3); eq(.4f, RapunzelTalents.pulseChance(hero), "10 percentage points");
        jam.extend(1); eq(4, jam.cooldown(), "extension adds exactly one turn");
    }
    private static void artifacts() {
        TestArtifact artifact = new TestArtifact(); artifact.activate(hero);
        for (int r = 1; r <= 2; r++) {
            artifact.charged = 0; rank(Talent.ARTIFACT_RESONANCE, r);
            Talent.onArtifactUsed(hero);
            RapunzelTalents.ArtifactResonance buff = hero.buff(RapunzelTalents.ArtifactResonance.class);
            eq(1 + 2*r, buff.left(), "recharge duration");
            for (int t = 0; t < 1 + 2*r; t++) buff.act();
            eq(1 + 2*r, artifact.charged, "actual accelerated charges");
            buff.act(); eq(1 + 2*r, artifact.charged, "no extra charge on expiration");
        }
    }
    private static void sight() {
        rank(Talent.HERE_I_GO, 1);
        TestMob first = enemy(56), during = enemy(57), later = enemy(58);
        RapunzelTalents.onEnemySeen(hero, first);
        eq(2, hero.buff(RapunzelTalents.PilgrimHaste.class).cooldown(), "rank 1 haste turns");
        eq(15, hero.buff(RapunzelTalents.PilgrimCooldown.class).cooldown(), "shared cooldown");
        // Hero.speed additionally updates its sprite; test the shared movement multiplier headlessly.
        Buff.prolong(first, RapunzelTalents.PilgrimHaste.class, 2);
        eq(1.5f, first.speed(), "50% movement boost");
        RapunzelTalents.onEnemySeen(hero, during);
        Buff.detach(hero, RapunzelTalents.PilgrimCooldown.class); Buff.detach(hero, RapunzelTalents.PilgrimHaste.class);
        RapunzelTalents.onEnemySeen(hero, during); RapunzelTalents.onEnemySeen(hero, first);
        check(hero.buff(RapunzelTalents.PilgrimHaste.class) == null, "no delayed or repeat trigger");
        rank(Talent.HERE_I_GO, 2); RapunzelTalents.onEnemySeen(hero, later);
        eq(3, hero.buff(RapunzelTalents.PilgrimHaste.class).cooldown(), "rank 2 haste turns");
    }
    private static void curses() {
        rank(Talent.OMINOUS_INTUITION, 1);
        ClothArmor armor = new ClothArmor(); armor.cursed = true;
        Talent.onItemCollected(hero, armor); boolean known = armor.cursedKnown;
        for (int i = 0; i < 20; i++) Talent.onItemCollected(hero, armor);
        check(armor.cursedKnown == known, "no repeated acquisition rolls");
        check(!armor.levelKnown, "rank 1 does not reveal level");
        rank(Talent.OMINOUS_INTUITION, 2); Talent.onItemCollected(hero, armor);
        check(armor.cursedKnown && !armor.levelKnown, "rank 2 curse knowledge only");
        rank(Talent.OMINOUS_INTUITION, 3); Talent.onItemCollected(hero, armor);
        check(armor.isIdentified(), "rank 3 complete curse identification");
    }
    private static void grace() {
        subclass(HeroSubClass.PURE_GRACE); rank(Talent.GRACE_COUNTERATTACK, 1);
        for (int r = 1; r <= 3; r++) {
            Buff.detach(hero, Barrier.class); Buff.detach(hero, RapunzelTalents.GraceCounter.class);
            rank(Talent.GRACE_VEIL, r); hero.HP = hero.HT - 1;
            eq(1, hero.heal(100), "actual heal clamped"); eq(1 + 3*r, hero.shielding(), "healing veil shield");
            eq(2, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "heal and shield grant both stacks");
            Buff.detach(hero, Barrier.class); Buff.detach(hero, RapunzelTalents.GraceCounter.class);
            eq(0, hero.heal(100), "full HP receives no heal"); eq(0, hero.shielding(), "no overhealing shield");
            check(hero.buff(RapunzelTalents.GraceCounter.class) == null, "no overhealing stack");
        }
    }
    private static void counter() {
        subclass(HeroSubClass.PURE_GRACE); rank(Talent.GRACE_COUNTERATTACK, 3);
        Buff.affect(hero, Barrier.class).incShield(4);
        Buff.affect(hero, Barrier.class).setShield(3);
        eq(1, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "no stack for a shield that did not increase");
        hero.HP = 5; hero.heal(1);
        eq(2, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "capped at two stacks");
        TestMob mob = enemy(56);
        hero.belongings.abilityWeapon = new Cudgel(); hero.attack(mob);
        check(hero.buff(RapunzelTalents.GraceCounter.class) != null, "ability does not consume stacks");
        hero.belongings.abilityWeapon = null; hero.attack(mob);
        eq(22, mob.lastDamage, "two stacks each add six"); check(hero.buff(RapunzelTalents.GraceCounter.class) == null, "all consumed");
    }
    private static void mercy() {
        subclass(HeroSubClass.PURE_GRACE); rank(Talent.MERCIFUL_HAND, 1);
        TestMob near = ally(56), far = ally(60);
        hero.HP = 5; hero.heal(4); eq(11, near.HP, "25% outgoing"); eq(10, far.HP, "out of range");
        rank(Talent.MERCIFUL_HAND, 2); hero.heal(4); eq(13, near.HP, "50% outgoing");
        hero.HP = 5; near.heal(4); eq(5, hero.HP, "rank 2 has no return");
        rank(Talent.MERCIFUL_HAND, 3); near.heal(4); eq(7, hero.HP, "rank 3 return half"); eq(21, near.HP, "return healing does not forward again");
        hero.HP = 5; near.HP = near.HT; near.heal(10); eq(5, hero.HP, "ally overheal has no return");
    }
    private static void fractions() {
        subclass(HeroSubClass.PURE_GRACE); rank(Talent.MERCIFUL_HAND, 1);
        TestMob near = ally(56); hero.HP = 1;
        hero.heal(1); hero.heal(1); eq(10, near.HP, "fraction does not round up to free HP");
        Bundle saved = new Bundle(); near.buff(RapunzelTalents.MercyRemainder.class).storeInBundle(saved);
        Buff.detach(near, RapunzelTalents.MercyRemainder.class);
        RapunzelTalents.MercyRemainder restored = new RapunzelTalents.MercyRemainder(); restored.restoreFromBundle(saved); restored.attachTo(near);
        hero.heal(1); hero.heal(1); eq(11, near.HP, "saved fractions accumulate to one real HP");
    }
    private static void activeGates() {
        check(!VibratingStaff.INSTANCE.canCast(hero), "no class active before subclass");
        subclass(HeroSubClass.RAPUNZEL_PAPESS); rank(Talent.VIBRATING_STAFF, 1); rank(Talent.SHIELD_RUSH, 1);
        check(VibratingStaff.INSTANCE.canCast(hero), "staff available"); check(ShieldRush.INSTANCE.canCast(hero), "rush available");
        check(ClericSpell.getSpellList(hero, 3).contains(VibratingStaff.INSTANCE), "active in spell list");
        Buff.prolong(hero, VibratingStaff.StaffCooldown.class, 8); check(!VibratingStaff.INSTANCE.canCast(hero), "staff cooldown");
        check(ShieldRush.INSTANCE.canCast(hero), "independent cooldowns"); hero.rooted = true; check(!ShieldRush.INSTANCE.canCast(hero), "roots block rush");
        check(AntiMagic.RESISTS.contains(VibratingStaff.class), "staff registered as magic");
    }
    private static void shockwave() {
        subclass(HeroSubClass.RAPUNZEL_PAPESS); rank(Talent.RESONANT_OVERLOAD, 3); rank(Talent.JAMMING_PULSE, 2);
        TestMob target = enemy(56), splash = enemy(57), blocked = enemy(80), friendly = ally(68);
        level.solid[68] = true;
        Buff.prolong(target, RapunzelTalents.Jamming.class, 3);
        Buff.prolong(splash, RapunzelTalents.Jamming.class, 3);
        VibratingStaff.strike(hero, target, 10, 3);
        eq(13, target.lastDamage, "jammed primary amplified"); eq(7, splash.lastDamage, "jammed half shockwave amplified");
        eq(0, blocked.lastDamage, "wave does not cross wall"); eq(0, friendly.lastDamage, "wave excludes allies");
        eq(4, target.buff(RapunzelTalents.Jamming.class).cooldown(), "primary jam extended");
        check(target.buff(RapunzelTalents.PilgrimAttacked.class) == null, "spell is not a first normal attack");
        check(blocked.buff(RapunzelTalents.Jamming.class) == null, "spell does not roll pulse");
    }
    private static void rush() {
        subclass(HeroSubClass.RAPUNZEL_PAPESS); rank(Talent.RESONANT_OVERLOAD, 3);
        TestMob target = enemy(56);
        ShieldRush.Knockback push = ShieldRush.planKnockback(target, 55);
        eq(58, push.destination, "push two tiles"); check(!push.collided, "open path has no collision");
        level.solid[57] = true; push = ShieldRush.planKnockback(target, 55);
        eq(56, push.destination, "wall stops displacement"); check(push.collided, "wall collision detected");
        eq(6, ShieldRush.impactDamage(hero, target, 6, 1, true), "rank 1 no collision bonus");
        eq(10, ShieldRush.impactDamage(hero, target, 6, 2, true), "rank 2 collision bonus");
        Buff.prolong(target, RapunzelTalents.Jamming.class, 2);
        eq(13, ShieldRush.impactDamage(hero, target, 6, 3, true), "rank 3 overload");
        eq(3, target.buff(RapunzelTalents.Jamming.class).cooldown(), "rush extends jam");
        target.rooted = true; level.solid[57] = false; check(ShieldRush.planKnockback(target, 55).collided, "rooted target cannot be pushed");
    }
    private static void persistence() {
        rank(Talent.HERE_I_GO, 2); TestMob mob = enemy(56);
        RapunzelTalents.beginAttack(hero, mob); RapunzelTalents.onEnemySeen(hero, mob);
        Bundle saved = new Bundle(); mob.storeInBundle(saved); TestMob restored = new TestMob(); restored.restoreFromBundle(saved);
        check(restored.buff(RapunzelTalents.PilgrimSeen.class) != null, "saved sight marker"); check(restored.buff(RapunzelTalents.PilgrimAttacked.class) != null, "saved attack marker");
        Bundle heroBundle = new Bundle(); hero.storeInBundle(heroBundle);
        TestHero newHero = new TestHero(); Dungeon.hero = newHero; newHero.restoreFromBundle(heroBundle);
        eq(15, newHero.buff(RapunzelTalents.PilgrimCooldown.class).cooldown(), "saved cooldown");
        ClothArmor armor = new ClothArmor(); armor.rapunzelCurseChecked = true; Bundle itemBundle = new Bundle(); armor.storeInBundle(itemBundle);
        ClothArmor newArmor = new ClothArmor(); newArmor.restoreFromBundle(itemBundle); check(newArmor.rapunzelCurseChecked, "saved curse acquisition marker");
    }
    private static void migration() {
        for (HeroSubClass legacy : new HeroSubClass[]{HeroSubClass.PRIEST, HeroSubClass.PALADIN}) {
            Bundle saved = new Bundle(); hero.storeInBundle(saved); saved.put("subClass", legacy);
            Bundle t1 = new Bundle(); t1.put("SATIATED_SPELLS", 2); t1.put("HOLY_INTUITION", 1); saved.put("talents_tier_1", t1);
            Bundle t2 = new Bundle(); t2.put("SUNRAY", 2); saved.put("talents_tier_2", t2);
            Bundle t3 = new Bundle(); t3.put(legacy == HeroSubClass.PRIEST ? "HOLY_LANCE" : "LAY_ON_HANDS", 3); saved.put("talents_tier_3", t3);
            TestHero restored = new TestHero(); Dungeon.hero = restored; restored.restoreFromBundle(saved);
            check(restored.subClass == legacy.rapunzelMigration(), "legacy subclass migrated");
            eq(2, restored.pointsInTalent(Talent.SACRAMENT_VEIL), "T1 ranks retained"); eq(1, restored.pointsInTalent(Talent.IMPURE_CURIOSITY), "intuition rank retained");
            eq(2, restored.pointsInTalent(Talent.JAMMER_AMPLIFICATION), "T2 ranks retained");
            eq(3, restored.pointsInTalent(legacy == HeroSubClass.PRIEST ? Talent.GRACE_VEIL : Talent.VIBRATING_STAFF), "subclass T3 ranks retained");
        }
        hero.heroClass = HeroClass.ROGUE;
        check(RapunzelTalents.migrateTalent(hero, "SATIATED_SPELLS").equals("SATIATED_SPELLS"), "other class metamorph talent unaffected");
    }
    private static void preview() {
        eq(2, TalentsPane.previewTiers(TalentButton.Mode.INFO, 1, true), "class preview exposes T2 without badges");
        eq(1, TalentsPane.previewTiers(TalentButton.Mode.UPGRADE, 1, true), "upgrade mode unchanged");
        eq(1, TalentsPane.previewTiers(TalentButton.Mode.INFO, 1, false), "other info contexts unchanged");
    }
    private static void messages() {
        for (Languages language : new Languages[]{Languages.ENGLISH, Languages.KOREAN}) {
            Messages.setup(language);
            for (java.util.LinkedHashMap<Talent, Integer> tier : hero.talents) for (Talent talent : tier.keySet()) {
                check(!talent.title().equals(Messages.NO_TEXT_FOUND), "localized talent title " + talent);
                check(!talent.desc().equals(Messages.NO_TEXT_FOUND), "localized talent description " + talent);
            }
            check(!VibratingStaff.INSTANCE.desc().equals(Messages.NO_TEXT_FOUND), "localized staff description");
            check(!ShieldRush.INSTANCE.desc().equals(Messages.NO_TEXT_FOUND), "localized rush description");
            check(!HeroSubClass.PURE_GRACE.desc().equals(Messages.NO_TEXT_FOUND), "localized subclass description");
        }
    }

    private static void setupFiles() {
        com.watabou.noosa.Game.version = "rapunzel-test";
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        Preferences prefs = (Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(), new Class[]{Preferences.class}, (proxy, method, args) -> {
            if (method.getName().equals("contains")) return false;
            if (method.getName().startsWith("get") && args != null && args.length == 2) return args[1];
            if (method.getReturnType() == Preferences.class) return proxy;
            return defaultValue(method.getReturnType());
        });
        GameSettings.set(prefs);
        Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(), new Class[]{Application.class}, (proxy, method, args) -> {
            if (method.getName().equals("getPreferences")) return prefs;
            if (method.getName().equals("getType")) return Application.ApplicationType.HeadlessDesktop;
            return defaultValue(method.getReturnType());
        });
        Gdx.files = (Files) Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class}, (proxy, method, args) -> {
            if (method.getReturnType() == FileHandle.class) {
                String name = (String) args[0];
                boolean internal = method.getName().equals("internal") || method.getName().equals("classpath");
                return new FileHandle(new File(internal ? "core/src/main/assets/" + name : name));
            }
            return defaultValue(method.getReturnType());
        });
        FileUtils.setDefaultFileProperties(Files.FileType.Local, "core/build/rapunzel-test-data/");
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }
    public static class TestHero extends Hero {
        @Override public int damageRoll() { return 10; }
        @Override public int attackSkill(Char target) { return Char.INFINITE_ACCURACY; }
    }
    public static class TestMob extends Mob {
        public int lastDamage;
        public int evasion;
        public TestMob() { HP = HT = 100; alignment = Alignment.ENEMY; }
        @Override public int defenseSkill(Char enemy) { return evasion; }
        @Override public int defenseProc(Char enemy, int damage) { return damage; }
        @Override public void damage(int damage, Object source) { lastDamage = damage; HP = Math.max(1, HP - damage); }
        @Override public int drRoll() { return 0; }
    }
    public static class TestLevel extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
    }
    public static class TestArtifact extends Artifact {
        public float charged;
        @Override protected ArtifactBuff passiveBuff() { return new ArtifactBuff(); }
        @Override public void charge(Hero hero, float amount) { charged += amount; }
    }
    public static class TestWand extends WandOfMagicMissile {
        public void setUser(Hero hero) { curUser = hero; }
    }
}
