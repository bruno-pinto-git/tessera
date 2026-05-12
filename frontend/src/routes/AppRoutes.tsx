import { Route, Routes } from "react-router-dom";
import { Layout } from "@/components/Layout";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { HomePage } from "@/pages/HomePage";
import { UnauthorizedPage } from "@/pages/UnauthorizedPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { EventsPage } from "@/features/events/pages/EventsPage";
import { EventDetailPage } from "@/features/events/pages/EventDetailPage";
import { MyTicketsPage } from "@/features/tickets/pages/MyTicketsPage";
import { TicketsPage } from "@/features/tickets/pages/TicketsPage";
import { ValidatePage } from "@/features/validation/pages/ValidatePage";

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />

        {/* Public catalog */}
        <Route path="events" element={<EventsPage />} />
        <Route path="events/:id" element={<EventDetailPage />} />

        {/* Authenticated users */}
        <Route
          path="tickets/mine"
          element={
            <ProtectedRoute>
              <MyTicketsPage />
            </ProtectedRoute>
          }
        />

        {/* Dev sandbox (kept while the real purchase flow is under construction). */}
        <Route
          path="tickets/sandbox"
          element={
            <ProtectedRoute>
              <TicketsPage />
            </ProtectedRoute>
          }
        />

        {/* Staff / admin */}
        <Route
          path="validate"
          element={
            <ProtectedRoute roles={["staff", "admin"]}>
              <ValidatePage />
            </ProtectedRoute>
          }
        />

        <Route
          path="admin"
          element={
            <ProtectedRoute roles={["admin"]}>
              <div className="space-y-2">
                <h1 className="text-2xl font-bold tracking-tight">Admin</h1>
                <p className="text-muted-foreground">Área reservada a administradores.</p>
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
