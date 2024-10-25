import java.io.*;

public class Shell {
    // Track current directory
    private File currentDirectory = new File(System.getProperty("user.dir"));

    // Handle user commands
    public void handleCommand(String input) {
        // Split the input into commands, checking for pipes
        String[] commands = input.split("\\s*\\|\\s*");

        // Process the first command
        processCommand(commands[0]);

        // Handle any piping
        for (int i = 1; i < commands.length; i++) {
            // Pass the output of the previous command as input to the next command
            processPipe(commands[i - 1], commands[i]);
        }
    }

    public void printPrompt() {
        System.out.print(currentDirectory + "> ");
    }

    private void processCommand(String commandInput) {
        if (commandInput.contains(">")) {
            // Handle '>>' for appending
            if (commandInput.contains(">>")) {
                String[] parts = commandInput.split(">>");
                if (parts.length == 2) {
                    String command = parts[0].trim();  // The command to execute
                    String filePath = parts[1].trim().replace("\"", "");  // The file to append output to
                    redirectOutputToFile(command, filePath, true);  // Append mode
                } else {
                    System.out.println("Invalid usage of >>");
                }
            }
            // Handle '>' for overwriting
            else {
                String[] parts = commandInput.split(">");
                if (parts.length == 2) {
                    String command = parts[0].trim();  // The command to execute
                    String filePath = parts[1].trim().replace("\"", "");  // The file to write output to
                    redirectOutputToFile(command, filePath, false);  // Overwrite mode
                } else {
                    System.out.println("Invalid usage of >");
                }
            }
        } else {

            // Use regex to split by spaces while respecting quoted strings
            String[] tokens = commandInput.trim().split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String command = tokens[0];

            try {
                switch (command) {
                    case "echo":
                        if (tokens.length > 1) {
                            // Concatenate the rest of the tokens as the message to be echoed
                            String message = commandInput.substring(5); // Skip the 'echo' part
                            System.out.println(message);
                        } else {
                            System.out.println("Usage: echo <message>");
                        }
                        break;
                    // Handle 'ls' command (only allow ls, ls -a, ls -r)
                    case "ls":
                        if (tokens.length == 1) {
                            listFiles(false, false); // Regular 'ls'
                        } else if (tokens.length == 2 && tokens[1].equals("-a")) {
                            listFiles(true, false);  // 'ls -a'
                        } else if (tokens.length == 2 && tokens[1].equals("-r")) {
                            listFiles(false, true);  // 'ls -r'
                        } else {
                            System.out.println("Invalid usage. Supported ls commands: ls, ls -a, ls -r");
                        }
                        break;

                    // Handle 'mkdir' command
                    case "mkdir":
                        if (tokens.length == 2) {
                            makeDirectory(tokens[1].replace("\"", "")); // Handle quoted directory names
                        } else {
                            System.out.println("Usage: mkdir <directory_name>");
                        }
                        break;

                    // Handle 'rmdir' command
                    case "rmdir":
                        if (tokens.length == 2) {
                            removeDirectory(tokens[1].replace("\"", "")); // Handle quoted directory names
                        } else {
                            System.out.println("Usage: rmdir <directory_name>");
                        }
                        break;

                    // Handle 'rm' command
                    case "rm":
                        if (tokens.length == 2) {
                            removeFile(tokens[1].replace("\"", "")); // Handle quoted file names
                        } else {
                            System.out.println("Usage: rm <file_name>");
                        }
                        break;
                    // Handle 'cat' command
                    case "cat":
                        if (tokens.length == 2) {
                            catFile(tokens[1].replace("\"", "")); // Handle quoted file names
                        } else {
                            System.out.println("Usage: cat <file_name>");
                        }
                        break;

                    // Handle 'touch' command
                    case "touch":
                        if (tokens.length == 2) {
                            createFile(tokens[1].replace("\"", "")); // Handle quoted file names
                        } else {
                            System.out.println("Usage: touch <file_name>");
                        }
                        break;

                    // Handle 'mv' command
                    case "mv":
                        if (tokens.length == 3) {
                            moveFileOrDirectory(tokens[1].replace("\"", ""), tokens[2].replace("\"", "")); // Handle quoted file names
                        } else {
                            System.out.println("Usage: mv <source> <destination>");
                        }
                        break;

                    // Handle 'cd' command (with drive change)
                    case "cd":
                        if (tokens.length == 2) {
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
                            } else {
                                // Handle regular directory change, including folder names with spaces
                                changeDirectory(tokens[1].replace("\"", "")); // Remove quotes for directory name
                            }
                        } else {
                            System.out.println("Usage: cd <directory>");
                        }
                        break;

                    case "pwd":
                        if (tokens.length == 1) {
                            System.out.println(currentDirectory.getPath());
                        } else {
                            System.out.println("Usage: pwd");
                        }
                        break;

                    // Handle 'help' command
                    case "help":
                        if (tokens.length == 1) {
                            showHelp();
                        } else {
                            System.out.println("Usage: help");
                        }
                        break;

                    // Handle 'exit' command
                    case "exit":
                        if (tokens.length == 1) {
                            System.out.println("Exiting...");
                            System.exit(0); // Terminate the shell
                        } else {
                            System.out.println("Usage: exit");
                        }
                        break;

                    // Unrecognized command
                    default:
                        System.out.println("Command not recognized: " + command);
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        }
    }

    // Handle piping manually
    private void processPipe(String command1, String command2) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream);

