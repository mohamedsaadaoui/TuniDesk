import { useEffect, useState, type FormEvent } from "react";
import { categoryApi } from "../api";
import { errorMessage } from "../api/client";
import type { Category } from "../api/types";
import {
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Spinner,
  Textarea,
} from "../components/ui";

export function Categories() {
  const [items, setItems] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [active, setActive] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    categoryApi
      .list(false)
      .then(setItems)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const openCreate = () => {
    setEditing(null);
    setName("");
    setDescription("");
    setActive(true);
    setModalOpen(true);
  };

  const openEdit = (c: Category) => {
    setEditing(c);
    setName(c.name);
    setDescription(c.description ?? "");
    setActive(c.active);
    setModalOpen(true);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      const payload = { name, description, active };
      if (editing) {
        await categoryApi.update(editing.id, payload);
      } else {
        await categoryApi.create(payload);
      }
      setModalOpen(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (c: Category) => {
    if (!window.confirm(`Delete category "${c.name}"?`)) return;
    try {
      await categoryApi.remove(c.id);
      load();
    } catch (err) {
      alert(errorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        title="Categories"
        subtitle="Organize tickets by topic"
        actions={
          <Button onClick={openCreate}>+ New category</Button>
        }
      />

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <Spinner label="Loading categories…" />
      ) : (
        <Card>
          {items.length === 0 ? (
            <EmptyState title="No categories yet" />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((c) => (
                    <tr key={c.id}>
                      <td style={{ fontWeight: 650 }}>{c.name}</td>
                      <td className="muted">{c.description || "—"}</td>
                      <td>
                        <span className={`badge ${c.active ? "badge-resolved" : "badge-closed"}`}>
                          {c.active ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                          <Button variant="secondary" size="sm" onClick={() => openEdit(c)}>
                            Edit
                          </Button>
                          <Button variant="danger" size="sm" onClick={() => remove(c)}>
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
        </Card>
      )}

      <Modal open={modalOpen} title={editing ? "Edit category" : "New category"} onClose={() => setModalOpen(false)}>
        <form onSubmit={submit}>
          <Field label="Name" required>
            <Input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} required />
          </Field>
          <Field label="Description">
            <Textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={500} />
          </Field>
          <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13.5, marginBottom: 8 }}>
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            Active
          </label>
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
