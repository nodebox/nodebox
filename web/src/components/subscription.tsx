import { useAuth } from "../auth-context";

interface SubscriptionProps {}

function Subscription(_: SubscriptionProps): JSX.Element {
  const { userId, membership, linkToMembership } = useAuth()!;
  const isLinkToMembership = linkToMembership !== null;
  if (userId === null) return <></>;
  if (membership === null) return <></>;
  if (membership!.status !== "ok") return <></>;
  if (!membership.membership_type) return <></>;
  return (
    <div className="bg-zinc-600 text-xs px-3 py-1">
      <div className="bg-zinc-600 text-xs rounded-md w-full border border-zinc-500">
        <div>
          <div className="w-full m-1">{userId}</div>
        </div>
        <hr className="border-zinc-500"></hr>
        <div className="flex flex-row flex-nowrap m-1">
          <span className="w-[70px]">Plan:</span>
          <span>{membership.membership_type.charAt(0).toUpperCase() + membership.membership_type.slice(1)}</span>
        </div>
        {membership.membership_type === "plus" && (
          <div className="flex flex-row flex-nowrap m-1">
            <span className="w-[70px]">Valid Until:</span>
            <span>{new Date(membership.membership_until).toLocaleDateString()}</span>
          </div>
        )}
      </div>
      {userId && membership.membership_type !== "plus" && (
        <div className="text-zinc-500 mb-1">
          Private projects are only visible to you.
          {membership && membership.membership_type !== "plus" && (
            <>
              {" "}
              This is a premium feature.{" "}
              {isLinkToMembership ? (
                <>
                  <a className="text-blue-600" href={linkToMembership} target="_blank" rel="noopener noreferrer">
                    Upgrade your account
                  </a>
                  .
                </>
              ) : (
                "(feature is coming soon)"
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default Subscription;
