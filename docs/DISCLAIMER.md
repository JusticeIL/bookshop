# Disclaimer

This repository is a **home assignment / take-home interview project** built for
evaluation purposes only.

- It is **not a real store**: no books are sold, no payments are processed, and no
  goods are shipped.
- **Payment and shipping are mocked by design**, as permitted by the assignment brief.
  The checkout card field is decorative; nothing entered there is validated, stored,
  or transmitted, and the "payment reference" on orders is a randomly generated string.
- The application is intentionally kept small in scope, but its structure is
  production-shaped, and payment would remain mocked in production by design — no card
  data is ever collected, so the service stays out of PCI-DSS scope entirely.
- Book data (titles, covers, prices, stock) is seeded demo content; cover images are
  served from OpenLibrary's public cover CDN.
- All accounts created on the live demo are throwaway evaluation data and may be
  deleted at any time.
