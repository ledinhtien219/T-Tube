#!/usr/bin/env python3
"""Bump the Google Play build version for T Tube.

Usage:
  python3 scripts/bump_play_version.py 1.0.78 274
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
GRADLE = ROOT / "app" / "build.gradle.kts"
CHECKLIST = ROOT / "play" / "PLAY_CONSOLE_CHECKLIST.md"
STORE = ROOT / "play" / "STORE_LISTING_VI.md"


def fail(message: str) -> None:
    raise SystemExit(message)


def replace_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.MULTILINE)
    if count != 1:
        fail(f"Could not update {label}; expected exactly one match, got {count}.")
    return updated


def main() -> None:
    if len(sys.argv) != 3:
        fail("Usage: python3 scripts/bump_play_version.py <versionName> <versionCode>")

    version_name = sys.argv[1].strip()
    try:
        version_code = int(sys.argv[2])
    except ValueError:
        fail("versionCode must be an integer.")

    if not re.fullmatch(r"\d+\.\d+\.\d+", version_name):
        fail("versionName must look like 1.0.78.")
    if version_code <= 0:
        fail("versionCode must be positive.")

    gradle = GRADLE.read_text(encoding="utf-8")
    current_code_match = re.search(r"^\s*versionCode\s*=\s*(\d+)\s*$", gradle, re.MULTILINE)
    current_name_match = re.search(r'^\s*versionName\s*=\s*"([^"]+)"\s*$', gradle, re.MULTILINE)
    if not current_code_match or not current_name_match:
        fail("Could not read the current version from app/build.gradle.kts.")

    current_code = int(current_code_match.group(1))
    current_name = current_name_match.group(1)
    if version_code <= current_code:
        fail(f"New versionCode {version_code} must be greater than current {current_code}.")

    gradle = replace_once(
        gradle,
        r"^(\s*)versionCode\s*=\s*\d+\s*$",
        rf"\g<1>versionCode = {version_code}",
        "versionCode",
    )
    gradle = replace_once(
        gradle,
        r'^(\s*)versionName\s*=\s*"[^"]+"\s*$',
        rf'\g<1>versionName = "{version_name}"',
        "versionName",
    )
    GRADLE.write_text(gradle, encoding="utf-8")

    if CHECKLIST.exists():
        text = CHECKLIST.read_text(encoding="utf-8")
        text = re.sub(r"- Version: `[^`]+`", f"- Version: `{version_name}`", text, count=1)
        text = re.sub(r"- Version code: `\d+`", f"- Version code: `{version_code}`", text, count=1)
        text = re.sub(
            r"TTube_v[^`]+_FullAA_Internal_signed\.aab",
            f"TTube_v{version_name}_build{version_code}_FullAA_Internal_signed.aab",
            text,
            count=1,
        )
        CHECKLIST.write_text(text, encoding="utf-8")

    if STORE.exists():
        text = STORE.read_text(encoding="utf-8")
        text = re.sub(r"## Ghi chú phát hành [^\n]+", f"## Ghi chú phát hành {version_name}", text, count=1)
        text = re.sub(r"versionCode \d+", f"versionCode {version_code}", text, count=1)
        STORE.write_text(text, encoding="utf-8")

    print(f"Bumped Play build: {current_name} ({current_code}) -> {version_name} ({version_code})")
    print("Next: review git diff, commit, then push release/play.")


if __name__ == "__main__":
    main()
