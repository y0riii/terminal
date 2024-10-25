import java.io.*;

public class Shell {
    // Track current directory
    private File currentDirectory = new File(System.getProperty("user.dir"));
    StringBuilder outputBuilder = new StringBuilder();
    private OutputStream outputStream = System.out;

    // Handle user commands
    public void handleCommand(String input) {
        outputBuilder.setLength(0);
        outputStream = System.out;
        // Split the input into commands, checking for pipes
        String[] commands = input.split("\\s*\\|\\s*");

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
        System.out.print(currentDirectory + "> ");
    }

    private void processCommand(String commandInput) {
        String[] tokens = commandInput.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        int length = tokens.length;

        try {
            if (length >= 3 && (tokens[length - 2].equals(">") || tokens[length - 2].equals(">>"))) {
                File file = new File(currentDirectory, tokens[length - 1]);
                outputStream = new FileOutputStream(file, tokens[length - 2].equals(">>"));
                length -= 2;
            }

            // Use regex to split by spaces while respecting quoted strings
            String command = tokens[0];
            switch (command) {
                case "echo":
                    if (length < 2) {
                        System.out.println("Usage: echo <message>");
                        return;
                    }
                    for (int i = 1; i < length; i++) {
                        outputBuilder.append(tokens[i].replace("\"", ""));
                        outputBuilder.append(' ');
                    }
                    outputBuilder.append('\n');
                    break;
                // Handle 'ls' command (only allow ls, ls -a, ls -r)
                case "ls":
                    if (length == 1) {
                        listFiles(false, false); // Regular 'ls'
                    } else if (length == 2 && tokens[1].equals("-a")) {
                        listFiles(true, false);  // 'ls -a'
                    } else if (length == 2 && tokens[1].equals("-r")) {
                        listFiles(false, true);  // 'ls -r'
                    } else {
                        System.out.println("Invalid usage. Supported ls commands: ls, ls -a, ls -r");
                        return;
                    }
                    break;

                // Handle 'mkdir' command
                case "mkdir":
                    if (length == 2) {
                        makeDirectory(tokens[1].replace("\"", "")); // Handle quoted directory names
                    } else {
                        System.out.println("Usage: mkdir <directory_name>");
                    }
                    return;

                // Handle 'rmdir' command
                case "rmdir":
                    if (length == 2) {
                        removeDirectory(tokens[1].replace("\"", "")); // Handle quoted directory names
                    } else {
                        System.out.println("Usage: rmdir <directory_name>");
                    }
                    return;

                // Handle 'rm' command
                case "rm":
                    if (length == 2) {
                        removeFile(tokens[1].replace("\"", "")); // Handle quoted file names
                    } else {
                        System.out.println("Usage: rm <file_name>");
                    }
                    return;
                // Handle 'cat' command
                case "cat":
                    if (length == 2) {
                        catFile(tokens[1].replace("\"", "")); // Handle quoted file names
                    } else {
                        System.out.println("Usage: cat <file_name>");
                        return;
                    }
                    break;

                // Handle 'touch' command
                case "touch":
                    if (length == 2) {
                        createFile(tokens[1].replace("\"", "")); // Handle quoted file names
                    } else {
                        System.out.println("Usage: touch <file_name>");
                    }
                    return;

                // Handle 'mv' command
                case "mv":
                    if (length == 3) {
                        moveFileOrDirectory(tokens[1].replace("\"", ""), tokens[2].replace("\"", "")); // Handle quoted file names
                    } else {
                        System.out.println("Usage: mv <source> <destination>");
                    }
                    return;

                // Handle 'cd' command (with drive change)
                case "cd":
                    if (length != 2) {
                        System.out.println("Usage: cd <directory>");
                        return;
                    }

                    // Check if the input is a drive change (e.g., E:)
                    if (tokens[1].length() == 2 && Character.isLetter(tokens[1].charAt(0)) && tokens[1].charAt(1) == ':') {
                        // Change to the specified drive
                        File newDrive = new File(tokens[1] + "\\"); // For Windows paths
                        if (newDrive.exists() && newDrive.isDirectory()) {
                            currentDirectory = newDrive;
                            System.out.println("Changed to drive " + tokens[1]);
                        } else {
                            System.out.println("Drive not found: " + tokens[1]);
                        }
                        return;
                    } else {
                        // Handle regular directory change, including folder names with spaces
                        changeDirectory(tokens[1].replace("\"", "")); // Remove quotes for directory name
                    }
                    break;

                case "pwd":
                    if (length == 1) {
                        outputBuilder.append(currentDirectory.getPath());
                        outputBuilder.append('\n');
                    } else {
                        System.out.println("Usage: pwd");
                        return;
                    }
                    break;

                // Handle 'help' command
                case "help":
                    if (length == 1) {
                        showHelp();
                    } else {
                        System.out.println("Usage: help");
                        return;
                    }
                    break;

                // Handle 'exit' command
                case "exit":
                    if (length == 1) {
                        System.out.println("Exiting...");
                        System.exit(0); // Terminate the shell
                    } else {
                        System.out.println("Usage: exit");
                        return;
                    }
                    break;

                // Unrecognized command
                default:
                    System.out.println("Command not recognized: " + command);
            }
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }

    }

