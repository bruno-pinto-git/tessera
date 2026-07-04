# Deploy — Azure VM + Docker Compose

Runs the whole stack on a single Azure VM with HTTPS, using the free Azure
DNS name (no domain to buy) and the student credit. Designed so you can
**deallocate the VM when you're not testing** to make the €100 last.

Topology in production (one VM, one hostname, one TLS cert):

```
Internet ──443──> Caddy ──/auth/*──> Keycloak (prod, Postgres, /auth)
                       └──/*───────> nginx (SPA + /api ──> bff ──> ticket/match/statistics)
                                                                  ├─ RabbitMQ
                                                                  └─ Postgres ×3 (+ Keycloak's own)
```

## 0. Prerequisites
- Azure for Students subscription with credit.
- [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) locally (`az login`), or use the Portal/Cloud Shell.

## 1. Create the VM (Azure CLI)
Pick a unique DNS label — the public FQDN becomes `<label>.<region>.cloudapp.azure.com`.

```bash
RG=tessera-rg
LOC=swedencentral                  # see the "region gotcha" note below
VM=tessera-vm
DNS=tessera                        # must be globally unique in the region; add a suffix if taken

az group create -n $RG -l $LOC

az vm create \
  -g $RG -n $VM -l $LOC \
  --image Ubuntu2204 \
  --size Standard_B2as_v2 \
  --admin-username azureuser \
  --generate-ssh-keys \
  --public-ip-address-dns-name $DNS

# Open HTTP/HTTPS (SSH is opened by default)
az vm open-port -g $RG -n $VM --port 80,443 --priority 900
```

Your FQDN: `${DNS}.${LOC}.cloudapp.azure.com` (note it — it's the `TESSERA_FQDN`).

> **Region gotcha (Azure for Students).** The subscription is locked by policy to
> a small set of regions, and even inside those the cheap burstable/B-series sizes
> are often capacity-restricted. `westeurope`/`uksouth` are blocked; `swedencentral`
> had `Standard_B2as_v2` (2 vCPU / 8 GB, AMD, burstable) available. If a region or
> size is refused (`RequestDisallowedByAzure` / `SkuNotAvailable`), find what's
> allowed:
> ```bash
> # allowed regions for this subscription
> az policy assignment list --disable-scope-strict-match \
>   --query "[?parameters.listOfAllowedLocations].parameters.listOfAllowedLocations.value" -o json
> # unrestricted 8 GB / 2 vCPU sizes in a region (x86 — avoid the ARM 'p' variants)
> az vm list-skus -l <region> --all --query "[?resourceType=='virtualMachines' && restrictions[0]==null && (starts_with(name,'Standard_B2') || starts_with(name,'Standard_D2'))].{name:name, mem:capabilities[?name=='MemoryGB'].value|[0], vCPU:capabilities[?name=='vCPUs'].value|[0]}" -o table
> ```

## 2. Install Docker on the VM
```bash
ssh azureuser@<FQDN>
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && exit   # re-login so the group applies
ssh azureuser@<FQDN>
```

## 3. Get the code + configure
```bash
git clone -b main https://github.com/bruno-pinto-git/tessera.git
cd tessera
cp .env.prod.example .env
nano .env          # set TESSERA_FQDN=<your FQDN>, strong KC_DB_PASSWORD / KC_ADMIN_PASSWORD
                   # STRIPE_SECRET_KEY is optional — leave empty unless you want card payments
```

## 4. Build & launch
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```
First run is slow (~5–10 min: builds 4 Spring services + the SPA, pulls images).
Caddy then fetches a Let's Encrypt cert automatically for your FQDN.

Watch it come up:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f caddy keycloak
```

## 5. Verify
- App: `https://<FQDN>/`
- Keycloak admin: `https://<FQDN>/auth/admin` (user `admin`, your `KC_ADMIN_PASSWORD`)
- Login on the SPA (e.g. `admin`/`admin` from the imported realm) and click through.

(Optional) seed demo data via the API exactly like locally, hitting `https://<FQDN>/api/v1/...`.

## 6. Cost control — turn it off when idle
The VM only costs while **running**. When you're done testing:
```bash
az vm deallocate -g $RG -n $VM     # stops billing compute; you keep the disk (~small cost)
az vm start      -g $RG -n $VM     # bring it back (same FQDN); containers auto-restart
```
`restart: unless-stopped` + the persistent volumes mean the stack comes back on its own after `az vm start`.

## 7. One-click deploy / power from GitHub (optional)
`.github/workflows/deploy-azure.yml` gives a manual button (Actions tab →
**deploy-azure** → *Run workflow*) with three operations:
- **deploy** — powers the VM on, pulls the chosen branch (default `main`), `up -d --build`, leaves it running.
- **start** / **stop** — just power the VM on / deallocate it (same as `az vm start`/`deallocate`, but from GitHub).

App secrets stay in the VM's `.env` — they never go to GitHub. The workflow only
needs to power the VM and SSH in. One-time setup:

1. **Service principal** (lets the action start/stop the VM), scoped to just this RG:
   ```bash
   az ad sp create-for-rbac --name tessera-deploy \
     --role "Virtual Machine Contributor" \
     --scopes /subscriptions/<SUB_ID>/resourceGroups/$RG \
     --json-auth
   ```
   Copy the whole JSON output.
2. **SSH key** the action will use (or reuse the one you log in with). The private
   key goes in a secret; the public key must be in the VM's `~/.ssh/authorized_keys`.
3. **Add repo secrets** (Settings → Secrets and variables → Actions → *New repository secret*):
   | Secret | Value |
   |---|---|
   | `AZURE_CREDENTIALS` | the SP JSON from step 1 |
   | `AZURE_RG` | your resource group name |
   | `AZURE_VM` | your VM name |
   | `VM_HOST` | your FQDN (or public IP) |
   | `VM_USER` | `azureuser` |
   | `VM_SSH_KEY` | the private SSH key from step 2 |

**Release flow:** `main` is the deploy source. When `develop` is demo-ready, merge it
into `main` and push; then run **deploy**. (You can also type `develop` in the *ref*
box to test-deploy without promoting.)

> The VM must be provisioned once (sections 1–4) before the button works. This is a
> dispatch-only button on purpose — a `push`-triggered deploy would fail while the VM
> is off, or force it to stay on and burn credit.

## 8. Android app (your colleague)
Point the Android client at the public URLs:
- API base: `https://<FQDN>/api/v1`
- Keycloak: `https://<FQDN>/auth` (realm `tessera`, the appropriate client)

## Troubleshooting
- **Login redirect rejected** — the `tessera-web` client allows `/*` + webOrigins `+`, which should cover the FQDN. If a redirect is still refused, add `https://<FQDN>/*` to *Valid redirect URIs* and `https://<FQDN>` to *Web origins* in the KC admin console (Clients → tessera-web).
- **No TLS cert / cert errors** — Let's Encrypt needs the FQDN to resolve to the VM and ports 80+443 open. Check `az vm open-port` ran and `docker compose logs caddy`.
- **Out of memory / containers killed** — the `B2ms` (8 GB) fits the capped stack; if tight, bump to `Standard_B4ms` or consolidate the three app Postgres into one.
- **Update after a push** — `git pull` then re-run the `up -d --build` command, or use the **deploy-azure** GitHub button (section 7).
