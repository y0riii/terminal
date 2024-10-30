import java.io.*;
import java.util.Arrays;

public class Shell {
    // Track current directory
    private File currentDirectory = new File(System.getProperty("user.dir"));
    private OutputStream outputStream = System.out;
    private final StringBuilder outputBuilder = new StringBuilder();

    // Handle user commands
    public void handleCommand(String input) {
        outputBuilder.setLength(0);
        outputStream = System.out;
        // Split the input into commands, checking for pipes
        String[] commands = input.split("\\s*\\|\\s*", -1);

        // Process the first command
        processCommand(commands[0]);

        // Handle any piping
        for (int i = 1; i < commands.length; i++) {
            // Pass the output of the previous command as input to the next command
            processPipe(commands[i]);
        }
        printOutput();
    }

    public void printPrompt() {
        System.out.print(currentDirectory.getPath() + "> ");
    }

    String[] getArgs(String[] tokens) throws FileNotFoundException {
        int length = tokens.length;
        for (int i = 0; i < length; i++) {
            tokens[i] = tokens[i].replace("\"", "");
        }
        if (length >= 3 && (tokens[length - 2].equals(">") || tokens[length - 2].equals(">>"))) {
            File file = getFile(tokens[length - 1]);
            outputStream = new FileOutputStream(file, tokens[length - 2].equals(">>"));
            length -= 2;
        }
        return Arrays.copyOfRange(tokens, 1, length);
    }

    private void processCommand(String commandInput) {
        try {
            String[] tokens = commandInput.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String command = tokens[0];
            String[] args = getArgs(tokens);

            switch (command) {
                case "echo":
                    executeEcho(args);
                    break;

                case "ls":
                    executeLs(args);
                    break;

                case "mkdir":
                    executeMkdir(args);
                    break;

                case "rmdir":
                    executeRmdir(args);
                    break;

                case "rm":
                    executeRm(args);
                    break;

                case "cat":
                    executeCat(args);
                    break;

                case "touch":
                    executeTouch(args);
                    break;

                case "mv":
                    executeMv(args);
                    break;

                case "cd":
                    executeCd(args);
                    break;

                case "pwd":
                    executePwd(args);
                    break;

                case "grep":
                    executeGrep(args, true);
                    break;

                case "help":
                    executeHelp(args);
                    break;

                // Unrecognized command
                default:
                    System.out.println("Command not recognized: " + command);
            }
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }

    }

