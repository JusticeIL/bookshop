# Disclaimer

This repository is a **home assignment / take-home interview project** built for
evaluation purposes only.

- It is **not a real store**: no books are sold, no payments are processed, and no
  goods are shipped.
- **Payment and shipping are mocked by design**, as permitted by the assignment brief.
  The checkout card field is decorative; nothing entered there is validated, stored,
  or transmitted, and the "payment reference" on orders is a randomly generated string.
- The application is intentionally kept simple and is **not production-ready**
  (e.g. no rate limiting, no email verification, no refresh-token rotation, no
  observability stack). The architecture document lists what would change for
  production.
- Book data (titles, covers, prices, stock) is seeded demo content; cover images are
  served from OpenLibrary's public cover CDN.
- All accounts created on the live demo are throwaway evaluation data and may be
  deleted at any time.
