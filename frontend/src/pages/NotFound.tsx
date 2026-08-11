import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function NotFound() {
  const { isAuthenticated } = useAuth();
  return (
    <div className="auth-screen">
      <div className="auth-card" style={{ textAlign: "center" }}>
        <h1 className="auth-title">404</h1>
        <p className="auth-sub">This page does not exist.</p>
        <Link className="btn btn-primary btn-md" to={isAuthenticated ? "/" : "/login"}>
          Go home
        </Link>
      </div>
    </div>
  );
}
