import { useState, type FormEvent } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { authApi } from "../api";
import { errorMessage } from "../api/client";
import { Button, Field, Input } from "../components/ui";

export function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [done, setDone] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (!token) return <Navigate to="/login" replace />;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");

    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({ token, newPassword: password });
      setDone(true);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="brand-logo">TD</span>
          <span style={{ fontSize: 18, fontWeight: 750 }}>TuniDesk</span>
        </div>

        {done ? (
          <>
            <h1 className="auth-title">Password updated</h1>
            <p className="auth-sub">Your password has been changed. You can now sign in.</p>
            <Link className="btn btn-primary btn-md" to="/login">
              Go to sign in
            </Link>
          </>
        ) : (
          <>
            <h1 className="auth-title">Choose a new password</h1>
            <p className="auth-sub">Pick a strong password of at least 8 characters.</p>

            {error && <div className="form-error">{error}</div>}

            <form onSubmit={onSubmit}>
              <Field label="New password" required>
                <Input
                  type="password"
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  minLength={8}
                  required
                />
              </Field>

              <Field label="Confirm new password" required>
                <Input
                  type="password"
                  autoComplete="new-password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="••••••••"
                  minLength={8}
                  required
                />
              </Field>

              <Button type="submit" disabled={loading}>
                {loading ? "Saving…" : "Reset password"}
              </Button>
            </form>
          </>
        )}

        <div className="auth-switch">
          Didn't request this? <Link to="/login">Back to sign in</Link>
        </div>
      </div>
    </div>
  );
}
