package dev.firecrawl.anydoc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A supported native target: OS, CPU, and C library. Classifiers match the directory under {@code
 * /native/} inside the JAR and the Rust triple used to build the library.
 */
public final class Platform {

    public static final String LINUX_X86_64 = "linux-x86_64";
    public static final String LINUX_AARCH64 = "linux-aarch64";
    public static final String LINUX_X86_64_MUSL = "linux-x86_64-musl";
    public static final String LINUX_AARCH64_MUSL = "linux-aarch64-musl";
    public static final String MACOS_X86_64 = "macos-x86_64";
    public static final String MACOS_AARCH64 = "macos-aarch64";
    public static final String WINDOWS_X86_64 = "windows-x86_64";

    /** Linux glibc builds are linked against 2.17 (CentOS 7 / manylinux2014). */
    public static final String GLIBC_MIN = "2.17";

    private static final List<Platform> ALL =
            List.of(
                    new Platform(LINUX_X86_64, "x86_64-unknown-linux-gnu", "linux", "x86_64", "gnu"),
                    new Platform(LINUX_AARCH64, "aarch64-unknown-linux-gnu", "linux", "aarch64", "gnu"),
                    new Platform(
                            LINUX_X86_64_MUSL, "x86_64-unknown-linux-musl", "linux", "x86_64", "musl"),
                    new Platform(
                            LINUX_AARCH64_MUSL, "aarch64-unknown-linux-musl", "linux", "aarch64", "musl"),
                    new Platform(MACOS_X86_64, "x86_64-apple-darwin", "macos", "x86_64", ""),
                    new Platform(MACOS_AARCH64, "aarch64-apple-darwin", "macos", "aarch64", ""),
                    new Platform(WINDOWS_X86_64, "x86_64-pc-windows-msvc", "windows", "x86_64", ""));

    private final String classifier;
    private final String rustTarget;
    private final String os;
    private final String arch;
    private final String libc;

    private Platform(String classifier, String rustTarget, String os, String arch, String libc) {
        this.classifier = classifier;
        this.rustTarget = rustTarget;
        this.os = os;
        this.arch = arch;
        this.libc = libc;
    }

    /** Directory name under {@code /native/} and the Maven classifier. */
    public String classifier() {
        return classifier;
    }

    /** Rust target triple, e.g. {@code x86_64-unknown-linux-gnu}. */
    public String rustTarget() {
        return rustTarget;
    }

    /** {@code linux}, {@code macos}, or {@code windows}. */
    public String os() {
        return os;
    }

    /** {@code x86_64} or {@code aarch64}. */
    public String arch() {
        return arch;
    }

    /** {@code gnu}, {@code musl}, or empty when the OS has one C library. */
    public String libc() {
        return libc;
    }

    /** File name of the JNI library on this platform. */
    public String libraryFileName() {
        return switch (os) {
            case "windows" -> "anydoc_java.dll";
            case "macos" -> "libanydoc_java.dylib";
            default -> "libanydoc_java.so";
        };
    }

    /** Classpath resource that holds the JNI library, e.g. {@code /native/linux-x86_64/libanydoc_java.so}. */
    public String resourcePath() {
        return "/native/" + classifier + "/" + libraryFileName();
    }

    public static List<Platform> all() {
        return ALL;
    }

    public static Optional<Platform> fromClassifier(String classifier) {
        if (classifier == null || classifier.isBlank()) {
            return Optional.empty();
        }
        return ALL.stream().filter(p -> p.classifier.equals(classifier)).findFirst();
    }

