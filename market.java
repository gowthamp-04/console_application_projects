import java.util.*;

class User {
    String email;
    String password;
    String role;

    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}

class Customer extends User {
    double credit = 1000;
    int loyaltyPoints = 0;
    double totalSpent = 0;

    public Customer(String email, String password) {
        super(email, password, "Customer");
    }
}

class Product {
    int id;
    String name;
    double price;
    int quantity;
    boolean everBought = false;

    public Product(int id, String name, double price, int qty) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = qty;
    }
}

class CartItem {
    Product product;
    int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    double getTotal() {
        return product.price * quantity;
    }
}

class Cart {
    List<CartItem> items = new ArrayList<>();

    public void addProduct(Product product, int qty) {
        for (CartItem item : items) {
            if (item.product.id == product.id) {
                item.quantity += qty;
                return;
            }
        }
        items.add(new CartItem(product, qty));
    }

    public void updateProduct(int productId, int newQty) {
        for (CartItem item : items) {
            if (item.product.id == productId) {
                item.quantity = newQty;
                return;
            }
        }
    }

    public void removeProduct(int productId) {
        items.removeIf(item -> item.product.id == productId);
    }

    public double getTotalAmount() {
        return items.stream().mapToDouble(CartItem::getTotal).sum();
    }

    public void clearCart() {
        items.clear();
    }
}

class AuthService {
    static Map<String, User> users = new HashMap<>();

    public static void register(String email, String password, String role) {
        users.put(email, new User(email, password, role));
    }

    public static User login(String email, String password) {
        User user = users.get(email);
        if (user != null && user.password.equals(password)) {
            return user;
        }
        return null;
    }
}

class Inventory {
    static Map<Integer, Product> products = new HashMap<>();
    static int nextId = 1;

    public static void addProduct(String name, double price, int quantity) {
        Product p = new Product(nextId++, name, price, quantity);
        products.put(p.id, p);
    }

    public static void modifyProduct(int id, String newName, double newPrice, int newQty) {
        Product p = products.get(id);
        if (p != null) {
            p.name = newName;
            p.price = newPrice;
            p.quantity = newQty;
        }
    }

    public static void deleteProduct(int id) {
        products.remove(id);
    }

    public static List<Product> getProductsSortedByName() {
        return products.values().stream().sorted(Comparator.comparing(p -> p.name)).toList();
    }

    public static List<Product> getProductsSortedByPrice() {
        return products.values().stream().sorted(Comparator.comparingDouble(p -> p.price)).toList();
    }

