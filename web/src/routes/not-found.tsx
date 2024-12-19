import { Link } from "wouter";

export default function NotFound() {
  return (
    <section className="flex flex-col items-center justify-center min-h-screen bg-gray-900 text-white space-y-4">
      <h1 className="text-3xl font-bold">404 - Page Not Found</h1>
      <p className="text-lg">Sorry, the page you are looking for does not exist.</p>
      <Link
        className="inline-flex h-9 items-center justify-center rounded-md bg-zinc-50 px-4 py-2 text-sm font-medium text-zinc-900 shadow transition-colors hover:bg-gray-200 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-gray-600 disabled:pointer-events-none disabled:opacity-50"
        href="/"
      >
        Return to Homepage
      </Link>
    </section>
  );
}
