function required(name: string, value: string | undefined): string {
  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }
  return value;
}

export const env = {
  keycloak: {
    url: required("VITE_KEYCLOAK_URL", import.meta.env.VITE_KEYCLOAK_URL),
    realm: required("VITE_KEYCLOAK_REALM", import.meta.env.VITE_KEYCLOAK_REALM),
    clientId: required("VITE_KEYCLOAK_CLIENT_ID", import.meta.env.VITE_KEYCLOAK_CLIENT_ID),
  },
};
