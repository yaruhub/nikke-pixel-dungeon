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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.EmergencyRepair;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings;
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
        run("immediate food shielding and recovery", RapunzelTalentTest::food);
        run("failed protocols preserve charges and haste eligibility", RapunzelTalentTest::failedProtocols);
        run("target guidance guarantees jamming without legacy illumination", RapunzelTalentTest::guidance);
        run("haste refunds first successful protocol only", RapunzelTalentTest::refunds);
        run("food recovery, refund flags and legacy marker compatibility", RapunzelTalentTest::protocolPersistence);
        run("emergency repair unlock, cost, actual healing and Pure Grace synergy", RapunzelTalentTest::repair);
        run("healing potion no longer grants Saving Hand shielding", RapunzelTalentTest::potion);
        run("curiosity speed and instant equip identification", RapunzelTalentTest::curiosity);
        run("instant wand identification on actual use", RapunzelTalentTest::wand);
        run("first normal attack, repeated targets, and misses", RapunzelTalentTest::firstAttack);
        run("pre-existing jamming versus same-hit pulse", RapunzelTalentTest::amplification);
        run("jamming probability, duration and accuracy", RapunzelTalentTest::jamming);
        run("source-aware artifact resonance excludes the core and never recurses", RapunzelTalentTest::artifacts);
        run("legacy artifact talent API retains other class effects", RapunzelTalentTest::legacyArtifactEffects);
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
        run("directMagic", RapunzelTalentTest::directMagic);
        run("packetDedup", RapunzelTalentTest::packetDedup);
        run("unlimitedCounter", RapunzelTalentTest::unlimitedCounter);
        run("output", RapunzelTalentTest::output);
        run("equipment", RapunzelTalentTest::equipment);
        run("prayer", RapunzelTalentTest::prayer);
        run("purification", RapunzelTalentTest::purification);
        run("echo", RapunzelTalentTest::echo);
        run("descent", RapunzelTalentTest::descent);
        run("rampage", RapunzelTalentTest::rampage);
        run("overdrive", RapunzelTalentTest::overdrive);
        run("chain", RapunzelTalentTest::chain);
        run("miracle", RapunzelTalentTest::miracle);
        run("fourthMigration", RapunzelTalentTest::fourthMigration);
        run("wardGrace", RapunzelTalentTest::wardGrace);
        run("naturalCharging", RapunzelTalentTest::naturalCharging);
        run("collision", RapunzelTalentTest::collision);
        run("jamImmunity", RapunzelTalentTest::jamImmunity);
        run("prayerGrace", RapunzelTalentTest::prayerGrace);
        run("fourthMessages", RapunzelTalentTest::fourthMessages);
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
        level.heaps = new com.watabou.utils.SparseArray<>();
        level.setSize(12, 12);
        Arrays.fill(level.map, Terrain.EMPTY);
        Arrays.fill(level.passable, true);
        Arrays.fill(level.openSpace, true);
        for(int cell=0;cell<level.length();cell++) if(cell/level.width()==0 || cell/level.width()==level.height()-1 || cell%level.width()==0 || cell%level.width()==level.width()-1) {
            level.map[cell]=Terrain.WALL; level.solid[cell]=true; level.losBlocking[cell]=true; level.passable[cell]=false;
        }
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
    private static TestTome core() {
        TestTome core = new TestTome();
        hero.belongings.artifact = core;
        core.activate(hero);
        return core;
    }
    private static void food() {
        TestTome core = core();
        for (int r=1; r<=2; r++) {
            Buff.detach(hero, Barrier.class); Buff.detach(hero, RapunzelTalents.BlessedSacrament.class);
            rank(Talent.SACRAMENT_VEIL,r); rank(Talent.BLESSED_SACRAMENT,r); hero.HP=5;
            Talent.onFoodEaten(hero,100,null);
            eq(1+2*r,hero.shielding(),"instant meal shield"); eq(5,hero.HP,"recovery starts with next tick");
            check(hero.buff(RapunzelTalents.SacramentReady.class)==null,"no preparation required");
            RapunzelTalents.BlessedSacrament recovery=hero.buff(RapunzelTalents.BlessedSacrament.class);
            check(recovery!=null,"recovery begins on food");
            core.setCharge(3); GuidingLight.INSTANCE.onSpellCast(core,hero);
            eq(1+2*r,hero.shielding(),"protocol grants no additional meal shield");
            for(int i=0;i<1+r;i++) recovery.act();
            eq(6+r,hero.HP,"exact recovery duration");
            check(hero.buff(RapunzelTalents.BlessedSacrament.class)==null,"recovery expires");
        }
        Buff.affect(hero,RapunzelTalents.SacramentReady.class);
        Talent.onFoodEaten(hero,100,null);
        check(hero.buff(RapunzelTalents.SacramentReady.class)==null,"obsolete save marker removed");
    }
    private static void potion() {
        com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.initColors();
        for (int r = 1; r <= 2; r++) {
            Buff.detach(hero, Barrier.class); rank(Talent.SAVING_HAND, r);
            hero.HP = 5;
            new PotionOfHealing().apply(hero);
            eq(0, hero.shielding(), "drinking a healing potion grants no Saving Hand shield");
            check(hero.buff(RapunzelTalents.SacramentReady.class) == null, "potion does not prepare a meal");
        }
    }
    private static void failedProtocols() {
        TestTome core = core();
        rank(Talent.SACRAMENT_VEIL, 2); rank(Talent.BLESSED_SACRAMENT, 2); rank(Talent.HERE_I_GO, 2); rank(Talent.SAVING_HAND, 2);
        Talent.onFoodEaten(hero, 100, null); RapunzelTalents.onEnemySeen(hero, enemy(56));
        TestGuidance guidance = new TestGuidance();
        guidance.select(core, hero, null); guidance.select(core, hero, 58);
        core.cursed = true; HolyWard.INSTANCE.onCast(core, hero); core.cursed = false;
        Buff.affect(hero, MagicImmune.class); EmergencyRepair.INSTANCE.onCast(core, hero); Buff.detach(hero, MagicImmune.class);
        hero.HP = hero.HT; EmergencyRepair.INSTANCE.onCast(core, hero);
        eq(3, core.availableCharge(), "failed protocols spend no charges");
        core.setCharge(0); guidance.select(core, hero, 56); EmergencyRepair.INSTANCE.onCast(core, hero);
        eq(0, core.availableCharge(), "unaffordable protocols cannot refund charges");
        check(hero.buff(RapunzelTalents.SacramentReady.class) == null, "no preparation after failures");
        check(hero.buff(RapunzelTalents.BlessedSacrament.class) != null, "food already started healing");
        eq(5, hero.shielding(), "food already granted shield");
        check(!hero.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "failures retain haste refund eligibility");
    }
    private static void guidance() {
        rank(Talent.JAMMER_AMPLIFICATION, 2);
        for (int r = 1; r <= 2; r++) {
            rank(Talent.JAMMING_PULSE, r);
            TestMob target = enemy(56);
            GuidingLight.strike(hero, target, 100);
            eq(100, target.lastDamage, "new guiding jam cannot amplify same hit");
            eq(1 + r, target.buff(RapunzelTalents.Jamming.class).cooldown(), "guidance guarantees 2/3-turn jamming");
            check(target.buff(GuidingLight.Illuminated.class) == null, "Rapunzel guidance has no legacy illumination");
            check(target.buff(GuidingLight.WasIlluminatedTracker.class) == null, "Rapunzel guidance has no legacy tracker");
            check(target.buff(RapunzelTalents.PilgrimAttacked.class) == null, "guidance is not a normal attack");
            GuidingLight.strike(hero, target, 100); eq(120, target.lastDamage, "pre-jammed guidance gets amplification");
        }
        rank(Talent.JAMMER_OPTIMIZATION, 1); TestMob optimized = enemy(57);
        GuidingLight.strike(hero, optimized, 10);
        eq(4, optimized.buff(RapunzelTalents.Jamming.class).cooldown(), "common T3 extends guaranteed jamming");
        rank(Talent.JAMMING_PULSE, 0); TestMob untrained = enemy(58);
        GuidingLight.strike(hero, untrained, 10);
        check(untrained.buff(RapunzelTalents.Jamming.class) == null, "untrained guidance grants no jam");
        check(!GuidingLight.validRapunzelTarget(hero, ally(59)), "allies are invalid guidance targets");
        hero.heroClass = HeroClass.ROGUE;
        TestMob legacy = enemy(60); GuidingLight.strike(hero, legacy, 10);
        check(legacy.buff(GuidingLight.Illuminated.class) != null, "other class retains legacy illumination");
        check(legacy.buff(GuidingLight.WasIlluminatedTracker.class) != null, "other class retains legacy tracker");
        hero.heroClass = HeroClass.CLERIC;
        Bundle saved = new Bundle(); legacy.storeInBundle(saved); TestMob restored = new TestMob(); restored.restoreFromBundle(saved);
        check(restored.buff(GuidingLight.Illuminated.class) == null, "legacy saved illumination is dropped for Rapunzel");
        check(restored.buff(GuidingLight.WasIlluminatedTracker.class) == null, "legacy saved tracker is dropped for Rapunzel");
        GuidingLight.strike(hero, legacy, 10);
        check(legacy.buff(GuidingLight.Illuminated.class) == null, "existing legacy illumination is cleaned on guidance hit");
        TestTome core = core(); rank(Talent.SACRAMENT_VEIL, 2); rank(Talent.BLESSED_SACRAMENT, 2); rank(Talent.HERE_I_GO, 2);
        rank(Talent.JAMMING_PULSE, 2);
        Talent.onFoodEaten(hero, 100, null); TestMob hit = enemy(61); RapunzelTalents.onEnemySeen(hero, hit);
        TestMob lost = enemy(62); lost.HP = 0;
        check(!GuidingLight.completeRapunzelHit(core, hero, lost, 10), "target lost before impact fails protocol");
        eq(3, core.availableCharge(), "lost target consumes no core charge");
        check(hero.buff(RapunzelTalents.SacramentReady.class) == null, "lost target creates no preparation");
        check(!hero.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "lost target retains refund eligibility");
        check(GuidingLight.completeRapunzelHit(core, hero, hit, 10), "valid impact completes actual protocol");
        eq(3, core.availableCharge(), "actual guiding impact spends and refunds one charge");
        eq(5, hero.shielding(), "actual guiding impact triggers prepared shielding");
        check(hero.buff(RapunzelTalents.BlessedSacrament.class) != null, "actual guiding impact starts prepared recovery");
        check(hero.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "actual guiding impact consumes refund eligibility");
    }
    private static void refunds() {
        rank(Talent.HERE_I_GO, 2); TestTome core = core();
        RapunzelTalents.onEnemySeen(hero, enemy(56));
        HolyWeapon.INSTANCE.onSpellCast(core, hero); eq(2, core.availableCharge(), "cost 2 refunds exactly 1");
        HolyWard.INSTANCE.onSpellCast(core, hero); eq(1, core.availableCharge(), "second protocol cannot refund again");
        Buff.detach(hero, RapunzelTalents.PilgrimCooldown.class); Buff.detach(hero, RapunzelTalents.PilgrimHaste.class);
        RapunzelTalents.onEnemySeen(hero, enemy(57)); core.setCharge(3);
        new TestProtocol(.5f).onCast(core, hero); eq(3, core.availableCharge(), "fractional spend refunds no more than .5");
        check(hero.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "fractional refund uses opportunity");
        Buff.detach(hero, RapunzelTalents.PilgrimCooldown.class); Buff.detach(hero, RapunzelTalents.PilgrimHaste.class);
        RapunzelTalents.onEnemySeen(hero, enemy(58)); core.setCharge(3);
        new TestProtocol(0).onCast(core, hero); eq(3, core.availableCharge(), "free protocol grants no charge");
        check(hero.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "first successful free protocol still uses opportunity");
        HolyWard.INSTANCE.onSpellCast(core, hero); eq(2, core.availableCharge(), "paid protocol after first free success is not refunded");
        Buff.detach(hero, RapunzelTalents.PilgrimHaste.class);
        core.setCharge(3); HolyWard.INSTANCE.onSpellCast(core, hero); eq(2, core.availableCharge(), "no refund outside haste");
        Buff.detach(hero, RapunzelTalents.PilgrimCooldown.class);
        RapunzelTalents.onEnemySeen(hero, enemy(59)); core.setCharge(3);
        HolyWard.INSTANCE.onSpellCast(core, hero); eq(3, core.availableCharge(), "new haste activation gets new one-time refund");
    }
    private static void protocolPersistence() {
        rank(Talent.SACRAMENT_VEIL, 2); rank(Talent.BLESSED_SACRAMENT, 2); rank(Talent.HERE_I_GO, 2);
        TestTome core = core(); Talent.onFoodEaten(hero, 100, null); RapunzelTalents.onEnemySeen(hero, enemy(56));
        Bundle saved = new Bundle(); hero.storeInBundle(saved);
        TestHero restored = new TestHero(); Dungeon.hero = restored; restored.restoreFromBundle(saved);
        check(restored.buff(RapunzelTalents.SacramentReady.class) == null, "no pending preparation saved");
        check(!restored.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "unused refund survives save");
        HolyTome restoredCore = restored.belongings.getItem(HolyTome.class);
        GuidingLight.INSTANCE.onSpellCast(restoredCore, restored);
        eq(5, restored.shielding(), "restored preparation triggers shielding");
        eq(3, restoredCore.availableCharge(), "restored haste refunds first successful use");
        check(restored.buff(RapunzelTalents.BlessedSacrament.class) != null, "restored preparation starts healing");
        Bundle used = new Bundle(); restored.storeInBundle(used);
        TestHero usedRestored = new TestHero(); Dungeon.hero = usedRestored; usedRestored.restoreFromBundle(used);
        check(usedRestored.buff(RapunzelTalents.SacramentReady.class) == null, "consumed preparation remains consumed");
        check(usedRestored.buff(RapunzelTalents.PilgrimHaste.class).refundUsed(), "used refund survives save");
        HolyTome usedCore = usedRestored.belongings.getItem(HolyTome.class);
        GuidingLight.INSTANCE.onSpellCast(usedCore, usedRestored); eq(2, usedCore.availableCharge(), "reload cannot repeat refund");
        // Old saves have no refund flag; old Cleric's pending meal is migrated.
        Dungeon.hero = hero;
        Buff.detach(hero, RapunzelTalents.SacramentReady.class);
        Buff.affect(hero, Talent.SatiatedSpellsTracker.class);
        Bundle legacy = new Bundle(); hero.storeInBundle(legacy);
        TestHero legacyRestored = new TestHero(); Dungeon.hero = legacyRestored; legacyRestored.restoreFromBundle(legacy);
        check(legacyRestored.buff(RapunzelTalents.SacramentReady.class) == null, "legacy pending meal discarded safely");
        check(legacyRestored.buff(Talent.SatiatedSpellsTracker.class) == null, "legacy prepared meal tracker removed");
        Bundle oldHaste = new Bundle(); RapunzelTalents.PilgrimHaste old = new RapunzelTalents.PilgrimHaste(); old.restoreFromBundle(oldHaste);
        check(!old.refundUsed(), "old haste without flag defaults to unused");
    }
    private static void repair() {
        TestTome core = core(); hero.lvl = 7;
        check(!ClericSpell.getSpellList(hero, 2).contains(EmergencyRepair.INSTANCE), "untrained recovery not in T2 list");
        for (int r = 1; r <= 2; r++) {
            rank(Talent.SAVING_HAND, r); hero.HP = 5; core.setCharge(3);
            check(ClericSpell.getSpellList(hero, 2).contains(EmergencyRepair.INSTANCE), "Saving Hand unlocks recovery");
            EmergencyRepair.INSTANCE.onCast(core, hero);
            eq(5 + (r == 1 ? 4 : 6) + hero.lvl/2, hero.HP, "recovery rank formula");
            eq(1, core.availableCharge(), "recovery costs two charges");
            eq(0, hero.shielding(), "Saving Hand itself does not grant shielding");
        }
        subclass(HeroSubClass.PURE_GRACE);
        rank(Talent.GRACE_VEIL, 1); rank(Talent.MERCIFUL_HAND, 2); rank(Talent.GRACE_COUNTERATTACK, 3);
        rank(Talent.SACRAMENT_VEIL, 2); rank(Talent.BLESSED_SACRAMENT, 2);
        hero.lvl = 8; hero.HP = 5; core.setCharge(3); TestMob nearby = ally(56);
        Talent.onFoodEaten(hero, 100, null); EmergencyRepair.INSTANCE.onCast(core, hero);
        eq(15, hero.HP, "recovery goes through normal actual healing");
        eq(15, nearby.HP, "actual recovery forwards 50 percent to ally");
        eq(9, hero.shielding(), "Pure Grace veil plus prepared food shield");
        eq(3, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "food shield, healing, healing shield generate three counter stacks");
        RapunzelTalents.BlessedSacrament buff = hero.buff(RapunzelTalents.BlessedSacrament.class);
        for (int t=0; t<3; t++) buff.act();
        eq(18, hero.HP, "recovery triggers prepared 3-turn healing");
        Buff.detach(hero, Barrier.class); Buff.detach(hero, RapunzelTalents.GraceCounter.class);
        hero.HP = hero.HT-1; core.setCharge(3); EmergencyRepair.INSTANCE.onCast(core, hero);
        eq(hero.HT, hero.HP, "recovery clamps actual healing to missing HP");
        eq(4, hero.shielding(), "only actual healing fires Pure Grace shielding");
        core.setCharge(3); Talent.onFoodEaten(hero, 100, null); EmergencyRepair.INSTANCE.onCast(core, hero);
        eq(3, core.availableCharge(), "full-HP recovery spends no charge");
        check(hero.buff(RapunzelTalents.SacramentReady.class) == null, "full-HP failure creates no preparation");
        core.setQuickSpell(EmergencyRepair.INSTANCE);
        Bundle saved = new Bundle(); core.storeInBundle(saved); HolyTome restored = new HolyTome(); restored.restoreFromBundle(saved);
        Bundle after = new Bundle(); restored.storeInBundle(after);
        check(after.getClass("quick_cls") == EmergencyRepair.class, "new recovery quick protocol survives save");
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
        check(missed.buff(RapunzelTalents.PilgrimAttacked.class) == null, "miss keeps first-hit opportunity");
        missed.evasion = 0; hero.attack(missed); eq(14, missed.lastDamage, "first successful hit after miss adds four");
        hero.attack(missed); eq(10, missed.lastDamage, "successful hit consumes opportunity exactly once");
        TestMob blocked = enemy(58); blocked.rejectDamage = true;
        hero.attack(blocked);
        check(blocked.buff(RapunzelTalents.PilgrimAttacked.class) == null, "negative defenseProc preserves first-hit opportunity");
        blocked.rejectDamage = false; blocked.armor = 100;
        hero.attack(blocked); eq(4, blocked.lastDamage, "zero base damage still enters damage path and adds bonus");
        check(blocked.buff(RapunzelTalents.PilgrimAttacked.class) != null, "armor-blocked hit consumes opportunity");
        hero.attack(blocked); eq(0, blocked.lastDamage, "armor-blocked hit cannot repeat bonus");
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
        TestTome core = core(); TestArtifact artifact = new TestArtifact(); artifact.activate(hero);
        TestToolkit toolkit = new TestToolkit();
        for (int r = 1; r <= 2; r++) {
            artifact.charged = 0; core.accelerated = 0; core.setCharge(3); rank(Talent.ARTIFACT_RESONANCE, r);
            GuidingLight.INSTANCE.onSpellCast(core, hero);
            eq(2, core.availableCharge(), "core cannot recharge itself on use");
            RapunzelTalents.ArtifactResonance buff = hero.buff(RapunzelTalents.ArtifactResonance.class);
            eq(1 + 2*r, buff.left(), "other-artifact recharge duration");
            for (int t = 0; t < 1 + 2*r; t++) buff.act();
            eq(1 + 2*r, artifact.charged, "other artifact receives actual accelerated charge");
            eq(0, core.accelerated, "core excluded from resonance charge ticks");
            eq(2, core.availableCharge(), "ticks do not recharge core");
            buff.act(); eq(1 + 2*r, artifact.charged, "expiration grants no extra charge");
            core.setCharge(0); Talent.onArtifactUsed(hero, artifact);
            eq(.5f*r, core.availableCharge(), "other artifact directly grants .5/1 core charge");
            check(hero.buff(RapunzelTalents.ArtifactResonance.class) == null, "other artifact cannot start resonance recursively");
            core.setCharge(0); Talent.onArtifactUsed(hero);
            eq(0, core.availableCharge(), "unknown source cannot fabricate core charge");
            toolkit.setEnergy(1); toolkit.consumeEnergy(0); eq(0, core.availableCharge(), "zero toolkit energy consumption is not actual artifact use");
            toolkit.setEnergy(0); toolkit.consumeEnergy(1); eq(0, core.availableCharge(), "empty toolkit consumption grants no core charge");
            toolkit.setEnergy(1); toolkit.consumeEnergy(1); eq(.5f*r, core.availableCharge(), "real toolkit energy consumption carries source and recharges core");
            core.setCharge(0);
            core.cursed = true; Talent.onArtifactUsed(hero, artifact); eq(0, core.availableCharge(), "cursed core cannot be recharged"); core.cursed = false;
            Buff.affect(hero, MagicImmune.class); Talent.onArtifactUsed(hero, artifact); eq(0, core.availableCharge(), "magic immunity blocks core recharge"); Buff.detach(hero, MagicImmune.class);
        }
    }
    private static void legacyArtifactEffects() {
        hero.heroClass = HeroClass.ROGUE;
        hero.talents.get(1).put(Talent.ENHANCED_RINGS, 2);
        Talent.onArtifactUsed(hero);
        eq(6, hero.buff(EnhancedRings.class).cooldown(), "old API retains Enhanced Rings");
        Buff.detach(hero, EnhancedRings.class);
        Talent.onArtifactUsed(hero, new TestArtifact());
        eq(6, hero.buff(EnhancedRings.class).cooldown(), "source-aware API retains Enhanced Rings");
        TestTome core = core(); core.setCharge(2);
        Buff.affect(hero, ArtifactRecharge.class).set(1).act();
        eq(1, core.accelerated, "ordinary legacy recharge still includes core");
        eq(2.25f, core.availableCharge(), "ordinary core charge behavior remains unchanged");
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
        eq(2, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "two independent gain events");
        TestMob mob = enemy(56);
        hero.attackProc(mob, 10);
        eq(2, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "direct proc outside a normal attack preserves stacks");
        check(mob.buff(RapunzelTalents.PilgrimAttacked.class) == null, "direct proc outside an attack does not mark first hit");
        hero.belongings.abilityWeapon = new Cudgel(); hero.attack(mob);
        check(hero.buff(RapunzelTalents.GraceCounter.class) != null, "ability does not consume stacks");
        hero.belongings.abilityWeapon = null;
        mob.evasion = Char.INFINITE_EVASION;
        check(!hero.attack(mob), "normal counter attack misses");
        eq(2, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "miss retains both counter stacks");
        mob.evasion = 0; mob.rejectDamage = true;
        hero.attack(mob);
        eq(2, hero.buff(RapunzelTalents.GraceCounter.class).stacks(), "negative defenseProc retains counter stacks");
        mob.rejectDamage = false; hero.attack(mob);
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
        rank(Talent.JAMMER_AMPLIFICATION, 1);
        eq(143, RapunzelTalents.resonanceDamage(hero, target, 100), "Papess overload and rank 1 amplification stack");
        rank(Talent.JAMMER_AMPLIFICATION, 2);
        eq(156, RapunzelTalents.resonanceDamage(hero, target, 100), "Papess overload and rank 2 amplification stack");
        rank(Talent.RESONANT_OVERLOAD, 0);
        eq(120, ShieldRush.impactDamage(hero, target, 100, 1, false), "rush amplification works without overload investment");
        VibratingStaff.strike(hero, target, 100, 1);
        eq(120, target.lastDamage, "staff amplification works without overload investment");
        target.rooted = true; level.solid[57] = false; check(ShieldRush.planKnockback(target, 55).collided, "rooted target cannot be pushed");
        check(ShieldRush.canResolveImpact(hero, target, 55), "valid rush arrival enters impact path");
        hero.pos = 54; check(!ShieldRush.canResolveImpact(hero, target, 55), "trap-redirection prevents successful rush completion");
        hero.pos = 55; target.HP = 0; check(!ShieldRush.canResolveImpact(hero, target, 55), "target lost to trap prevents successful rush completion");
        target.HP = 100; hero.HP = 0; check(!ShieldRush.canResolveImpact(hero, target, 55), "hero death prevents successful rush completion");
    }
    private static void persistence() {
        rank(Talent.HERE_I_GO, 2); TestMob mob = enemy(56);
        hero.attack(mob); RapunzelTalents.onEnemySeen(hero, mob);
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
        Bundle localPatch = new Bundle(); hero.storeInBundle(localPatch);
        Bundle localT1 = new Bundle(); localT1.put("SANCTUARY_MEAL", 2); localPatch.put("talents_tier_1", localT1);
        Bundle localT2 = new Bundle(); localT2.put("BLESSED_MEAL", 1); localT2.put("HAND_OF_SALVATION", 2);
        localPatch.put("talents_tier_2", localT2);
        TestHero localRestored = new TestHero(); Dungeon.hero = localRestored; localRestored.restoreFromBundle(localPatch);
        eq(2, localRestored.pointsInTalent(Talent.SACRAMENT_VEIL), "local sanctuary meal rank retained");
        eq(1, localRestored.pointsInTalent(Talent.BLESSED_SACRAMENT), "local blessed meal rank retained");
        eq(2, localRestored.pointsInTalent(Talent.SAVING_HAND), "local salvation hand rank retained");
        hero.heroClass = HeroClass.ROGUE;
        for (String oldName : new String[]{"SANCTUARY_MEAL", "BLESSED_MEAL", "HAND_OF_SALVATION"}) {
            check(oldName.equals(RapunzelTalents.migrateTalent(hero, oldName)), "local aliases unchanged for other classes");
        }
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
            String[] expected = language == Languages.KOREAN
                    ? new String[]{"표적 유도", "화력 지원", "방호 프로토콜", "응급 복구", "갑니다! 헉, 간다고?"}
                    : new String[]{"Target Guidance", "Fire Support", "Defense Protocol", "Emergency Repair", "Here I Go! Wait, Really?"};
            check(GuidingLight.INSTANCE.name().equals(expected[0]), "guidance protocol display name");
            check(HolyWeapon.INSTANCE.name().equals(expected[1]), "fire support display name");
            check(HolyWard.INSTANCE.name().equals(expected[2]), "defense protocol display name");
            check(EmergencyRepair.INSTANCE.name().equals(expected[3]), "repair display name");
            check(Talent.HERE_I_GO.title().equals(expected[4]), "haste exclamation title");
            check(!EmergencyRepair.INSTANCE.desc().equals(Messages.NO_TEXT_FOUND), "repair description resolves");
            check(!new RapunzelTalents.SacramentReady().desc().equals(Messages.NO_TEXT_FOUND), "preparation description resolves");
            hero.heroClass = HeroClass.ROGUE;
            check(GuidingLight.INSTANCE.name().equals(Messages.get(GuidingLight.INSTANCE, "name")), "other class retains legacy protocol name");
            hero.heroClass = HeroClass.CLERIC;
            check(!VibratingStaff.INSTANCE.desc().equals(Messages.NO_TEXT_FOUND), "localized staff description");
            check(!ShieldRush.INSTANCE.desc().equals(Messages.NO_TEXT_FOUND), "localized rush description");
            check(!HeroSubClass.PURE_GRACE.desc().equals(Messages.NO_TEXT_FOUND), "localized subclass description");
        }
    }

    private static void armor(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability) {
        hero.armorAbility=ability; hero.talents.get(3).clear(); Talent.initArmorTalents(hero);
    }
    private static void directMagic() {
        rank(Talent.JAMMING_PULSE,2); int applied=0;
        for(int i=0;i<200;i++) { TestMob mob=enemy(56); RapunzelTalents.magicDamage(hero,mob,1,new WandOfMagicMissile(),false); if(mob.buff(RapunzelTalents.Jamming.class)!=null) applied++; level.mobs.remove(mob); }
        check(applied>35 && applied<85,"direct magic pulse probability");
        TestMob mob=enemy(56);
        for(int i=0;i<100;i++) mob.damage(0,new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison());
        check(mob.buff(RapunzelTalents.Jamming.class)==null,"DOTs cannot pulse");
        for(int i=0;i<100;i++) mob.damage(0,new Object());
        check(mob.buff(RapunzelTalents.Jamming.class)==null,"environment cannot pulse");
    }
    private static void packetDedup() {
        rank(Talent.JAMMING_PULSE,2); int applied=0;
        for(int i=0;i<200;i++) {
            TestMob mob=enemy(56);
            RapunzelTalents.DirectAttack captured;
            try(RapunzelTalents.DirectAttack event=RapunzelTalents.directAttack(hero,true)) {
                for(int j=0;j<20;j++) {
                    if (j%2==0) mob.damage(0,new WandOfMagicMissile());
                    else RapunzelTalents.magicDamage(hero,mob,0,new WandOfMagicMissile(),false);
                }
                captured = RapunzelTalents.captureDirectAttack(hero);
            }
            RapunzelTalents.resumeDirectAttack(captured, () -> {
                for(int j=0;j<20;j++) mob.damage(0,new WandOfMagicMissile());
            });
            if(mob.buff(RapunzelTalents.Jamming.class)!=null) applied++; level.mobs.remove(mob);
        }
        check(applied>35 && applied<85,"20 packets still yield one 30% roll");
    }
    private static void unlimitedCounter() {
        subclass(HeroSubClass.PURE_GRACE); rank(Talent.GRACE_COUNTERATTACK,3);
        for(int count:new int[]{3,10,21,35}) {
            Buff.detach(hero,RapunzelTalents.GraceCounter.class);
            RapunzelTalents.GraceCounter counter=Buff.affect(hero,RapunzelTalents.GraceCounter.class);
            for(int i=0;i<count;i++) counter.addStack();
            Bundle saved=new Bundle(); counter.storeInBundle(saved);
            RapunzelTalents.GraceCounter restored=new RapunzelTalents.GraceCounter(); restored.restoreFromBundle(saved);
            eq(count,restored.stacks(),"unlimited stack save");
            TestMob mob=enemy(56); mob.evasion=Char.INFINITE_EVASION; hero.attack(mob);
            eq(count,counter.stacks(),"miss preserves all stacks");
            mob.evasion=0; mob.armor=100; hero.attack(mob);
            eq(count*6,mob.lastDamage,"armor zero damage hit consumes every stack");
            check(hero.buff(RapunzelTalents.GraceCounter.class)==null,"all stacks consumed"); level.mobs.remove(mob);
        }
    }
    private static void output() {
        TestTome core=core(); hero.lvl=1;
        eq(0,RapunzelTalents.coreOutput(hero,core),"early output minimum");
        hero.lvl=20; hero.STR=16; core.level(4);
        eq(10,RapunzelTalents.coreOutput(hero,core),"level strength tome output");
        eq(12,GuidingLight.minimumDamage(hero,core),"guidance minimum scales"); eq(18,GuidingLight.maximumDamage(hero,core),"guidance maximum scales");
        Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AdrenalineSurge.class).reset(2,200);
        eq(11,RapunzelTalents.coreOutput(hero,core),"actual STR includes buff bonus");
        Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AdrenalineSurge.class);
        eq(7,RapunzelTalents.fireSupport(hero),"fire support output scaling");
        eq(4,RapunzelTalents.wardReduction(hero),"ward output scaling");
        hero.STR=9; hero.lvl=1; core.level(0);
        eq(0,RapunzelTalents.coreOutput(hero,core),"floor negative strength and clamp");
    }
    public static class CountingEnchant extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment {
        public int calls;
        @Override public int proc(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon,Char attacker,Char defender,int damage) { calls++; return damage+3; }
        @Override public com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing glowing() { return new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing(0xFFFFFF); }
    }
    public static class CountingGlyph extends com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph {
        public int calls;
        @Override public int proc(com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor armor,Char attacker,Char defender,int damage) { calls++; return damage-2; }
        @Override public com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing glowing() { return new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing(0xFFFFFF); }
    }
    private static void equipment() {
        core(); hero.lvl=20; hero.STR=16;
        Cudgel weapon=new Cudgel(); CountingEnchant enchant=new CountingEnchant(); weapon.enchantment=enchant; hero.belongings.weapon=weapon;
        Buff.prolong(hero,HolyWeapon.HolyWepBuff.class,50); TestMob mob=enemy(56);
        eq(13,weapon.proc(hero,mob,10),"original enchant proc retained"); eq(1,enchant.calls,"original enchant called once");
        eq(RapunzelTalents.fireSupport(hero),mob.lastDamage,"additional magic packet scales");
        check(weapon.hasEnchant(CountingEnchant.class,hero),"enchantment lookup preserved");
        ClothArmor gear=new ClothArmor(); CountingGlyph glyph=new CountingGlyph(); gear.glyph=glyph; hero.belongings.armor=gear;
        Buff.prolong(hero,HolyWard.HolyArmBuff.class,50);
        eq(20-2-RapunzelTalents.wardReduction(hero),gear.proc(mob,hero,20),"original glyph plus scaled ward");
        eq(1,glyph.calls,"original glyph called once"); check(gear.hasGlyph(CountingGlyph.class,hero),"glyph lookup preserved");
    }
    private static void prayer() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer()); hero.lvl=10; hero.HT=200;
        TestMob ally=ally(56), far=ally(60), enemy=enemy(57); int enemyHP=enemy.HP;
        hero.HP=10; com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.pray(hero);
        eq(23,hero.HP,"prayer heals self"); eq(23,ally.HP,"prayer heals nearby ally"); eq(10,far.HP,"far ally excluded"); eq(enemyHP,enemy.HP,"enemy excluded"); eq(10,hero.shielding(),"prayer base shield");
        for(int r=1;r<=4;r++) { rank(Talent.OVERFLOWING_GRACE,r); eq(Math.round(13*(1+.2f*r)),com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.healing(hero),"overflow rank"); }
        rank(Talent.PURIFYING_GRACE,4); eq(Math.round(13*2.05f),com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.healing(hero),"additive 105% healing");
    }
    private static void purification() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer());
        for(int r=1;r<=4;r++) {
            rank(Talent.PURIFYING_GRACE,r);
            Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison.class);
            Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class);
            Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class);
            Buff.affect(hero,RapunzelTalents.PilgrimCooldown.class);
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.pray(hero);
            check(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison.class)==null,"poison cleansed");
            check(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class)==null,"weakness cleansed");
            check(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class)==null,"blindness cleansed");
            check(hero.buff(RapunzelTalents.PilgrimCooldown.class)!=null,"system marker preserved");
            if(r>=2) eq(r>=3?20:10,hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity.class).cooldown(),"exact immunity duration");
        }
    }
    private static void echo() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer()); hero.HT=200;
        for(int r=1;r<=4;r++) {
            rank(Talent.PRAYER_ECHO,r); rank(Talent.PURIFYING_GRACE,2); hero.HP=1; Buff.detach(hero,Barrier.class);
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.pray(hero);
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.PrayerEcho echo=hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.PrayerEcho.class);
            eq(3,echo.cooldown(),"echo delay exactly 3"); eq(Math.round(8*(.15f+.1f*r)),echo.heal,"echo healing rank"); eq(Math.round(10*(.15f+.1f*r)),echo.shield,"echo shielding rank");
            Bundle saved=new Bundle(); echo.storeInBundle(saved);
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.PrayerEcho restored=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.PrayerEcho(); restored.restoreFromBundle(saved);
            eq(hero.id(),restored.ids[0],"echo target saved"); eq(3,restored.cooldown(),"echo timer saved");
            Buff.detach(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity.class); Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class);
            int before=hero.HP,shield=hero.shielding(); echo.act();
            eq(before+restored.heal,hero.HP,"echo actual heal"); eq(shield+restored.shield,hero.shielding(),"echo actual shield");
            check(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class)!=null,"echo does not cleanse"); check(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity.class)==null,"echo does not regrant immunity");
        }
    }
    private static void descent() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent()); core(); hero.lvl=10;
        TestMob center=enemy(56), other=enemy(57), ally=ally(58), far=enemy(62);
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.descend(hero,56);
        check(center.lastDamage>=Math.round(15*1.5f) && center.lastDamage<=Math.round(19*1.5f),"center 50% bonus");
        check(other.lastDamage>=15 && other.lastDamage<=19,"area magic range"); eq(0,ally.lastDamage,"allies excluded"); eq(0,far.lastDamage,"outside radius excluded");
        eq(4,center.buff(RapunzelTalents.Jamming.class).cooldown(),"base guaranteed jam");
        for(int r=1;r<=4;r++) { rank(Talent.RESONANCE_COLLAPSE,r); eq(Math.round(100*(1+.15f*r)*1.1f),com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.scaledDamage(hero,center,100,false),"collapse prejam extra"); }
        TestMob fresh=enemy(59); eq(160,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.scaledDamage(hero,fresh,100,false),"new jam no retroactive bonus");
    }
    private static void rampage() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent());
        for(int r=1;r<=4;r++) {
            rank(Talent.JAMMER_RAMPAGE,r); TestMob mob=enemy(56);
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.jam(hero,mob);
            eq(5,mob.buff(RapunzelTalents.Jamming.class).cooldown(),"rampage extra turn");
            eq(r>=2?.75f:.8f,mob.buff(RapunzelTalents.Jamming.class).accuracyMultiplier(),"rampage accuracy");
            RapunzelTalents.magicDamage(hero,mob,100,new WandOfMagicMissile(),false); eq(r>=3?110:100,mob.lastDamage,"rampage direct magic vulnerability"); level.mobs.remove(mob);
        }
    }
    private static void overdrive() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive()); TestTome core=core(); core.setCharge(0);
        check(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive.start(hero),"overdrive starts"); eq(3,core.availableCharge(),"fills core"); eq(10,hero.buff(RapunzelTalents.CoreOverdriveState.class).cooldown(),"base duration");
        eq(.5f,core.protocolCost(hero,GuidingLight.INSTANCE),"half float cost");
        new TestProtocol(1).onCast(core,hero); eq(2.5f,core.availableCharge(),"fractional spending"); check(hero.buff(RapunzelTalents.ProtocolHaste.class)!=null,"successful protocol grants haste"); TestMob moving=ally(56); Buff.prolong(moving,RapunzelTalents.ProtocolHaste.class,1); eq(1.5f,moving.speed(),"protocol movement 50%"); eq(1,hero.buff(RapunzelTalents.ProtocolHaste.class).cooldown(),"one-turn movement");
        for(int r=1;r<=4;r++) { rank(Talent.EXTENDED_OUTPUT,r); eq(10+2*r,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive.duration(hero),"extended output rank"); }
        Buff.detach(hero,RapunzelTalents.CoreOverdriveState.class); eq(1,core.protocolCost(hero,GuidingLight.INSTANCE),"normal costs restored");
    }
    private static void chain() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive()); TestTome core=core();
        for(int r=1;r<=4;r++) {
            rank(Talent.PROTOCOL_CHAIN,r); com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive.start(hero);
            new TestProtocol(2).onCast(core,hero);
            eq(Math.max(0,1-.25f*r),core.protocolCost(hero,new TestProtocol(2)),"half then flat reduction");
            float before=core.availableCharge(); new TestProtocol(2).onCast(core,hero);
            eq(before-Math.max(0,1-.25f*r),core.availableCharge(),"chain actual cost");
            eq(.25f*r,hero.buff(RapunzelTalents.CoreOverdriveState.class).chain,"chain rearmed after success");
            core.cursed=true; new TestProtocol(2).onCast(core,hero); core.cursed=false;
            eq(.25f*r,hero.buff(RapunzelTalents.CoreOverdriveState.class).chain,"failure preserves chain");
        }
    }
    private static void miracle() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive()); TestTome core=core(); hero.HT=200;
        for(int r=1;r<=4;r++) {
            rank(Talent.PILGRIMS_MIRACLE,r); hero.HP=10; Buff.detach(hero,Barrier.class);
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive.start(hero);
            eq(10+(r==2?0:r==4?8:5),hero.HP,"miracle healing rank"); eq(r==1?0:r==4?8:5,hero.shielding(),"miracle shielding rank");
        }
        for(int i=0;i<12;i++) Buff.prolong(enemy(56+i),RapunzelTalents.Jamming.class,5);
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive.start(hero);
        RapunzelTalents.CoreOverdriveState od=hero.buff(RapunzelTalents.CoreOverdriveState.class); eq(2,od.reserve,"reserve cap");
        Bundle saved=new Bundle(); od.storeInBundle(saved); RapunzelTalents.CoreOverdriveState restored=new RapunzelTalents.CoreOverdriveState(); restored.restoreFromBundle(saved); eq(2,restored.reserve,"reserve saved");
        new TestProtocol(1).onCast(core,hero); eq(3,core.availableCharge(),"reserve pays first"); eq(1.5f,od.reserve,"reserve consumed by actual half cost");
        new TestProtocol(0).onCast(core,hero); eq(1.5f,od.reserve,"free protocol leaves reserve");
        od.act(); check(hero.buff(RapunzelTalents.CoreOverdriveState.class)==null,"reserve expires with overdrive");
    }
    private static void fourthMigration() {
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] old={new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm(),new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity(),new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany()};
        String[][] names={{"DIVINE_INTERVENTION","JUDGEMENT","FLASH"},{"BODY_FORM","MIND_FORM","SPIRIT_FORM"},{"BEAMING_RAY","LIFE_LINK","STASIS"}};
        for(int i=0;i<old.length;i++) {
            armor(old[i]); for(Talent t:old[i].talents()) rank(t,3);
            Bundle saved=new Bundle();hero.storeInBundle(saved); TestHero restored=new TestHero(); Dungeon.hero=restored;restored.restoreFromBundle(saved);
            check(restored.armorAbility.getClass()==RapunzelTalents.migrateAbility(old[i]).getClass(),"legacy armor ability migrated");
            for(String name:names[i]) eq(3,restored.pointsInTalent(Talent.valueOf(RapunzelTalents.migrateTalent(restored,name))),"legacy tier4 points migrated");
            eq(3,restored.pointsInTalent(Talent.HEROIC_ENERGY),"heroic energy preserved"); Dungeon.hero=hero;
        }
    }

    private static void wardGrace() {
        TestTome core=core(); hero.lvl=20; hero.STR=16; subclass(HeroSubClass.PURE_GRACE); rank(Talent.GRACE_COUNTERATTACK,1);
        HolyWard.applyWard(core,hero);
        eq(RapunzelTalents.coreOutput(hero)/2,hero.shielding(),"Pure Grace ward barrier");
        eq(1,hero.buff(RapunzelTalents.GraceCounter.class).stacks(),"ward actual shielding grants counter stack");
        eq(50,hero.buff(HolyWard.HolyArmBuff.class).cooldown(),"ward duration");
    }
    private static void naturalCharging() {
        TestTome core=core(); core.setCharge(0);
        HolyTome.TomeRecharge charging=hero.buff(HolyTome.TomeRecharge.class); charging.act(); float normal=core.availableCharge();
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive());
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.CoreOverdrive.start(hero); core.setCharge(0); charging.act();
        eq(normal*2,core.availableCharge(),"natural charging exactly doubles");
    }
    private static void collision() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent());
        TestMob mob=enemy(56); level.solid[58]=true;
        rank(Talent.FORCED_VIBRATION,1); eq(0,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.push(hero,mob,55,100),"rank1 one tile no collision");
        rank(Talent.FORCED_VIBRATION,2); eq(0,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.push(hero,mob,55,100),"rank2 two tiles no damage");
        rank(Talent.FORCED_VIBRATION,3); eq(25,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.push(hero,mob,55,100),"wall collision quarter damage");
        rank(Talent.FORCED_VIBRATION,4); eq(25,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.push(hero,mob,55,100),"rank4 collision");
        eq(1,mob.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class).cooldown(),"collision paralysis 1 turn");
        TestMob edge = enemy(58); eq(25,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.push(hero,edge,57,100),"outer boundary wall collision");
        mob.immovable(); eq(0,com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.push(hero,mob,55,100),"immovable excluded");
    }
    private static void jamImmunity() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent()); rank(Talent.JAMMER_RAMPAGE,4);
        TestMob ordinary=enemy(56); ordinary.jamImmune();
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.jam(hero,ordinary);
        eq(2,ordinary.buff(RapunzelTalents.Jamming.class).cooldown(),"ordinary immunity minimum jam");
        TestMob boss=enemy(57); boss.jamImmune(); boss.boss();
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.jam(hero,boss);
        check(boss.buff(RapunzelTalents.Jamming.class)==null,"protected boss immunity retained");
        TestMob resistant=enemy(58); resistant.jamResistant=true;
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.PapessDescent.jam(hero,resistant);
        eq(2,resistant.buff(RapunzelTalents.Jamming.class).cooldown(),"zero resistance duration minimum 2");
        Bundle saved=new Bundle(); ordinary.storeInBundle(saved); TestMob restored=new TestMob();restored.restoreFromBundle(saved);
        check(restored.buff(RapunzelTalents.Jamming.class).magicVulnerability,"rampage vulnerability saved"); check(restored.buff(RapunzelTalents.Jamming.class).rampageAccuracy,"rampage accuracy saved");
    }
    private static void prayerGrace() {
        armor(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer()); subclass(HeroSubClass.PURE_GRACE);
        rank(Talent.GRACE_VEIL,1); rank(Talent.GRACE_COUNTERATTACK,1); rank(Talent.PURIFYING_GRACE,4);rank(Talent.OVERFLOWING_GRACE,4);
        hero.HT=200;hero.HP=1;
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel.SaintPrayer.pray(hero);
        eq(17,hero.HP,"one increased prayer healing event"); eq(14,hero.shielding(),"prayer plus grace veil");
        eq(3,hero.buff(RapunzelTalents.GraceCounter.class).stacks(),"one heal, veil shield, prayer shield");
    }
    private static void fourthMessages() {
        for(Languages lang:new Languages[]{Languages.ENGLISH,Languages.KOREAN}) {
            Messages.setup(lang);
            for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability:HeroClass.CLERIC.armorAbilities()) {
                armor(ability);check(!ability.name().equals(Messages.NO_TEXT_FOUND),"armor name exists");check(!ability.desc().contains(Messages.NO_TEXT_FOUND),"armor description exists");
                for(Talent t:ability.talents()) {check(!t.title().equals(Messages.NO_TEXT_FOUND),"tier4 title exists");check(!t.desc().contains(Messages.NO_TEXT_FOUND),"tier4 desc exists");}
            }
            check(Talent.HERE_I_GO.title().contains(lang==Languages.KOREAN?"갑니다!":"Here"),"haste title punctuation remains");
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
        public void immovable() { properties.add(Char.Property.IMMOVABLE); }
        public void boss() { properties.add(Char.Property.BOSS); }
        public void jamImmune() { immunities.add(RapunzelTalents.Jamming.class); }
        public boolean jamResistant;
        @Override public float resist(Class effect) { return jamResistant && effect == RapunzelTalents.Jamming.class ? 0f : super.resist(effect); }
        public int lastDamage;
        public int evasion;
        public int armor;
        public boolean rejectDamage;
        public TestMob() { HP = HT = 100; alignment = Alignment.ENEMY; }
        @Override public int defenseSkill(Char enemy) { return evasion; }
        @Override public int defenseProc(Char enemy, int damage) { return rejectDamage ? -1 : damage; }
        @Override public void damage(int damage, Object source) { damage = RapunzelTalents.directPacket(this, damage, source); lastDamage = damage; HP = Math.max(1, HP - damage); }
        @Override public int drRoll() { return armor; }
    }
    public static class TestLevel extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
    }
    public static class TestProtocol extends ClericSpell {
        private final float cost;
        public TestProtocol(float cost) { this.cost = cost; }
        @Override public float chargeUse(Hero hero) { return cost; }
        @Override public void onCast(HolyTome core, Hero hero) { if (core.canCast(hero, this)) onSpellCast(core, hero); }
    }
    public static class TestGuidance extends GuidingLight {
        public void select(HolyTome core, Hero hero, Integer cell) { onTargetSelected(core, hero, cell); }
    }
    public static class TestToolkit extends AlchemistsToolkit {
        public void setEnergy(int amount) { charge = amount; }
    }
    public static class TestTome extends HolyTome {
        public float accelerated;
        public TestTome() { exp = Integer.MIN_VALUE; }
        public void setCharge(float amount) { charge = (int)amount; partialCharge = amount-charge; }
        @Override public void charge(Hero hero, float amount) { accelerated += amount; super.charge(hero, amount); }
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
