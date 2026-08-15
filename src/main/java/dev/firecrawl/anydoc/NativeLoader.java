package dev.firecrawl.anydoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@code libanydoc_java} for the running {@link Platform}. Search order: {@code
 * anydoc.native.path}, {@code java.library.path}, staged {@code native/dist}, cargo output,
 * then the matching {@code /native/{classifier}/} resource inside the JAR.
 */
final class NativeLoader {

    private static volatile boolean loaded;

    private NativeLoader() {}

    static void load() {
        if (loaded) {
            return;
        }
        synchronized (NativeLoader.class) {
            if (loaded) {
                return;
            }
            doLoad();
            loaded = true;
        }
    }

    private static void doLoad() {
        String explicit = System.getProperty("anydoc.native.path");
        if (explicit != null && !Strings.isBlank(explicit)) {
            System.load(Paths.get(explicit).toAbsolutePath().toString());
            return;
        }

        Platform platform = Platform.detect();
        UnsatisfiedLinkError loadLibrary = null;
        try {
            System.loadLibrary("anydoc_java");
            return;
        } catch (UnsatisfiedLinkError error) {
            loadLibrary = error;
        }

        for (Path candidate : fileCandidates(platform)) {
            if (Files.isRegularFile(candidate)) {
                System.load(candidate.toAbsolutePath().toString());
                return;
            }
        }

        if (extractFromJar(platform)) {
            return;
        }

        UnsatisfiedLinkError error =
                new UnsatisfiedLinkError(
                        "Could not load "
                                + platform.libraryFileName()
                                + " for "
                                + platform.classifier()
                                + ". Build it with `scripts/build-native.sh` (Zig-linked against glibc "
                                + Platform.GLIBC_MIN
                                + " on Linux), or set -Danydoc.native.path=/path/to/"
                                + platform.libraryFileName()
                                + ". Supported: "
                                + Platform.classifiers()
                                + ".");
        if (loadLibrary != null) {
            error.addSuppressed(loadLibrary);
        }
        throw error;
    }

    static List<Path> fileCandidates(Platform platform) {
        String fileName = platform.libraryFileName();
        Path cwd = Paths.get(System.getProperty("user.dir", "."));
        List<Path> roots = new ArrayList<Path>();
        roots.add(cwd);
        roots.add(cwd.resolve("native"));
        Path parent = cwd.getParent();
        if (parent != null) {
            roots.add(parent);
            roots.add(parent.resolve("native"));
        }
        String dir = System.getProperty("anydoc.native.dir");
        if (dir != null && !Strings.isBlank(dir)) {
            roots.add(Paths.get(dir));
        }
        List<Path> candidates = new ArrayList<>();
        for (Path root : roots) {
            candidates.add(root.resolve("dist").resolve(platform.classifier()).resolve(fileName));
            candidates.add(root.resolve(platform.classifier()).resolve(fileName));
            candidates.add(root.resolve("target").resolve(platform.rustTarget()).resolve("release").resolve(fileName));
            candidates.add(root.resolve("target").resolve(platform.rustTarget()).resolve("debug").resolve(fileName));
            candidates.add(root.resolve("target").resolve("release").resolve(fileName));
            candidates.add(root.resolve("target").resolve("debug").resolve(fileName));
            candidates.add(root.resolve(fileName));
        }
        return candidates;
    }

    private static boolean extractFromJar(Platform platform) {
        String resource = platform.resourcePath();
        try (InputStream in = NativeLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                return false;
            }
            Path tmp = Files.createTempFile("anydoc_java-", suffix(platform.libraryFileName()));
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            System.load(tmp.toAbsolutePath().toString());
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract " + resource, e);
        }
    }

    private static String suffix(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
