import { useAuth } from "@/auth/useAuth";

export function HomePage() {
  const { authenticated, username } = useAuth();

  return (
    <div>
      <h1>Bem-vindo ao Tessera</h1>
      {authenticated ? (
        <p>Olá, {username}.</p>
      ) : (
        <p>Faz login para gerir bilhetes e consultar jogos.</p>
      )}
    </div>
  );
}
