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
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
public class PapessDescent extends ArmorAbility {
    { baseChargeUse = 50; }
    @Override public Talent[] talents() { return new Talent[]{Talent.RESONANCE_COLLAPSE, Talent.FORCED_VIBRATION, Talent.JAMMER_RAMPAGE, Talent.HEROIC_ENERGY}; }
    @Override public int icon() { return HeroIcon.POWER_OF_MANY; }
    @Override public String targetingPrompt() { return Messages.get(this, "prompt"); }
    @Override public int targetedPos(Char user, int dst) { return dst; }
    public static int minimumDamage(Hero h) { return 8 + h.lvl/2 + RapunzelTalents.coreOutput(h); }
    public static int maximumDamage(Hero h) { return 12 + h.lvl/2 + RapunzelTalents.coreOutput(h); }
    public static int scaledDamage(Hero h, Char enemy, int damage, boolean center) {
        boolean jammed = enemy.buff(RapunzelTalents.Jamming.class) != null;
        int p = h.pointsInTalent(Talent.RESONANCE_COLLAPSE);
        float multiplier = 1 + .15f*p;
        if (p > 0 && jammed) multiplier *= 1.1f;
        if (center) multiplier *= 1.5f;
        return Math.round(damage * multiplier);
    }
    public static void jam(Hero hero, Char enemy) {
        int p = hero.pointsInTalent(Talent.JAMMER_RAMPAGE);
        int duration = 4 + (p >= 1 ? 1 : 0) + (hero.hasTalent(Talent.JAMMER_OPTIMIZATION) ? 1 : 0);
        Buff.prolong(enemy, RapunzelTalents.Jamming.class, duration);
        RapunzelTalents.Jamming jam = enemy.buff(RapunzelTalents.Jamming.class);
        if (jam == null && p >= 4 && !Char.hasProp(enemy, Char.Property.BOSS)) {
            // Bypass ordinary ailment immunity only, never a boss's protected state.
            jam = new RapunzelTalents.Jamming();
            if (!jam.forceAttach(enemy)) jam = null;
            else jam.minimumDuration(2f);
        }
        if (jam != null) { if (p >= 4 && jam.cooldown() < 2f && !Char.hasProp(enemy, Char.Property.BOSS)) jam.minimumDuration(2f); jam.rampageAccuracy = p >= 2; jam.magicVulnerability = p >= 3; }
    }
    public static int push(Hero hero, Char enemy, int center, int damage) {
        int p = hero.pointsInTalent(Talent.FORCED_VIBRATION);
        if (p == 0 || enemy.rooted || Char.hasProp(enemy, Char.Property.IMMOVABLE)) return 0;
        int width = Dungeon.level.width();
        int dx = Integer.compare(enemy.pos % width, center % width), dy = Integer.compare(enemy.pos / width, center / width);
        if (dx == 0 && dy == 0) { dx = Integer.compare(center % width, hero.pos % width); dy = Integer.compare(center / width, hero.pos / width); if (dx==0 && dy==0) dy=1; }
        int step=dx+dy*width, requested=p >= 2 ? 2 : 1;
        if (Char.hasProp(enemy, Char.Property.BOSS)) requested = (requested+1)/2;
        int reachable=0;
        for (int i=1; i<=requested; i++) {
            int cell=enemy.pos+i*step;
            if (cell<0 || cell>=Dungeon.level.length() || Dungeon.level.solid[cell]
                || Actor.findChar(cell)!=null || (Char.hasProp(enemy, Char.Property.LARGE) && !Dungeon.level.openSpace[cell])) break;
            reachable++;
        }
        int blockedCell = enemy.pos + (reachable + 1) * step;
        Char blocker = blockedCell >= 0 && blockedCell < Dungeon.level.length() ? Actor.findChar(blockedCell) : null;
        boolean wall = reachable < requested && blockedCell >= 0 && blockedCell < Dungeon.level.length()
            && (Dungeon.level.solid[blockedCell]
                || Char.hasProp(enemy, Char.Property.LARGE) && !Dungeon.level.openSpace[blockedCell]
                || blocker != null && Char.hasProp(blocker, Char.Property.IMMOVABLE));
        int collision = p>=3 && wall ? Math.round(damage*.25f) : 0;
        if (collision > 0) {
            int before = enemy.HP + enemy.shielding();
            RapunzelTalents.magicDamage(hero, enemy, collision, new Collision(), true);
            if (p >= 4 && enemy.isAlive() && enemy.HP + enemy.shielding() < before)
                Buff.prolong(enemy, Paralysis.class, 1f);
        }
        if (reachable>0 && enemy.isAlive()) WandOfBlastWave.throwChar(enemy, new Ballistica(enemy.pos, enemy.pos+step, Ballistica.MAGIC_BOLT), requested, false, false, new PapessDescent());
        return collision;
    }
    public static class Collision {}
    public static void descend(Hero hero, int center) {
        PapessDescent source = new PapessDescent();
        for (Char enemy : new java.util.ArrayList<Char>(Dungeon.level.mobs)) {
            if (!enemy.isAlive() || enemy.isInvulnerable(PapessDescent.class) || enemy.alignment != Char.Alignment.ENEMY || Dungeon.level.distance(center, enemy.pos)>3) continue;
            int damage = scaledDamage(hero, enemy, Hero.heroDamageIntRange(minimumDamage(hero), maximumDamage(hero)), enemy.pos==center);
            RapunzelTalents.magicDamage(hero, enemy, damage, source, true);
            if (enemy.isAlive()) { push(hero, enemy, center, damage); if (enemy.isAlive()) jam(hero, enemy); }
        }
    }
    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (target==null || !Dungeon.level.insideMap(target) || !hero.fieldOfView[target] || Dungeon.level.distance(hero.pos,target)>6 || armor.charge<chargeUse(hero)) return;
        descend(hero,target); armor.charge-=chargeUse(hero); Item.updateQuickslot(); Invisibility.dispel(); hero.spendAndNext(Actor.TICK);
    }
}
