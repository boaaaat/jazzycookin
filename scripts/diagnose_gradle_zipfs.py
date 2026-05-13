#!/usr/bin/env python3
"""Diagnose Java ZIP filesystem access in this Gradle sandbox."""

from __future__ import annotations

import subprocess
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_JAR = (
    ROOT
    / ".gradle-user/caches/modules-2/files-2.1/com.mojang/text2speech/1.17.9"
    / "3cad216e3a7f0c19b4b394388bc9ffc446f13b14/text2speech-1.17.9.jar"
)
PROBE_DIR = ROOT / "build/diagnostics/zipfs_probe"
PROBE_SOURCE = PROBE_DIR / "ZipFsProbe.java"


def main() -> int:
    jar = DEFAULT_JAR
    if not jar.exists():
        candidates = sorted(
            (ROOT / ".gradle-user/caches/modules-2/files-2.1").glob("**/text2speech-1.17.9.jar")
        )
        if candidates:
            jar = candidates[0]

    PROBE_DIR.mkdir(parents=True, exist_ok=True)
    PROBE_SOURCE.write_text(probe_source(), encoding="utf-8")

    modlauncher = first_existing("**/modlauncher-11.0.5.jar")
    bootstraplauncher = first_existing("**/bootstraplauncher-2.0.2.jar")
    probe_jars = [path for path in (jar, modlauncher, bootstraplauncher) if path is not None]

    print(f"Probe jar: {jar}")
    print(f"Jar readable by Python: {jar.exists() and jar.stat().st_size > 0}")
    result = subprocess.run(
        [java_command(), str(PROBE_SOURCE), *[str(path) for path in probe_jars]],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    print(result.stdout.rstrip())
    if "ZIPFS_CLOSE_ACCESS_DENIED" in result.stdout or "REALPATH_ACCESS_DENIED" in result.stdout:
        print("Detected Java path/ZIP filesystem access failure outside Gradle.")
        return 2
    return result.returncode


def first_existing(pattern: str) -> Path | None:
    cache = ROOT / ".gradle-user/caches/modules-2/files-2.1"
    if not cache.exists():
        return None
    candidates = sorted(cache.glob(pattern))
    return candidates[0] if candidates else None


def java_command() -> str:
    gradle_properties = ROOT / "gradle.properties"
    if gradle_properties.exists():
        for line in gradle_properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("org.gradle.java.home="):
                java_home = Path(line.split("=", 1)[1].strip())
                java_exe = java_home / "bin/java.exe"
                if java_exe.exists():
                    return str(java_exe)
    return "java"


def probe_source() -> str:
    return textwrap.dedent(
        """
        import java.net.URI;
        import java.nio.file.Files;
        import java.nio.file.FileSystem;
        import java.nio.file.FileSystems;
        import java.nio.file.Path;
        import java.util.Map;

        public class ZipFsProbe {
            public static void main(String[] args) throws Exception {
                Path repo = Path.of(".").toAbsolutePath().normalize();
                System.out.println("java.version=" + System.getProperty("java.version"));
                checkRealPath("repo", repo);
                checkRealPath("repo.build", repo.resolve("build"));
                checkRealPath("temp", Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize());
                checkRealPath("programData", Path.of("C:/ProgramData").toAbsolutePath().normalize());
                for (int index = 0; index < args.length; index++) {
                    Path jar = Path.of(args[index]).toAbsolutePath().normalize();
                    String label = "jar" + index;
                    System.out.println(label + ".path=" + jar);
                    System.out.println(label + ".exists=" + Files.exists(jar));
                    checkRealPath(label, jar);
                    checkZipFs(label, jar);
                }
            }

            private static void checkZipFs(String label, Path jar) {
                try {
                    FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toUri()), Map.of());
                    try {
                        System.out.println(label + ".ZIPFS_OPEN_OK");
                    } finally {
                        fs.close();
                        System.out.println(label + ".ZIPFS_CLOSE_OK");
                    }
                } catch (java.nio.file.AccessDeniedException denied) {
                    System.out.println(label + ".ZIPFS_CLOSE_ACCESS_DENIED " + denied.getFile());
                } catch (Throwable throwable) {
                    System.out.println(label + ".ZIPFS_ERROR " + throwable.getClass().getName() + ": " + throwable.getMessage());
                    throwable.printStackTrace(System.out);
                }
            }

            private static void checkRealPath(String label, Path path) {
                try {
                    System.out.println(label + ".realpath=" + path.toRealPath());
                } catch (java.nio.file.AccessDeniedException denied) {
                    System.out.println(label + ".REALPATH_ACCESS_DENIED " + denied.getFile());
                } catch (Throwable throwable) {
                    System.out.println(label + ".REALPATH_ERROR " + throwable.getClass().getName() + ": " + throwable.getMessage());
                }
            }
        }
        """
    ).strip() + "\n"


if __name__ == "__main__":
    raise SystemExit(main())
