# 라푼젤 1–4티어 플레이테스트 패치

작업 브랜치: `feature/rapunzel-t3-test`. `HeroClass.CLERIC` 슬롯을 사용합니다. 기존 1–3티어 전직과 해금 레벨은 유지하며 4티어 선택은 라푼젤 전용 능력으로 교체했습니다.

## 저티어·전술 지원 코어

- 음식 섭취 즉시 성찬의 장막 보호막 3/5를 얻습니다. 축복받은 성찬은 즉시 지속 회복을 시작하고 다음 턴부터 HP 1을 2/3회 회복합니다. 재섭취는 회복 시간을 갱신합니다. 코어 사용 대기는 없습니다.
- 재밍 펄스는 일반·투척 공격, 즉발 마법막대와 공격성 프로토콜에 적용합니다. 확률 20%/30%, 지속 2/3턴이며 재머 최적화의 지속·명중률·확률 보너스를 공통 적용합니다. 표적 유도는 확정 재밍을 유지합니다.
- 명시적 동기 직접 공격 구간에 대상별 판정 기록을 두어, 일반 공격의 화력 지원·인챈트 피해나 마법의 여러 패킷이 같은 대상에게 추가 재밍 확률 판정을 만들지 않습니다. 지속 피해, 설치된 장판의 후속 피해, 다른 캐릭터·함정·환경 피해는 제외합니다. 시각 효과 뒤의 즉발 콜백은 같은 공격의 판정 기록만 전달하며 대기 중 활성 피해 구간을 유지하지 않습니다.
- 재머 증폭과 공진 과부하는 기존 적용 범주와 수치를 유지하고 새 재밍보다 먼저 계산합니다. 순례자의 직감과 반격은 실제 일반 공격 명중 이후에만 소비합니다. 빗나감·피해 처리 거부는 보존하고 방어력으로 기본 피해가 0인 명중은 소비합니다.
- 은총의 반격은 실제 회복·보호막 증가마다 1스택을 얻고 중첩 상한이 없습니다. 다음 일반 공격 명중에서 전부 소비하며 스택당 추가 피해 2/4/6은 유지합니다.
- 공통 출력: `max(0, floor(lvl/4) + floor((hero.STR()-10)/2) + floor(tome.level()/2))`.

| 프로토콜 | 기본 충전 | 효과 |
| --- | --- | --- |
| 표적 유도 | 1 | 마법 피해 `2+출력` ~ `8+출력`; 재밍 펄스 투자 시 확정 재밍 |
| 화력 지원 | 2 | 50턴, 명중 시 별도 마법 피해 `2+floor(출력/2)`; 기존 인챈트·저주 유지 |
| 방호 프로토콜 | 1 | 50턴, 기존 피해 범주에 고정 감소 `1+floor(출력/3)`; 상형문자·저주 유지 |
| 퓨어 그레이스 방호 추가 효과 | — | 성공 사용 즉시 보호막 `floor(출력/2)` 및 실제 보호막 획득 훅 |
| 응급 복구 | 2 | 구원의 손길 해금, HP `4/6+floor(lvl/2)` 회복 |

유물 공명은 코어 사용 시 다른 유물만 3/5턴 가속하고 다른 유물의 실제 사용은 코어 충전 0.5/1을 회복합니다. source 유물을 구분해 재귀를 방지합니다. **갑니다! 헉, 간다고?**의 최초 발견·15턴 공용 대기시간·이동속도 +50%·2/3턴·첫 성공 사용 환급 규칙은 유지합니다.

## 신규 4티어

각 능력은 전용 특성 3개(각 최대 4포인트)와 기존 `HEROIC_ENERGY`를 사용합니다. 아래 방어구 비용은 영웅적 에너지 적용 전입니다.

### 성녀의 기도 — 비용 50

