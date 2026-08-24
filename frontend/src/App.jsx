import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import { useEffect, useState } from "react";
import axios from "axios";
import "./App.css";

const API =
  import.meta.env.VITE_API_URL ||
  "http://localhost:8080";

const api = axios.create({
  baseURL: API.replace(/\/$/, ""),
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token =
      localStorage.getItem("token");

    if (token) {
      config.headers =
        config.headers || {};

      config.headers.Authorization =
        `Bearer ${token}`;
    }

    return config;
  },
  (error) =>
    Promise.reject(error)
);

/* =========================================================
   HELPERS
   ========================================================= */

function getErrorMessage(
  error,
  fallback
) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback
  );
}

/* =========================================================
   AUTH PAGE
   ========================================================= */

function AuthPage({ onLogin }) {
  const [mode, setMode] =
    useState("login");

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="brand">
          AI-EOMS
        </div>

        <h1>
          {mode === "login"
            ? "Enterprise Operations"
            : "Create your account"}
        </h1>

        <p>
          {mode === "login"
            ? "AI-powered incident management platform"
            : "Join the AI-EOMS operations platform"}
        </p>

        {mode === "login" ? (
          <LoginForm
            onLogin={onLogin}
            onSwitchToSignup={() =>
              setMode("signup")
            }
          />
        ) : (
          <SignupForm
            onLogin={onLogin}
            onSwitchToLogin={() =>
              setMode("login")
            }
          />
        )}
      </div>
    </div>
  );
}

/* =========================================================
   LOGIN
   ========================================================= */

