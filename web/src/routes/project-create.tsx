import React from "react";
import { TextField, SubmitField } from "../components/fields";
import { useAuth } from "../auth-context";
import PageWrapper from "../components/page-wrapper";
import InlineMessage from "../components/inline-message";
import { apiRoot } from "../config";
import { v4 as uuidv4 } from "uuid";
import Icon from "../components/icon";
const projectId = uuidv4();

export default function ProjectCreate() {
  const [selectedOption, setSelectedOption] = React.useState("public");
  const { userId, membership, linkToMembership } = useAuth()!;
  const [error, setError] = React.useState(null);
  const [projectTitle, setProjectTitle] = React.useState("");

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    setError(null);
    e.preventDefault();
    const res = await fetch(`${apiRoot}/api/projects`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      body: JSON.stringify({
        userId,
        projectId: projectId,
        projectTitle: projectTitle,
        public: selectedOption || "public",
      }),
    });
    const json = await res.json();

    if (json.status !== "ok") {
      setError(json.message);
      return;
    }

    document.location.href = `/${userId}/${projectId}`;
  }

  const optionStyle = {
    borderWidth: "1px",
    borderStyle: "solid",
    borderColor: "transparent",
    margin: "8px",
    padding: "8px",
    cursor: "pointer",
    transition: "border-color 0.2s ease-in-out",
    borderRadius: "4px",
  };

  const selectedStyle = {
    borderColor: "blue",
  };

  const isPrivateDisabled = membership?.membership_type !== "plus";
  const isLinkToMembership = linkToMembership !== null;
  return (
    <>
      <PageWrapper>
        <div className="login-form py-10 px-10 flex flex-col items-stretch">
          <h1 className="text-xl text-center mb-7">Create Project</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <form className="flex flex-col gap-2" onSubmit={handleSubmit}>
            <TextField
              label="Project Title"
              name="projectTitle"
              placeholder="myProject"
              autoComplete="new-password"
              autoFocus
              value={projectTitle}
              onChange={(e) => setProjectTitle(e.target.value)}
            />
            <div className="form-row mb-6">
              <h3 className="text-zinc-100 mb-2 text-sm">Project Visibility</h3>
              <div className="form-options">
                <div
                  style={{
                    ...optionStyle,
                    ...(selectedOption === "public" ? selectedStyle : {}),
                  }}
                  onMouseEnter={(e) => (e.currentTarget.className = "border-blue-500")}
                  onMouseLeave={(e) => {
                    if (selectedOption !== "public") e.currentTarget.className = "border-zinc-500";
                  }}
                  onClick={() => setSelectedOption("public")}
                >
                  <div className="form-row ml-6 mt-3 mb-3">
                    <h3 className="text-zinc-100 mb-1 text-sm">Public</h3>
                    <h3 className="text-zinc-500 mb-1 text-sm">Public projects are visible to everyone.</h3>
                  </div>
                </div>

                <div
                  style={{
                    ...optionStyle,
                    ...(selectedOption === "private" ? selectedStyle : {}),
                    ...(isPrivateDisabled ? { cursor: "not-allowed", opacity: 0.5 } : {}),
                  }}
                  onMouseEnter={(e) => {
                    if (!isPrivateDisabled) e.currentTarget.style.borderColor = "blue";
                  }}
                  onMouseLeave={(e) => {
                    if (selectedOption !== "private") e.currentTarget.style.borderColor = "transparent";
                  }}
                  onClick={() => {
                    if (!isPrivateDisabled) setSelectedOption("private");
                  }}
                >
                  <div className="form-row ml-6 mt-3 mb-3">
                    <h3 className="text-zinc-100 mb-1 text-sm flex items-center">
                      Private <Icon name="lock" className="ml-2" />
                    </h3>
                    <h3 className="text-zinc-500 mb-1 text-sm">
                      Private projects are only visible to you.
                      {membership && membership.membership_type !== "plus" && (
                        <>
                          {" "}
                          This is a premium feature.{" "}
                          {isLinkToMembership ? (
                            <>
                              <a
                                className="text-blue-600"
                                href={linkToMembership}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                Upgrade your account
                              </a>
                              .
                            </>
                          ) : (
                            "(feature is coming soon)"
                          )}
                        </>
                      )}
                    </h3>
                  </div>
                </div>
              </div>
            </div>
            <SubmitField name="create" label="Create" disabled={projectTitle.trim().length === 0} />
          </form>
        </div>
      </PageWrapper>
    </>
  );
}
