import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function UnauthorizedPage() {
  return (
    <div className="flex flex-col items-center justify-center text-center py-24 gap-3">
      <p className="text-sm font-medium text-destructive">Acesso negado</p>
      <h1 className="text-3xl font-bold tracking-tight">Permissões insuficientes</h1>
      <p className="text-muted-foreground max-w-md">
        Não tens permissões para aceder a esta área. Se achas que devias ter acesso, contacta um
        administrador.
      </p>
      <Button asChild variant="outline" className="mt-2">
        <Link to="/">Voltar ao início</Link>
      </Button>
    </div>
  );
}
