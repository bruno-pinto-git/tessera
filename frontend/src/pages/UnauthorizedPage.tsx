import { Link } from "react-router-dom";

export function UnauthorizedPage() {
  return (
    <div>
      <h1>Acesso negado</h1>
      <p>Não tens permissões para aceder a esta página.</p>
      <Link to="/">Voltar ao início</Link>
    </div>
  );
}
