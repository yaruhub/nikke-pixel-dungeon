/* Distributed under the GNU General Public License v3 or later. */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

/** Saving Hand's self-recovery protocol; Char.heal handles all Pure Grace synergy. */
public class EmergencyRepair extends ClericSpell {
    public static final EmergencyRepair INSTANCE = new EmergencyRepair();
    @Override public int icon() { return HeroIcon.LAY_ON_HANDS; }
    @Override public float chargeUse(Hero hero) { return 2; }
    @Override public boolean canCast(Hero hero) {
        return hero.heroClass == HeroClass.CLERIC && hero.hasTalent(Talent.SAVING_HAND)
                && hero.isAlive() && hero.HP < hero.HT;
    }
    public static int healing(Hero hero) {
        return 2 + 2 * hero.pointsInTalent(Talent.SAVING_HAND) + hero.lvl / 2;
    }
    @Override public String desc() {
        return Messages.get(this, "desc", healing(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero));
    }
    @Override public void onCast(HolyTome core, Hero hero) {
        if (!core.canCast(hero, this)) return;
        int actual = hero.heal(healing(hero));
        if (hero.sprite != null) {
            hero.sprite.operate(hero.pos);
            hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(actual), FloatingText.HEALING);
        }
        onSpellCast(core, hero);
        hero.spendAndNext(1f);
    }
}
