/**
 * Seed data for "Os meus bilhetes" until `GET /api/v1/tickets/mine` is
 * wired. Three buckets: ready-to-use (PAID), waiting-payment (PENDING)
 * with a countdown, past (VALIDATED/CANCELLED).
 */

export interface PaidTicketView {
  id: string;
  code: string;
  homeClubId: string;
  awayClubId: string;
  day: string;
  venue: string;
  tier: string;
  price: string;
  gate: string;
  paidAt: string;
}

export interface PendingTicketView {
  id: string;
  code: string;
  homeClubId: string;
  awayClubId: string;
  day: string;
  tier: string;
  price: string;
  expiresIn: string;
}

export interface PastTicketView {
  id: string;
  homeClubId: string;
  awayClubId: string;
  day: string;
  tier: string;
  validatedAt?: string;
  cancelled?: boolean;
}

export const MOCK_PAID: PaidTicketView[] = [
  {
    id: "8e2f",
    code: "8e2f4a1b-9c63-4d11-b9a4-2c1f5e87c3d2",
    homeClubId: "ALJ",
    awayClubId: "PRA",
    day: "Sáb · 16 Mai · 16:00",
    venue: "Estádio Municipal do Olival",
    tier: "Bancada poente · Fila 12, lugar 8",
    price: "10,00 €",
    gate: "Portão B",
    paidAt: "12 Mai · 19:42",
  },
  {
    id: "a91c",
    code: "a91c7b8e-2d04-4a55-86f1-091c4f23ab17",
    homeClubId: "TIR",
    awayClubId: "ALJ",
    day: "Sáb · 23 Mai · 18:30",
    venue: "Estádio Cláudio Mendonça",
    tier: "Adepto visitante · em pé",
    price: "10,00 €",
    gate: "Portão D",
    paidAt: "10 Mai · 21:08",
  },
];

export const MOCK_PENDING: PendingTicketView[] = [
  {
    id: "5c4d",
    code: "5c4d3e7a-1f88-49b2-a0d6-7e15c2b4f9a3",
    homeClubId: "MAR",
    awayClubId: "ALJ",
    day: "Dom · 24 Mai · 16:00",
    tier: "Bancada nascente",
    price: "8,00 €",
    expiresIn: "Expira em 13 min",
  },
];

export const MOCK_PAST: PastTicketView[] = [
  {
    id: "1a2b",
    homeClubId: "LOU",
    awayClubId: "ALJ",
    day: "Sáb · 03 Mai · 17:00",
    tier: "Bancada poente",
    validatedAt: "Validado · 16:42",
  },
  {
    id: "9f8e",
    homeClubId: "ALJ",
    awayClubId: "TAB",
    day: "Dom · 27 Abr · 15:00",
    tier: "Bancada nascente",
    validatedAt: "Validado · 14:48",
  },
  {
    id: "3c7d",
    homeClubId: "ALJ",
    awayClubId: "MOR",
    day: "Sáb · 19 Abr · 16:00",
    tier: "Topo norte",
    cancelled: true,
  },
];
