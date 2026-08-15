package dev.firecrawl.anydoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class PlatformTest {

    @Test
    void allSevenTargetsAreNamed() {
        assertEquals(7, Platform.all().size());
        assertEquals(
                Optional.of("x86_64-unknown-linux-gnu"),
                Platform.fromClassifier("linux-x86_64").map(Platform::rustTarget));
        assertEquals(
                Optional.of("aarch64-unknown-linux-musl"),
                Platform.fromClassifier("linux-aarch64-musl").map(Platform::rustTarget));
        assertEquals(
                "/native/linux-x86_64/libanydoc_java.so",
                Platform.fromClassifier("linux-x86_64").orElseThrow().resourcePath());
        assertEquals(
                "anydoc_java.dll",
                Platform.fromClassifier("windows-x86_64").orElseThrow().libraryFileName());
        assertEquals(
                "libanydoc_java.dylib",
                Platform.fromClassifier("macos-aarch64").orElseThrow().libraryFileName());
    }

    @Test
    void detectMatchesThisJvm() {
        Platform platform = Platform.detect();
        assertEquals(Platform.currentOs(), platform.os());
        assertEquals(Platform.currentArch(), platform.arch());
        if ("linux".equals(platform.os())) {
            assertEquals(Platform.isMusl() ? "musl" : "gnu", platform.libc());
        }
        assertFalse(NativeLoader.fileCandidates(platform).isEmpty());
    }

    @Test
    void linuxInterpreterLooksLikeADynamicLinker() {
        if (!"linux".equals(Platform.currentOs())) {
            return;
        }
        String interp = Platform.linuxInterpreter();
        assertTrue(interp == null || interp.contains("ld"), interp);
        if (interp != null) {
            assertEquals(interp.contains("musl"), Platform.isMusl(), interp);
        }
    }

    @Test
    void glibcFloorIsCentos7() {
        assertEquals("2.17", Platform.GLIBC_MIN);
    }

    @Test
    void overrideSelectsAClassifier() {
        String previous = System.getProperty("anydoc.native.classifier");
        try {
            System.setProperty("anydoc.native.classifier", "linux-x86_64-musl");
            assertEquals("linux-x86_64-musl", Platform.detect().classifier());
        } finally {
            if (previous == null) {
                System.clearProperty("anydoc.native.classifier");
            } else {
                System.setProperty("anydoc.native.classifier", previous);
            }
        }
    }

    @Test
    void cargoOutputIsOnTheSearchPath() {
        Platform linux = Platform.fromClassifier("linux-x86_64").orElseThrow();
        assertTrue(
                NativeLoader.fileCandidates(linux).stream()
                        .map(Path::toString)
                        .anyMatch(p -> p.contains("x86_64-unknown-linux-gnu")));
    }
}
