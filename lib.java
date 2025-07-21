import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

class User {
    String email, password, role;
    String name;
    double deposit = 1500;
    List<String> borrowedBooks = new ArrayList<>();
    List<String> fineHistory = new ArrayList<>();

    public User(String name, String email, String password, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}

class Book {
    String isbn, name, author;
    int quantity, cost, borrowCount;

    public Book(String isbn, String name, String author, int quantity, int cost) {
        this.isbn = isbn;
        this.name = name;
        this.author = author;
        this.quantity = quantity;
        this.cost = cost;
        this.borrowCount = 0;
    }
}

class LibraryManagementSystem {
    static Scanner sc = new Scanner(System.in);
    static Map<String, User> users = new HashMap<>();
    static Map<String, Book> books = new HashMap<>();
    static Map<String, LocalDate> borrowDates = new HashMap<>();
    static User currentUser;

    public static void main(String[] args) {
        seedData();
        while (true) {
            System.out.println("\n--- Library Management System ---");
            System.out.print("Email: ");
            String email = sc.nextLine();
            System.out.print("Password: ");
            String pwd = sc.nextLine();

            if (login(email, pwd)) {
                if (currentUser.role.equals("admin")) adminMenu();
                else borrowerMenu();
            } else {
                System.out.println("Invalid credentials!");
            }
        }
    }

    static boolean login(String email, String pwd) {
        if (users.containsKey(email) && users.get(email).password.equals(pwd)) {
            currentUser = users.get(email);
            return true;
        }
        return false;
    }

    static void adminMenu() {
        while (true) {
            System.out.println("\n[Admin Menu]");
            System.out.println("1. Add Book\n2. View Books\n3. Add User\n4. Search Book\n5. Reports\n6. Logout");
            System.out.print("Choice: ");
            int ch = Integer.parseInt(sc.nextLine());
            switch (ch) {
                case 1 -> addBook();
                case 2 -> viewBooks();
                case 3 -> addUser();
                case 4 -> searchBook();
                case 5 -> generateReports();
                case 6 -> {
                    currentUser = null;
                    return;
                }
                default -> System.out.println("Invalid option");
            }
        }
    }

    static void borrowerMenu() {
        while (true) {
            System.out.println("\n[Borrower Menu]");
            System.out.println("1. View Books\n2. Borrow Book\n3. Return Book\n4. Fine History\n5. Logout");
            System.out.print("Choice: ");
            int ch = Integer.parseInt(sc.nextLine());
            switch (ch) {
                case 1 -> viewBooks();
                case 2 -> borrowBook();
                case 3 -> returnBook();
                case 4 -> showFines();
                case 5 -> {
                    currentUser = null;
                    return;
                }
                default -> System.out.println("Invalid option");
            }
        }
    }

    static void addBook() {
        System.out.print("ISBN: ");
        String isbn = sc.nextLine();
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Author: ");
        String author = sc.nextLine();
        System.out.print("Quantity: ");
        int qty = Integer.parseInt(sc.nextLine());
        System.out.print("Cost: ");
        int cost = Integer.parseInt(sc.nextLine());

        books.put(isbn, new Book(isbn, name, author, qty, cost));
        System.out.println("Book added successfully.");
    }

    static void viewBooks() {
        System.out.printf("%-10s %-20s %-10s %-10s\n", "ISBN", "Name", "Qty", "Cost");
        for (Book b : books.values()) {
            System.out.printf("%-10s %-20s %-10d %-10d\n", b.isbn, b.name, b.quantity, b.cost);
        }
    }

    static void searchBook() {
        System.out.print("Enter book name or ISBN to search: ");
        String input = sc.nextLine();
        for (Book b : books.values()) {
            if (b.isbn.equalsIgnoreCase(input) || b.name.equalsIgnoreCase(input)) {
                System.out.println("Found: " + b.name + " | Qty: " + b.quantity);
                return;
            }
        }
        System.out.println("Book not found.");
    }

    static void addUser() {
        System.out.print("Role (admin/borrower): ");
        String role = sc.nextLine();
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Email: ");
        String email = sc.nextLine();
        System.out.print("Password: ");
        String pwd = sc.nextLine();
        users.put(email, new User(name, email, pwd, role));
        System.out.println(role + " added.");
    }

    static void borrowBook() {
        if (currentUser.borrowedBooks.size() >= 3) {
            System.out.println("Limit reached. Return some books first.");
            return;
        }
        if (currentUser.deposit < 500) {
            System.out.println("Insufficient deposit. Need minimum ₹500.");
            return;
        }
        System.out.print("Enter ISBN to borrow: ");
        String isbn = sc.nextLine();
        if (!books.containsKey(isbn) || books.get(isbn).quantity == 0) {
            System.out.println("Book unavailable.");
            return;
        }
        if (currentUser.borrowedBooks.contains(isbn)) {
            System.out.println("Already borrowed this book.");
            return;
        }
        currentUser.borrowedBooks.add(isbn);
        borrowDates.put(currentUser.email + isbn, LocalDate.now());
        books.get(isbn).quantity--;
        books.get(isbn).borrowCount++;
        System.out.println("Book borrowed successfully.");
    }

    static void returnBook() {
        System.out.print("Enter ISBN to return: ");
        String isbn = sc.nextLine();
        String key = currentUser.email + isbn;
        if (!currentUser.borrowedBooks.contains(isbn)) {
            System.out.println("You haven’t borrowed this book.");
            return;
        }
        LocalDate borrowedDate = borrowDates.getOrDefault(key, LocalDate.now());
        System.out.print("Enter return date (dd/MM/yyyy): ");
        LocalDate returnDate = LocalDate.parse(sc.nextLine(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        long days = Duration.between(borrowedDate.atStartOfDay(), returnDate.atStartOfDay()).toDays();

        if (days > 15) {
            int cost = books.get(isbn).cost;
            long extra = days - 15;
            double fine = Math.min(80 * cost / 100.0, 2 * extra);
            currentUser.deposit -= fine;
            currentUser.fineHistory.add("Fine ₹" + fine + " for late return of book " + isbn);
            System.out.println("Fine applied: ₹" + fine);
        }

        currentUser.borrowedBooks.remove(isbn);
        borrowDates.remove(key);
        books.get(isbn).quantity++;
        System.out.println("Book returned successfully.");
    }

    static void showFines() {
        System.out.println("Fine History:");
        for (String f : currentUser.fineHistory) {
            System.out.println(f);
        }
    }

    static void generateReports() {
        System.out.println("\n--- Report: Low Stock Books ---");
        for (Book b : books.values()) {
            if (b.quantity < 2) System.out.println(b.name + " (Qty: " + b.quantity + ")");
        }

        System.out.println("\n--- Report: Most Borrowed Books ---");
        books.values().stream().filter(b -> b.borrowCount > 0)
                .sorted((a, b) -> b.borrowCount - a.borrowCount)
                .forEach(b -> System.out.println(b.name + " - Borrowed " + b.borrowCount + " times"));
    }

    static void seedData() {
        users.put("admin@lib.com", new User("Admin", "admin@lib.com", "admin123", "admin"));
        users.put("stu@lib.com", new User("Student", "stu@lib.com", "pass", "borrower"));
        books.put("ISBN1", new Book("ISBN1", "Java Programming", "James", 5, 400));
        books.put("ISBN2", new Book("ISBN2", "Python Basics", "Guido", 3, 350));
    }
}
