package ug.ac.vu.g09.core;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * The one place where the whole program reads from the keyboard.
 * @author Mande Roland
 */
public class InputHelper {
    private Scanner input;

    public InputHelper() {
        input = new Scanner(System.in);
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("That is not a whole number. Try again.");
            }
        }
    }

    public double readDouble(String prompt, double min, double max) {
        while (true) {
            // people type 1,200,000 so the commas are removed first
            String line = readLine(prompt).replace(",", "");
            try {
                double value = Double.parseDouble(line);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    System.out.println("That is not a valid amount. Try again.");
                } else if (value < min || value > max) {
                    System.out.printf("Please enter an amount from %,.0f to %,.0f.%n", min, max);
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.out.println("That is not a number. Try again.");
            }
        }
    }

    public String readText(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                System.out.println("This cannot be empty.");
            } else if (line.contains("|")) {
                System.out.println("Please do not use the | symbol.");
            } else {
                return line;
            }
        }
    }

    public String readDate(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (isValidDate(line)) {
                return line;
            }
            System.out.println("Use the format yyyy-mm-dd, for example 2026-10-04.");
        }
    }

    // also used by the model classes when they read dates from a file
    public static boolean isValidDate(String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        int year = Integer.parseInt(text.substring(0, 4));
        int month = Integer.parseInt(text.substring(5, 7));
        int day = Integer.parseInt(text.substring(8, 10));
        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            return false;
        }
        int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
        int last = days[month - 1];
        if (month == 2 && leap) {
            last = 29;
        }
        return day >= 1 && day <= last;
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return input.nextLine().trim();
        } catch (NoSuchElementException e) {
            // keyboard closed, so stop quietly instead of crashing
            System.out.println();
            System.out.println("Input has ended. Closing the program.");
            System.exit(0);
            return "";
        }
    }
}