    public static Product searchByName(String name) {
        return products.values().stream()
                .filter(p -> p.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}

class PaymentService {
    public static void processPayment(Customer customer, Cart cart) {
        double total = cart.getTotalAmount();
        if (total > customer.credit) {
            System.out.println("Insufficient credit.");
            return;
        }

        customer.credit -= total;
        customer.totalSpent += total;

        if (total >= 5000) {
            customer.credit += 100;
        } else {
            int pointsEarned = (int) (total / 100);
            customer.loyaltyPoints += pointsEarned;
            if (customer.loyaltyPoints >= 50) {
                customer.credit += 100;
                customer.loyaltyPoints -= 50;
            }
        }

        for (CartItem item : cart.items) {
            item.product.quantity -= item.quantity;
            item.product.everBought = true;
        }

        PurchaseHistory.addPurchase(customer, cart);
        cart.clearCart();
        System.out.println("Payment successful!");
    }
}

class PurchaseHistory {
    static Map<String, List<String>> history = new HashMap<>();

    public static void addPurchase(Customer customer, Cart cart) {
        String record = "Bill on " + new Date() + " -> ₹" + cart.getTotalAmount();
        history.computeIfAbsent(customer.email, k -> new ArrayList<>()).add(record);
    }

    public static void viewHistory(Customer customer) {
        List<String> logs = history.get(customer.email);
        if (logs == null || logs.isEmpty()) {
            System.out.println("No purchases yet.");
            return;
        }
        logs.forEach(System.out::println);
    }
}

class ReportService {
    public static void lowStockReport() {
        Inventory.products.values().stream()
                .filter(p -> p.quantity < 5)
                .forEach(p -> System.out.println(p.name + " - Qty: " + p.quantity));
    }

    public static void neverBoughtReport() {
        Inventory.products.values().stream()
                .filter(p -> !p.everBought)
                .forEach(p -> System.out.println(p.name));
    }

    public static void topCustomers(Map<String, Customer> customers) {
        customers.values().stream()
                .sorted((c1, c2) -> Double.compare(c2.totalSpent, c1.totalSpent))
                .limit(5)
                .forEach(c -> System.out.println(c.email + " - Spent: ₹" + c.totalSpent));
    }
}

public class market {
    static Scanner sc = new Scanner(System.in);
    static Map<String, Customer> customers = new HashMap<>();

    public static void main(String[] args) {
        AuthService.register("admin@shop.com", "admin123", "Admin");
        AuthService.register("cust@shop.com", "cust123", "Customer");
        customers.put("cust@shop.com", new Customer("cust@shop.com", "cust123"));

        Inventory.addProduct("Rice", 60, 20);
        Inventory.addProduct("Sugar", 45, 10);
        Inventory.addProduct("Oil", 120, 8);

        while (true) {
            System.out.print("\nEmail: ");
            String email = sc.nextLine();
            System.out.print("Password: ");
            String password = sc.nextLine();

            User user = AuthService.login(email, password);
            if (user == null) {
                System.out.println("Invalid login.");
                continue;
            }

            if (user.role.equals("Admin")) {
                adminMenu();
            } else {
                Customer cust = customers.get(user.email);
                customerMenu(cust);
            }
        }
    }

    static void adminMenu() {
        while (true) {
            System.out.println("\n1. Add Product\n2. Modify Product\n3. Delete Product\n4. View Products\n5. Search Product\n6. Low Stock\n7. Never Bought\n8. Exit");
            int ch = Integer.parseInt(sc.nextLine());
            switch (ch) {
                case 1 -> {
                    System.out.print("Name: "); String n = sc.nextLine();
                    System.out.print("Price: "); double p = Double.parseDouble(sc.nextLine());
                    System.out.print("Qty: "); int q = Integer.parseInt(sc.nextLine());
                    Inventory.addProduct(n, p, q);
                }
                case 2 -> {
                    System.out.print("ID: "); int id = Integer.parseInt(sc.nextLine());
                    System.out.print("New Name: "); String n = sc.nextLine();
                    System.out.print("Price: "); double p = Double.parseDouble(sc.nextLine());
                    System.out.print("Qty: "); int q = Integer.parseInt(sc.nextLine());
                    Inventory.modifyProduct(id, n, p, q);
                }
                case 3 -> {
                    System.out.print("ID: "); int id = Integer.parseInt(sc.nextLine());
                    Inventory.deleteProduct(id);
                }
                case 4 -> Inventory.getProductsSortedByName().forEach(prod ->
                        System.out.println(prod.id + ". " + prod.name + " - ₹" + prod.price + " (Qty: " + prod.quantity + ")"));
                case 5 -> {
                    System.out.print("Name: "); String n = sc.nextLine();
                    Product p = Inventory.searchByName(n);
                    System.out.println(p != null ? p.name + ", ₹" + p.price + ", Qty: " + p.quantity : "Not found");
                }
                case 6 -> ReportService.lowStockReport();
                case 7 -> ReportService.neverBoughtReport();
                case 8 -> { return; }
            }
        }
    }

    static void customerMenu(Customer cust) {
        Cart cart = new Cart();
        while (true) {
            System.out.println("\n1. View Products\n2. Add to Cart\n3. Edit Cart\n4. Pay\n5. History\n6. Exit");
            int ch = Integer.parseInt(sc.nextLine());
            switch (ch) {
                case 1 -> Inventory.getProductsSortedByName().forEach(prod ->
                        System.out.println(prod.id + ". " + prod.name + " - ₹" + prod.price + " (Qty: " + prod.quantity + ")"));
                case 2 -> {
                    System.out.print("Product ID: "); int id = Integer.parseInt(sc.nextLine());
                    System.out.print("Quantity: "); int q = Integer.parseInt(sc.nextLine());
                    Product p = Inventory.products.get(id);
                    if (p != null && p.quantity >= q) {
                        cart.addProduct(p, q);
                    } else {
                        System.out.println("Invalid product or quantity.");
                    }
                }
                case 3 -> {
                    System.out.print("Product ID: "); int id = Integer.parseInt(sc.nextLine());
                    System.out.print("New Quantity: "); int q = Integer.parseInt(sc.nextLine());
                    cart.updateProduct(id, q);
                }
                case 4 -> PaymentService.processPayment(cust, cart);
                case 5 -> PurchaseHistory.viewHistory(cust);
                case 6 -> { return; }
            }
        }
    }
}
