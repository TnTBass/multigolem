import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("check-multiloader-release-sources.py")


def load_checker():
    spec = importlib.util.spec_from_file_location("check_multiloader_release_sources", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


VALID_RELEASE = """
version="${GITHUB_REF_NAME#v}"
test -f "fabric/build/libs/multigolem-fabric-${version}.jar"
test -f "fabric/build/libs/multigolem-fabric-${version}-sources.jar"
test -f "neoforge/build/libs/multigolem-neoforge-${version}.jar"
test -f "neoforge/build/libs/multigolem-neoforge-${version}-sources.jar"
gh release create "${GITHUB_REF_NAME}" \\
  "fabric/build/libs/multigolem-fabric-${version}.jar" \\
  "fabric/build/libs/multigolem-fabric-${version}-sources.jar" \\
  "neoforge/build/libs/multigolem-neoforge-${version}.jar" \\
  "neoforge/build/libs/multigolem-neoforge-${version}-sources.jar"
./scripts/upload-modrinth.ps1 -Loader "fabric" -JarPath "fabric/build/libs/multigolem-fabric-$version.jar" -SourcesJarPath "fabric/build/libs/multigolem-fabric-$version-sources.jar"
./scripts/upload-modrinth.ps1 -Loader "neoforge" -JarPath "neoforge/build/libs/multigolem-neoforge-$version.jar" -SourcesJarPath "neoforge/build/libs/multigolem-neoforge-$version-sources.jar"
./scripts/upload-curseforge.ps1 -Loader "fabric" -JarPath "fabric/build/libs/multigolem-fabric-$version.jar"
./scripts/upload-curseforge.ps1 -Loader "neoforge" -JarPath "neoforge/build/libs/multigolem-neoforge-$version.jar"
"""

VALID_MARKETPLACE_RELEASE = VALID_RELEASE.replace(
    ' -SourcesJarPath "fabric/build/libs/multigolem-fabric-$version-sources.jar"',
    "",
).replace(
    ' -SourcesJarPath "neoforge/build/libs/multigolem-neoforge-$version-sources.jar"',
    "",
)


VALID_MODRINTH = """
param(
    [ValidateSet("fabric", "neoforge")]
    [string] $Loader = "fabric",
    [string] $JarPath = ""
)
$JarPath = "$Loader/build/libs/multigolem-$Loader-$Version.jar"
$loaders = @($Loader)
file_parts = @("file")
"""


VALID_CURSEFORGE = """
param(
    [ValidateSet("fabric", "neoforge")]
    [string] $Loader = "fabric",
    [string] $JarPath = ""
)
$JarPath = "$Loader/build/libs/multigolem-$Loader-$Version.jar"
$relationsProjects = @()
if ($Loader -eq "fabric") {
    $relationsProjects += @{ slug = "fabric-api"; type = "requiredDependency" }
}
if ($relationsProjects.Count -gt 0) {
    $metadata["relations"] = @{ projects = $relationsProjects }
}
"""


class CheckMultiloaderReleaseSourcesTest(unittest.TestCase):
    def write_repo(self, release=VALID_RELEASE, modrinth=VALID_MODRINTH, curseforge=VALID_CURSEFORGE):
        temp = tempfile.TemporaryDirectory()
        root = Path(temp.name)
        (root / ".github/workflows").mkdir(parents=True)
        (root / "scripts").mkdir()
        (root / ".github/workflows/release.yml").write_text(release, encoding="utf-8")
        (root / "scripts/upload-modrinth.ps1").write_text(modrinth, encoding="utf-8")
        (root / "scripts/upload-curseforge.ps1").write_text(curseforge, encoding="utf-8")
        self.addCleanup(temp.cleanup)
        return root

    def test_fails_on_unsuffixed_multigolem_jar_only_workflow(self):
        checker = load_checker()
        release = """
version="${GITHUB_REF_NAME#v}"
test -f "build/libs/multigolem-${version}.jar"
test -f "build/libs/multigolem-${version}-sources.jar"
gh release create "${GITHUB_REF_NAME}" "build/libs/multigolem-${version}.jar" "build/libs/multigolem-${version}-sources.jar"
"""

        errors = checker.check(self.write_repo(release=release))

        self.assertIn("fabric primary jar", "\n".join(errors))
        self.assertIn("unsuffixed", "\n".join(errors))

    def test_fails_on_duplicate_sources_jar_names(self):
        checker = load_checker()
        release = VALID_RELEASE.replace(
            "neoforge/build/libs/multigolem-neoforge-${version}-sources.jar",
            "fabric/build/libs/multigolem-fabric-${version}-sources.jar",
        )

        errors = checker.check(self.write_repo(release=release))

        self.assertIn("duplicate GitHub Release sources artifact", "\n".join(errors))

    def test_fails_on_empty_curseforge_relations_projects(self):
        checker = load_checker()
        curseforge = """
param([string] $Loader = "fabric", [string] $JarPath = "")
$JarPath = "$Loader/build/libs/multigolem-$Loader-$Version.jar"
$relationsProjects = @()
if ($relationsProjects.Count -gt 0) {
    $metadata["relations"] = @{ projects = $relationsProjects }
}
$metadata = @{
    relations = @{
        projects = @()
    }
}
"""

        errors = checker.check(self.write_repo(curseforge=curseforge))

        self.assertIn("empty CurseForge relations.projects", "\n".join(errors))

    def test_fails_when_modrinth_script_missing_loader_param(self):
        checker = load_checker()
        modrinth = """
param(
    [string] $JarPath = "",
    [string] $SourcesJarPath = ""
)
$JarPath = "fabric/build/libs/multigolem-$Version-fabric.jar"
$SourcesJarPath = "fabric/build/libs/multigolem-$Version-fabric-sources.jar"
"""

        errors = checker.check(self.write_repo(modrinth=modrinth))

        self.assertIn("Modrinth upload script must accept a loader argument", "\n".join(errors))

    def test_fails_when_marketplace_uploads_include_sources_jars(self):
        checker = load_checker()

        errors = checker.check(self.write_repo())

        self.assertIn("marketplace upload calls must not include sources jars", "\n".join(errors))

    def test_fails_when_modrinth_upload_declares_sources_file_part(self):
        checker = load_checker()
        modrinth = VALID_MODRINTH.replace(
            'file_parts = @("file")',
            'file_parts = @("file", "sources")',
        )

        errors = checker.check(self.write_repo(release=VALID_MARKETPLACE_RELEASE, modrinth=modrinth))

        self.assertIn("Modrinth upload script must not attach sources jars", "\n".join(errors))

    def test_fails_when_modrinth_upload_declares_reordered_sources_file_part(self):
        checker = load_checker()
        modrinth = VALID_MODRINTH.replace(
            'file_parts = @("file")',
            'file_parts = @("sources", "file")',
        )

        errors = checker.check(self.write_repo(release=VALID_MARKETPLACE_RELEASE, modrinth=modrinth))

        self.assertIn("Modrinth upload script must not attach sources jars", "\n".join(errors))

    def test_passes_on_separate_fabric_and_neoforge_upload_metadata(self):
        checker = load_checker()

        self.assertEqual([], checker.check(self.write_repo(release=VALID_MARKETPLACE_RELEASE)))


if __name__ == "__main__":
    unittest.main()
