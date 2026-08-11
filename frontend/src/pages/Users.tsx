import { useEffect, useState, type FormEvent } from "react";
import { userApi } from "../api";
import { errorMessage } from "../api/client";
import type { Page, User } from "../api/types";
import {
  Avatar,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
} from "../components/ui";
import { formatDateTime, fullName } from "../lib/format";

const ROLES = ["ADMIN", "AGENT", "USER"];

export function Users() {
  const [data, setData] = useState<Page<User> | null>(null);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("USER");
  const [enabled, setEnabled] = useState(true);
  const [selectedRoles, setSelectedRoles] = useState<string[]>(["USER"]);
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    userApi
      .list({ search: search || undefined, page, size: 10 })
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, [page, search]);

  const openCreate = () => {
    setEditing(null);
    setFirstName("");
    setLastName("");
    setEmail("");
    setPhoneNumber("");
    setPassword("");
    setRole("USER");
    setEnabled(true);
    setSelectedRoles(["USER"]);
    setModalOpen(true);
  };

  const openEdit = (u: User) => {
    setEditing(u);
    setFirstName(u.firstName);
    setLastName(u.lastName);
    setEmail(u.email);
    setPhoneNumber(u.phoneNumber ?? "");
    setPassword("");
    setEnabled(u.enabled);
    setSelectedRoles(u.roles);
    setModalOpen(true);
  };

  const toggleRole = (r: string) => {
    setSelectedRoles((prev) =>
      prev.includes(r) ? prev.filter((x) => x !== r) : [...prev, r]
    );
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      if (editing) {
        await userApi.update(editing.id, {
          firstName,
          lastName,
          email,
          phoneNumber,
          enabled,
          roles: selectedRoles,
        });
      } else {
        await userApi.create({
          firstName,
          lastName,
          email,
          password,
          phoneNumber,
          role,
        });
      }
      setModalOpen(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (u: User) => {
    if (!window.confirm(`Delete user ${u.email}?`)) return;
    try {
      await userApi.remove(u.id);
      load();
    } catch (err) {
      alert(errorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        title="Users"
        subtitle="Manage accounts and roles"
        actions={<Button onClick={openCreate}>+ New user</Button>}
      />

      <div className="filters">
        <Input
          className="search"
          placeholder="Search by name or email…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <Spinner label="Loading users…" />
      ) : (
        <Card>
          {data && data.content.length === 0 ? (
            <EmptyState title="No users found" />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Email</th>
                    <th>Roles</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                          <Avatar firstName={u.firstName} lastName={u.lastName} />
                          <span style={{ fontWeight: 650 }}>{fullName(u.firstName, u.lastName)}</span>
                        </div>
                      </td>
                      <td>{u.email}</td>
                      <td>
                        <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
                          {u.roles.map((r) => (
                            <span key={r} className="tag">
                              {r}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <span className={`badge ${u.enabled ? "badge-resolved" : "badge-closed"}`}>
                          {u.enabled ? "Enabled" : "Disabled"}
                        </span>
                      </td>
                      <td className="muted">{formatDateTime(u.createdAt)}</td>
                      <td>
                        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                          <Button variant="secondary" size="sm" onClick={() => openEdit(u)}>
                            Edit
                          </Button>
                          <Button variant="danger" size="sm" onClick={() => remove(u)}>
                            Delete
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {data && (
            <div style={{ padding: "0 16px" }}>
              <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
            </div>
          )}
        </Card>
      )}

      <Modal open={modalOpen} title={editing ? "Edit user" : "New user"} onClose={() => setModalOpen(false)}>
        <form onSubmit={submit}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <Field label="First name" required>
              <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
            </Field>
            <Field label="Last name" required>
              <Input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
            </Field>
          </div>
          <Field label="Email" required>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </Field>
          <Field label="Phone number">
            <Input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
          </Field>

          {editing ? (
            <>
              <Field label="Roles">
                <div style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
                  {ROLES.map((r) => (
                    <label key={r} style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13.5 }}>
                      <input
                        type="checkbox"
                        checked={selectedRoles.includes(r)}
                        onChange={() => toggleRole(r)}
                      />
                      {r}
                    </label>
                  ))}
                </div>
              </Field>
              <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13.5, marginBottom: 14 }}>
                <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
                Enabled
              </label>
            </>
          ) : (
            <>
              <Field label="Password" required>
                <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
              </Field>
              <Field label="Role">
                <Select value={role} onChange={(e) => setRole(e.target.value)}>
                  <option value="USER">USER</option>
                  <option value="AGENT">AGENT</option>
                  <option value="ADMIN">ADMIN</option>
                </Select>
              </Field>
            </>
          )}

          <div className="form-actions">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Saving…" : "Save"}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
