import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/** Small JDK-only helper for locating Java and packaging a fresh release, including resources. */
public final class ReleaseJar {
    private ReleaseJar() { }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("java-home")) {
            System.out.println(System.getProperty("java.home"));
            return;
        }
        if (args.length != 4) { throw new IllegalArgumentException("Usage: ReleaseJar <classes> <jar> <version> <main-class>"); }
        Path classes = Path.of(args[0]).toRealPath();
        Path jar = Path.of(args[1]).toAbsolutePath().normalize();
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, args[3]);
        attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, "Stein Food Hub");
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, args[2]);
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar, StandardOpenOption.CREATE_NEW), manifest);
                var files = Files.walk(classes)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String entry = classes.relativize(file).toString().replace('\\', '/');
                if (entry.equals("order_history.txt") || entry.startsWith("receipt-history/") || entry.endsWith(".receipt")) {
                    throw new IOException("User data must not be included in a release: " + entry);
                }
                output.putNextEntry(new JarEntry(entry));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        System.out.println("Packaged " + jar.getFileName() + " (version " + args[2] + ").");
    }
}
