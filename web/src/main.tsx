import React, { Suspense } from "react";
import ReactDOM from "react-dom/client";
import { Link, Route, Switch } from "wouter";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import { AuthProvider } from "./auth-context";
const Home = React.lazy(() => import("./routes/home"));
const Terms = React.lazy(() => import("./routes/terms"));
const Privacy = React.lazy(() => import("./routes/privacy"));
const Login = React.lazy(() => import("./routes/login"));
const Signup = React.lazy(() => import("./routes/signup"));
const ForgotPassword = React.lazy(() => import("./routes/forgot-password"));
const ResetEmailSent = React.lazy(() => import("./routes/reset-email-sent"));
const ResetPassword = React.lazy(() => import("./routes/reset-password"));
const ProjectCreate = React.lazy(() => import("./routes/project-create"));
const RawEditor = React.lazy(() => import("./routes/raw-editor"));
const Embed = React.lazy(() => import("./routes/embed"));
const EmbedPreview = React.lazy(() => import("./routes/embed-preview"));
const ProjectList = React.lazy(() => import("./routes/project-list"));
const Editor = React.lazy(() => import("./editor"));
const Gallery = React.lazy(() => import("./routes/gallery"));
const GalleryCategory = React.lazy(() => import("./routes/gallery-category"));

import "./index.css";
import "./no-select.css";

function Loading() {
  return <div>Loading...</div>;
}

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

ReactDOM.createRoot(document.getElementById("root")!).render(
  <AuthProvider>
    <Suspense fallback={<Loading />}>
      <Switch>
        <Route path="/" component={Home} />
        <Route path="/terms" component={Terms} />
        <Route path="/privacy" component={Privacy} />
        <Route path="/auth/login" component={Login} />
        <Route path="/auth/signup" component={Signup} />
        <Route path="/auth/forgot-password" component={ForgotPassword} />
        <Route path="/auth/reset-email-sent" component={ResetEmailSent} />
        <Route path="/auth/reset-password" component={ResetPassword} />
        <Route path="/create" component={ProjectCreate} />
        <Route path="/raw/:userId/:projectId" component={RawEditor} />
        <Route path="/embed/:userId/:projectId/" component={Embed} />
        <Route path="/embed/:userId/:projectId/:item" component={Embed} />
        <Route path="/embed-preview/:userId/:projectId" component={EmbedPreview} />
        <Route path="/embed-preview/:userId/:projectId/:item" component={EmbedPreview} />
        <Route path="/gallery" component={Gallery} />
        <Route path="/gallery/:category" component={GalleryCategory} />
        <Route path="/:userId" component={ProjectList} />
        <Route path="/:userId/:projectId/:version" component={Editor} />
        <Route path="/:userId/:projectId" component={Editor} />
        <Route component={NotFound} />
      </Switch>
    </Suspense>
    <ToastContainer position="top-center" autoClose={5000} closeOnClick pauseOnFocusLoss pauseOnHover theme="dark" />
  </AuthProvider>,
);
