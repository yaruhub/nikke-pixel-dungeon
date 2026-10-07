/* Distributed under the GNU General Public License v3 or later. */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rapunzel;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
public class CoreOverdrive extends ArmorAbility {
    { baseChargeUse = 35; }
    @Override public Talent[] talents() { return new Talent[]{Talent.EXTENDED_OUTPUT, Talent.PROTOCOL_CHAIN, Talent.PILGRIMS_MIRACLE, Talent.HEROIC_ENERGY}; }
    @Override public int icon() { return HeroIcon.TRINITY; }
    public static int duration(Hero hero) { return 10 + 2 * hero.pointsInTalent(Talent.EXTENDED_OUTPUT); }
    public static boolean start(Hero hero) {
        HolyTome core = hero.belongings.getItem(HolyTome.class);
        if (core == null || core.cursed || hero.buff(MagicImmune.class) != null) return false;
        core.fillCharge();
        RapunzelTalents.CoreOverdriveState od = Buff.affect(hero, RapunzelTalents.CoreOverdriveState.class);
        od.resetDuration(duration(hero)); od.chain = 0; od.reserve = 0;
        int p = hero.pointsInTalent(Talent.PILGRIMS_MIRACLE);
        if (p == 1 || p >= 3) hero.heal(p == 4 ? 8 : 5);
        if (p >= 2) RapunzelTalents.giveBarrier(hero, p == 4 ? 8 : 5);
        if (p == 4) for (Char mob : Dungeon.level.mobs)
            if (mob.isAlive() && mob.alignment == Char.Alignment.ENEMY && mob.buff(RapunzelTalents.Jamming.class) != null) od.reserve = Math.min(2f, od.reserve + .25f);
        return true;
    }
    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (armor.charge < chargeUse(hero) || !start(hero)) return;
        armor.charge -= chargeUse(hero); Item.updateQuickslot(); Invisibility.dispel(); hero.spendAndNext(Actor.TICK);
    }
}
