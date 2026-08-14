import { Link } from 'react-router-dom';

/** Attribution for third-party assets and services used by this demo. */
export default function CreditsPage() {
  return (
    <section className="credits-page glass-panel">
      <h1>Credits &amp; sources</h1>

      <h2>Images</h2>
      <p>
        Book cover images are served from the{' '}
        <a href="https://openlibrary.org/dev/docs/api/covers" target="_blank" rel="noreferrer">
          OpenLibrary Covers API
        </a>{' '}
        (Internet Archive), used under their open data terms. Books without a cover use a
        locally generated placeholder. The favicon is an original inline SVG created for this
        project.
      </p>

      <h2>Icons</h2>
      <p>
        Interface icons (📚, 🏠, ☀️, 🌙 and similar) are standard system emoji; the bin icon on
        cancellable orders is an original inline SVG. All trademarks belong to their owners.
      </p>

      <h2>Book data</h2>
      <p>
        Titles, authors, page counts, prices and stock figures are seeded demo data; prices do
        not reflect real retail prices.
      </p>

      <h2>Technology</h2>
      <p>
        Built with React, TypeScript, Vite, Zustand and React Router on the frontend; a Java 21
        / Spring Boot 3 REST API with Spring Security, Spring Authorization Server (OAuth2)
        and Flyway on the backend; PostgreSQL 16 for storage and Redis for caching.
      </p>

      <p>
        <Link to="/" className="btn btn-secondary">
          ← Back to the catalog
        </Link>
      </p>
    </section>
  );
}