자신과 반경 3칸 이내 아군에게 HP `8+floor(lvl/2)` 회복과 보호막 10을 제공합니다. `Char.heal()`과 `Barrier`를 사용해 퓨어 그레이스의 실제 회복·보호막 효과를 정상 발동합니다.

- 넘치는 은총: 회복량 +20/40/60/80%.
- 정화의 은총: +1 표준 정화 가능 해로운 효과 제거, +2 BlobImmunity 10턴, +3 20턴, +4 기본 회복량 +25%. 넘치는 은총과 합산하며 한 번만 회복합니다. 시스템·보스·정렬 마커는 제거하지 않습니다.
- 기도의 메아리: 3턴 후 원래 대상 중 같은 층·분기에 살아 있는 유효한 대상에게 회복·보호막의 25/35/45/55%를 다시 줍니다. 정화와 BlobImmunity는 반복하지 않습니다. 대상 ID·회복량·보호막·층·분기·지연 시간을 저장합니다.

### 파페사의 강림 — 비용 50

사거리 6, 지정 지점 반경 3칸의 적에게 마법 피해 `8+floor(lvl/2)+출력` ~ `12+floor(lvl/2)+출력`. 중심 대상은 +50%. 기본 재밍 4턴을 부여하며 재밍 펄스를 추가 판정하지 않습니다. 아군과 피해 무적 상태는 제외합니다.

- 공진 붕괴: 피해 +15/30/45/60%, 1포인트 이상이면 공격 전부터 재밍된 적에게 추가 +10%.
- 강제 진동: +1 바깥으로 1칸, +2 2칸, +3 벽·고정 공간 장애로 막힌 경우 본 피해의 25% 추가 충돌 피해, +4 실제 충돌 피해가 발생한 적에게 마비 1턴. 기존 밀치기 유틸리티로 이동·낙하 처리를 재사용합니다. 보스 거리 감소·고정형/속박 면역을 유지합니다.
- 재머 폭주: +1 강림 재밍 +1턴, +2 명중률 감소 추가 5%p, +3 강림 재밍 중 직접 마법 피해 +10%, +4 일반 재밍 저항·면역에 최소 2턴. 보호된 보스/시스템 상태는 유지합니다. 강림 재밍의 명중률·취약 플래그를 저장합니다.

### 코어 오버드라이브 — 비용 35

사용 가능한 코어를 최대 충전하고 기본 10턴 동안 프로토콜 비용을 50% 감소시킵니다. 자연 충전 속도는 기존 RingOfEnergy 배율에 2배를 곱합니다. 성공 프로토콜 사용마다 이동속도 +50%를 1턴 부여합니다. 무료 기술은 이동 효과만 받을 수 있습니다.

- 확장 출력: 지속 12/14/16/18턴.
- 프로토콜 연쇄: 성공 사용 이후 다음 비용에 0.25/0.50/0.75/1.00을 추가 할인합니다. 50% 할인 후 적용, 최저 0. 실패/취소는 보존하며 성공하면 소비하고 다시 다음 할인으로 갱신합니다.
- 순례자의 기적: +1 HP 5, +2 보호막 5, +3 둘 다 5, +4 둘 다 8. +4는 살아 있는 재밍 적마다 0.25의 예비 충전(최대 2)을 저장합니다. 예비 충전이 이후 실제 비용을 먼저 보전하므로 코어가 최대 충전 상태여도 증발하지 않습니다. 무료 기술에는 사용하지 않습니다.
- 오버드라이브 시간·연쇄 할인·예비 충전은 저장/복원되고 종료 시 상태가 제거됩니다. 최초 발견 가속의 환급은 예비 충전으로 보전된 부분을 제외한 실제 코어 소비를 넘지 않습니다.

## 세이브 이전

