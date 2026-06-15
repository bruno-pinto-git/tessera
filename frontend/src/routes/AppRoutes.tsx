import { Route, Routes } from "react-router-dom";
import { Layout } from "@/components/Layout";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { HomePage } from "@/pages/HomePage";
import { UnauthorizedPage } from "@/pages/UnauthorizedPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { AdminDashboard } from "@/pages/AdminDashboard";
import { ClubManagerHome } from "@/pages/ClubManagerHome";
import { ClubDetailPage } from "@/pages/ClubDetailPage";
import { TeamDetailPage } from "@/pages/TeamDetailPage";
import { EventsPage } from "@/features/events/pages/EventsPage";
import { EventDetailPage } from "@/features/events/pages/EventDetailPage";
import { MyTicketsPage } from "@/features/tickets/pages/MyTicketsPage";
import { TicketsPage } from "@/features/tickets/pages/TicketsPage";
import { ClubsAdminPage } from "@/features/clubs/pages/ClubsAdminPage";
import { UsersAdminPage } from "@/features/users/pages/UsersAdminPage";
import { VenuesAdminPage } from "@/features/venues/pages/VenuesAdminPage";
import { MatchesAdminPage } from "@/features/matches/pages/MatchesAdminPage";
import { PlayersAdminPage } from "@/features/players/pages/PlayersAdminPage";
import { TeamsAdminPage } from "@/features/teams/pages/TeamsAdminPage";
import { MatchSheetEditorPage } from "@/features/sheets/pages/MatchSheetEditorPage";

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
          path="club"
          element={
            <ProtectedRoute roles={["club-manager", "staff"]}>
              <ClubManagerHome />
            </ProtectedRoute>
          }
        />
        <Route
          path="club/:id"
          element={
            <ProtectedRoute roles={["club-manager", "staff", "platform-admin"]}>
              <ClubDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="team/:id"
          element={
            <ProtectedRoute roles={["club-manager", "platform-admin", "staff"]}>
              <TeamDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="matches/:matchId/sheet"
          element={
            <ProtectedRoute roles={["club-manager", "staff", "platform-admin"]}>
              <MatchSheetEditorPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="admin"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/clubs"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <ClubsAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/clubs/:id"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <ClubDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <UsersAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/teams"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <TeamsAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/players"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <PlayersAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/venues"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <VenuesAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/matches"
          element={
            <ProtectedRoute roles={["platform-admin"]}>
              <MatchesAdminPage />
            </ProtectedRoute>
          }
        />

        <Route path="unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
