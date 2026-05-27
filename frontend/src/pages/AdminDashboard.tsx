import { Link } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Building2,
  MapPin,
  Users,
  UserSquare2,
  UserCog,
  CalendarRange,
  ClipboardList,
} from "lucide-react";

interface Section {
  title: string;
  description: string;
  to: string;
  icon: React.ReactNode;
  status: "ready" | "soon";
}

const SECTIONS: Section[] = [
  {
    title: "Clubes",
    description: "Gere os clubes registados na plataforma.",
    to: "/admin/clubs",
    icon: <Building2 className="size-5 text-primary" />,
    status: "ready",
  },
  {
    title: "Utilizadores",
    description: "Cria e elimina utilizadores; atribui-os a clubes a partir da página do clube.",
    to: "/admin/users",
    icon: <UserCog className="size-5 text-primary" />,
    status: "ready",
  },
  {
    title: "Estádios",
    description: "Locais onde os jogos decorrem (capacidade, morada).",
    to: "/admin/venues",
    icon: <MapPin className="size-5 text-primary" />,
    status: "ready",
  },
  {
    title: "Equipas",
    description: "Equipas dentro de cada clube (sénior, sub-19, feminina…).",
    to: "/admin/teams",
    icon: <Users className="size-5 text-primary" />,
    status: "soon",
  },
  {
    title: "Jogadores",
    description: "Plantéis e dados dos jogadores.",
    to: "/admin/players",
    icon: <UserSquare2 className="size-5 text-primary" />,
    status: "soon",
  },
  {
    title: "Jogos",
    description: "Calendário, resultados e estado dos jogos.",
    to: "/admin/matches",
    icon: <CalendarRange className="size-5 text-primary" />,
    status: "ready",
  },
  {
    title: "Fichas técnicas",
    description: "Convocatórias, golos, cartões e substituições.",
    to: "/admin/match-sheets",
    icon: <ClipboardList className="size-5 text-primary" />,
    status: "soon",
  },
];

export function AdminDashboard() {
  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">Admin</h1>
        <p className="text-sm text-muted-foreground">
          Área de gestão da plataforma. Apenas administradores têm acesso.
        </p>
      </header>

      <section className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {SECTIONS.map((s) => (
          <SectionCard key={s.to} section={s} />
        ))}
      </section>
    </div>
  );
}

function SectionCard({ section }: { section: Section }) {
  const ready = section.status === "ready";
  const inner = (
    <Card
      className={
        ready
          ? "transition-colors hover:border-primary/40 hover:bg-muted/30 cursor-pointer"
          : "opacity-60"
      }
    >
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {section.icon}
            <CardTitle className="text-base">{section.title}</CardTitle>
          </div>
          {!ready && <Badge variant="secondary">Em breve</Badge>}
        </div>
        <CardDescription>{section.description}</CardDescription>
      </CardHeader>
      <CardContent className="text-xs text-muted-foreground">
        {ready ? "Abrir →" : "Disponível em próxima iteração."}
      </CardContent>
    </Card>
  );

  return ready ? <Link to={section.to}>{inner}</Link> : inner;
}
