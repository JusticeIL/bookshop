-- Adds the required per-book description.
--
-- Added in three steps so the migration is safe on a database that already
-- holds rows: create the column nullable, backfill every existing book, then
-- tighten it to NOT NULL. New books must always carry a description.

ALTER TABLE books ADD COLUMN description VARCHAR(500);

UPDATE books SET description = 'A practical guide to writing readable, maintainable code, built around naming, small functions and honest tests.' WHERE title = 'Clean Code';
UPDATE books SET description = 'Pragmatic habits for the working developer, from DRY code and automation to owning your craft and your career.' WHERE title = 'The Pragmatic Programmer';
UPDATE books SET description = 'How modern storage, replication, partitioning and stream processing really work, and the trade-offs behind each.' WHERE title = 'Designing Data-Intensive Applications';
UPDATE books SET description = 'Seventy-eight rules for writing better Java, covering generics, streams, immutability and API design.' WHERE title = 'Effective Java';
UPDATE books SET description = 'A catalogue of behaviour-preserving code transformations, and the discipline of applying them in small safe steps.' WHERE title = 'Refactoring';
UPDATE books SET description = 'Modelling complex business domains in software through a shared language, bounded contexts and aggregates.' WHERE title = 'Domain-Driven Design';
UPDATE books SET description = 'Classic essays on why adding people to a late software project makes it later, and other hard-won lessons.' WHERE title = 'The Mythical Man-Month';
UPDATE books SET description = 'The MIT classic on abstraction, recursion and interpreters, teaching how to think about computation itself.' WHERE title = 'Structure and Interpretation of Computer Programs';
UPDATE books SET description = 'The definitive reference on algorithms and data structures, with rigorous analysis and thorough proofs.' WHERE title = 'Introduction to Algorithms';
UPDATE books SET description = 'An exhaustive handbook of software construction: design, defensive coding, debugging and code quality.' WHERE title = 'Code Complete';
UPDATE books SET description = 'Techniques for safely changing untested codebases by finding seams and getting tests in place first.' WHERE title = 'Working Effectively with Legacy Code';
UPDATE books SET description = 'A visual, example-driven introduction to the classic design patterns and when each one actually helps.' WHERE title = 'Head First Design Patterns';
UPDATE books SET description = 'Building deployment pipelines that make releasing software a routine, low-risk and repeatable event.' WHERE title = 'Continuous Delivery';
UPDATE books SET description = 'On professionalism in software: estimates, saying no, managing pressure and taking responsibility for your work.' WHERE title = 'The Clean Coder';
UPDATE books SET description = 'An illustrated, friendly path through sorting, search, graphs, greedy methods and dynamic programming.' WHERE title = 'Grokking Algorithms';
UPDATE books SET description = 'How Google runs production systems, covering error budgets, on-call practice, monitoring and postmortems.' WHERE title = 'Site Reliability Engineering';
UPDATE books SET description = 'Programming questions with worked solutions, plus guidance on preparing for technical interviews.' WHERE title = 'Cracking the Coding Interview';
UPDATE books SET description = 'A deep look at how JavaScript actually behaves: scope, closures, coercion, prototypes and the type system.' WHERE title = 'You Don''t Know JS Yet';
UPDATE books SET description = 'Patterns for layering enterprise systems: domain logic, data mapping, concurrency and web presentation.' WHERE title = 'Patterns of Enterprise Application Architecture';
UPDATE books SET description = 'Lessons from decades of engineering at scale, on code review, testing, deprecation and sustaining a codebase.' WHERE title = 'Software Engineering at Google';

-- Safety net: any row not matched above still gets a value before the
-- constraint is applied, so the migration cannot fail on unexpected data.
UPDATE books SET description = 'No description available for this title yet.' WHERE description IS NULL;

ALTER TABLE books ALTER COLUMN description SET NOT NULL;