    // Handle piping manually
    private void processPipe(String command) {
        try {
            if (outputStream != System.out) return;
            String[] tokens = command.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            int length = tokens.length;
            if (length >= 3 && (tokens[length - 2].equals(">") || tokens[length - 2].equals(">>"))) {
                File file = new File(currentDirectory, tokens[length - 1]);
                outputStream = new FileOutputStream(file, tokens[length - 2].equals(">>"));
                length -= 2;
            }
            switch (tokens[0]) {
                case "grep":
                    if (length != 2) {
                        System.out.println("Usage: grep <pattern>");
                        return;
                    }
                    grep(tokens[1].replace("\"", ""));
                    break;
                case "TODO LATER":
                default:
                    System.out.println("Command not recognized: " + command);
            }

        } catch (Exception e) {
            System.out.println("Error in piping commands: " + e.getMessage());
        }
    }

    private void grep(String pattern) {
        String[] lines = outputBuilder.toString().split("\n");
        outputBuilder.setLength(0);
        for (String line : lines) {
            if (line.contains(pattern)) {
                outputBuilder.append(line);
                outputBuilder.append('\n');
            }
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

    // Create a directory
    private void makeDirectory(String dirName) {
        File dir = new File(currentDirectory, dirName);
        if (dir.mkdir()) {
            System.out.println("Directory created: " + dirName);
        } else {
            System.out.println("Failed to create directory: " + dirName);
        }
    }

    // Remove a directory
    private void removeDirectory(String dirName) {
        File dir = new File(currentDirectory, dirName);
        if (dir.isDirectory() && dir.delete()) {
            System.out.println("Directory removed: " + dirName);
        } else {
            System.out.println("Failed to remove directory: " + dirName);
        }
    }

    // Remove a file
    private void removeFile(String fileName) {
        File file = new File(currentDirectory, fileName);
        if (file.isFile() && file.delete()) {
            System.out.println("File removed: " + fileName);
        } else {
            System.out.println("Failed to remove file: " + fileName);
        }
    }

    // Display file contents
    private void catFile(String fileName) {
        File file = new File(currentDirectory, fileName);
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
            outputBuilder.append(fileName);
            outputBuilder.append('\n');
        }
    }

    // Create a file
    private void createFile(String fileName) {
        File file = new File(currentDirectory, fileName);
        try {
            if (file.createNewFile()) {
                System.out.println("File created: " + fileName);
            } else {
                System.out.println("File already exists: " + fileName);
            }
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }

    // Move or rename a file or directory
    // Move or rename a file or directory
    private void moveFileOrDirectory(String source, String destination) {
        try {
            File src = new File(currentDirectory, source);
            File dest = new File(destination);

            // If the destination is not absolute, treat it as relative to the current directory
            if (!dest.isAbsolute()) {
                dest = new File(currentDirectory, destination);
            }

            // Check if the destination is a directory
            if (dest.isDirectory()) {
                // Move the source file/directory inside the destination directory
                File newDest = new File(dest, src.getName()); // Move the file into the destination directory
                if (src.renameTo(newDest)) {
                    System.out.println("Moved " + src.getPath() + " to " + newDest.getPath());
                } else {
                    System.out.println("Failed to move " + source + " to " + dest.getPath());
                }
            } else {
                // If destination is not a directory, rename the source file
                if (src.renameTo(dest)) {
                    System.out.println("Renamed " + src.getPath() + " to " + dest.getPath());
                } else {
                    System.out.println("Failed to rename " + source + " to " + destination);
                }
            }
        } catch (Exception e) {
            System.out.println("Error moving/renaming: " + e.getMessage());
        }
    }

    // Change current working directory
    // Change current working directory
    private void changeDirectory(String dirName) {
        try {
            File dir = new File(dirName);

            // If the path is not absolute, append it to the current directory
            if (!dir.isAbsolute()) {
                dir = new File(currentDirectory, dirName);
            }

            // Resolve the canonical path (handles ".." and ".", removes redundant separators)
            File resolvedDir = dir.getCanonicalFile();

            // Check if the resolved path is a directory and exists
            if (resolvedDir.isDirectory()) {
                currentDirectory = resolvedDir;
                System.out.println("Directory changed to: " + currentDirectory.getPath());
            } else {
                System.out.println("Directory not found: " + dirName);
            }
        } catch (IOException e) {
            System.out.println("Error changing directory: " + e.getMessage());
        }
    }

    // Show help message
    private void showHelp() {
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
                  pwd                             Print the current directory
                  help                            Show this help message
                  exit                            Exit the shell
                """);
    }

    private void printOutput() {
        if (outputStream == System.out) {
            System.out.print(outputBuilder.toString());
            return;
        }
        try (PrintStream printStream = new PrintStream(outputStream)) {
            printStream.print(outputBuilder.toString());
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }
}
