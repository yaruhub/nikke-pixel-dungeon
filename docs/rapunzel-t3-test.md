# 라푼젤 1–3티어 구현 기록

작업 브랜치: `feature/rapunzel-t3-test`. 기준 커밋: `8311722f412216ffce0ef3c1698c4a59c19589d4` (작업 시작 당시 `origin/master`).

## 주요 구현 방식

- `HeroClass.CLERIC`의 특성 슬롯과 전직 선택을 라푼젤로 교체했습니다. 1티어 4개, 2티어 5개, 공통 3티어 2개와 전직별 3티어 3개를 연결했습니다.
- `RapunzelTalents`에서 음식, 일반 공격, 최초 실제 시야, 장비 획득, 유물 사용, 실제 회복과 보호막 증가를 처리합니다. 재머 증폭은 일반 공격·표적 유도·진동 지팡이·쉴드 돌진에 적용하며 새 재밍 부여보다 먼저 계산합니다. 마법막대·주문·무기 액티브는 일반 공격 효과를 발생시키지 않습니다.
- 최초 공격·목격 기록은 적 개체의 영구 버프로 저장합니다. 공용 목격 대기시간 동안 만난 적도 기록하며, 성찬 준비·재밍·가속·가속당 환급 사용 여부·회복 턴·대기시간·반격 스택·전달 회복 소수량은 세이브에 보존합니다.
- 실제 회복은 `Char.heal()`에서 최대 HP로 제한한 변화량으로 계산합니다. 자연 회복, 음식, 물약, 이슬, 식물, 마법막대와 아군의 회복 경로를 연결했습니다. 최대 HP 증가·부활·소환 초기 HP는 회복으로 취급하지 않습니다.
- 퓨어 그레이스의 회복 전달은 재전달을 차단합니다. 전달된 회복으로 라푼젤의 보호막과 반격은 발동할 수 있습니다. 실제 회복 1회 + 실제 보호막 증가 1회로 반격 2스택을 얻습니다.
- 파페사 액티브는 전술 지원 코어(`HolyTome`)의 3티어 주문 목록과 빠른 사용 목록에 등록했습니다. 진동 지팡이는 마법 피해, 쉴드 돌진은 방어력 감소를 받는 피해입니다. 공진 과부하는 두 액티브에만 적용합니다.
- 캐릭터 선택 화면의 특성 정보는 진행도와 관계없이 1·2티어를 보여줍니다. 게임 내 특성 해금 레벨과 투자 규칙은 유지했습니다.
- 한국어·영어 이름과 설명을 추가했습니다. 기존 특성/전직 아이콘을 재사용합니다.

## 코어와 저티어 연동

