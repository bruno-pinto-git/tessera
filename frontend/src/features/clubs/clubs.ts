export type CrestTone = "forest" | "oxblood" | "navy" | "ochre" | "slate" | "cream";

export interface Club {
  id: string;
  name: string;
  short: string;
  initials: string;
  tone: CrestTone;
}

export const CLUBS: Record<string, Club> = {
  ALJ: { id: "ALJ", name: "CD Aljustrel", short: "Aljustrel", initials: "AL", tone: "forest" },
  PRA: { id: "PRA", name: "SC Praiense", short: "Praiense", initials: "PR", tone: "navy" },
  LOU: { id: "LOU", name: "GD Lousada", short: "Lousada", initials: "LO", tone: "oxblood" },
  TAB: { id: "TAB", name: "AD Tabuadense", short: "Tabuadense", initials: "TB", tone: "ochre" },
  TIR: { id: "TIR", name: "FC Tirsense", short: "Tirsense", initials: "TI", tone: "slate" },
  CAM: { id: "CAM", name: "AD Camacha", short: "Camacha", initials: "CM", tone: "cream" },
  MAR: { id: "MAR", name: "SC Marinhense", short: "Marinhense", initials: "MA", tone: "forest" },
  UNC: { id: "UNC", name: "AC União Coimbra", short: "Coimbra", initials: "UC", tone: "oxblood" },
  MOR: { id: "MOR", name: "CD Mortágua", short: "Mortágua", initials: "MO", tone: "navy" },
};

export function clubBy(id: string): Club {
  const c = CLUBS[id];
  if (!c) throw new Error(`Unknown club id: ${id}`);
  return c;
}