function LoginForm({
  onLogin,
  onSwitchToSignup,
}) {
  const [username, setUsername] =
    useState("");

  const [password, setPassword] =
    useState("");

  const [error, setError] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const login = async (event) => {
    event.preventDefault();

    setError("");

    const cleanUsername =
      username.trim();

    if (!cleanUsername || !password) {
      setError(
        "Username and password are required."
      );
      return;
    }

    try {
      setLoading(true);

      const { data } =
        await api.post(
          "/api/auth/login",
          {
            username: cleanUsername,
            password,
          }
        );

      if (!data?.token) {
        throw new Error(
          "Login succeeded but no authentication token was returned."
        );
      }

      localStorage.setItem(
        "token",
        data.token
      );

      localStorage.setItem(
        "user",
        JSON.stringify(data)
      );

      onLogin(data);
    } catch (error) {
      console.error(
        "Login failed:",
        error
      );

      setError(
        getErrorMessage(
          error,
          "Invalid username or password."
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <form onSubmit={login}>
        <label htmlFor="login-username">
          Username
        </label>

        <input
          id="login-username"
          type="text"
          value={username}
          onChange={(event) =>
            setUsername(
              event.target.value
            )
          }
          placeholder="Enter your username"
          autoComplete="username"
          required
        />

        <label htmlFor="login-password">
          Password
        </label>

        <input
          id="login-password"
          type="password"
          value={password}
          onChange={(event) =>
            setPassword(
              event.target.value
            )
          }
          placeholder="Enter your password"
          autoComplete="current-password"
          required
        />

        {error && (
          <div className="error">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
        >
          {loading
            ? "Signing in..."
            : "Sign in"}
        </button>
      </form>

      <div className="auth-switch">
        <span>
          Don't have an account?
        </span>

        <button
          type="button"
          className="link-button"
          onClick={onSwitchToSignup}
        >
          Create account
        </button>
      </div>
    </>
  );
}

/* =========================================================
   SIGNUP
   ========================================================= */

function SignupForm({
  onLogin,
  onSwitchToLogin,
}) {
  const [form, setForm] =
    useState({
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
      firstName: "",
      lastName: "",
    });

  const [error, setError] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const updateField = (
    field,
    value
  ) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const signup = async (event) => {
    event.preventDefault();

    setError("");

    const username =
      form.username.trim();

    const email =
      form.email.trim();

    const firstName =
      form.firstName.trim();

    const lastName =
      form.lastName.trim();

    if (
      !username ||
      !email ||
      !form.password ||
      !firstName
    ) {
      setError(
        "Please complete all required fields."
      );
      return;
    }

    if (form.password.length < 8) {
      setError(
        "Password must contain at least 8 characters."
      );
      return;
    }

    if (
      form.password !==
      form.confirmPassword
    ) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    try {
      setLoading(true);

      await api.post(
        "/api/auth/register",
        {
          username,
          email,
          password:
            form.password,
          firstName,
          lastName:
            lastName || null,
        }
      );

      const { data } =
        await api.post(
          "/api/auth/login",
          {
            username,
            password:
              form.password,
          }
        );

      if (!data?.token) {
        throw new Error(
          "Account was created but automatic login failed because no token was returned."
        );
      }

      localStorage.setItem(
        "token",
        data.token
      );

      localStorage.setItem(
        "user",
        JSON.stringify(data)
      );

      onLogin(data);
    } catch (error) {
      console.error(
        "Registration failed:",
        error
      );

      setError(
        getErrorMessage(
          error,
          "Unable to create your account."
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <form onSubmit={signup}>
        <div className="name-grid">
          <div>
            <label htmlFor="first-name">
              First name
            </label>

            <input
              id="first-name"
              type="text"
              value={
                form.firstName
              }
              onChange={(event) =>
                updateField(
                  "firstName",
                  event.target.value
                )
              }
              placeholder="First name"
              autoComplete="given-name"
              required
            />
          </div>

          <div>
            <label htmlFor="last-name">
              Last name
            </label>

            <input
              id="last-name"
              type="text"
              value={
                form.lastName
              }
              onChange={(event) =>
                updateField(
                  "lastName",
                  event.target.value
                )
              }
              placeholder="Last name"
              autoComplete="family-name"
            />
          </div>
        </div>

        <label htmlFor="signup-username">
          Username
        </label>

        <input
          id="signup-username"
          type="text"
          value={form.username}
          onChange={(event) =>
            updateField(
              "username",
              event.target.value
            )
          }
          placeholder="Choose a username"
          autoComplete="username"
          minLength={3}
          maxLength={100}
          required
        />

        <label htmlFor="signup-email">
          Email
        </label>

        <input
          id="signup-email"
          type="email"
          value={form.email}
          onChange={(event) =>
            updateField(
              "email",
              event.target.value
            )
          }
          placeholder="you@example.com"
          autoComplete="email"
          required
        />

        <label htmlFor="signup-password">
          Password
        </label>

        <input
          id="signup-password"
          type="password"
          value={form.password}
          onChange={(event) =>
            updateField(
              "password",
              event.target.value
            )
          }
          placeholder="Minimum 8 characters"
          autoComplete="new-password"
          minLength={8}
          required
        />

        <label htmlFor="confirm-password">
          Confirm password
        </label>

        <input
          id="confirm-password"
          type="password"
          value={
            form.confirmPassword
          }
          onChange={(event) =>
            updateField(
              "confirmPassword",
              event.target.value
            )
          }
          placeholder="Re-enter your password"
          autoComplete="new-password"
          minLength={8}
          required
        />

        {error && (
          <div className="error">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
        >
          {loading
            ? "Creating account..."
            : "Create account"}
        </button>
      </form>

      <div className="auth-switch">
        <span>
          Already have an account?
        </span>

        <button
          type="button"
          className="link-button"
          onClick={onSwitchToLogin}
        >
          Sign in
        </button>
      </div>
    </>
  );
}

/* =========================================================
   CREATE INCIDENT MODAL
   ========================================================= */

function CreateIncidentModal({
  onClose,
  onCreated,
}) {
  const [title, setTitle] =
    useState("");

  const [description, setDescription] =
    useState("");

  const [severity, setSeverity] =
    useState("MEDIUM");

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const createIncident = async (
    event
  ) => {
    event.preventDefault();

    const cleanTitle =
      title.trim();

    if (!cleanTitle) {
      setError(
        "Title is required."
      );
      return;
    }

    try {
      setLoading(true);
      setError("");

      const { data } =
        await api.post(
          "/api/incidents",
          {
            title: cleanTitle,
            description:
              description.trim(),
            severity,
          }
        );

      if (!data?.id) {
        throw new Error(
          "The server did not return the created incident."
        );
      }

      onCreated(data);
      onClose();
    } catch (error) {
      console.error(
        "Create incident failed:",
        error
      );

      setError(
        getErrorMessage(
          error,
          "Failed to create incident."
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-incident-title"
      >
        <div className="modal-header">
          <div>
            <p className="eyebrow">
              INCIDENT MANAGEMENT
            </p>

            <h2 id="create-incident-title">
              Create Incident
            </h2>
          </div>

          <button
            type="button"
            className="secondary"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <form
          onSubmit={
            createIncident
          }
        >
          <label htmlFor="incident-title">
            Title
          </label>

          <input
            id="incident-title"
            placeholder="e.g. Payment service outage"
            value={title}
            onChange={(event) =>
              setTitle(
                event.target.value
              )
            }
            required
          />

          <label htmlFor="incident-description">
            Description
          </label>

          <textarea
            id="incident-description"
            placeholder="Describe what happened..."
            value={description}
            onChange={(event) =>
              setDescription(
                event.target.value
              )
            }
          />

          <label htmlFor="incident-severity">
            Severity
          </label>

          <select
            id="incident-severity"
            value={severity}
            onChange={(event) =>
              setSeverity(
                event.target.value
              )
            }
          >
            <option value="LOW">
              LOW
            </option>

            <option value="MEDIUM">
              MEDIUM
            </option>

            <option value="HIGH">
              HIGH
            </option>

            <option value="CRITICAL">
              CRITICAL
            </option>
          </select>

          {error && (
            <div className="error">
              {error}
            </div>
          )}

          <div className="modal-actions">
            <button
              type="button"
              className="secondary"
              onClick={onClose}
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Creating..."
                : "Create Incident"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* =========================================================
   INCIDENT DETAILS MODAL
   ========================================================= */

function IncidentDetailsModal({
  incident,
  onClose,
  onUpdated,
}) {
  const [title, setTitle] =
    useState(
      incident?.title || ""
    );

  const [description, setDescription] =
    useState(
      incident?.description || ""
    );

  const [severity, setSeverity] =
    useState(
      incident?.severity ||
        "MEDIUM"
    );

  const [status, setStatus] =
    useState(
      incident?.status ||
        "OPEN"
    );

  const [assignedTo, setAssignedTo] =
    useState(
      incident?.assignedTo
        ? String(
            incident.assignedTo
          )
        : ""
    );

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const [aiLoading, setAiLoading] =
    useState(false);

  const [aiAnalysis, setAiAnalysis] =
    useState("");

  const saveChanges = async (
    event
  ) => {
    event.preventDefault();

    const cleanTitle =
      title.trim();

    if (!cleanTitle) {
      setError(
        "Title is required."
      );
      return;
    }

    const originalAssignedTo =
      incident?.assignedTo
        ? String(
            incident.assignedTo
          )
        : "";

    const cleanAssignedTo =
      assignedTo.trim();

    if (
      cleanAssignedTo &&
      !/^\d+$/.test(
        cleanAssignedTo
      )
    ) {
      setError(
        "Assigned user ID must be a valid number."
      );
      return;
    }

    try {
      setLoading(true);
      setError("");

      const { data } =
        await api.put(
          `/api/incidents/${incident.id}`,
          {
            title: cleanTitle,
            description:
              description.trim(),
            severity,
            status,
          }
        );

      let updatedIncident =
        data;

      const assignmentChanged =
        cleanAssignedTo !==
        originalAssignedTo;

      if (assignmentChanged) {
        if (!cleanAssignedTo) {
          setError(
            "Clearing an assignment is not supported by this form. Enter a valid user ID."
          );
          setLoading(false);
          return;
        }

        const assignResponse =
          await api.patch(
            `/api/incidents/${incident.id}/assign`,
            {
              assignedTo:
                Number(
                  cleanAssignedTo
                ),
            }
          );

        updatedIncident =
          assignResponse.data;
      }

      onUpdated(
        updatedIncident
      );

      onClose();
    } catch (error) {
      console.error(
        "Update incident failed:",
        error
      );

      setError(
        getErrorMessage(
          error,
          "Unable to update incident."
        )
      );
    } finally {
      setLoading(false);
    }
  };

  const generateAiAnalysis =
    async () => {
      if (!incident?.id) {
        return;
      }

      try {
        setAiLoading(true);
        setError("");
        setAiAnalysis("");

        const response =
  await api.post(
    `/api/incidents/${incident.id}/ai-analysis`,
    null,
    {
      timeout: 70000
    }
  );

        const result =
          response?.data?.analysis ??
          response?.data?.message ??
          response?.data;

        if (
          result === null ||
          result === undefined
        ) {
          throw new Error(
            "The AI service returned an empty response."
          );
        }

        setAiAnalysis(
          typeof result ===
            "string"
            ? result
            : JSON.stringify(
                result,
                null,
                2
              )
        );
      } catch (error) {
        console.error(
          "AI analysis failed:",
          error
        );

        setError(
          getErrorMessage(
            error,
            "Unable to generate AI analysis."
          )
        );
      } finally {
        setAiLoading(false);
      }
    };

  return (
    <div className="modal-overlay">
      <div
        className="modal incident-details-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="incident-details-title"
      >
        <div className="modal-header">
          <div>
            <p className="eyebrow">
              INCIDENT #{incident.id}
            </p>

            <h2 id="incident-details-title">
              Incident Details
            </h2>
          </div>

          <button
            type="button"
            className="secondary"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <form
          onSubmit={saveChanges}
        >
          <label htmlFor="details-title">
            Title
          </label>

          <input
            id="details-title"
            value={title}
            onChange={(event) =>
              setTitle(
                event.target.value
              )
            }
            required
          />

          <label htmlFor="details-description">
            Description
          </label>

          <textarea
            id="details-description"
            value={description}
            onChange={(event) =>
              setDescription(
                event.target.value
              )
            }
          />

          <div className="details-grid">
            <div>
              <label htmlFor="details-severity">
                Severity
              </label>

              <select
                id="details-severity"
                value={severity}
                onChange={(event) =>
                  setSeverity(
                    event.target.value
                  )
                }
              >
                <option value="LOW">
                  LOW
                </option>

                <option value="MEDIUM">
                  MEDIUM
                </option>

                <option value="HIGH">
                  HIGH
                </option>

                <option value="CRITICAL">
                  CRITICAL
                </option>
              </select>
            </div>

            <div>
              <label htmlFor="details-status">
                Status
              </label>

              <select
                id="details-status"
                value={status}
                onChange={(event) =>
                  setStatus(
                    event.target.value
                  )
                }
              >
                <option value="OPEN">
                  OPEN
                </option>

                <option value="IN_PROGRESS">
                  IN_PROGRESS
                </option>

                <option value="RESOLVED">
                  RESOLVED
                </option>

                <option value="CLOSED">
                  CLOSED
                </option>
              </select>
            </div>
          </div>

          <label htmlFor="assigned-user">
            Assign To User ID
          </label>

          <input
            id="assigned-user"
            type="number"
            min="1"
            step="1"
            placeholder="Enter user ID"
            value={assignedTo}
            onChange={(event) =>
              setAssignedTo(
                event.target.value
              )
            }
          />

          {error && (
            <div className="error">
              {error}
            </div>
          )}

          <div className="incident-information">
            <div>
              <span>
                Incident ID
              </span>

              <strong>
                #{incident.id}
              </strong>
            </div>

            <div>
              <span>
                Created By
              </span>

              <strong>
                #{incident.createdBy ??
                  "N/A"}
              </strong>
            </div>

            <div>
              <span>
                Created At
              </span>

              <strong>
                {incident.createdAt
                  ? new Date(
                      incident.createdAt
                    ).toLocaleString()
                  : "N/A"}
              </strong>
            </div>

            <div>
              <span>
                Current Assignee
              </span>

              <strong>
                {incident.assignedTo
                  ? `#${incident.assignedTo}`
                  : "Unassigned"}
              </strong>
            </div>
          </div>

          <div className="ai-analysis-section">
            <div className="ai-analysis-header">
              <div>
                <p className="eyebrow">
                  AI INTELLIGENCE
                </p>

                <h3>
                  Incident Analysis
                </h3>

                <p className="ai-analysis-subtitle">
                  Generate an operational
                  assessment using AI.
                </p>
              </div>

              <button
                type="button"
                className="ai-button"
                disabled={
                  aiLoading
                }
                onClick={
                  generateAiAnalysis
                }
              >
                {aiLoading
                  ? "Analyzing..."
                  : "Generate AI Analysis"}
              </button>
            </div>

            {aiLoading && (
              <div className="ai-loading">
                <span className="ai-spinner" />

                <span>
                  AI is analyzing this
                  incident...
                </span>
              </div>
            )}

            {aiAnalysis && (
              <div className="ai-result">
                <div className="ai-result-title">
                  <span>
                    AI OPERATIONAL ASSESSMENT
                  </span>
                </div>

                <pre>
                  {aiAnalysis}
                </pre>
              </div>
            )}
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="secondary"
              onClick={onClose}
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Saving..."
                : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* =========================================================
   DASHBOARD
   ========================================================= */

function Dashboard({
  user,
  logout,
}) {
  const [incidents, setIncidents] =
    useState([]);

  const [system, setSystem] =
    useState(null);

  const [auditLogs, setAuditLogs] =
    useState([]);

  const [showAuditLogs, setShowAuditLogs] =
    useState(false);

  const [auditLoading, setAuditLoading] =
    useState(false);

  const [showCreate, setShowCreate] =
    useState(false);

  const [
    selectedIncident,
    setSelectedIncident,
  ] = useState(null);

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [error, setError] =
    useState("");

  const [success, setSuccess] =
    useState("");

  const loadIncidents = async (
    showLoader = true
  ) => {
    try {
      if (showLoader) {
        setLoading(true);
      } else {
        setRefreshing(true);
      }

      setError("");

      const { data } =
        await api.get(
          "/api/incidents"
        );

      setIncidents(
        Array.isArray(data)
          ? data
          : []
      );
    } catch (error) {
      console.error(
        "Load incidents failed:",
        error
      );

      setError(
        getErrorMessage(
          error,
          "Unable to load incidents."
        )
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadIncidents(true);
  }, []);

  const loadAuditLogs =
    async () => {
      try {
        setAuditLoading(true);
        setError("");

        const { data } =
          await api.get(
            "/api/audit"
          );

        setAuditLogs(
          Array.isArray(data)
            ? data
            : []
        );

        setShowAuditLogs(true);
      } catch (error) {
        console.error(
          "Load audit logs failed:",
          error
        );

        setError(
          getErrorMessage(
            error,
            "Unable to load audit logs."
          )
        );
      } finally {
        setAuditLoading(false);
      }
    };

  const checkSystem =
    async () => {
      try {
        setSystem(null);
        setError("");

        const { data } =
          await api.get(
            "/api/system/info"
          );

        setSystem(data);
      } catch (error) {
        console.error(
          "System health check failed:",
          error
        );

        setSystem({
          status: "OFFLINE",
          error: getErrorMessage(
            error,
            "Unable to reach backend."
          ),
        });
      }
    };

  const showSuccess =
    (message) => {
      setSuccess(message);

      window.setTimeout(() => {
        setSuccess("");
      }, 4000);
    };

  const handleCreated =
    (incident) => {
      setIncidents(
        (current) => [
          incident,
          ...current,
        ]
      );

      showSuccess(
        `Incident #${incident.id} created successfully.`
      );
    };

  const handleUpdated =
    (updatedIncident) => {
      setIncidents(
        (current) =>
          current.map(
            (incident) =>
              incident.id ===
              updatedIncident.id
                ? updatedIncident
                : incident
          )
      );

      showSuccess(
        `Incident #${updatedIncident.id} updated successfully.`
      );
    };

  const deleteIncident =
    async (id) => {
      const confirmed =
        window.confirm(
          "Are you sure you want to delete this incident?"
        );

      if (!confirmed) {
        return;
      }

      try {
        setError("");

        await api.delete(
          `/api/incidents/${id}`
        );

        setIncidents(
          (current) =>
            current.filter(
              (incident) =>
                incident.id !== id
            )
        );

        showSuccess(
          `Incident #${id} deleted successfully.`
        );
      } catch (error) {
        console.error(
          "Delete incident failed:",
          error
        );

        setError(
          getErrorMessage(
            error,
            "Unable to delete incident."
          )
        );
      }
    };

  const changeStatus =
    async (id, status) => {
      try {
        setError("");

        const { data } =
          await api.patch(
            `/api/incidents/${id}/status`,
            { status }
          );

        setIncidents(
          (current) =>
            current.map(
              (incident) =>
                incident.id === id
                  ? data
                  : incident
            )
        );

        showSuccess(
          `Incident #${id} status changed to ${status}.`
        );
      } catch (error) {
        console.error(
          "Change status failed:",
          error
        );

        setError(
          getErrorMessage(
            error,
            "Unable to update incident status."
          )
        );

        await loadIncidents(
          false
        );
      }
    };

  const activeIncidents =
    incidents.filter(
      (incident) =>
        incident.status !==
          "RESOLVED" &&
        incident.status !==
          "CLOSED"
    ).length;

  const criticalIncidents =
    incidents.filter(
      (incident) =>
        incident.severity ===
        "CRITICAL"
    ).length;

  const resolvedIncidents =
    incidents.filter(
      (incident) =>
        incident.status ===
          "RESOLVED" ||
        incident.status ===
          "CLOSED"
    ).length;

  return (
    <div className="app">
      <header>
        <div>
          <strong>
            AI-EOMS
          </strong>

          <span>
            {" "}
            Enterprise Operations
          </span>
        </div>

        <div className="header-right">
          <span>
            {user?.firstName ||
              user?.username ||
              "User"}
          </span>

          <button
            className="secondary"
            onClick={logout}
          >
            Logout
          </button>
        </div>
      </header>

      <main>
        <section className="hero">
          <div>
            <p className="eyebrow">
              CONTROL CENTER
            </p>

            <h1>
              Enterprise Operations
              Dashboard
            </h1>

            <p>
              Monitor incidents,
              infrastructure health and
              operational intelligence
              from one place.
            </p>
          </div>

          <div className="status-card">
            <div className="status-dot" />

            <div>
              <strong>
                Backend
              </strong>

              <span>
                Operational
              </span>
            </div>
          </div>
        </section>

        <section className="stats">
          <div className="stat">
            <span>
              Active Incidents
            </span>

            <strong>
              {activeIncidents}
            </strong>
          </div>

          <div className="stat">
            <span>
              Critical
            </span>

            <strong>
              {criticalIncidents}
            </strong>
          </div>

          <div className="stat">
            <span>
              Resolved
            </span>

            <strong>
              {resolvedIncidents}
            </strong>
          </div>

          <div className="stat">
            <span>
              System Health
            </span>

            <strong>
              {system?.status ===
              "OFFLINE"
                ? "DOWN"
                : "UP"}
            </strong>
          </div>
        </section>

        {error && (
          <div className="error global-message">
            {error}
          </div>
        )}

        {success && (
          <div className="success global-message">
            {success}
          </div>
        )}

        <section className="panel-grid">
          <div className="panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">
                  INCIDENT MANAGEMENT
                </p>

                <h2>
                  Incidents
                </h2>
              </div>

              <div className="panel-actions">
                <button
                  className="secondary"
                  onClick={() =>
                    loadIncidents(
                      false
                    )
                  }
                  disabled={
                    refreshing
                  }
                >
                  {refreshing
                    ? "Refreshing..."
                    : "Refresh"}
                </button>

                <button
                  onClick={() =>
                    setShowCreate(
                      true
                    )
                  }
                >
                  Create Incident
                </button>
              </div>
            </div>

            {loading ? (
              <div className="empty">
                <p>
                  Loading incidents...
                </p>
              </div>
            ) : incidents.length ===
              0 ? (
              <div className="empty">
                <h3>
                  No incidents yet
                </h3>

                <p>
                  Create your first
                  incident to begin
                  monitoring operations.
                </p>
              </div>
            ) : (
              <div className="incident-list">
                {incidents.map(
                  (incident) => (
                    <div
                      className="incident-card"
                      key={
                        incident.id
                      }
                    >
                      <div className="incident-card-header">
                        <div>
                          <h3>
                            {
                              incident.title
                            }
                          </h3>

                          <p>
                            {incident.description ||
                              "No description provided."}
                          </p>
                        </div>

                        <span
                          className={`severity severity-${(
                            incident.severity ||
                            "MEDIUM"
                          ).toLowerCase()}`}
                        >
                          {
                            incident.severity
                          }
                        </span>
                      </div>

                      <div className="incident-meta">
                        <span>
                          Status:
                          <strong>
                            {" "}
                            {
                              incident.status
                            }
                          </strong>
                        </span>

                        <span>
                          ID:
                          <strong>
                            {" "}
                            #
                            {
                              incident.id
                            }
                          </strong>
                        </span>

                        {incident.assignedTo && (
                          <span>
                            Assigned to:
                            <strong>
                              {" "}
                              #
                              {
                                incident.assignedTo
                              }
                            </strong>
                          </span>
                        )}
                      </div>

                      <div className="incident-actions">
                        <button
                          className="secondary"
                          onClick={() =>
                            setSelectedIncident(
                              incident
                            )
                          }
                        >
                          View / Edit
                        </button>

                        <select
                          value={
                            incident.status ||
                            "OPEN"
                          }
                          onChange={(
                            event
                          ) =>
                            changeStatus(
                              incident.id,
                              event.target
                                .value
                            )
                          }
                        >
                          <option value="OPEN">
                            OPEN
                          </option>

                          <option value="IN_PROGRESS">
                            IN_PROGRESS
                          </option>

                          <option value="RESOLVED">
                            RESOLVED
                          </option>

                          <option value="CLOSED">
                            CLOSED
                          </option>
                        </select>

                        <button
                          className="danger"
                          onClick={() =>
                            deleteIncident(
                              incident.id
                            )
                          }
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  )
                )}
              </div>
            )}
          </div>

          <div className="panel audit-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">
                  SECURITY &
                  COMPLIANCE
                </p>

                <h2>
                  Audit Logs
                </h2>
              </div>

              <button
                className="secondary"
                onClick={
                  loadAuditLogs
                }
                disabled={
                  auditLoading
                }
              >
                {auditLoading
                  ? "Loading..."
                  : "View Logs"}
              </button>
            </div>

            {!showAuditLogs ? (
              <div className="empty">
                <p>
                  View security and
                  operational activity.
                </p>
              </div>
            ) : auditLogs.length ===
              0 ? (
              <div className="empty">
                <p>
                  No audit events found.
                </p>
              </div>
            ) : (
              <div className="audit-list">
                {auditLogs.map(
                  (log) => (
                    <div
                      className="audit-item"
                      key={log.id}
                    >
                      <div className="audit-item-top">
                        <strong>
                          {
                            log.action
                          }
                        </strong>

                        <span>
                          {log.createdAt
                            ? new Date(
                                log.createdAt
                              ).toLocaleString()
                            : "N/A"}
                        </span>
                      </div>

                      <p>
                        {log.description ||
                          "No description provided."}
                      </p>

                      <div className="audit-meta">
                        <span>
                          User #
                          {log.userId ??
                            "SYSTEM"}
                        </span>

                        {log.entityType && (
                          <span>
                            {
                              log.entityType
                            }

                            {log.entityId
                              ? ` #${log.entityId}`
                              : ""}
                          </span>
                        )}

                        {log.ipAddress && (
                          <span>
                            IP{" "}
                            {
                              log.ipAddress
                            }
                          </span>
                        )}
                      </div>
                    </div>
                  )
                )}
              </div>
            )}
          </div>

          <div className="panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">
                  SYSTEM
                </p>

                <h2>
                  Health
                </h2>
              </div>

              <button
                className="secondary"
                onClick={
                  checkSystem
                }
              >
                Check
              </button>
            </div>

            {system ? (
              <pre>
                {JSON.stringify(
                  system,
                  null,
                  2
                )}
              </pre>
            ) : (
              <div className="empty">
                <p>
                  Click Check to query
                  the backend.
                </p>
              </div>
            )}
          </div>
        </section>
      </main>

      {showCreate && (
        <CreateIncidentModal
          onClose={() =>
            setShowCreate(false)
          }
          onCreated={
            handleCreated
          }
        />
      )}

      {selectedIncident && (
        <IncidentDetailsModal
          incident={
            selectedIncident
          }
          onClose={() =>
            setSelectedIncident(
              null
            )
          }
          onUpdated={
            handleUpdated
          }
        />
      )}
    </div>
  );
}

/* =========================================================
   APP
   ========================================================= */

function App() {
  const [user, setUser] =
    useState(() => {
      try {
        const token =
          localStorage.getItem(
            "token"
          );

        const savedUser =
          localStorage.getItem(
            "user"
          );

        if (!token || !savedUser) {
          return null;
        }

        return JSON.parse(
          savedUser
        );
      } catch {
        localStorage.removeItem(
          "token"
        );

        localStorage.removeItem(
          "user"
        );

        return null;
      }
    });

  const logout = () => {
    localStorage.removeItem(
      "token"
    );

    localStorage.removeItem(
      "user"
    );

    setUser(null);
  };

  return (
    <BrowserRouter>
      <Routes>
        {!user ? (
          <Route
            path="*"
            element={
              <AuthPage
                onLogin={setUser}
              />
            }
          />
        ) : (
          <>
            <Route
              path="/"
              element={
                <Dashboard
                  user={user}
                  logout={logout}
                />
              }
            />

            <Route
              path="*"
              element={
                <Navigate
                  to="/"
                  replace
                />
              }
            />
          </>
        )}
      </Routes>
    </BrowserRouter>
  );
}

export default App;