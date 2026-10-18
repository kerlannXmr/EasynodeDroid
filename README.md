# EasynodeDroid

Version Android d'EasyMonerod : un nœud Monero complet (`monerod`) qui tourne directement sur le téléphone, avec démarrage/arrêt en un bouton, suivi de la synchronisation et blocage des nœuds espions.



## Correspondance avec le script Linux

| Script `easynode_linux.sh` | Application Android |
|---|---|
| 1 · Mise à jour système, ufw, fail2ban, SSH | Sans objet : Android gère les mises à jour et l'app est isolée dans son bac à sable |
| 2 · Installer Monero CLI + vérification GPG | Fait **au build** (CI) : monerod vérifié puis embarqué dans l'APK |
| 3 · Tor (services onion SSH + RPC) | Option **Tor** : Tor intégré à l'app (pas besoin d'Orbot) → adresse **.onion:18089** (RPC restreint) affichée dans Réglages et Wallet + transactions diffusées via Tor (`--tx-proxy`). Pas de SSH (impossible sans root) |
| 4 · bitmonero.conf + mot de passe RPC | Onglet **Réglages** (RPC local ou réseau, authentification). Blockchain toujours élaguée |
| 5 · DNS chiffrés | Réglage Android natif : *Paramètres › Réseau › DNS privé* → `dns.adguard-dns.com` |
| 6 / 7 · Disque externe / démontage | Choix **Mémoire interne / Carte SD** + bouton de suppression des données |
| 8 · Démarrage + liste d'IP bannies | Bouton **Démarrer** : liste Boog900 téléchargée et chargée via `--ban-list` |
| 9 · Arrêt (CTRL+C) | Bouton **Arrêter** ou action de la notification (arrêt propre SIGTERM) |
| 10 · Langue | Suit la langue du téléphone (français / anglais) |
| 11-12 · Infos / Notes | Onglet **Wallet** (adresse à saisir dans le wallet, identifiants, port 18080) |

## Bon à savoir

- **ARM64 uniquement** (quasiment tous les téléphones depuis 2017) : c'est la seule version Android de monerod publiée par le projet Monero.
- **Espace** : ~95 Go (blockchain toujours élaguée ; une base complète copiée est élaguée par monerod au démarrage). Une carte SD A2/U3 fonctionne mais reste plus lente.
- **Première synchro** : plusieurs heures à plusieurs jours. Laisser le téléphone branché, en Wi-Fi, et désactiver l'optimisation de batterie (bouton dans Réglages). Par défaut l'app refuse de démarrer sur données mobiles.
- **Wallet sur le téléphone** (Cake, Monerujo, Feather…) : nœud `127.0.0.1:18081`. **Wallet sur un PC du même Wi-Fi** : activer « Ouvrir le RPC au réseau local », l'adresse `IP:18081` s'affiche dans l'onglet Wallet.
- Le service tourne en premier plan avec une notification permanente : c'est obligatoire sous Android pour un processus de longue durée.

## Structure

```
app/src/main/java/org/kerlann/easynode/
  NodeService.kt    service qui lance/arrête monerod, suit la synchro (RPC get_info)
  NodeSettings.kt   réglages (équivalent bitmonero.conf)
  BanList.kt        téléchargement + nettoyage de la liste d'IP bannies
  RpcClient.kt      JSON-RPC avec authentification Digest
  Storage.kt        mémoire interne / carte SD, espace libre
  ui/Screens.kt     interface (Nœud, Réglages, Journal, Wallet)
scripts/fetch_monerod.sh   téléchargement + vérification GPG/SHA256 de monerod
.github/workflows/build.yml  build automatique de l'APK
```
