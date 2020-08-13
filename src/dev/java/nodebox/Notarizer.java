package nodebox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Notarizer {
    private static final int SECONDS = 1000;
    private static final Pattern statusPattern = Pattern.compile("^\\s*([\\w\\s]+):\\s(.*)$");

    static class NotarizationStatus {
        String status;
        HashMap<String, String> properties;

        public NotarizationStatus(String rawText) {
            properties = new HashMap<>();
            String[] lines = rawText.split("\\n");
            for (String line : lines) {
                Matcher m = statusPattern.matcher(line);
                if (m.matches()) {
                    String key = m.group(1);
                    String value = m.group(2);
                    if (key.equals("Status")) {
                        status = value;
                    }
                    properties.put(key, value);
                }
            }
        }

        @Override
        public String toString() {
            return "NotarizationStatus{" +
                    "status='" + status + '\'' +
                    ", properties=" + properties +
                    '}';
        }
    }

    private static String runShellCommand(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder().command(command).start();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        process.waitFor();
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append('\n');
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        File dmgFile = new File(args[0]);
        if (!dmgFile.exists()) {
            System.out.println("File " + args[0] + " does not exist.");
            return;

        }
        String requestUUID = startNotarization(dmgFile);
        boolean success = waitForNotarization(requestUUID);
        if (success) {
            stapleNotarization(dmgFile);
        }
    }

    private static String startNotarization(File dmgFile) {
//        ArrayList<String> command = new ArrayList<>();
//        command.add("cat");
//        command.add("_notarize_output.txt");
        ArrayList<String> command = new ArrayList<>();
        command.add("xcrun");
        command.add("altool");
        command.add("--notarize-app");
        command.add("--primary-bundle-id");
        command.add("be.emrg.nodebox");
        command.add("--username");
        command.add("frederik@debleser.be");
        command.add("--password");
        command.add("@keychain:Developer-altool");
        command.add("--file");
        command.add(dmgFile.getAbsolutePath());
        try {
            String result = runShellCommand(command);
            Pattern r = Pattern.compile("RequestUUID\\s+=\\s+([0-9a-f\\-]+)");
            Matcher m = r.matcher(result);
            if (m.find()) {
                return m.group(1);
            } else {
                System.out.println("Could not find RequestUUID in return value");
                System.out.println(result);
                throw new RuntimeException("Could not find RequestUUID in return value.");
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception when running xcrun altool --notarize-app.", e);
        }
    }

    private static boolean waitForNotarization(String requestUUID) {
        NotarizationStatus status;
        do {
            status = getNotarizationStatus(requestUUID);
            System.out.println(status);
            if (!status.status.equals("in progress")) {
                break;
            }
            try {
                Thread.sleep(30 * SECONDS);
            } catch (InterruptedException ignored) {
            }
        } while (true);

        if (status.status.equals("success")) {
            return true;
        } else if (status.status.equals("invalid")) {
            System.out.println("Notarization came back as invalid. Check the log files at:");
            System.out.println(status.properties.get("LogFileURL"));
            return false;
        } else {
            System.out.println(status);
            return false;
        }
    }

    private static NotarizationStatus getNotarizationStatus(String requestUUID) {
        ArrayList<String> command = new ArrayList<>();
//        command.add("cat");
//        command.add("_notarize_in_progress.txt");

        command.add("xcrun");
        command.add("altool");

        command.add("--username");
        command.add("frederik@debleser.be");
        command.add("--password");
        command.add("@keychain:Developer-altool");
        command.add("--notarization-info");
        command.add(requestUUID);

        try {
            String result = runShellCommand(command);
            return new NotarizationStatus(result);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception when running xcrun altool --notarization-info.", e);
        }
    }

    private static void stapleNotarization(File dmgFile) {
        ArrayList<String> command = new ArrayList<>();
//        command.add("cat");
//        command.add("_notarize_staple.txt");
        command.add("xcrun");
        command.add("stapler");
        command.add("staple");
        command.add(dmgFile.getAbsolutePath());

        try {
            String result = runShellCommand(command);
            System.out.println(result);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception when running xcrun stapler staple.", e);
        }
    }


}