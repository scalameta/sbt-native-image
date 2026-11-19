import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CheckMetadata {
    public static void main(String[] args) throws Exception {
        String prefix = "-H:ConfigurationFileDirectories=";
        List<String> directories = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                Path path = Path.of(arg.substring(prefix.length()));
                if (!Files.readString(path.resolve("reflect-config.json")).equals("[]")) {
                    throw new AssertionError("Unexpected metadata in " + path);
                }
                directories.add(path.getParent().getFileName() + "/" + path.getFileName());
            }
        }
        List<String> expected = args[0].isEmpty() ? List.of() : Arrays.asList(args[0].split(","));
        if (!directories.equals(expected)) {
            throw new AssertionError("Expected " + expected + ", got " + directories);
        }
        Files.writeString(Path.of(args[args.length - 1]), "stub executable");
    }
}
