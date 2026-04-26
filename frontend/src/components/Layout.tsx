import { Link, Outlet } from "react-router-dom";
import { useAuth } from "@/auth/useAuth";

export function Layout() {
  const { authenticated, username, login, logout, hasRole } = useAuth();

  return (
    <div style={{ fontFamily: "system-ui, sans-serif" }}>
      <header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "12px 24px",
          borderBottom: "1px solid #e5e5e5",
        }}
      >
        <nav style={{ display: "flex", gap: 16 }}>
          <Link to="/">Tessera</Link>
          {hasRole("admin") && <Link to="/tickets">Bilhetes</Link>}
          {hasRole("admin") && <Link to="/admin">Admin</Link>}
        </nav>
        <div>
          {authenticated ? (
            <>
              <span style={{ marginRight: 12 }}>{username}</span>
              <button onClick={logout}>Sair</button>
            </>
          ) : (
            <button onClick={login}>Entrar</button>
          )}
        </div>
      </header>
      <main style={{ padding: 24 }}>
        <Outlet />
      </main>
    </div>
  );
}
