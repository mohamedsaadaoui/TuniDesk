import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { authApi } from "../api";
import { errorMessage } from "../api/client";
import { Button, Field, Input } from "../components/ui";

export function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await authApi.forgotPassword({ email });
      setSent(true);
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

        {sent ? (
          <>
            <h1 className="auth-title">Check your inbox</h1>
            <p className="auth-sub">
              If an account exists for <strong>{email}</strong>, we sent a link to reset your
              password. The link expires in 60 minutes.
            </p>
          </>
        ) : (
          <>
            <h1 className="auth-title">Forgot your password?</h1>
            <p className="auth-sub">Enter your email and we will send you a reset link.</p>

            {error && <div className="form-error">{error}</div>}

            <form onSubmit={onSubmit}>
              <Field label="Email" required>
                <Input
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@company.com"
                  required
                />
              </Field>

              <Button type="submit" disabled={loading}>
                {loading ? "Sending…" : "Send reset link"}
              </Button>
            </form>
          </>
        )}

        <div className="auth-switch">
          Remembered it? <Link to="/login">Back to sign in</Link>
        </div>
      </div>
    </div>
  );
}
