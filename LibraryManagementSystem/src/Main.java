import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        LibraryService lib = new LibraryService();
        while (true) {
            System.out.println("\n===== LIBRARY MANAGEMENT SYSTEM =====");
            System.out.println("1. Add Book");
            System.out.println("2. View All Books");
            System.out.println("3. Search Book");
            System.out.println("4. Delete Book");
            System.out.println("5. Add Member");
            System.out.println("6. View Members");
            System.out.println("7. Issue Book");
            System.out.println("8. Return Book");
            System.out.println("9. View Issued Books");
            System.out.println("0. Exit");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> lib.addBook(readStr("Title: "), readStr("Author: "), readInt("Quantity: "));
                case 2 -> lib.viewBooks();
                case 3 -> lib.searchBooks(readStr("Search keyword: "));
                case 4 -> lib.deleteBook(readInt("Book ID: "));
                case 5 -> lib.addMember(readStr("Name: "), readStr("Email: "), readStr("Phone: "));
                case 6 -> lib.viewMembers();
                case 7 -> lib.issueBook(readInt("Book ID: "), readInt("Member ID: "));
                case 8 -> lib.returnBook(readInt("Issue ID: "));
                case 9 -> lib.viewIssuedBooks();
                case 0 -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static String readStr(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
