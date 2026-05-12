import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center text-center py-24 gap-3">
      <p className="text-sm font-medium text-muted-foreground">Erro 404</p>
      <h1 className="text-3xl font-bold tracking-tight">Página não encontrada</h1>
      <p className="text-muted-foreground max-w-md">
        O endereço que pediste não existe ou foi movido.
      </p>
      <Button asChild className="mt-2">
        <Link to="/">Voltar ao início</Link>
      </Button>
    </div>
  );
}
