# Cloud Test 고정 서명 설정

`feature/rapunzel-t3-test`의 Cloud Test APK는 GitHub Actions Secrets로 제공한 고정 키만 사용합니다. `cloudTest`의 전용 signingConfig이며 debug/release 서명 설정은 그대로입니다. Secret 누락, 잘못된 base64, 임시 파일과 Secret 내용 불일치는 실패로 처리하고 기본 debug 키로 대체하지 않습니다.

## 최초 1회: 고정 키 생성

JDK의 `keytool`이 있는 개인 컴퓨터에서 실행합니다. 저장소 밖에 생성하고 이후에도 이 파일과 비밀번호를 그대로 보관해야 합니다.

```bash
mkdir -p "$HOME/.nikke-signing"
chmod 700 "$HOME/.nikke-signing"
keytool -genkeypair \
  -alias cloud-test \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -storetype PKCS12 \
  -keystore "$HOME/.nikke-signing/cloud-test.p12" \
  -dname "CN=Nikke Pixel Dungeon Cloud Test"
chmod 600 "$HOME/.nikke-signing/cloud-test.p12"
```

키 저장소 비밀번호를 묻는 프롬프트에 충분히 긴 비밀번호를 입력하고 재입력합니다. 이 PKCS12 생성 방식에서는 키 비밀번호도 같은 비밀번호를 사용합니다. 이미 생성한 고정 키가 있다면 새 키를 생성하지 말고 해당 파일·별칭·비밀번호를 사용하세요.

base64는 **keystore 파일 전체**를 인코딩해야 합니다. 파일 경로나 인증서만 인코딩하면 안 됩니다. 아래 명령은 운영체제별 `base64` 옵션 차이 없이 한 줄 파일을 만듭니다.

```bash
python3 - <<'PY'
import base64
from pathlib import Path
source = Path.home() / '.nikke-signing/cloud-test.p12'
output = source.with_suffix('.p12.base64')
output.write_text(base64.b64encode(source.read_bytes()).decode('ascii'))
output.chmod(0o600)
PY
```

`.base64` 파일도 개인 키를 담은 민감 파일입니다. keystore·base64·비밀번호를 Git에 추가하거나 이슈/채팅에 붙여 넣지 마세요. 생성한 키와 비밀번호를 안전하게 백업하세요.

## GitHub Repository Secrets 등록

저장소의 **Settings → Secrets and variables → Actions → New repository secret**에서 다음 네 개를 등록합니다:

https://github.com/yaruhub/nikke-pixel-dungeon/settings/secrets/actions

| Secret 이름 | 입력할 값 |
| --- | --- |
| `CLOUD_TEST_KEYSTORE` | `~/.nikke-signing/cloud-test.p12.base64` 파일의 전체 한 줄 내용. 파일 경로가 아닙니다. |
| `CLOUD_TEST_STORE_PASSWORD` | keytool에서 입력한 키 저장소 비밀번호 |
| `CLOUD_TEST_KEY_ALIAS` | 위 명령 그대로 생성했다면 `cloud-test` |
| `CLOUD_TEST_KEY_PASSWORD` | 위 PKCS12 생성 방식에서는 저장소 비밀번호와 동일한 값 |

GitHub CLI를 사용한다면 다음 명령으로 base64 파일을 터미널에 출력하지 않고 등록할 수 있습니다:

```bash
gh secret set CLOUD_TEST_KEYSTORE --repo yaruhub/nikke-pixel-dungeon \
  < "$HOME/.nikke-signing/cloud-test.p12.base64"
printf '%s' 'cloud-test' | gh secret set CLOUD_TEST_KEY_ALIAS --repo yaruhub/nikke-pixel-dungeon
gh secret set CLOUD_TEST_STORE_PASSWORD --repo yaruhub/nikke-pixel-dungeon
gh secret set CLOUD_TEST_KEY_PASSWORD --repo yaruhub/nikke-pixel-dungeon
```

마지막 두 명령은 비밀번호를 대화식으로 입력합니다. 비밀번호를 `--body` 명령 인수에 넣어 셸 히스토리에 남기지 마세요. 기존 키를 사용하는 경우 해당 키의 별칭과 비밀번호를 등록합니다.

## 빌드와 인증서 확인

Secrets 등록 후 **Actions → Build APK → Run workflow**에서 `feature/rapunzel-t3-test`를 선택합니다. push로 실행한 작업이 Secret 누락으로 실패했다면 등록 후 다시 실행하세요.

Actions는 네 Secret의 존재를 먼저 검사하고 `$RUNNER_TEMP/cloud-test-signing.keystore`에 base64를 디코딩합니다. Gradle은 동일한 네 환경변수를 읽으며 별도의 `-PcloudTestKeystorePath`로 임시 파일 경로만 받습니다. 파일 내용은 Secret의 base64와 비교합니다. 임시 파일은 빌드 성공/실패 후 제거하며 업로드하는 artifact에는 APK만 포함합니다.

APK 생성 후 다음 검증이 수행됩니다:

```bash
apksigner verify --verbose --print-certs android/build/outputs/apk/cloudTest/android-cloudTest.apk
```

로그의 `Signer #1 certificate SHA-256 digest`를 첫 고정 서명 빌드와 이후 빌드에서 비교하세요. 같은 키·같은 서명 인증서를 유지하면 항상 같습니다. 임시 debug 키로 만들어진 기존 앱의 인증서는 새 고정 키와 다르므로, **최초 전환 때에는 기존 Cloud Test 앱을 한 번 제거하고 새 APK를 설치해야 할 수 있습니다**. 삭제 전 필요한 세이브를 백업하세요. 이후에는 같은 키로 생성한 APK로 업데이트할 수 있습니다.

## 로컬 Cloud Test 빌드

개인 터미널에서 네 환경변수를 설정하고, base64를 임시 파일로 디코딩한 뒤 실행합니다. 비밀번호를 셸 히스토리에 기록하지 않도록 `read -s`를 사용합니다.

```bash
export CLOUD_TEST_KEYSTORE="$(cat "$HOME/.nikke-signing/cloud-test.p12.base64")"
export CLOUD_TEST_KEY_ALIAS=cloud-test
read -r -s -p 'Store password: ' CLOUD_TEST_STORE_PASSWORD; echo
export CLOUD_TEST_STORE_PASSWORD
export CLOUD_TEST_KEY_PASSWORD="$CLOUD_TEST_STORE_PASSWORD"
cloud_test_signing_dir="$(mktemp -d)"
umask 077
printf '%s' "$CLOUD_TEST_KEYSTORE" | base64 --decode > "$cloud_test_signing_dir/key.p12"
./gradlew :android:assembleCloudTest --no-daemon \
  "-PcloudTestKeystorePath=$cloud_test_signing_dir/key.p12"
rm -f "$cloud_test_signing_dir/key.p12"
rmdir "$cloud_test_signing_dir"
unset CLOUD_TEST_KEYSTORE CLOUD_TEST_STORE_PASSWORD CLOUD_TEST_KEY_ALIAS CLOUD_TEST_KEY_PASSWORD
```

예시는 Linux/Git Bash 기준입니다. macOS 기본 base64 디코딩은 `base64 -D`를 사용합니다. debug/release 빌드에는 이 변수나 임시 파일이 필요하지 않습니다.
