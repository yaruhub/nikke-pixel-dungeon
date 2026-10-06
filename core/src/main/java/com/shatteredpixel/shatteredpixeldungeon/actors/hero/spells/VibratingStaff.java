/* Distributed under the GNU General Public License v3 or later. */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.RapunzelTalents;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/** Cooldown-based Papess magic; it deliberately never calls Hero.attack. */
public class VibratingStaff extends TargetedClericSpell {
    public static final VibratingStaff INSTANCE = new VibratingStaff();
    public static final int RANGE = 6;
    public static final int COOLDOWN = 8;
    @Override public int icon() { return HeroIcon.HOLY_LANCE; }
    @Override public float chargeUse(Hero hero) { return 0; }
    @Override public boolean canCast(Hero hero) {
        return hero.subClass == HeroSubClass.RAPUNZEL_PAPESS && hero.hasTalent(Talent.VIBRATING_STAFF)
                && hero.buff(StaffCooldown.class) == null;
    }
    public static int minDamage(int rank) { return 3 + 3 * rank; }
    public static int maxDamage(int rank) { return 7 + 3 * rank; }
    @Override public String desc() {
        int rank = Dungeon.hero.pointsInTalent(Talent.VIBRATING_STAFF);
        return Messages.get(this, "desc", minDamage(rank), maxDamage(rank), RANGE, COOLDOWN);
    }
    @Override protected void onTargetSelected(HolyTome tome, Hero hero, Integer cell) {
        if (cell == null || !tome.canCast(hero, this)) return;
        Ballistica aim = new Ballistica(hero.pos, cell, targetingFlags());
        Char target = Actor.findChar(aim.collisionPos);
        if (target == null || target.alignment != Char.Alignment.ENEMY || !hero.fieldOfView[target.pos]
                || Dungeon.level.distance(hero.pos, target.pos) > RANGE) {
            GLog.w(Messages.get(this, "invalid_target"));
            return;
        }
        int rank = hero.pointsInTalent(Talent.VIBRATING_STAFF);
        int damage = Hero.heroDamageIntRange(minDamage(rank), maxDamage(rank));
        int center = target.pos;
        hero.busy();
        QuickSlotButton.target(target);
        hero.sprite.zap(center);
        hero.sprite.parent.add(new Beam.SunRay(hero.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(center)));
        Sample.INSTANCE.play(Assets.Sounds.RAY);
        strike(hero, target, damage, rank);
        Buff.prolong(hero, StaffCooldown.class, COOLDOWN);
        onSpellCast(tome, hero);
        hero.spendAndNext(1f);
    }
    /** Damage-only half of the cast, also exercised without a graphics context. */
    public static void strike(Hero hero, Char target, int damage, int rank) {
        int center = target.pos;
        target.damage(RapunzelTalents.resonanceDamage(hero, target, damage), INSTANCE);
        if (rank >= 3) {
            for (Char enemy : Dungeon.level.mobs.toArray(new Char[0])) {
                if (enemy != target && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY
                        && Dungeon.level.distance(center, enemy.pos) <= 2
                        && new Ballistica(center, enemy.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos == enemy.pos) {
                    enemy.damage(RapunzelTalents.resonanceDamage(hero, enemy, Math.round(damage * 0.5f)), INSTANCE);
                }
            }
        }
    }
    public static class StaffCooldown extends FlavourBuff {
        @Override public int icon() { return BuffIndicator.TIME; }
    }
}
