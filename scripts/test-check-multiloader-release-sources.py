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
test -f "fabric/build/libs/multigolem-${version}-fabric.jar"
test -f "fabric/build/libs/multigolem-${version}-fabric-sources.jar"
test -f "neoforge/build/libs/multigolem-${version}-neoforge.jar"
test -f "neoforge/build/libs/multigolem-${version}-neoforge-sources.jar"
gh release create "${GITHUB_REF_NAME}" \\
  "fabric/build/libs/multigolem-${version}-fabric.jar" \\
  "fabric/build/libs/multigolem-${version}-fabric-sources.jar" \\
  "neoforge/build/libs/multigolem-${version}-neoforge.jar" \\
  "neoforge/build/libs/multigolem-${version}-neoforge-sources.jar"
./scripts/upload-modrinth.ps1 -Loader "fabric" -JarPath "fabric/build/libs/multigolem-$version-fabric.jar" -SourcesJarPath "fabric/build/libs/multigolem-$version-fabric-sources.jar"
./scripts/upload-modrinth.ps1 -Loader "neoforge" -JarPath "neoforge/build/libs/multigolem-$version-neoforge.jar" -SourcesJarPath "neoforge/build/libs/multigolem-$version-neoforge-sources.jar"
./scripts/upload-curseforge.ps1 -Loader "fabric" -JarPath "fabric/build/libs/multigolem-$version-fabric.jar"
./scripts/upload-curseforge.ps1 -Loader "neoforge" -JarPath "neoforge/build/libs/multigolem-$version-neoforge.jar"
"""


VALID_MODRINTH = """
param(
    [ValidateSet("fabric", "neoforge")]
    [string] $Loader = "fabric",
    [string] $JarPath = "",
    [string] $SourcesJarPath = ""
)
$JarPath = "$Loader/build/libs/multigolem-$Version-$Loader.jar"
$SourcesJarPath = "$Loader/build/libs/multigolem-$Version-$Loader-sources.jar"
$loaders = @($Loader)
"""


VALID_CURSEFORGE = """
param(
    [ValidateSet("fabric", "neoforge")]
    [string] $Loader = "fabric",
    [string] $JarPath = ""
)
$JarPath = "$Loader/build/libs/multigolem-$Version-$Loader.jar"
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
            "neoforge/build/libs/multigolem-${version}-neoforge-sources.jar",
            "fabric/build/libs/multigolem-${version}-fabric-sources.jar",
        )

        errors = checker.check(self.write_repo(release=release))

        self.assertIn("duplicate GitHub Release sources artifact", "\n".join(errors))

    def test_fails_on_empty_curseforge_relations_projects(self):
        checker = load_checker()
        curseforge = """
param([string] $Loader = "fabric", [string] $JarPath = "")
$JarPath = "$Loader/build/libs/multigolem-$Version-$Loader.jar"
$metadata = @{
    relations = @{
        projects = @()
    }
}
"""

        errors = checker.check(self.write_repo(curseforge=curseforge))

        self.assertIn("empty CurseForge relations.projects", "\n".join(errors))

    def test_passes_on_separate_fabric_and_neoforge_upload_metadata(self):
        checker = load_checker()

        self.assertEqual([], checker.check(self.write_repo()))


if __name__ == "__main__":
    unittest.main()
