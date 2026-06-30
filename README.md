# CampusOne

CampusOne is a polished frontend MVP for a unified Pakistani university
community platform. It brings academic resources, student discussions,
marketplace listings, events, internships, gamification, and AI-assisted study
workflows into one responsive product.

> Everything your campus needs, in one place.

## Features

- Premium public landing, login, and signup experiences
- Responsive student dashboard with announcements and quick actions
- Student profiles, contribution history, badges, and achievements
- Searchable notes library with filters, preview, upload, and bookmarks
- Discussions and Q&A with votes, replies, topics, and contributor widgets
- Student marketplace with listings, seller previews, filters, and wishlists
- Internship discovery with saved roles, career tips, and application previews
- Campus events with calendar filtering, RSVP, saved events, and registration
- Leaderboard with XP, challenges, streaks, badges, and department rankings
- Frontend-only AI Study Assistant with rich dummy conversations and study tools
- Complete settings, notification center, responsive navigation, toasts, modals,
  empty states, and loading skeletons

All content and interactions currently use realistic local dummy data. No
backend, authentication service, payment service, or AI API is connected.

## Tech Stack

- React 19
- TypeScript
- Vite
- Tailwind CSS 4
- React Router
- Lucide React icons
- ESLint

## Current Status

The frontend MVP is feature-complete and production-build ready for portfolio
demonstrations, product validation, and future API integration.

- All planned frontend routes are available.
- Interactive actions provide visible local feedback.
- Layouts support desktop, laptop, tablet, and mobile sizes.
- Forms, navigation, dialogs, and controls include accessible labels and focus
  states.
- `npm run lint` and `npm run build` are the release quality gates.

## Screenshots

Add release screenshots to `docs/screenshots/` before publishing the public
portfolio:

- `landing-page.png`
- `student-dashboard.png`
- `notes-library.png`
- `ai-study-assistant.png`
- `mobile-navigation.png`

## Run Locally

Requirements: Node.js 20 or newer and npm.

```bash
cd CampusOne
npm install
npm run dev
```

Open the local address printed by Vite.

Useful commands:

```bash
npm run lint
npm run build
npm run preview
```

## Project Structure

```text
src/
  assets/       Static asset guidance
  components/   Shared UI, layout, cards, and form controls
  data/         Realistic frontend dummy data
  pages/        Route-level product experiences
  routes/       Central route definitions
  styles/       Design tokens and global motion
  types/        Shared TypeScript models
  utils/        Small framework-independent helpers
```

## Future Backend Plan

The local data modules are intentionally separated from page components so they
can be replaced incrementally with API clients. A future backend can add:

1. Authentication, university verification, and session management
2. Persistent profiles, notes, discussions, listings, events, and bookmarks
3. File storage and content moderation
4. Search, notification delivery, and recommendation services
5. Secure internship and marketplace messaging
6. AI study workflows with consent, usage limits, and safety controls

## Author

Designed and developed by **Mustafa Nasir**.
