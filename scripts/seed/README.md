To run the demo preparation for azure:

.\scripts\seed\seed-demo.ps1 `
  -BffUrl https://tessera.swedencentral.cloudapp.azure.com `
-KeycloakUrl https://tessera.swedencentral.cloudapp.azure.com/auth `
-AdminPassword '<password-admin-prod>'

To run on local:
# 1. ir para a pasta do projeto
cd "C:\Users\bruno.pinto\Documents\ISEL\2025-2026\SV\Projecto e Seminario\tessera"

# 2. correr o seed
.\scripts\seed\seed-demo.ps1