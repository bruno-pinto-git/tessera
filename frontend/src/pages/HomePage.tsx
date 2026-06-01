import { Link } from "react-router-dom";
import { useAuth } from "@/auth/useAuth";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Calendar, Ticket } from "lucide-react";

export function HomePage() {
  const { authenticated, username, login } = useAuth();

  return (
    <div className="space-y-12">
      <section className="space-y-4 text-center md:text-left">
        <h1 className="text-4xl font-bold tracking-tight">Bem-vindo ao Tessera</h1>
        <p className="text-lg text-muted-foreground max-w-2xl">
          Plataforma de gestão de bilheteira e ficha técnica para clubes de futebol de divisões
          inferiores.
        </p>
        {authenticated ? (
          <p className="text-sm text-muted-foreground">
            Estás autenticado como <span className="font-medium text-foreground">{username}</span>.
          </p>
        ) : (
          <div className="flex gap-3 justify-center md:justify-start pt-2">
            <Button onClick={login}>Entrar</Button>
            <Button variant="outline" asChild>
              <Link to="/events">Ver jogos</Link>
            </Button>
          </div>
        )}
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        <FeatureCard
          icon={<Calendar className="size-5 text-primary" />}
          title="Calendário de jogos"
          description="Consulta os próximos jogos, estádios, e resultados em tempo real."
          to="/events"
          ctaLabel="Explorar jogos"
        />
        <FeatureCard
          icon={<Ticket className="size-5 text-primary" />}
          title="Bilhetes digitais"
          description="Compra bilhetes online e apresenta o QR code à entrada do estádio."
          to="/tickets/mine"
          ctaLabel="Os meus bilhetes"
        />
      </section>
    </div>
  );
}

function FeatureCard({
  icon,
  title,
  description,
  to,
  ctaLabel,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  to: string;
  ctaLabel: string;
}) {
  return (
    <Card className="flex flex-col">
      <CardHeader>
        <div className="flex items-center gap-2">
          {icon}
          <CardTitle className="text-base">{title}</CardTitle>
        </div>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="mt-auto">
        <Button variant="ghost" size="sm" asChild>
          <Link to={to}>{ctaLabel} →</Link>
        </Button>
      </CardContent>
    </Card>
  );
}
