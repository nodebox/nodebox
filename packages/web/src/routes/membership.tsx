import StaticPage from "../components/static-page";

export default function Membership() {
  return (
    <StaticPage title="NodeBox Live Plus Membership">
      <article className="max-w-4xl m-auto flex flex-col gap-4 py-16">
        <h1 className="text-3xl font-bold">NodeBox Live Plus</h1>

        <p className="text-lg">Upgrade to NodeBox Live Plus to unlock premium features:</p>

        <div className="bg-zinc-800 p-6 rounded-lg my-4">
          <h2 className="text-xl font-bold mb-4">Plus Membership Benefits</h2>
          <ul className="list-disc list-inside space-y-2">
            <li>Create unlimited private projects</li>
            <li>Share projects for online viewing</li>
            <li>Priority support</li>
            <li>Early access to new features</li>
          </ul>
        </div>

        <h2 className="text-xl font-bold mt-4">How to Upgrade</h2>
        <p>
          To get a Plus membership, please contact our support team at{" "}
          <a href="mailto:support@nodebox.live" className="text-blue-400 hover:underline">
            support@nodebox.live
          </a>
        </p>

        <p className="mt-4">
          Our team will assist you with setting up your Plus membership and answer any questions you might have.
        </p>

        <div className="bg-blue-900 bg-opacity-30 border border-blue-700 p-6 rounded-lg my-6">
          <h3 className="font-bold mb-2">Ready to Upgrade?</h3>
          <p>
            Send an email to{" "}
            <a href="mailto:support@nodebox.live" className="text-blue-400 font-bold hover:underline">
              support@nodebox.live
            </a>{" "}
            with the subject line "Plus Membership Request" and include your account email.
          </p>
        </div>
      </article>
    </StaticPage>
  );
}
