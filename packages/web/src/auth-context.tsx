import { createContext, useState, useContext, useEffect } from "react";
import { apiRoot } from "./config";

interface Membership {
  status: string;
  membership_type: string;
  membership_until: string;
}
interface AuthContextType {
  authToken: string | null;
  userId: string | null;
  login: (newToken: string, newUserId: string, newMembership: Membership) => void;
  logout: () => void;
  membership: Membership;
  linkToMembership: string | null;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [authToken, setAuthToken] = useState<string | null>(localStorage.getItem("token"));
  const [userId, setUserId] = useState<string | null>(localStorage.getItem("userId"));
  const [membership, setMembership] = useState<Membership>(() => {
    const membershipInfo = localStorage.getItem("membershipInfo");
    return membershipInfo ? JSON.parse(membershipInfo) : { membership_type: "free", membership_until: "" };
  });

  const login = (newToken: string, newUserId: string, newMembership: Membership) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("userId", newUserId);
    localStorage.setItem("membership", JSON.stringify(newMembership));
    localStorage.setItem("shown_messages", "");
    setAuthToken(newToken);
    setUserId(newUserId);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("membership");
    localStorage.removeItem("shown_messages");
    setAuthToken(null);
    setUserId(null);
  };

  useEffect(() => {
    const fetchMembership = async () => {
      const response = await fetch(`${apiRoot}/api/membership/${userId}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });
      const data = await response.json();
      setMembership(data);
    };

    if (userId) {
      fetchMembership();
    }
  }, [userId]);

  return (
    <AuthContext.Provider value={{ authToken, userId, login, logout, membership, linkToMembership: null }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