    /**
     * Detect the running JVM's platform. Override with {@code -Danydoc.native.classifier=...} when
     * the automatic guess is wrong (rare: a musl JVM that still looks like glibc).
     */
    public static Platform detect() {
        String override = System.getProperty("anydoc.native.classifier");
        if (override != null && !override.isBlank()) {
            return fromClassifier(override)
                    .orElseThrow(
                            () ->
                                    new UnsatisfiedLinkError(
                                            "Unknown anydoc.native.classifier '"
                                                    + override
                                                    + "'; expected one of "
                                                    + classifiers()));
        }
        String os = currentOs();
        String arch = currentArch();
        String libc = "linux".equals(os) && isMusl() ? "musl" : "linux".equals(os) ? "gnu" : "";
        return ALL.stream()
                .filter(p -> p.os.equals(os) && p.arch.equals(arch) && p.libc.equals(libc))
                .findFirst()
                .orElseThrow(
                        () ->
                                new UnsatisfiedLinkError(
                                        "Unsupported platform "
                                                + System.getProperty("os.name")
                                                + " / "
                                                + System.getProperty("os.arch")
                                                + " (detected "
                                                + os
                                                + "-"
                                                + arch
                                                + (libc.isEmpty() ? "" : "-" + libc)
                                                + "). Supported: "
                                                + classifiers()));
    }

    static String classifiers() {
        return String.join(", ", ALL.stream().map(Platform::classifier).toList());
    }

    static String currentOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            return "linux";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "macos";
        }
        if (os.contains("win")) {
            return "windows";
        }
        return os.replace(' ', '_');
    }

    static String currentArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return switch (arch) {
            case "amd64", "x86_64", "x64" -> "x86_64";
            case "aarch64", "arm64" -> "aarch64";
            default -> arch;
        };
    }

    /**
     * True when this process is linked against musl. Reads the ELF interpreter of {@code
     * /proc/self/exe}; falls back to looking for {@code ld-musl-*}.
     */
    static boolean isMusl() {
        String interp = linuxInterpreter();
        if (interp != null) {
            return interp.contains("musl");
        }
        String arch = currentArch();
        return Files.isRegularFile(Path.of("/lib/ld-musl-" + arch + ".so.1"));
    }

    static String linuxInterpreter() {
        Path exe = Path.of("/proc/self/exe");
        if (!Files.isRegularFile(exe) && !Files.isSymbolicLink(exe)) {
            return null;
        }
        try (SeekableByteChannel ch = Files.newByteChannel(exe)) {
            ByteBuffer ident = ByteBuffer.allocate(16);
            if (ch.read(ident) != 16) {
                return null;
            }
            ident.flip();
            if (ident.get() != 0x7f || ident.get() != 'E' || ident.get() != 'L' || ident.get() != 'F') {
                return null;
            }
            int eiClass = ident.get() & 0xff; // 1=32, 2=64
            int eiData = ident.get() & 0xff; // 1=LE, 2=BE
            ByteOrder order = eiData == 2 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            boolean elf64 = eiClass == 2;
            ch.position(0);
            ByteBuffer header = ByteBuffer.allocate(elf64 ? 64 : 52).order(order);
            if (ch.read(header) != header.capacity()) {
                return null;
            }
            header.flip();
            long phoff = elf64 ? header.getLong(32) : Integer.toUnsignedLong(header.getInt(28));
            int phentsize = Short.toUnsignedInt(header.getShort(elf64 ? 54 : 42));
            int phnum = Short.toUnsignedInt(header.getShort(elf64 ? 56 : 44));
            ByteBuffer ph = ByteBuffer.allocate(phentsize).order(order);
            for (int i = 0; i < phnum; i++) {
                ph.clear();
                ch.position(phoff + (long) i * phentsize);
                if (ch.read(ph) != phentsize) {
                    return null;
                }
                ph.flip();
                int type = ph.getInt(0);
                if (type != 3) { // PT_INTERP
                    continue;
                }
                long offset = elf64 ? ph.getLong(8) : Integer.toUnsignedLong(ph.getInt(4));
                long filesz = elf64 ? ph.getLong(32) : Integer.toUnsignedLong(ph.getInt(16));
                if (filesz <= 1 || filesz > 4096) {
                    return null;
                }
                ByteBuffer buf = ByteBuffer.allocate((int) filesz);
                ch.position(offset);
                if (ch.read(buf) != filesz) {
                    return null;
                }
                byte[] raw = buf.array();
                int end = raw.length;
                while (end > 0 && raw[end - 1] == 0) {
                    end--;
                }
                return new String(raw, 0, end);
            }
            return null;
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Platform p && classifier.equals(p.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classifier);
    }

    @Override
    public String toString() {
        return classifier;
    }
}
