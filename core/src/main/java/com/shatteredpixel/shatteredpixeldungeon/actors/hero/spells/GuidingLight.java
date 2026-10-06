/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.RapunzelTalents;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class GuidingLight extends TargetedClericSpell {

	public static final GuidingLight INSTANCE = new GuidingLight();

	@Override
	public int icon() {
		return HeroIcon.GUIDING_LIGHT;
	}

	@Override
	protected void onTargetSelected(HolyTome tome, Hero hero, Integer target) {
		if (target == null || !tome.canCast(hero, this)){
			return;
		}

        Ballistica aim = new Ballistica(hero.pos, target, targetingFlags());
        if (hero.heroClass == HeroClass.CLERIC && !validRapunzelTarget(hero, Actor.findChar(aim.collisionPos))) {
            GLog.w(Messages.get(this, "invalid_target"));
            return;
        }

		if (Actor.findChar( aim.collisionPos ) == hero){
			GLog.i( Messages.get(Wand.class, "self_target") );
			return;
		}

		if (Actor.findChar(aim.collisionPos) != null) {
			QuickSlotButton.target(Actor.findChar(aim.collisionPos));
		} else {
			QuickSlotButton.target(Actor.findChar(target));
		}

		hero.busy();
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
		hero.sprite.zap(target);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.LIGHT_MISSILE, hero.sprite, aim.collisionPos, new Callback() {
			@Override
			public void call() {

                Char ch = Actor.findChar(aim.collisionPos);
                if (hero.heroClass == HeroClass.CLERIC) {
                    if (!completeRapunzelHit(tome, hero, ch, Hero.heroDamageIntRange(2, 8))) {
                        hero.spend(1f);
                        hero.next();
                        return;
                    }
                } else if (ch != null) {
                    strike(hero, ch, Hero.heroDamageIntRange(2, 8));
                }
                if (ch != null) {
                    Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f));
                    ch.sprite.burst(0xFFFFFF44, 3);
                } else {
                    Dungeon.level.pressCell(aim.collisionPos);
                }
                hero.spend(1f);
                hero.next();
                if (hero.heroClass != HeroClass.CLERIC) onSpellCast(tome, hero);
				if (hero.subClass == HeroSubClass.PRIEST && hero.buff(GuidingLightPriestCooldown.class) == null) {
					Buff.prolong(hero, GuidingLightPriestCooldown.class, 50f);
					ActionIndicator.refresh();
				}

			}
		});
	}

    public static boolean validRapunzelTarget(Hero hero, Char target) {
        return target != null && target.isAlive() && target.alignment == Char.Alignment.ENEMY
                && hero.fieldOfView[target.pos];
    }

    /** A target lost before bolt impact is a failed protocol, preserving preparation and refunds. */
    public static boolean completeRapunzelHit(HolyTome core, Hero hero, Char target, int damage) {
        if (hero.heroClass != HeroClass.CLERIC || !core.canCast(hero, INSTANCE) || !validRapunzelTarget(hero, target)) return false;
        strike(hero, target, damage);
        INSTANCE.onSpellCast(core, hero);
        return true;
    }

    /** Shared by the rendered bolt and headless regression checks. */
    public static void strike(Hero hero, Char target, int damage) {
        if (hero.heroClass == HeroClass.CLERIC) {
            damage = RapunzelTalents.jammerDamage(hero, target, damage);
            Buff.detach(target, Illuminated.class);
            Buff.detach(target, WasIlluminatedTracker.class);
            target.damage(damage, INSTANCE);
            if (target.isAlive() && target.alignment == Char.Alignment.ENEMY && hero.hasTalent(Talent.JAMMING_PULSE)) {
                Buff.prolong(target, RapunzelTalents.Jamming.class, RapunzelTalents.pulseDuration(hero));
            }
        } else {
            target.damage(damage, INSTANCE);
            if (target.isAlive()) {
                Buff.affect(target, Illuminated.class);
                Buff.affect(target, WasIlluminatedTracker.class);
            }
        }
    }

	@Override
	public float chargeUse(Hero hero) {
		if (hero.subClass == HeroSubClass.PRIEST
			&& hero.buff(GuidingLightPriestCooldown.class) == null){
			return 0;
		} else {
			return 1;
		}
	}

	public String desc(){
		String desc = Messages.get(this, rapunzelBasicProtocol() ? "desc_rapunzel" : "desc");
		if (Dungeon.hero.subClass == HeroSubClass.PRIEST){
			desc += "\n\n" + Messages.get(this, "desc_priest");
		}
		return desc + "\n\n" + Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
	}

	public static class GuidingLightPriestCooldown extends FlavourBuff {

		@Override
		public int icon() {
			return BuffIndicator.ILLUMINATED;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.brightness(0.5f);
		}

		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }

		@Override
		public void detach() {
			super.detach();
			ActionIndicator.refresh();
		}
	}

	public static class Illuminated extends Buff {
        @Override public boolean attachTo(Char target) {
            return (Dungeon.hero == null || Dungeon.hero.heroClass != HeroClass.CLERIC) && super.attachTo(target);
        }

		{
			type = buffType.NEGATIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ILLUMINATED;
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.ILLUMINATED);
			else target.sprite.remove(CharSprite.State.ILLUMINATED);
		}

		@Override
		public String desc() {
			String desc = super.desc();

			if (Dungeon.hero.subClass == HeroSubClass.PRIEST){
				desc += "\n\n" + Messages.get(this, "desc_priest");
			} else if (Dungeon.hero.heroClass != HeroClass.CLERIC){
				desc += "\n\n" + Messages.get(this, "desc_generic");
			}

			return desc;
		}
	}

	public static class WasIlluminatedTracker extends Buff {
        @Override public boolean attachTo(Char target) {
            return (Dungeon.hero == null || Dungeon.hero.heroClass != HeroClass.CLERIC) && super.attachTo(target);
        }
    }
}
