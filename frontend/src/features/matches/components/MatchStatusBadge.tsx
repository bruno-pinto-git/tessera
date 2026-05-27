import { Badge } from "@/components/ui/badge";
import { matchStatusLabel, type MatchStatus } from "../api/matchesApi";

const VARIANT_BY_STATUS: Record<MatchStatus, "default" | "secondary" | "destructive" | "outline"> =
  {
    SCHEDULED: "outline",
    LIVE: "default",
    FINISHED: "secondary",
    POSTPONED: "secondary",
    ABANDONED: "destructive",
    CANCELLED: "destructive",
  };

export function MatchStatusBadge({ status }: { status: MatchStatus }) {
  return <Badge variant={VARIANT_BY_STATUS[status]}>{matchStatusLabel(status)}</Badge>;
}