    private void executeEcho(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: echo <message>");
            return;
        }
        for (String arg : args) {
            outputBuilder.append(arg);
            outputBuilder.append(' ');
        }
        outputBuilder.append('\n');
    }

    private void executeLs(String[] args) {
        if (args.length == 0) {
            listFiles(false, false); // Regular 'ls'
        } else if (args.length == 1 && args[0].equals("-a")) {
            listFiles(true, false); // 'ls -a'
        } else if (args.length == 1 && args[0].equals("-r")) {
            listFiles(false, true); // 'ls -r'
        } else {
            System.out.println("Invalid usage. Supported ls commands: ls, ls -a, ls -r");
        }
    }

    // Create a directory
    private void executeMkdir(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: mkdir <directory_name>");
            return;
        }
        File dir = getFile(args[0]);
        if (dir.mkdir()) {
            System.out.println("Directory created: " + args[0]);
        } else {
            System.out.println("Failed to create directory: " + args[0]);
        }
    }

    private void executeRmdir(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: rmdir <directory_name>");
            return;
        }
        File dir = getFile(args[0]);
        if (dir.isDirectory() && dir.delete()) {
            System.out.println("Directory removed: " + args[0]);
        } else {
            System.out.println("Failed to remove directory: " + args[0]);
        }
    }

    // Remove a file
    private void executeRm(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: rm <file_name>");
            return;
        }
        File file = getFile(args[0]);
        if (file.isFile() && file.delete()) {
            System.out.println("File removed: " + args[0]);
        } else {
            System.out.println("Failed to remove file: " + args[0]);
        }
    }

    // Display file contents
    private void executeCat(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: cat <file_name>");
            return;
        }
        File file = getFile(args[0]);
        if (file.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line);
                    outputBuilder.append('\n');
                }
            } catch (IOException e) {
                outputBuilder.append("Error reading file: ");
                outputBuilder.append(e.getMessage());
                outputBuilder.append('\n');
            }
        } else {
            outputBuilder.append("File not found: ");
            outputBuilder.append(args[0]);
            outputBuilder.append('\n');
        }
    }

    // Create a file
    private void executeTouch(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: touch <file_name>");
            return;
        }
        File file = getFile(args[0]);
        try {
            if (file.createNewFile()) {
                System.out.println("File created: " + args[0]);
            } else {
                System.out.println("File already exists: " + args[0]);
            }
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }

    // Move or rename a file or directory
    private void executeMv(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: mv <source> <destination>");
            return;
        }
        try {
            File src = getFile(args[0]);
            File dest = getFile(args[1]);

            // Check if the destination is a directory
            if (dest.isDirectory()) {
                // Move the source file/directory inside the destination directory
                File newDest = new File(dest, src.getName()); // Move the file into the destination directory
                if (src.renameTo(newDest)) {
                    System.out.println("Moved " + src.getPath() + " to " + newDest.getPath());
                } else {
                    System.out.println("Failed to move " + args[0] + " to " + dest.getPath());
                }
            } else {
                // If destination is not a directory, rename the source file
                if (src.renameTo(dest)) {
                    System.out.println("Renamed " + src.getPath() + " to " + dest.getPath());
                } else {
                    System.out.println("Failed to rename " + args[0] + " to " + args[1]);
                }
            }
        } catch (Exception e) {
            System.out.println("Error moving/renaming: " + e.getMessage());
        }
    }

    // Change current working directory
    private void executeCd(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: cd <directory_name>");
            return;
        }
        try {
            File dir = getFile(args[0]);

            // Resolve the canonical path (handles ".." and ".", removes redundant
            // separators)
            File resolvedDir = dir.getCanonicalFile();

            // Check if the resolved path is a directory and exists
            if (resolvedDir.isDirectory()) {
                currentDirectory = resolvedDir;
                System.out.println("Directory changed to: " + currentDirectory.getPath());
            } else {
                System.out.println("Directory not found: " + args[0]);
            }
        } catch (IOException e) {
            System.out.println("Error changing directory: " + e.getMessage());
        }
    }

    private void executePwd(String[] args) {
        if (args.length != 0) {
            System.out.println("Usage: pwd");
            return;
        }
        outputBuilder.append(currentDirectory.getPath());
        outputBuilder.append('\n');
    }

    private void executeGrep(String[] args, boolean useCat) {
        if (args.length != 1 && !useCat) {
            System.out.println("Usage: grep <pattern>");
            return;
        }
        if (args.length != 2 && useCat) {
            System.out.println("Usage: grep <pattern> <file_name>");
            return;
        }
        if (useCat) {
            executeCat(new String[]{args[1]});
        }
        String[] lines = outputBuilder.toString().split("\n");
        outputBuilder.setLength(0);
        for (String line : lines) {
            if (line.contains(args[0])) {
                outputBuilder.append(line);
                outputBuilder.append('\n');
            }
        }
    }

    // Show help message
    private void executeHelp(String[] args) {
        if (args.length != 0) {
            System.out.println("Usage: help");
            return;
        }
        outputBuilder.append("""
                Available commands:
                  echo <message>                  Echo the message to the console
                  ls [-a] [-r]                    List files in the current directory
                  mkdir <directory_name>          Create a new directory
                  rmdir <directory_name>          Remove an empty directory
                  rm <file_name>                  Remove a file
                  cat <file_name>                 Display file contents
                  touch <file_name>               Create a new file
                  mv <source> <destination>       Move or rename a file or directory
                  cd <directory>                  Change the current directory
                  grep <pattern> <file_name>      Search for pattern in file
                  pwd                             Print the current directory
                  help                            Show this help message
                  exit                            Exit the shell
                """);
    }

    // Handle piping manually
    private void processPipe(String commandInput) {
        try {
            if (outputStream != System.out)
                return;
            String[] tokens = commandInput.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String command = tokens[0];
            String[] args = getArgs(tokens);
            switch (command) {
                case "grep":
                    executeGrep(args, false);
                    break;
                case "TODO LATER":
                default:
                    System.out.println("Command not recognized after pipe: " + command);
            }

        } catch (Exception e) {
            System.out.println("Error in piping commands: " + e.getMessage());
        }
    }

    // List files in the current directory
    private void listFiles(boolean showHidden, boolean recursive) {
        File[] files = currentDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!showHidden && file.isHidden()) {
                    continue;
                }
                outputBuilder.append(file.getName());
                outputBuilder.append('\n');
                if (recursive && file.isDirectory()) {
                    listFilesRecursive(file, showHidden);
                }
            }
        } else {
            System.out.println("Unable to list files.");
        }
    }

    // Recursive function to list files in subdirectories
    private void listFilesRecursive(File dir, boolean showHidden) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!showHidden && file.isHidden()) {
                    continue;
                }
                outputBuilder.append(dir.getPath());
                outputBuilder.append('\\');
                outputBuilder.append(file.getName());
                outputBuilder.append('\n');
                if (file.isDirectory()) {
                    listFilesRecursive(file, showHidden);
                }
            }
        }
    }

    private File getFile(String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(currentDirectory, path);
        }
        return file;
    }

    private void printOutput() {
        if (outputStream == System.out) {
            System.out.print(outputBuilder);
            return;
        }
        try (PrintStream printStream = new PrintStream(outputStream)) {
            printStream.print(outputBuilder);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }
}
