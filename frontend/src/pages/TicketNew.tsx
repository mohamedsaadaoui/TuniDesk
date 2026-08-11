import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { categoryApi, ticketApi } from "../api";
import { errorMessage } from "../api/client";
import type { Category, TicketPriority } from "../api/types";
import {
  Button,
  Card,
  Field,
  Input,
  PageHeader,
  Select,
  Spinner,
  Textarea,
} from "../components/ui";

export function TicketNew() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<Category[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
  const [categoryId, setCategoryId] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingCategories, setLoadingCategories] = useState(true);

  useEffect(() => {
    categoryApi
      .list(true)
      .then((cats) => {
        setCategories(cats);
        if (cats.length > 0) setCategoryId(cats[0].id);
      })
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoadingCategories(false));
  }, []);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const ticket = await ticketApi.create({ title, description, priority, categoryId });
      navigate(`/tickets/${ticket.id}`, { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  if (loadingCategories) return <Spinner label="Loading categories…" />;

  return (
    <div style={{ maxWidth: 680 }}>
      <PageHeader title="New ticket" subtitle="Describe the issue you need help with" />

      <Card style={{ padding: 22 }}>
        {error && <div className="form-error">{error}</div>}

        <form onSubmit={onSubmit}>
          <Field label="Title" required>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Cannot connect to the VPN"
              maxLength={200}
              required
            />
          </Field>

          <Field label="Description">
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide details about the issue…"
              maxLength={4000}
            />
          </Field>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
            <Field label="Priority" required>
              <Select value={priority} onChange={(e) => setPriority(e.target.value as TicketPriority)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </Select>
            </Field>

            <Field label="Category" required>
              <Select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                {categories.length === 0 && <option value="">No categories available</option>}
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </Field>
          </div>

          <div className="form-actions">
            <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading || categories.length === 0}>
              {loading ? "Creating…" : "Create ticket"}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
