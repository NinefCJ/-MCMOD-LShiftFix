# LShiftFix

> **Minecraft 1.8.9 Forge 입력 수정 모드** — IME 활성화 시 "웅크린 상태에서 점프 불가" 문제 해결.

[![Modrinth](https://img.shields.io/badge/Modrinth-LShiftFix-1bd964?style=flat-square&logo=modrinth)](https://modrinth.com/project/lshiftfix)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.9-6c9f3f?style=flat-square)](https://modrinth.com/project/lshiftfix)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

## 문제

한국어 IME가 활성화된 Minecraft 1.8.9에서 **Shift(웅크리기)를 누른 채 Space(점프)를 눌러도 점프가 되지 않는** 문제가 발생합니다. 점프 입력이 OS의 IME에 의해 삼켜지기 때문입니다.

**근본 원인**: 한국어 IME는 `Shift+Space`를 한영 전환 단축키로 등록합니다. Windows는 `WM_KEYDOWN`을 앱에 전달하기 전에 `ImmTranslateMessage`를 거치며, IME가 여기서 조합 키를 소비합니다. 그 결과 Minecraft의 `Keyboard.next()` 이벤트 큐에 키 이벤트가 도달하지 않고, `keyBindJump.isKeyDown()`이 영원히 `false`로 남습니다.

## 수정 원리

LShiftFix는 `EntityPlayerSP.updateMovementInput()`의 끝에 주입합니다:

1. 플레이어의 점프 키(기본 Space, 재바인딩 지원)를 읽음
2. `Keyboard.isKeyDown(keyCode)`으로 **하드웨어 상태를 직접 폴링**
3. 물리적으로 눌려있는데 `movementInput.jump`가 `false`이면 `true`로 강제 설정

LWJGL의 `keyDownBuffer`는 `WM_KEYDOWN`보다 낮은 레이어의 `WindowProc`에서 갱신되므로, IME의 이벤트 큐 가로채기에 영향받지 않습니다. 이 수정은 **IME 비의존적**이며 Microsoft IME, Sogou, Microsoft Pinyin, Google Input, MS-IME 등 모든 IME에서 작동합니다.

## 설치

| 의존성 | 필수 |
|---|---|
| Minecraft 1.8.9 | 예 |
| Forge `11.15.1.x`(`.2318` 권장) | 예 |
| [MixinBooter](https://modrinth.com/mod/mixinbooter)(1.8.9 빌드) | 예 |

`MixinBooter-*.jar`와 `LShiftFix-1.0.0.jar` 모두 `.minecraft/mods/`에 넣으세요.

## 설정

`.minecraft/config/lshiftfix.cfg`:

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `enableDebugLog` | `false` | IME 삼킴 이벤트 로그 출력 |
| `enableAllKeyPolling` | `false` | 모든 이동 키(WASD + 웅크리기) 폴링 |
| `enableGuiImeGuard` | `true` | 채팅 GUI 내 Shift 누출 억제 |
| `debugLogCooldownTicks` | `20` | 디버그 로그 최소 간격(tick) |

## 게임 내 명령어

| 명령어 | 효과 |
|---|---|
| `/lshiftfix` 또는 `/lshiftfix status` | 현재 상태 표시 |
| `/lshiftfix reload` | 설정 다시 읽기 |
| `/lshiftfix debug on\|off` | 디버그 로깅 토글 |
| `/lshiftfix polling on\|off` | 전체 키 폴링 토글 |
| `/lshiftfix guard on\|off` | GUI IME 가드 토글 |

OP 레벨 2 필요.

## 설정 GUI

Mods 메뉴 → LShiftFix → Config에서 cfg 파일 편집 없이 토글 가능.

## IME 단축키 비활성화(권장)

LShiftFix로 점프는 수정되지만, 다른 맥락에서의 실수를 방지하기 위해 IME의 `Shift+Space` 단축키 비활성화를 권장합니다:

- **Microsoft IME(한국어)**: 설정 → 고급 → 키 할당 → `Shift+Space` 한영 전환 비활성화
- **Sogou**: 설정 → 고급 → 시스템 단축키 → "전각/반각(Shift+Space)" 제거
- **Microsoft Pinyin**: 설정 → 언어 → 중국어 → 옵션 → Microsoft Pinyin → 옵션 → 고급 → "전각/반각: Shift+Space" 체크 해제

## 호환성

- **클라이언트 전용**(명령어 등록을 위해 양쪽에서 로드됨)
- **GUI 안전**: 채팅 / 책 / 표지판 GUI 중에는 자동 비활성화
- **재바인딩 존중**: `gameSettings.keyBindJump`를 런타임에 읽음
- **마우스 바인딩 보호**: 점프가 마우스 버튼에 바인딩된 경우 건너뜀
- **OptiFine**: 충돌 없음, 시작 시 로그 출력
- **PlayerAPI**: 감지 후 경고
- **Lunar Client / Badlion**: 감지 후 메인 폴링 자동 비활성화(이들은 이미 수정되어 있음)

## 빌드

```bash
# JDK 8 필수(ForgeGradle 2.1 제한)
gradle wrapper --gradle-version 2.14.1
./gradlew build
# 출력: build/libs/LShiftFix-1.0.0.jar
```

CI는 GitHub Actions에 사전 구성되어 있습니다 — [`.github/workflows/build.yml`](./.github/workflows/build.yml) 참조.

## 문서

- [中文 README](./README.md)
- [English README](./README_EN.md)
- [日本語 README](./README_JA.md)
- [FAQ（中文）](./FAQ.md)
- [Architecture（English）](./ARCHITECTURE.md)

## 라이선스

[MIT](./LICENSE) © 2026 ninefyu
