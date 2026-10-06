# 라푼젤 1–3티어 구현 기록

작업 브랜치: `feature/rapunzel-t3-test`. 기준 커밋: `8311722f412216ffce0ef3c1698c4a59c19589d4` (작업 시작 당시 `origin/master`).

## 주요 구현 방식

- `HeroClass.CLERIC`의 특성 슬롯과 전직 선택을 라푼젤로 교체했습니다. 1티어 4개, 2티어 5개, 공통 3티어 2개와 전직별 3티어 3개를 연결했습니다.
- `RapunzelTalents`에서 음식, 일반 공격, 최초 실제 시야, 장비 획득, 유물 사용, 실제 회복과 보호막 증가를 처리합니다. 재밍 증폭은 새 재밍 확률 판정보다 먼저 계산합니다. 마법막대·주문·무기 액티브는 일반 공격 효과를 발생시키지 않습니다.
- 최초 공격·목격 기록은 적 개체의 영구 버프로 저장합니다. 공용 목격 대기시간 동안 만난 적도 기록하며, 재밍·가속·회복 턴·대기시간·반격 스택·전달 회복 소수량은 세이브에 보존합니다.
- 실제 회복은 `Char.heal()`에서 최대 HP로 제한한 변화량으로 계산합니다. 자연 회복, 음식, 물약, 이슬, 식물, 마법막대와 아군의 회복 경로를 연결했습니다. 최대 HP 증가·부활·소환 초기 HP는 회복으로 취급하지 않습니다.
- 퓨어 그레이스의 회복 전달은 재전달을 차단합니다. 전달된 회복으로 라푼젤의 보호막과 반격은 발동할 수 있습니다. 실제 회복 1회 + 실제 보호막 증가 1회로 반격 2스택을 얻습니다.
- 파페사 액티브는 전술 지원 코어(`HolyTome`)의 3티어 주문 목록과 빠른 사용 목록에 등록했습니다. 진동 지팡이는 마법 피해, 쉴드 돌진은 방어력 감소를 받는 피해입니다. 공진 과부하는 두 액티브에만 적용합니다.
- 캐릭터 선택 화면의 특성 정보는 진행도와 관계없이 1·2티어를 보여줍니다. 게임 내 특성 해금 레벨과 투자 규칙은 유지했습니다.
- 한국어·영어 이름과 설명을 추가했습니다. 기존 특성/전직 아이콘을 재사용합니다.

## 수치가 지정되지 않은 부분의 기본값

| 항목 | 구현값 |
| --- | --- |
| 진동 지팡이 | 사거리 6, 피해 6–10 / 9–13 / 12–16, 대기시간 8턴, 충전 소모 없음 |
| 진동 지팡이 +3 | 대상 주변 2칸 적에게 기본 피해 50%, 벽 차단, 아군 제외 |
| 쉴드 돌진 | 사거리 4, 피해 4–8, 최대 2칸 밀치기(보스 1칸), 대기시간 10턴, 충전 소모 없음 |
| 쉴드 돌진 +2/+3 | 충돌 추가 피해 4 / 돌진 후 보호막 5 |
| 자비의 손길 | 아군 거리 2칸 이내, 소수 회복량은 대상별 누적·저장 |
| 최초 공격/반격 | 첫 일반 공격 시도에 소비되므로 빗나가도 기회·스택 소비 |
| 음식 재사용 | 회복 남은 턴 갱신, 보호막은 획득량만큼 증가 |

## 세이브 호환

- 이전 `PRIEST` → `PURE_GRACE`, `PALADIN` → `RAPUNZEL_PAPESS`로 복원하며 특성 포인트를 대응 슬롯으로 이전합니다. 레거시 enum 이름은 유지하여 역직렬화를 지원합니다.
- 전직 업적은 기존 Priest/Paladin 배지와 연결합니다. 현재 선택 목록에서는 레거시 전직을 제외합니다.
- 이전 전직에서 사용하던 빠른 주문이 현재 주문 목록에 없으면 선택을 해제합니다. 기존 물품과 기본 코어 주문은 유지합니다.
- 예전 세이브에는 최초 공격/목격 기록이 없으므로 갱신 이후 처음 만나는 시점부터 기록합니다. 새 세이브를 이전 버전에서 여는 역방향 호환은 보장하지 않습니다.