| 구 능력 | 신규 능력 | 구 특성 → 신규 특성 |
| --- | --- | --- |
| AscendedForm | 성녀의 기도 | DIVINE_INTERVENTION/JUDGEMENT/FLASH → OVERFLOWING_GRACE/PURIFYING_GRACE/PRAYER_ECHO |
| Trinity | 코어 오버드라이브 | BODY_FORM/MIND_FORM/SPIRIT_FORM → EXTENDED_OUTPUT/PROTOCOL_CHAIN/PILGRIMS_MIRACLE |
| PowerOfMany | 파페사의 강림 | BEAMING_RAY/LIFE_LINK/STASIS → RESONANCE_COLLAPSE/FORCED_VIBRATION/JAMMER_RAMPAGE |

특성 포인트와 HEROIC_ENERGY를 보존합니다. 구 능력 클래스와 enum은 역직렬화용으로 유지하고 선택 목록에서는 제외합니다. 구 형상 버프는 불러오기 때 정리하여 신규 능력에 구 형상 효과가 섞이지 않게 합니다.

기존 PRIEST/PALADIN 전직과 1–3티어 이름 이전, 로컬 SANCTUARY_MEAL/BLESSED_MEAL/HAND_OF_SALVATION 별칭을 유지합니다. 구 SacramentReady 클래스는 읽기 전용 호환 표식으로 남겨 불러오기 시 제거합니다. 대기 중인 구 SatiatedSpellsTracker도 제거하며, 이미 시작한 회복과 기존 가속 환급 사용 여부는 보존합니다. 신규 저장을 이전 버전에서 여는 역방향 호환은 지원하지 않습니다.

## 검증과 APK

```bash
./gradlew :core:rapunzelTest --no-daemon
./gradlew :android:assembleCloudTest --no-daemon
apksigner verify --verbose android/build/outputs/apk/cloudTest/android-cloudTest.apk
```

회귀 테스트: **49개 시나리오, 598개 검사**. 기존 29개 시나리오를 새 음식·스택 규칙에 맞게 갱신하고 직접 마법·다중 패킷·무제한 스택 저장·실제 STR 출력·인챈트/상형문자 유지·신규 4티어·면역·메아리·예비 충전·이전·한국어/영어 설명을 검사합니다.

APK: `android/build/outputs/apk/cloudTest/android-cloudTest.apk`. `.cloudtest` ID와 `-INDEV-CLOUDTEST` 버전명, 별도 Cloud Test 이름을 유지합니다. release/debug와 workflow 설정은 이번 패치에서 변경하지 않습니다.

## 플레이테스트

- 실제 Android UI에서 신규 세 능력 선택, 특성 투자, 조준·취소, 빠른 코어 프로토콜 조작.
- 다수 적·아군이 있는 기도/강림에서 시각 효과, 밀치기 애니메이션·낭떠러지·보스 면역.
- 메아리 대기 중 층 이동·아군 사망·저장/종료/재실행, 오버드라이브 중 실제 게임 재실행.
- 기존 로컬 세이브와 구 Cleric 4티어 능력을 선택한 실제 세이브 로드.
- 초중후반 출력과 무제한 반격 스택·오버드라이브 예비 충전의 체감 밸런스. 확정 수치는 변경하지 않았습니다.

## 변경 파일

- `core/src/main/assets/messages/actors/actors.properties`
- `core/src/main/assets/messages/actors/actors_ko.properties`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/HeroClass.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/RapunzelTalents.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Talent.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/rapunzel/CoreOverdrive.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/rapunzel/PapessDescent.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/rapunzel/SaintPrayer.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/ClericSpell.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/GuidingLight.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HolyLance.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HolyWard.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/HolyWeapon.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/Judgement.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/Radiance.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/ShieldRush.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/Sunray.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/spells/VibratingStaff.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Skeleton.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/armor/Armor.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/armor/glyphs/AntiMagic.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/ChaliceOfBlood.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/artifacts/HolyTome.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/CursedWand.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/Wand.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/Weapon.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/melee/MeleeWeapon.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/RapunzelTalentTest.java`
- `docs/rapunzel-t3-test.md`
