-- Seed catalog. Covers use OpenLibrary's public cover CDN (no key required).
-- A couple of rows intentionally have NULL image_url to exercise the fallback UI.

INSERT INTO books (title, author, pages, image_url, price, stock) VALUES
('Clean Code', 'Robert C. Martin', 464, 'https://covers.openlibrary.org/b/isbn/9780132350884-M.jpg', 34.99, 12),
('The Pragmatic Programmer', 'Andrew Hunt & David Thomas', 352, 'https://covers.openlibrary.org/b/isbn/9780135957059-M.jpg', 39.99, 8),
('Designing Data-Intensive Applications', 'Martin Kleppmann', 616, 'https://covers.openlibrary.org/b/isbn/9781449373320-M.jpg', 44.50, 5),
('Effective Java', 'Joshua Bloch', 412, 'https://covers.openlibrary.org/b/isbn/9780134685991-M.jpg', 42.00, 10),
('Refactoring', 'Martin Fowler', 448, 'https://covers.openlibrary.org/b/isbn/9780134757599-M.jpg', 41.25, 7),
('Domain-Driven Design', 'Eric Evans', 560, 'https://covers.openlibrary.org/b/isbn/9780321125217-M.jpg', 49.99, 4),
('The Mythical Man-Month', 'Frederick P. Brooks Jr.', 336, 'https://covers.openlibrary.org/b/isbn/9780201835953-M.jpg', 29.99, 15),
('Structure and Interpretation of Computer Programs', 'Abelson & Sussman', 657, 'https://covers.openlibrary.org/b/isbn/9780262510875-M.jpg', 27.80, 6),
('Introduction to Algorithms', 'Cormen, Leiserson, Rivest & Stein', 1312, 'https://covers.openlibrary.org/b/isbn/9780262046305-M.jpg', 89.00, 3),
('Code Complete', 'Steve McConnell', 960, 'https://covers.openlibrary.org/b/isbn/9780735619678-M.jpg', 38.40, 9),
('Working Effectively with Legacy Code', 'Michael Feathers', 456, 'https://covers.openlibrary.org/b/isbn/9780131177055-M.jpg', 43.10, 5),
('Head First Design Patterns', 'Freeman & Robson', 694, 'https://covers.openlibrary.org/b/isbn/9781492078005-M.jpg', 47.90, 11),
('Continuous Delivery', 'Jez Humble & David Farley', 512, 'https://covers.openlibrary.org/b/isbn/9780321601919-M.jpg', 45.60, 6),
('The Clean Coder', 'Robert C. Martin', 256, NULL, 31.50, 14),
('Grokking Algorithms', 'Aditya Bhargava', 256, 'https://covers.openlibrary.org/b/isbn/9781617292231-M.jpg', 33.25, 13),
('Site Reliability Engineering', 'Beyer, Jones, Petoff & Murphy', 552, 'https://covers.openlibrary.org/b/isbn/9781491929124-M.jpg', 40.00, 7),
('Cracking the Coding Interview', 'Gayle Laakmann McDowell', 708, 'https://covers.openlibrary.org/b/isbn/9780984782857-M.jpg', 35.99, 20),
('You Don''t Know JS Yet', 'Kyle Simpson', 278, NULL, 24.99, 16),
('Patterns of Enterprise Application Architecture', 'Martin Fowler', 560, 'https://covers.openlibrary.org/b/isbn/9780321127426-M.jpg', 52.30, 4),
('Software Engineering at Google', 'Winters, Manshreck & Wright', 602, 'https://covers.openlibrary.org/b/isbn/9781492082798-M.jpg', 46.75, 8);
