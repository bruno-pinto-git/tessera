/**
 * Seed catalog used by the design implementation before the events
 * endpoint is wired. Mirrors the shape we expect the real
 * `useEvents()` query to return (a flattened event + match join).
 *
 * Replace the export with a real hook once `GET /api/v1/events` (and
 * the matching match-service lookups) are in place. Keep the type
 * surface identical so call-sites don't need to change.
 */

export type MatchStatus = "SCHEDULED" | "LIVE" | "FINISHED" | "CANCELLED";

export interface MatchListItem {
  id: number;
  homeClubId: string;
  awayClubId: string;
  day: string; // "Sáb"
  date: string; // "16 Mai"
  time: string; // "16:00"
  /** ISO datetime for the actual kickoff (used by detail page). */
  kickoffAt: string;
  venue: string;
  competition: string;
  /** Cheapest price tier, EUR. */
  priceFrom: number;
  status: MatchStatus;
  hot?: boolean;
  remaining?: string;
}

export const MOCK_MATCHES: MatchListItem[] = [
  {
    id: 1,
    homeClubId: "ALJ",
    awayClubId: "PRA",
    day: "Sáb",
    date: "16 Mai",
    time: "16:00",
    kickoffAt: "2026-05-16T15:00:00Z",
    venue: "Estádio Municipal do Olival",
    competition: "Campeonato de Portugal · Série D · J28",
    priceFrom: 8,
    status: "SCHEDULED",
    hot: true,
    remaining: "Restam 42 lugares",
  },
  {
    id: 2,
    homeClubId: "LOU",
    awayClubId: "TAB",
    day: "Dom",
    date: "17 Mai",
    time: "11:00",
    kickoffAt: "2026-05-17T10:00:00Z",
    venue: "Campo do Lagar",
    competition: "Campeonato de Portugal · Série D · J28",
    priceFrom: 8,
    status: "SCHEDULED",
    remaining: "Bilheteira aberta",
  },
  {
    id: 3,
    homeClubId: "TIR",
    awayClubId: "CAM",
    day: "Sáb",
    date: "23 Mai",
    time: "18:30",
    kickoffAt: "2026-05-23T17:30:00Z",
    venue: "Estádio Cláudio Mendonça",
    competition: "Taça de Portugal · 1.ª eliminatória",
    priceFrom: 10,
    status: "SCHEDULED",
    hot: true,
    remaining: "Últimos 18 bilhetes",
  },
  {
    id: 4,
    homeClubId: "MAR",
    awayClubId: "UNC",
    day: "Dom",
    date: "24 Mai",
    time: "16:00",
    kickoffAt: "2026-05-24T15:00:00Z",
    venue: "Parque Desportivo da Várzea",
    competition: "Campeonato de Portugal · Série D · J29",
    priceFrom: 8,
    status: "SCHEDULED",
    remaining: "Bilheteira aberta",
  },
  {
    id: 5,
    homeClubId: "MOR",
    awayClubId: "ALJ",
    day: "Sáb",
    date: "30 Mai",
    time: "17:00",
    kickoffAt: "2026-05-30T16:00:00Z",
    venue: "Campo de Santa Eulália",
    competition: "Campeonato de Portugal · Série D · J30",
    priceFrom: 8,
    status: "SCHEDULED",
    remaining: "Bilheteira aberta",
  },
  {
    id: 6,
    homeClubId: "PRA",
    awayClubId: "LOU",
    day: "Dom",
    date: "31 Mai",
    time: "15:00",
    kickoffAt: "2026-05-31T14:00:00Z",
    venue: "Estádio Municipal do Olival",
    competition: "Campeonato de Portugal · Série D · J30",
    priceFrom: 10,
    status: "SCHEDULED",
    remaining: "Bilheteira aberta",
  },
];

export interface PriceTier {
  name: string;
  description: string;
  normal: number;
  socio: number;
  left: number;
  scarce?: boolean;
}

export interface MatchDetail extends MatchListItem {
  matchday: string; // "Jornada 28"
  /**
   * ID of the ticket-service `event` row backing this match. The
   * PurchaseModal uses this to POST /api/v1/tickets. Until the catalog
   * is wired to a real `GET /events` endpoint, the value comes from the
   * mock and the admin is expected to seed the matching event in the
   * ticket-service DB.
   */
  eventId: number;
  capacity: number;
  sold: number;
  pending: number;
  validated: number;
  tiers: PriceTier[];
  gatesOpen: string;
  parking: string;
  accessibility: string;
}

export const MOCK_MATCH_DETAIL: Record<number, MatchDetail> = {
  1: {
    ...MOCK_MATCHES[0],
    matchday: "Jornada 28",
    eventId: 1,
    capacity: 2800,
    sold: 1240,
    pending: 86,
    validated: 0,
    tiers: [
      {
        name: "Bancada poente",
        description: "Lugar sentado coberto",
        normal: 10,
        socio: 5,
        left: 312,
      },
      {
        name: "Bancada nascente",
        description: "Lugar sentado, sol",
        normal: 8,
        socio: 4,
        left: 540,
      },
      {
        name: "Topo norte",
        description: "Apoio adepto, em pé",
        normal: 6,
        socio: 3,
        left: 380,
      },
      {
        name: "Camarote",
        description: "12 lugares, restauração incluída",
        normal: 25,
        socio: 18,
        left: 8,
        scarce: true,
      },
    ],
    gatesOpen: "14:30 (1h30 antes)",
    parking: "Gratuito · Rua das Tílias",
    accessibility: "Rampa portão B",
  },
};
