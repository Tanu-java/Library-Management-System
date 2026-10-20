CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

CREATE TABLE IF NOT EXISTS books (
    book_id    INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    author     VARCHAR(100) NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    available  INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS members (
    member_id  INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(100) UNIQUE,
    phone      VARCHAR(15)
);

CREATE TABLE IF NOT EXISTS issued_books (
    issue_id    INT AUTO_INCREMENT PRIMARY KEY,
    book_id     INT NOT NULL,
    member_id   INT NOT NULL,
    issue_date  DATE NOT NULL,
    due_date    DATE NOT NULL,
    return_date DATE NULL,
    fine        DECIMAL(8,2) DEFAULT 0,
    FOREIGN KEY (book_id)   REFERENCES books(book_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);

-- Sample data
INSERT INTO books (title, author, quantity, available) VALUES
 ('Database System Concepts', 'Silberschatz', 3, 3),
 ('Java: The Complete Reference', 'Herbert Schildt', 2, 2);
INSERT INTO members (name, email, phone) VALUES
 ('Rahul Sharma', 'rahul@example.com', '9876543210');