- 기본 프로토콜은 **표적 유도 / 화력 지원 / 방호 프로토콜**입니다. 클래스명 `GuidingLight` / `HolyWeapon` / `HolyWard`는 유지하여 기존 빠른 프로토콜 저장값을 복원합니다. 라푼젤 외 클래스의 기존 명칭과 발광 동작은 유지합니다.
- 음식은 즉시 보호막·지속 회복을 주지 않고 하나의 **성찬 준비**를 만듭니다. 다음 성공한 코어 프로토콜 한 번이 준비를 소모해 성찬의 장막의 보호막 3/5와 축복받은 성찬의 HP 1/턴 × 2/3턴을 함께 발동합니다. 재섭취는 준비를 중첩하지 않습니다. 이미 진행 중인 회복은 유지하며, 준비를 사용한 다음 프로토콜이 회복 지속시간을 갱신합니다.
- 대상 선택 취소, 잘못된 대상, 충전 부족, 오염·마법 면역으로 인한 사용 거부, 표적 유도 발사 후 대상 소실, 함정으로 인한 돌진 중단은 준비와 환급 기회를 소비하지 않습니다.
- 표적 유도는 마법 피해 2–8과 재밍 펄스 +1/+2의 확정 재밍 2/3턴을 적용합니다. 공통 3티어 재머 최적화의 지속시간 증가도 적용합니다. 라푼젤의 legacy Illuminated/WasIlluminatedTracker는 부여하지 않고 기존 저장 효과도 제외합니다.
- 재머 증폭은 이미 재밍된 적에게 해당 공격·프로토콜의 피해를 10%/20% 증가시킵니다. 파페사 액티브의 기존 공진 과부하 배율은 이 결과에 적용합니다(예: +20%와 +30% 동시 적용은 ×1.2×1.3). 새로 부여한 재밍은 같은 타격을 증폭하지 않습니다.
- 유물 공명은 성공한 코어 프로토콜 후 **다른 유물만** 3/5턴 가속합니다. 다른 유물의 실제 사용은 코어에 충전 0.5/1을 직접 추가합니다. 사용한 유물 인스턴스를 넘기는 API를 추가했고 기존 인자 1개 API와 다른 클래스의 효과는 유지했습니다. 직접 충전·가속은 사용 이벤트를 재호출하지 않아 자기 충전과 재귀를 방지합니다. 비어 있는 연금술 도구나 에너지 0 소모도 충전을 만들지 않습니다.
- 구원의 손길은 회복 물약 보호막 대신 **응급 복구**를 해금합니다. 충전 2·1턴을 소모해 자기 HP를 4/6 + 레벨/2(내림), 잃은 HP 한도 내에서 회복합니다. 만피에서는 실패로 처리합니다. `Char.heal()`로 은총의 장막·자비의 손길·은총의 반격을 정상 발동하며 준비한 성찬도 함께 사용합니다.
- **갑니다! 헉, 간다고?**의 최초 발견·15턴 대기시간 규칙은 유지합니다. 각 가속 중 첫 성공 프로토콜은 사용 후 실제 소모량 한도 내에서 최대 충전 1을 환급합니다. 첫 성공이 0 충전 기술이면 환급 없이 기회를 소모합니다. 환급 사용 여부는 버프 설명에 표시하고 저장합니다.
- 3티어와 `PURE_GRACE` / `RAPUNZEL_PAPESS` 구성 및 기존 액티브 사거리·기본 피해·대기시간은 유지합니다.

## 수치가 지정되지 않은 부분의 기본값

| 항목 | 구현값 |
| --- | --- |
| 진동 지팡이 | 사거리 6, 피해 6–10 / 9–13 / 12–16, 대기시간 8턴, 충전 소모 없음 |
| 진동 지팡이 +3 | 대상 주변 2칸 적에게 기본 피해 50%, 벽 차단, 아군 제외 |
| 쉴드 돌진 | 사거리 4, 피해 4–8, 최대 2칸 밀치기(보스 1칸), 대기시간 10턴, 충전 소모 없음 |
| 쉴드 돌진 +2/+3 | 충돌 추가 피해 4 / 돌진 후 보호막 5 |
| 자비의 손길 | 아군 거리 2칸 이내, 소수 회복량은 대상별 누적·저장 |
| 최초 공격/반격 | 일반 공격 적중 후 피해 처리 진입 시 소비. 빗나감·피해 처리 거부 시 보존, 방어력으로 기본 피해가 0인 적중은 소비 |
| 음식 재사용 | 성찬 준비만 갱신·중첩 없음. 성공한 다음 코어 사용 시 지속 회복 갱신 및 보호막 획득 |

## 세이브 호환

- 성찬 준비와 가속당 환급 사용 여부를 버프로 저장합니다. 이전 저장의 대기 중 `SatiatedSpellsTracker`는 성찬 준비로 이전하고, 환급 필드가 없는 이전 가속은 미사용으로 복원합니다. 기존에 시작한 `BlessedSacrament` 회복과 `ArtifactResonance` 지속시간은 보존합니다. 응급 복구도 빠른 프로토콜 저장·복원을 지원합니다.

- 기존 로컬 라푼젤 패치의 `SANCTUARY_MEAL` → `SACRAMENT_VEIL`, `BLESSED_MEAL` → `BLESSED_SACRAMENT`, `HAND_OF_SALVATION` → `SAVING_HAND` 이름과 투자 포인트를 이전합니다.

