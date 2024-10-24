import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Shell shell = new Shell(); // Create a new shell instance
        Scanner scanner = new Scanner(System.in);
        String input;
        boolean running = true;

        System.out.println("Welcome to the Command Prompt Interpreter!");
        System.out.println("Type 'exit' to quit.");

        // Main loop to read and process commands using the shell
        while (running) {
            System.out.print(Shell.currentDirectory + ">");
            input = scanner.nextLine().trim(); // Get the user's input

            // Check if the user wants to exit the shell
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                running = false; // Break the loop and exit
            } else {
                shell.handleCommand(input); // Pass the input to the shell for handling
            }
        }

        scanner.close(); // Close the scanner when finished
    }
}
