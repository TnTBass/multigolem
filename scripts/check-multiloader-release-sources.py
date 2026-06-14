#!/usr/bin/env python3
import argparse
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_RELEASE_MARKERS = {
    "fabric primary jar": "fabric/build/libs/multigolem-fabric-${version}.jar",
    "fabric sources jar": "fabric/build/libs/multigolem-fabric-${version}-sources.jar",
    "neoforge primary jar": "neoforge/build/libs/multigolem-neoforge-${version}.jar",
    "neoforge sources jar": "neoforge/build/libs/multigolem-neoforge-${version}-sources.jar",
}

UNSUFFIXED_ARTIFACT_PATTERNS = [
    r"build/libs/multigolem-\$\{version\}\.jar",
    r"build/libs/multigolem-\$version\.jar",
    r"build/libs/multigolem-\$\{version\}-sources\.jar",
    r"build/libs/multigolem-\$version-sources\.jar",
    r"build/libs/multigolem-\$\{version\}-(fabric|neoforge)\.jar",
    r"build/libs/multigolem-\$version-(fabric|neoforge)\.jar",
    r"build/libs/multigolem-\$\{version\}-(fabric|neoforge)-sources\.jar",
    r"build/libs/multigolem-\$version-(fabric|neoforge)-sources\.jar",
]


def read(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def check(root: Path = ROOT) -> list[str]:
    release = read(root / ".github/workflows/release.yml")
    modrinth = read(root / "scripts/upload-modrinth.ps1")
    curseforge = read(root / "scripts/upload-curseforge.ps1")
    errors: list[str] = []

    if not release:
        errors.append(".github/workflows/release.yml is missing.")
    if not modrinth:
        errors.append("scripts/upload-modrinth.ps1 is missing.")
    if not curseforge:
        errors.append("scripts/upload-curseforge.ps1 is missing.")
    if errors:
        return errors

    for label, marker in REQUIRED_RELEASE_MARKERS.items():
        if marker not in release:
            errors.append(f"release workflow must verify/upload {label}: {marker}")

    for pattern in UNSUFFIXED_ARTIFACT_PATTERNS:
        if re.search(pattern, release):
            errors.append("release workflow still references an unsuffixed multigolem artifact.")
            break

    release_asset_block = github_release_asset_block(release)
    sources_assets = re.findall(r'"([^"]*multigolem-[^"]*\$\{version\}[^"]*-sources\.jar)"', release_asset_block)
    duplicate_sources = sorted({asset for asset in sources_assets if sources_assets.count(asset) > 1})
    for asset in duplicate_sources:
        errors.append(f"duplicate GitHub Release sources artifact: {asset}")

    for loader in ("fabric", "neoforge"):
        if f'-Loader "{loader}"' not in release and f"-Loader '{loader}'" not in release:
            errors.append(f"release workflow must call upload scripts with -Loader \"{loader}\".")
        if f"{loader}/build/libs/multigolem-{loader}-$version.jar" not in release:
            errors.append(f"release workflow must pass explicit {loader} jar path to upload scripts.")

    if "$Loader" not in modrinth:
        errors.append("Modrinth upload script must accept a loader argument.")
    if "fabric/build/libs" not in modrinth and "$Loader/build/libs/multigolem-$Loader-$Version.jar" not in modrinth:
        errors.append("Modrinth upload script must support loader-suffixed primary jar paths.")
    if "neoforge/build/libs" not in modrinth and "$Loader/build/libs/multigolem-$Loader-$Version-sources.jar" not in modrinth:
        errors.append("Modrinth upload script must support loader-suffixed sources jar paths.")

    if "$Loader" not in curseforge:
        errors.append("CurseForge upload script must accept a loader argument.")
    if "fabric/build/libs" not in curseforge and "$Loader/build/libs/multigolem-$Loader-$Version.jar" not in curseforge:
        errors.append("CurseForge upload script must support loader-suffixed primary jar paths.")
    if re.search(r"relations\s*=\s*@\{\s*projects\s*=\s*@\(\s*\)\s*\}", curseforge, re.DOTALL):
        errors.append("CurseForge upload script must omit empty CurseForge relations.projects.")
    if "$relationsProjects.Count -gt 0" not in curseforge or '["relations"]' not in curseforge:
        errors.append("CurseForge upload script must add relations only when relation projects exist.")

    return errors


def github_release_asset_block(release: str) -> str:
    lines = release.splitlines()
    for index, line in enumerate(lines):
        if "gh release create" not in line:
            continue
        block = [line]
        for following in lines[index + 1:]:
            stripped = following.strip()
            if stripped.startswith("./scripts/upload-") or stripped.startswith("- name: "):
                break
            block.append(following)
        return "\n".join(block)
    return ""


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args(argv)

    errors = check(args.root)
    if errors:
        raise SystemExit("Multiloader release source check failed: " + "; ".join(errors))

    print("Multiloader release source check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
