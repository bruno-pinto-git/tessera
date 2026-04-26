import { Route, Routes } from "react-router-dom";
import { Layout } from "@/components/Layout";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { HomePage } from "@/pages/HomePage";
import { UnauthorizedPage } from "@/pages/UnauthorizedPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { TicketsPage } from "@/features/tickets/pages/TicketsPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />

        <Route
          path="tickets"
          element={
            <ProtectedRoute roles={["admin"]}>
              <TicketsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="admin"
          element={
            <ProtectedRoute roles={["admin"]}>
              <div>
                <h1>Admin</h1>
                <p>Área reservada a administradores.</p>
              </div>
            </ProtectedRoute>
          }
        />

        <Route path="unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
