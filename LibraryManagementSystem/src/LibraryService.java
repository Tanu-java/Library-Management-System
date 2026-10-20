import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LibraryService {
    private static final int LOAN_DAYS = 14;
    private static final double FINE_PER_DAY = 2.0; // Rs. per late day

    // ---------- Books ----------
    public void addBook(String title, String author, int qty) {
        String sql = "INSERT INTO books (title, author, quantity, available) VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, qty);
            ps.setInt(4, qty);
            ps.executeUpdate();
            System.out.println("Book added successfully.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void viewBooks() {
        printBooks("SELECT * FROM books", null);
    }

    public void searchBooks(String keyword) {
        printBooks("SELECT * FROM books WHERE title LIKE ? OR author LIKE ?", "%" + keyword + "%");
    }

    private void printBooks(String sql, String param) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) { ps.setString(1, param); ps.setString(2, param); }
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-5s %-35s %-22s %-6s %-6s%n", "ID", "Title", "Author", "Qty", "Avail");
                System.out.println("-".repeat(80));
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-5d %-35s %-22s %-6d %-6d%n",
                        rs.getInt("book_id"), rs.getString("title"), rs.getString("author"),
                        rs.getInt("quantity"), rs.getInt("available"));
                }
                if (!any) System.out.println("No books found.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void deleteBook(int bookId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM books WHERE book_id=?")) {
            ps.setInt(1, bookId);
            System.out.println(ps.executeUpdate() > 0 ? "Book deleted." : "Book not found.");
        } catch (SQLException e) {
            System.out.println("Cannot delete (book may have issue history): " + e.getMessage());
        }
    }

    // ---------- Members ----------
    public void addMember(String name, String email, String phone) {
        String sql = "INSERT INTO members (name, email, phone) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.executeUpdate();
            System.out.println("Member added successfully.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void viewMembers() {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM members")) {
            System.out.printf("%-5s %-25s %-28s %-15s%n", "ID", "Name", "Email", "Phone");
            System.out.println("-".repeat(75));
            while (rs.next()) {
                System.out.printf("%-5d %-25s %-28s %-15s%n", rs.getInt("member_id"),
                    rs.getString("name"), rs.getString("email"), rs.getString("phone"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ---------- Issue / Return ----------
    public void issueBook(int bookId, int memberId) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false); // transaction: both updates succeed or neither
            try {
                // 1. check availability
                int available;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT available FROM books WHERE book_id=? FOR UPDATE")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { System.out.println("Book not found."); con.rollback(); return; }
                        available = rs.getInt(1);
                    }
                }
                if (available <= 0) { System.out.println("Book not available."); con.rollback(); return; }

                // 2. check member exists
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM members WHERE member_id=?")) {
                    ps.setInt(1, memberId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { System.out.println("Member not found."); con.rollback(); return; }
                    }
                }

                // 3. insert issue record
                LocalDate today = LocalDate.now();
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO issued_books (book_id, member_id, issue_date, due_date) VALUES (?,?,?,?)")) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setDate(3, Date.valueOf(today));
                    ps.setDate(4, Date.valueOf(today.plusDays(LOAN_DAYS)));
                    ps.executeUpdate();
                }

                // 4. reduce available copies
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET available = available - 1 WHERE book_id=?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                con.commit();
                System.out.println("Book issued. Due date: " + today.plusDays(LOAN_DAYS));
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void returnBook(int issueId) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int bookId;
                LocalDate dueDate;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT book_id, due_date FROM issued_books WHERE issue_id=? AND return_date IS NULL")) {
                    ps.setInt(1, issueId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("No active issue found with that ID.");
                            con.rollback();
                            return;
                        }
                        bookId = rs.getInt("book_id");
                        dueDate = rs.getDate("due_date").toLocalDate();
                    }
                }

                LocalDate today = LocalDate.now();
                long lateDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
                double fine = lateDays * FINE_PER_DAY;

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE issued_books SET return_date=?, fine=? WHERE issue_id=?")) {
                    ps.setDate(1, Date.valueOf(today));
                    ps.setDouble(2, fine);
                    ps.setInt(3, issueId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET available = available + 1 WHERE book_id=?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                con.commit();
                System.out.println("Book returned. Late days: " + lateDays + ", Fine: Rs. " + fine);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void viewIssuedBooks() {
        String sql = "SELECT i.issue_id, b.title, m.name, i.issue_date, i.due_date, i.return_date, i.fine "
                   + "FROM issued_books i JOIN books b ON i.book_id=b.book_id "
                   + "JOIN members m ON i.member_id=m.member_id ORDER BY i.issue_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-6s %-28s %-18s %-12s %-12s %-12s %-6s%n",
                "IssID", "Book", "Member", "Issued", "Due", "Returned", "Fine");
            System.out.println("-".repeat(100));
            while (rs.next()) {
                System.out.printf("%-6d %-28s %-18s %-12s %-12s %-12s %-6.1f%n",
                    rs.getInt("issue_id"), rs.getString("title"), rs.getString("name"),
                    rs.getDate("issue_date"), rs.getDate("due_date"),
                    rs.getDate("return_date") == null ? "Not yet" : rs.getDate("return_date").toString(),
                    rs.getDouble("fine"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
