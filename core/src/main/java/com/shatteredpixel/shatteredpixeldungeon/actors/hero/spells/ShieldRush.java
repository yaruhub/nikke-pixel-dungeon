/* Distributed under the GNU General Public License v3 or later. */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.RapunzelTalents;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class ShieldRush extends TargetedClericSpell {
    public static final ShieldRush INSTANCE = new ShieldRush();
    public static final int RANGE = 4;
    public static final int COOLDOWN = 10;
    @Override public int icon() { return HeroIcon.WALL_OF_LIGHT; }
    @Override public int targetingFlags() { return Ballistica.PROJECTILE; }
    @Override public float chargeUse(Hero hero) { return 0; }
    @Override public boolean canCast(Hero hero) {
        return hero.subClass == HeroSubClass.RAPUNZEL_PAPESS && hero.hasTalent(Talent.SHIELD_RUSH)
                && hero.buff(RushCooldown.class) == null && !hero.rooted;
    }
    @Override public String desc() { return Messages.get(this, "desc", RANGE, COOLDOWN); }
    @Override protected void onTargetSelected(HolyTome tome, Hero hero, Integer cell) {
        if (cell == null || !tome.canCast(hero, this)) return;
        Ballistica aim = new Ballistica(hero.pos, cell, targetingFlags());
        Char enemy = Actor.findChar(aim.collisionPos);
        if (enemy == null || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY
                || !hero.fieldOfView[enemy.pos] || Dungeon.level.distance(hero.pos, enemy.pos) > RANGE) {
            GLog.w(Messages.get(this, "invalid_target")); return;
        }
        int landing = aim.path.get(Math.max(0, aim.dist - 1));
        for (int i = 1; i < aim.dist; i++) {
            int tile = aim.path.get(i);
            if (!Dungeon.level.passable[tile] || Actor.findChar(tile) != null
                    || (Char.hasProp(hero, Char.Property.LARGE) && !Dungeon.level.openSpace[tile])) {
                GLog.w(Messages.get(this, "blocked")); return;
            }
        }
        int rank = hero.pointsInTalent(Talent.SHIELD_RUSH);
        hero.busy();
        QuickSlotButton.target(enemy);
        int start = hero.pos;
        hero.pos = landing;
        if (start != landing) Actor.add(new Pushing(hero, start, landing));
        hero.sprite.place(landing);
        Buff.prolong(hero, RushCooldown.class, COOLDOWN);
        onSpellCast(tome, hero);
        Dungeon.level.occupyCell(hero);
        if (!hero.isAlive()) return;
        if (hero.pos != landing) {
            hero.spendAndNext(1f);
            return;
        }
        Knockback push = planKnockback(enemy, landing);
        int damage = impactDamage(hero, enemy, Hero.heroDamageIntRange(4, 8), rank, push.collided);
        enemy.damage(damage, this);
        if (enemy.isAlive() && push.destination != enemy.pos) {
            int oldPos = enemy.pos;
            enemy.pos = push.destination;
            Actor.add(new Pushing(enemy, oldPos, push.destination));
            Dungeon.level.occupyCell(enemy);
        }
        if (rank >= 3) RapunzelTalents.giveBarrier(hero, 5);
        Dungeon.observe();
        GameScene.updateFog();
        hero.spendAndNext(1f);
    }
    public static final class Knockback {
        public final int destination;
        public final boolean collided;
        private Knockback(int destination, boolean collided) {
            this.destination = destination;
            this.collided = collided;
        }
    }
    public static Knockback planKnockback(Char enemy, int landing) {
        int dx = Integer.signum(enemy.pos % Dungeon.level.width() - landing % Dungeon.level.width());
        int dy = Integer.signum(enemy.pos / Dungeon.level.width() - landing / Dungeon.level.width());
        int destination = enemy.pos;
        boolean collided = enemy.rooted || Char.hasProp(enemy, Char.Property.IMMOVABLE);
        int pushes = Char.hasProp(enemy, Char.Property.BOSS) ? 1 : 2;
        if (!collided) {
            for (int i = 0; i < pushes; i++) {
                int x = destination % Dungeon.level.width() + dx;
                int y = destination / Dungeon.level.width() + dy;
                int next = y * Dungeon.level.width() + x;
                if (x < 0 || x >= Dungeon.level.width() || y < 0 || y >= Dungeon.level.height()
                        || Dungeon.level.solid[next] || Actor.findChar(next) != null
                        || (Char.hasProp(enemy, Char.Property.LARGE) && !Dungeon.level.openSpace[next])) {
                    collided = true; break;
                }
                destination = next;
            }
        }
        return new Knockback(destination, collided);
    }
    public static int impactDamage(Hero hero, Char enemy, int baseDamage, int rank, boolean collided) {
        int damage = Math.max(0, baseDamage - enemy.drRoll()) + (rank >= 2 && collided ? 4 : 0);
        return RapunzelTalents.resonanceDamage(hero, enemy, damage);
    }
    public static class RushCooldown extends FlavourBuff {
        @Override public int icon() { return BuffIndicator.TIME; }
    }
}
