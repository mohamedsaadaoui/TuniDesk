import { useEffect, useState, type FormEvent } from "react";
import { slaApi } from "../api";
import { errorMessage } from "../api/client";
import type { SlaPolicy } from "../api/types";
import {
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  PriorityBadge,
  Spinner,
} from "../components/ui";

export function SlaPolicies() {
  const [items, setItems] = useState<SlaPolicy[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [editing, setEditing] = useState<SlaPolicy | null>(null);
  const [name, setName] = useState("");
  const [hours, setHours] = useState(24);
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    slaApi
      .list()
      .then(setItems)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const openEdit = (p: SlaPolicy) => {
    setEditing(p);
    setName(p.name);
    setHours(p.hours);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    if (!editing) return;
    setSaving(true);
    try {
      await slaApi.update(editing.id, { name, hours });
      setEditing(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="SLA policies" subtitle="Resolution deadlines per ticket priority" />

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <Spinner label="Loading SLA policies…" />
      ) : (
        <Card>
          {items.length === 0 ? (
            <EmptyState title="No SLA policies" />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Priority</th>
                    <th>Policy name</th>
                    <th>Resolution hours</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <PriorityBadge priority={p.priority} />
                      </td>
                      <td style={{ fontWeight: 650 }}>{p.name}</td>
                      <td>{p.hours} hours</td>
                      <td>
                        <div style={{ display: "flex", justifyContent: "flex-end" }}>
                          <Button variant="secondary" size="sm" onClick={() => openEdit(p)}>
                            Edit
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

      <Modal open={Boolean(editing)} title={`Edit ${editing?.priority ?? ""} policy`} onClose={() => setEditing(null)}>
        <form onSubmit={submit}>
          <Field label="Policy name" required>
            <Input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} required />
          </Field>
          <Field label="Resolution hours" required>
            <Input
              type="number"
              min={1}
              value={hours}
              onChange={(e) => setHours(Number(e.target.value))}
              required
            />
          </Field>
          <div className="form-actions">
            <Button type="button" variant="secondary" onClick={() => setEditing(null)}>
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