## 검증

```bash
./gradlew :core:rapunzelTest --no-daemon
./gradlew :android:assembleDebug --no-daemon
```

자동 회귀 검증: **23개 시나리오, 187개 검사 통과** (`:core:rapunzelTest`).

Android 검증: 요청한 `./gradlew :android:assembleDebug --no-daemon` **성공** (1분 7초). `android/build/outputs/apk/debug/android-debug.apk` 생성 및 `apksigner verify` 통과.

자동 검증은 그래픽 문맥 없이 실제 공격/마법막대 사용/버프 틱/회복/세이브 복원 경로와 액티브 피해·밀치기 계산을 검사합니다. 렌더링과 터치 조작은 아래 플레이테스트로 별도 확인해야 합니다.

## 남은 플레이테스트

- 새 계정 캐릭터 선택에서 1·2티어 설명 표시, 실제 레벨별 특성 투자와 전직 선택 확인.
- 근접·투척 일반 공격, 실패한 첫 공격, 재밍 적용/연장과 버프 표시, 적 최초 목격 및 15턴 대기시간 체감 확인.
- 다양한 음식·회복 물약·유물에서 턴 소모, 지속시간, 자연 충전과 공명의 중첩 및 충전 밸런스 확인.
- 유령·대지 수호자·감시탑·타락 아군의 자연/능동 회복과 자비 전달, 만피/사망 아군 제외, 반격 스택과 보호막 소멸 확인.
- 파페사 코어 메뉴·빠른 사용·타겟 선택·애니메이션 확인. 벽/문/대형 적/보스/함정/구덩이/속박/이동 불가 적에서 돌진과 충돌 확인.
- 진동 지팡이 충격파의 벽 차단·아군 제외·마법 저항, 저주 코어 및 마법 면역 시 사용 제한 확인.
- 실제 기존 Priest/Paladin 저장 파일을 복사해 전직·특성·빠른 주문·업적 마이그레이션 확인. 이동·회복·재밍·대기시간 도중 종료/재개 확인.
- 액티브 임시 수치와 계속 회복할 때의 보호막 증가량을 실전에서 조정할 필요가 있는지 확인.

## 변경 파일 전체

- `core/build.gradle`
- `core/src/main/assets/messages/actors/actors.properties`
- `core/src/main/assets/messages/actors/actors_ko.properties`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Badges.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs/WaterOfHealth.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Healing.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/MagicalSleep.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/Regeneration.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/ShieldBuff.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/WellFed.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/HeroClass.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/HeroSubClass.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/RapunzelTalents.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Talent.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/duelist/Challenge.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/duelist/ElementalStrike.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/mage/ElementalBlast.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/BlessSpell.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/ClericSpell.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HallowedGround.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/LayOnHands.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/ShieldRush.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/VibratingStaff.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Bat.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/CrystalGuardian.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Mob.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Necromancer.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/RotLasher.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Succubus.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/YogFist.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/Dewdrop.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/Item.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/armor/curses/Metabolism.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/armor/glyphs/AntiMagic.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/ChaliceOfBlood.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/DriedRose.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/HolyTome.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/FrozenCarpaccio.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/Pasty.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/PhantomMeat.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/SupplyRation.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/potions/PotionOfHealing.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/potions/elixirs/ElixirOfAquaticRejuvenation.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/remains/TornPage.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/CursedWand.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/Wand.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/WandOfLivingEarth.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/WandOfTransfusion.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/WandOfWarding.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/enchantments/Vampiric.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/plants/Sungrass.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/TalentsPane.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndHeroInfo.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/RapunzelTalentTest.java`
- `docs/rapunzel-t3-test.md`