- 이전 `PRIEST` → `PURE_GRACE`, `PALADIN` → `RAPUNZEL_PAPESS`로 복원하며 특성 포인트를 대응 슬롯으로 이전합니다. 레거시 enum 이름은 유지하여 역직렬화를 지원합니다.
- 전직 업적은 기존 Priest/Paladin 배지와 연결합니다. 현재 선택 목록에서는 레거시 전직을 제외합니다.
- 이전 전직에서 사용하던 빠른 주문이 현재 주문 목록에 없으면 선택을 해제합니다. 기존 물품과 기본 코어 주문은 유지합니다.
- 예전 세이브에는 최초 공격/목격 기록이 없으므로 갱신 이후 처음 만나는 시점부터 기록합니다. 새 세이브를 이전 버전에서 여는 역방향 호환은 보장하지 않습니다.

## 검증

```bash
./gradlew :core:rapunzelTest --no-daemon
./gradlew :android:assembleCloudTest --no-daemon
```

자동 회귀 검증: **29개 시나리오, 352개 검사 통과** (`:core:rapunzelTest`).

Android 검증: 요청한 `./gradlew :android:assembleCloudTest --no-daemon` **성공**. `android/build/outputs/apk/cloudTest/android-cloudTest.apk` 생성 및 `apksigner verify` 통과.

자동 검증은 그래픽 문맥 없이 실제 공격/마법막대 사용/버프 틱/회복/세이브 복원, 코어 성공·실패 완료 경로, 비용·환급·공명, 프로토콜 표시명과 액티브 피해·밀치기 계산을 검사합니다. 렌더링과 터치 조작은 아래 플레이테스트로 별도 확인해야 합니다.

## 남은 플레이테스트

- 새 계정 캐릭터 선택에서 1·2티어 설명 표시, 실제 레벨별 특성 투자와 전직 선택 확인.
- 근접·투척 일반 공격, 실패한 첫 공격, 재밍 적용/연장과 버프 표시, 적 최초 목격 및 15턴 대기시간 체감 확인.
- 음식→준비→성공 프로토콜 연계, 취소·실패·대상 소실 시 보존, 응급 복구 및 코어/다른 유물의 양방향 공명과 실제 충전 표시 확인. 가속당 최초 0/1/2 충전 프로토콜 환급 및 저장 후 재사용 방지 확인.
- 유령·대지 수호자·감시탑·타락 아군의 자연/능동 회복과 자비 전달, 만피/사망 아군 제외, 반격 스택과 보호막 소멸 확인.
- 파페사 코어 메뉴·빠른 사용·타겟 선택·애니메이션 확인. 벽/문/대형 적/보스/함정/구덩이/속박/이동 불가 적에서 돌진과 충돌 확인.
- 진동 지팡이 충격파의 벽 차단·아군 제외·마법 저항, 저주 코어 및 마법 면역 시 사용 제한 확인.
- 실제 기존 Priest/Paladin 저장 파일을 복사해 전직·특성·빠른 주문·업적 마이그레이션 확인. 이동·회복·재밍·대기시간 도중 종료/재개 확인.
- 액티브 임시 수치와 계속 회복할 때의 보호막 증가량을 실전에서 조정할 필요가 있는지 확인.

## 변경 파일 전체

- `.github/workflows/build-apk.yml`
- `android/build.gradle`
- `core/build.gradle`
- `core/src/main/assets/messages/actors/actors.properties`
- `core/src/main/assets/messages/actors/actors_ko.properties`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Badges.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs/WaterOfHealth.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/ArtifactRecharge.java`
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
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/EmergencyRepair.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/GuidingLight.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HallowedGround.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HolyWard.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HolyWeapon.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/LayOnHands.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/ShieldRush.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/SpiritForm.java`
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
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/AlchemistsToolkit.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/Artifact.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/ChaliceOfBlood.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/CloakOfShadows.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/DriedRose.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/EtherealChains.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/HolyTome.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/HornOfPlenty.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/MasterThievesArmband.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/SandalsOfNature.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/SkeletonKey.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/TalismanOfForesight.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/TimekeepersHourglass.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/UnstableSpellbook.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/FrozenCarpaccio.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/Pasty.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/PhantomMeat.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/food/SupplyRation.java`
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
