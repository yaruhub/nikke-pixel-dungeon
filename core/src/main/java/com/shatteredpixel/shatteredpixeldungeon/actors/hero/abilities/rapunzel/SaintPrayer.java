/* Distributed under the GNU General Public License v3 or later. */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
public class SaintPrayer extends ArmorAbility {
    { baseChargeUse = 50; }
    @Override public Talent[] talents() { return new Talent[]{Talent.OVERFLOWING_GRACE, Talent.PURIFYING_GRACE, Talent.PRAYER_ECHO, Talent.HEROIC_ENERGY}; }
    @Override public int icon() { return HeroIcon.ASCENDED_FORM; }
    public static int healing(Hero hero) {
        return Math.round((8 + hero.lvl / 2) * (1f + .2f * hero.pointsInTalent(Talent.OVERFLOWING_GRACE)
                + (hero.pointsInTalent(Talent.PURIFYING_GRACE) >= 4 ? .25f : 0f)));
    }
    public static float echoRatio(Hero hero) { return .15f + .1f * hero.pointsInTalent(Talent.PRAYER_ECHO); }
    /** Whitelist standard removable ailments. Never remove trackers, alignment or boss state. */
    public static void cleanse(Char ch) {
        for (Buff buff : ch.buffs()) {
            if (buff instanceof Poison || buff instanceof Burning || buff instanceof Bleeding
                || buff instanceof Ooze || buff instanceof Paralysis || buff instanceof Roots
                || buff instanceof Cripple || buff instanceof Blindness || buff instanceof Vertigo
                || buff instanceof Weakness || buff instanceof Vulnerable || buff instanceof Hex
                || buff instanceof Degrade || buff instanceof Slow || buff instanceof Chill
                || buff instanceof Frost || buff instanceof Charm || buff instanceof Terror
                || buff instanceof Dread || buff instanceof MagicalSleep || buff instanceof Corrosion
                || buff instanceof Amok || buff instanceof Doom || buff instanceof Daze || buff instanceof SoulMark
                || buff instanceof RapunzelTalents.Jamming) buff.detach();
        }
    }
    public static void pray(Hero hero) {
        ArrayList<Char> targets = new ArrayList<>(); targets.add(hero);
        for (Char ally : new ArrayList<Char>(Dungeon.level.mobs))
            if (ally.isAlive() && ally.alignment == Char.Alignment.ALLY && Dungeon.level.distance(hero.pos, ally.pos) <= 3) targets.add(ally);
        int heal = healing(hero), purify = hero.pointsInTalent(Talent.PURIFYING_GRACE);
        for (Char ch : targets) {
            ch.heal(heal); RapunzelTalents.giveBarrier(ch, 10);
            if (purify > 0) cleanse(ch);
            if (purify >= 2) Buff.prolong(ch, BlobImmunity.class, purify >= 3 ? 20f : 10f);
        }
        if (hero.hasTalent(Talent.PRAYER_ECHO)) {
            PrayerEcho echo = new PrayerEcho();
            echo.ids = new int[targets.size()];
            for (int i=0; i<targets.size(); i++) echo.ids[i] = targets.get(i).id();
            echo.heal = Math.round(heal * echoRatio(hero)); echo.shield = Math.round(10 * echoRatio(hero));
            echo.floor = Dungeon.depth; echo.branch = Dungeon.branch;
            echo.attachTo(hero); echo.delay(3f);
        }
    }
    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (armor.charge < chargeUse(hero)) return;
        pray(hero); armor.charge -= chargeUse(hero); Item.updateQuickslot(); Invisibility.dispel(); hero.spendAndNext(Actor.TICK);
    }
    public static class PrayerEcho extends Buff {
        public void delay(float turns) { postpone(turns); }
        public int[] ids = new int[0]; public int heal, shield, floor, branch;
        { type = buffType.POSITIVE; actPriority = HERO_PRIO - 1; }
        @Override public boolean act() {
            if (Dungeon.depth == floor && Dungeon.branch == branch) for (int id : ids) {
                Char ch = Dungeon.hero.id() == id ? Dungeon.hero : null;
                for (Char mob : Dungeon.level.mobs) if (mob.id() == id) ch = mob;
                if (ch != null && ch.isAlive() && (ch == Dungeon.hero || ch.alignment == Char.Alignment.ALLY)) {
                    ch.heal(heal); RapunzelTalents.giveBarrier(ch, shield);
                }
            }
            detach(); return true;
        }
        @Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("targets", ids); b.put("heal", heal); b.put("shield", shield); b.put("floor", floor); b.put("branch", branch); }
        @Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); ids=b.getIntArray("targets"); heal=b.getInt("heal"); shield=b.getInt("shield"); floor=b.getInt("floor"); branch=b.getInt("branch"); }
    }
}
