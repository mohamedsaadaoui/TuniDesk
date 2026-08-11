import { useState, type FormEvent } from "react";
import { userApi } from "../api";
import { errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Button, Card, Field, Input, PageHeader } from "../components/ui";
import { formatDateTime } from "../lib/format";

export function Profile() {
  const { user, setUser } = useAuth();

  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [lastName, setLastName] = useState(user?.lastName ?? "");
  const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber ?? "");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  if (!user) return null;

  const submitProfile = async (e: FormEvent) => {
    e.preventDefault();
    setSavingProfile(true);
    setError("");
    setMessage("");
    try {
      const updated = await userApi.profile({ firstName, lastName, phoneNumber });
      setUser(updated);
      setMessage("Profile updated");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSavingProfile(false);
    }
  };

  const submitPassword = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setMessage("");
    if (newPassword !== confirmPassword) {
      setError("New passwords do not match");
      return;
    }
    setSavingPassword(true);
    try {
      await userApi.changePassword({ currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setMessage("Password changed");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div>
      <PageHeader title="My profile" subtitle="Manage your account details" />

      {message && <div className="form-success">{message}</div>}
      {error && <div className="form-error">{error}</div>}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, alignItems: "start" }}>
        <Card style={{ padding: 20 }}>
          <h3 style={{ marginBottom: 14 }}>Profile information</h3>
          <form onSubmit={submitProfile} className="stack">
            <Field label="First name" required>
              <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} required maxLength={100} />
            </Field>
            <Field label="Last name" required>
              <Input value={lastName} onChange={(e) => setLastName(e.target.value)} required maxLength={100} />
            </Field>
            <Field label="Phone number">
              <Input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} maxLength={20} />
            </Field>
            <div className="form-actions" style={{ marginTop: 4 }}>
              <Button type="submit" disabled={savingProfile}>
                {savingProfile ? "Saving…" : "Save profile"}
              </Button>
            </div>
          </form>
        </Card>

        <Card style={{ padding: 20 }}>
          <h3 style={{ marginBottom: 14 }}>Change password</h3>
          <form onSubmit={submitPassword} className="stack">
            <Field label="Current password" required>
              <Input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </Field>
            <Field label="New password" required>
              <Input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={6}
                autoComplete="new-password"
              />
            </Field>
            <Field label="Confirm new password" required>
              <Input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={6}
                autoComplete="new-password"
              />
            </Field>
            <div className="form-actions" style={{ marginTop: 4 }}>
              <Button type="submit" disabled={savingPassword}>
                {savingPassword ? "Changing…" : "Change password"}
              </Button>
            </div>
          </form>
        </Card>
      </div>

      <Card style={{ padding: 20, marginTop: 16 }}>
        <h3 style={{ marginBottom: 14 }}>Account details</h3>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">Email</span>
            <span className="info-value">{user.email}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Roles</span>
            <span className="info-value">{user.roles.join(", ")}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Member since</span>
            <span className="info-value">{formatDateTime(user.createdAt)}</span>
          </div>
        </div>
      </Card>
    </div>
  );
}