            // Redirect output of the first command to the output stream
            PrintStream originalOut = System.out;
            System.setOut(printStream);

            processCommand(command1); // Execute the first command

            // Restore the original output
            System.setOut(originalOut);

            // Get the output of the first command
            String output = outputStream.toString();
            // Now process the second command with the output from the first command
            simulateInputForNextCommand(command2, output);
        } catch (Exception e) {
            System.out.println("Error in piping commands: " + e.getMessage());
        }
    }

    // Simulate input for the next command
    private void simulateInputForNextCommand(String commandInput, String simulatedInput) {
        try {
            // Use a ByteArrayInputStream to simulate input
            ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
            System.setIn(inputStream);

            // Process the second command
            processCommand(commandInput);

            // Restore the original input stream
            System.setIn(System.in);
        } catch (Exception e) {
            System.out.println("Error processing next command: " + e.getMessage());
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
                System.out.println(file.getName());
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
                System.out.println(dir.getPath() + "/" + file.getName());
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
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("File not found: " + fileName);
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

    private void redirectOutputToFile(String commandInput, String filePath, boolean append) {
        // Create the file relative to the current directory
        File outputFile = new File(currentDirectory, filePath);

        try (FileWriter fileWriter = new FileWriter(outputFile, append);
             BufferedWriter writer = new BufferedWriter(fileWriter)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream);

            // Temporarily redirect System.out to capture command output
            PrintStream originalOut = System.out;
            System.setOut(printStream);

            // Process the original command (before the '>' or '>>')
            processCommand(commandInput);

            // Restore original output stream
            System.setOut(originalOut);

            // Write captured output to the file
            writer.write(outputStream.toString());
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    // Show help message
    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  echo <message>                  Echo the message to the console");
        System.out.println("  ls [-a] [-r]                    List files in the current directory");
        System.out.println("  mkdir <directory_name>          Create a new directory");
        System.out.println("  rmdir <directory_name>          Remove an empty directory");
        System.out.println("  rm <file_name>                  Remove a file");
        System.out.println("  cat <file_name>                 Display file contents");
        System.out.println("  touch <file_name>               Create a new file");
        System.out.println("  mv <source> <destination>       Move or rename a file or directory");
        System.out.println("  cd <directory>                  Change the current directory");
        System.out.println("  pwd                              Print the current directory");
        System.out.println("  help                             Show this help message");
        System.out.println("  exit                             Exit the shell");
    }
}
